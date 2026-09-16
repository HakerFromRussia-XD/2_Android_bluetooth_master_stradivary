package com.bailout.stickk.ubi4.firmware.user

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    /** Wait for a real connection/network/foreground event, never a retry timer. */
    suspend fun awaitChange()
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
            val saved = backend.readJournal()?.takeIf { it.isNotBlank() && it != "null" }
                ?.let { Json.decodeFromString<UserFirmwareJournal>(it) }
            if (saved != null) {
                require(saved.deviceId == deviceId) { "Session belongs to another device" }
                journal = saved
                publish("preparing")
            } else {
                mutableState.value = UserFirmwareUiState("checking")
                val boards = backend.boards()
                val targets = backend.targets(boards)
                val queue = UserFirmwarePolicy.queue(boards, targets)
                platformLog("USER_DFU", "targets=${targets.joinToString { "${it.module.address}:${it.version}" }} queue=${queue.joinToString { "${it.module.address}:${it.version}" }}")
                check(backend.isSameDevice()) { "Device changed during firmware check" }
                if (queue.isEmpty()) {
                    mutableState.value = UserFirmwareUiState()
                } else {
                    journal = UserFirmwareJournal(deviceId, queue)
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
            // Persist consent and the frozen set before any destructive command.
            while (true) {
                try {
                    persist()
                    check(backend.isSameDevice()) { "Waiting for the original device" }
                    publish("preparing")
                    journal!!.targets.filterNot { it.module.address in journal!!.completed }.forEach { backend.validate(it) }
                    break
                } catch (error: Exception) {
                    currentCoroutineContext().ensureActive()
                    publish("waiting", detail = error.message.orEmpty())
                    backend.awaitChange()
                }
            }
            for (target in journal!!.targets) {
                val address = target.module.address
                if (address in journal!!.completed) continue
                while (true) {
                    try {
                        check(backend.isSameDevice()) { "Waiting for the original device" }
                        publish("verifying")
                        val board = backend.probe(address)
                        if (UserFirmwarePolicy.completed(board, target)) {
                            journal = journal!!.copy(completed = journal!!.completed + address)
                            persist()
                            break
                        }
                        val firstAttempt = address !in journal!!.attempted
                        val mayStart = firstAttempt && board.version != null && board.version < target.version
                        if (!mayStart && !UserFirmwarePolicy.retry(board, target)) {
                            publish("waiting", detail = "Waiting for confirmed board state and firmware version")
                            backend.awaitChange()
                            continue
                        }
                        journal = journal!!.copy(attempted = journal!!.attempted + address)
                        persist()
                        publish("updating")
                        try {
                            backend.transfer(target) { publish("updating", it) }
                        } catch (_: Exception) {
                            currentCoroutineContext().ensureActive()
                            // A failed transfer immediately returns to fresh state/version reads.
                            // There is no delay, retry count limit or partial-byte resume here.
                        }
                    } catch (error: Exception) {
                        currentCoroutineContext().ensureActive()
                        publish("waiting", detail = error.message.orEmpty())
                        if (error !is kotlinx.coroutines.TimeoutCancellationException || !backend.isSameDevice()) backend.awaitChange()
                    }
                }
            }
            publish("complete", 100)
        } finally { running = false }
    }

    fun needsResume(): Boolean = journal != null && state.value.phase == "preparing"
    suspend fun acknowledge() {
        if (state.value.phase != "complete") return
        backend.writeJournal("null")
        journal = null
        mutableState.value = UserFirmwareUiState()
    }
    private suspend fun persist() = backend.writeJournal(Json.encodeToString(journal!!))
    private fun publish(phase: String, progress: Int = 0, detail: String = "") {
        val saved = journal ?: return
        mutableState.value = UserFirmwareUiState(phase,
            (saved.completed.size + 1).coerceAtMost(saved.targets.size), saved.targets.size, progress, detail)
    }
}
