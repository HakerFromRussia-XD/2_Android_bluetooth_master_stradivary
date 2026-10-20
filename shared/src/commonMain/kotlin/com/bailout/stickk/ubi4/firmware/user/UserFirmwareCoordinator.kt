package com.bailout.stickk.ubi4.firmware.user

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.bailout.stickk.ubi4.utility.logging.platformLog

interface UserFirmwareBackend {
    suspend fun boards(): List<UserFirmwareBoard>
    suspend fun targets(boards: List<UserFirmwareBoard>): List<UserFirmwareTarget>
    suspend fun validate(target: UserFirmwareTarget)
    suspend fun probe(address: Int): UserFirmwareBoard
    suspend fun transfer(target: UserFirmwareTarget, progress: (Int) -> Unit)
    suspend fun readJournal(): String?
    suspend fun writeJournal(text: String)
    val changeRevision: Long
    /** Ignore events preceding this attempt; reconnect may arrive before its error is reported. */
    suspend fun awaitChange(afterRevision: Long)
    fun isSameDevice(): Boolean
}

class UserFirmwareCoordinator(private val deviceId: String, private val backend: UserFirmwareBackend) {
    private val mutableState = MutableStateFlow(UserFirmwareUiState())
    val state = mutableState.asStateFlow()
    private var journal: UserFirmwareJournal? = null
    private var running = false

    suspend fun check() {
        if (running || journal != null) return
        running = true
        try {
            val loaded = backend.readJournal()?.takeIf { it.isNotBlank() && it != "null" }
                ?.let { Json.decodeFromString<UserFirmwareJournal>(it) }
            // Before the first write, rebuild from live boards rather than resume an old offer.
            val saved = loaded?.takeIf {
                it.formatVersion >= 2 && (it.attempted.isNotEmpty() || it.completed.isNotEmpty())
            }
            if (loaded != null && saved == null) {
                backend.writeJournal("null")
                platformLog("USER_DFU", "discarded obsolete or unstarted user-update journal")
            }
            if (saved != null) {
                require(saved.deviceId == deviceId) { "Session belongs to another device" }
                journal = saved
                publish("preparing")
            } else {
                mutableState.value = UserFirmwareUiState("checking")
                val boards = withTimeout(15_000) { backend.boards() }
                val targets = withTimeout(60_000) { backend.targets(boards) }
                val queue = UserFirmwarePolicy.queue(boards, targets)
                platformLog("USER_DFU", "targets=${targets.joinToString { "${it.module.address}:${it.version}" }} queue=${queue.joinToString { "${it.module.address}:${it.version}" }}")
                check(backend.isSameDevice()) { "Device changed during firmware check" }
                if (queue.isEmpty()) {
                    mutableState.value = UserFirmwareUiState()
                } else {
                    journal = UserFirmwareJournal(deviceId, queue, formatVersion = 2)
                    publish("offered")
                }
            }
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            platformLog("USER_DFU", "check failed ${error::class.simpleName}: ${error.message}")
            mutableState.value = UserFirmwareUiState("unavailable", detail = error.message.orEmpty())
        } finally { running = false }
    }

    suspend fun start() {
        if (running || journal == null) return
        running = true
        try {
            while (true) {
                val attemptRevision = backend.changeRevision
                try {
                    runPendingTargets()
                    publish("complete", 100)
                    return
                } catch (error: Exception) {
                    currentCoroutineContext().ensureActive()
                    platformLog("USER_DFU", "update stopped ${error::class.simpleName}: ${error.message}")
                    if (journal!!.attempted.isEmpty() && journal!!.completed.isEmpty()) {
                        backend.writeJournal("null")
                        journal = null
                        mutableState.value = UserFirmwareUiState("unavailable", detail = error.message.orEmpty())
                        return
                    }
                    publish("waiting", detail = error.message.orEmpty())
                    platformLog("USER_DFU", "recovery waiting after_revision=$attemptRevision")
                    backend.awaitChange(attemptRevision)
                    platformLog("USER_DFU", "recovery resumed revision=${backend.changeRevision}")
                }
            }
        } finally { running = false }
    }

    private suspend fun runPendingTargets() {
        persist()
        check(backend.isSameDevice()) { "Waiting for the original device" }
        publish("preparing")
        withTimeout(60_000) {
            journal!!.targets.filterNot { it.module.address in journal!!.completed }
                .forEach { backend.validate(it) }
        }
        for (target in journal!!.targets) {
            val address = target.module.address
            if (address in journal!!.completed) continue
            check(backend.isSameDevice()) { "Waiting for the original device" }
            publish("verifying")
            val board = withTimeout(15_000) { backend.probe(address) }
            platformLog(
                "USER_DFU",
                "probe address=$address target=${target.version} installed=${board.version} " +
                    "main=${board.isMain} confirmed=${board.isConfirmed} attempted=${address in journal!!.attempted}"
            )
            val recoveringBoot = address in journal!!.attempted && !board.isMain
            check(board.address == address && (board.isConfirmed || recoveringBoot)) { "Unconfirmed board at $address" }
            if (!UserFirmwarePolicy.completed(board, target)) {
                val mayStart = board.isConfirmed && board.version != null && board.version < target.version
                check(mayStart || recoveringBoot) {
                    "Board state does not allow updating address=$address"
                }
                journal = journal!!.copy(attempted = journal!!.attempted + address)
                persist()
                publish("updating")
                platformLog("USER_DFU", "transfer begin address=$address target=${target.version} restart_from=0")
                transferWithProgressTimeout(target)
            }
            journal = journal!!.copy(completed = journal!!.completed + address)
            persist()
        }
    }

    private suspend fun transferWithProgressTimeout(target: UserFirmwareTarget) = coroutineScope {
        val progress = Channel<Unit>(Channel.CONFLATED)
        val transfer = async {
            backend.transfer(target) {
                publish("updating", it)
                progress.trySend(Unit)
            }
        }
        try {
            // Bound silence during boot entry, writes and final CRC, not the entire upload.
            while (true) {
                val finished = withTimeout(60_000) {
                    select<Boolean> {
                        transfer.onAwait { true }
                        progress.onReceive { false }
                    }
                }
                if (finished) break
            }
        } finally {
            transfer.cancel()
            progress.close()
        }
    }

    fun needsResume(): Boolean = journal != null && state.value.phase == "preparing"
    suspend fun acknowledge() {
        if (state.value.phase != "complete") return
        backend.writeJournal("null")
        journal = null
        mutableState.value = UserFirmwareUiState()
    }

    /**
     * The offer has not been accepted and is not persisted yet. Keep its
     * in-memory queue so the same app session does not immediately show the
     * offer again, but do not start or persist an update operation.
     */
    fun postpone() {
        if (state.value.phase != "offered") return
        mutableState.value = UserFirmwareUiState()
    }
    private suspend fun persist() = backend.writeJournal(Json.encodeToString(journal!!))
    private fun publish(phase: String, progress: Int = 0, detail: String = "") {
        val saved = journal ?: return
        mutableState.value = UserFirmwareUiState(phase,
            (saved.completed.size + 1).coerceAtMost(saved.targets.size), saved.targets.size, progress, detail)
    }
}
