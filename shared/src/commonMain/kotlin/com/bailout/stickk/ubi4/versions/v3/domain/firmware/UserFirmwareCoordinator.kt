package com.bailout.stickk.ubi4.versions.v3.domain.firmware

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserFirmwareBackend<Version : Comparable<Version>, Target : V3UserFirmwareTarget<Version>> {
    suspend fun boards(): List<V3UserFirmwareBoard<Version>>
    suspend fun targets(boards: List<V3UserFirmwareBoard<Version>>): List<Target>
    suspend fun validate(target: Target)
    suspend fun probe(address: Int): V3UserFirmwareBoard<Version>
    suspend fun transfer(target: Target, progress: (Int) -> Unit)
    suspend fun readJournal(): V3UserFirmwareSession<Target>?
    suspend fun writeJournal(session: V3UserFirmwareSession<Target>?)
    /** Wait for a real connection/network/foreground event, never a retry timer. */
    suspend fun awaitChange()
    fun isSameDevice(): Boolean
}

class UserFirmwareCoordinator<Version : Comparable<Version>, Target : V3UserFirmwareTarget<Version>>(
    private val deviceId: String,
    private val backend: UserFirmwareBackend<Version, Target>,
    private val log: (tag: String, message: String) -> Unit,
) {
    private val mutableState = MutableStateFlow(V3UserFirmwareStatus())
    val state = mutableState.asStateFlow()
    private var session: V3UserFirmwareSession<Target>? = null
    private var running = false

    suspend fun check() {
        if (running || session != null) return
        running = true
        try {
            val loaded = backend.readJournal()
            val saved = loaded?.takeIf { V3UserFirmwarePolicy.canResumeJournal(it.formatVersion) }
            if (loaded != null && saved == null) {
                // Older journals did not persist a successful GOOD_CRC step,
                // so they can only deadlock a resumed user update.
                backend.writeJournal(null)
                log("USER_DFU", "discarded obsolete user-update journal")
            }
            if (saved != null) {
                require(saved.deviceId == deviceId) { "Session belongs to another device" }
                session = saved
                publish("preparing")
            } else {
                mutableState.value = V3UserFirmwareStatus("checking")
                val boards = backend.boards()
                val targets = backend.targets(boards)
                val queue = V3UserFirmwarePolicy.queue(boards.associate { it.address to it.version }, targets,
                    { it.address }, { it.version })
                log("USER_DFU", "targets=${targets.joinToString { "${it.address}:${it.version}" }} queue=${queue.joinToString { "${it.address}:${it.version}" }}")
                check(backend.isSameDevice()) { "Device changed during firmware check" }
                if (queue.isEmpty()) {
                    mutableState.value = V3UserFirmwareStatus()
                } else {
                    session = V3UserFirmwareSession(deviceId, queue, formatVersion = 2)
                    publish("offered")
                }
            }
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            log("USER_DFU", "check failed ${error::class.simpleName}: ${error.message}")
            mutableState.value = V3UserFirmwareStatus("unavailable", detail = error.message.orEmpty())
        } finally { running = false }
    }

    suspend fun start() {
        if (running || session == null) return
        running = true
        try {
            // Persist consent and the frozen set before any destructive command.
            while (true) {
                try {
                    persist()
                    check(backend.isSameDevice()) { "Waiting for the original device" }
                    publish("preparing")
                    session!!.targets.filterNot { it.address in session!!.completed }.forEach { backend.validate(it) }
                    break
                } catch (error: Exception) {
                    currentCoroutineContext().ensureActive()
                    if (!backend.isSameDevice()) {
                        publish("verifying", detail = error.message.orEmpty())
                        backend.awaitChange()
                    }
                    // The device is still connected: immediately repeat the
                    // validation request. Progress is driven only by its
                    // callback; there is no timer/backoff between attempts.
                }
            }
            for (target in session!!.targets) {
                val address = target.address
                if (address in session!!.completed) continue
                while (true) {
                    try {
                        check(backend.isSameDevice()) { "Waiting for the original device" }
                        publish("verifying")
                        val board = backend.probe(address)
                        log(
                            "USER_DFU",
                            "probe address=$address target=${target.version} installed=${board.version} " +
                                "main=${board.isMain} attempted=${address in session!!.attempted}"
                        )
                        if (V3UserFirmwarePolicy.completed(board.isMain, board.version, target.version)) {
                            session = session!!.copy(completed = session!!.completed + address)
                            persist()
                            break
                        }
                        val firstAttempt = address !in session!!.attempted
                        val mayStart = V3UserFirmwarePolicy.canStartFirstAttempt(firstAttempt, board.version, target.version)
                        if (!mayStart && !V3UserFirmwarePolicy.retry(board.isMain, board.version, target.version)) {
                            log(
                                "USER_DFU",
                                "waiting address=$address firstAttempt=$firstAttempt installed=${board.version} " +
                                    "target=${target.version} main=${board.isMain}"
                            )
                            publish("verifying", detail = "Waiting for confirmed board state and firmware version")
                            backend.awaitChange()
                            continue
                        }
                        session = session!!.copy(attempted = session!!.attempted + address)
                        persist()
                        publish("updating")
                        var transferred = false
                        try {
                            backend.transfer(target) { publish("updating", it) }
                            transferred = true
                        } catch (error: Exception) {
                            currentCoroutineContext().ensureActive()
                            log(
                                "USER_DFU",
                                "transfer failed address=$address target=${target.version} " +
                                    "${error::class.simpleName}: ${error.message}"
                            )
                            // A failed transfer immediately returns to fresh state/version reads.
                            // There is no delay, retry count limit or partial-byte resume here.
                        }
                        if (transferred) {
                            // GOOD_CRC is the bootloader confirmation that this
                            // board's main program was received successfully.
                            // Continue the frozen queue from this callback; the
                            // next board must not wait for a later advertisement.
                            session = session!!.copy(completed = session!!.completed + address)
                            persist()
                            break
                        }
                    } catch (error: Exception) {
                        currentCoroutineContext().ensureActive()
                        log(
                            "USER_DFU",
                            "state probe failed address=$address target=${target.version} " +
                                "${error::class.simpleName}: ${error.message}"
                        )
                        if (!backend.isSameDevice()) {
                            publish("verifying", detail = error.message.orEmpty())
                            backend.awaitChange()
                        }
                        // A transfer/probe failure on an active BLE session is
                        // followed immediately by a fresh mode/version probe.
                        // Do not wait for a timer or a second user action.
                    }
                }
            }
            publish("complete", 100)
        } finally { running = false }
    }

    fun needsResume(): Boolean = session != null && state.value.phase == "preparing"
    suspend fun acknowledge() {
        if (state.value.phase != "complete") return
        backend.writeJournal(null)
        session = null
        mutableState.value = V3UserFirmwareStatus()
    }

    /**
     * The offer has not been accepted and is not persisted yet. Keep its
     * in-memory queue so the same app session does not immediately show the
     * offer again, but do not start or persist an update operation.
     */
    fun postpone() {
        if (state.value.phase != "offered") return
        mutableState.value = V3UserFirmwareStatus()
    }
    private suspend fun persist() = backend.writeJournal(session!!)
    private fun publish(phase: String, progress: Int = 0, detail: String = "") {
        val saved = session ?: return
        mutableState.value = V3UserFirmwareStatus(phase,
            (saved.completed.size + 1).coerceAtMost(saved.targets.size), saved.targets.size, progress,
            blocksInteraction = V3UserFirmwarePolicy.blocksInteraction(phase), detail = detail)
    }
}
