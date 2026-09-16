package com.bailout.stickk.ubi4.firmware.user

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.firmware.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.RunProgramType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

interface UserFirmwareHost : UserFirmwareArchiveReader {
    fun prepareTransfer(callback: (Boolean) -> Unit)
}

/** A single app-owned instance. UI renders it; all sequencing is shared by Android and iOS. */
class UserFirmwareUpdates(private val directory: String, private val host: UserFirmwareHost) {
    private val scope = MainScope()
    private val changed = Channel<Unit>(Channel.CONFLATED)
    private val mutableState = MutableStateFlow(UserFirmwareUiState())
    private var userRole = false
    private var deviceId = ""
    private var coordinator: UserFirmwareCoordinator? = null
    private var operation: Job? = null
    private var forwarding: Job? = null
    private val repository = UserFirmwareRepository(directory, host)
    private var hadDisconnect = true

    init {
        scope.launch {
            BLEState.state.collect { state ->
                if (state != BLEState.State.READY) { hadDisconnect = true; return@collect }
                changed.trySend(Unit)
                if (hadDisconnect) {
                    hadDisconnect = false
                    checkIfNeeded()
                }
            }
        }
        scope.launch {
            FirmwareInfoState.boardListUpdatedFlow.collect {
                if (coordinator?.state?.value?.phase == "waiting") changed.trySend(Unit)
                if (operation?.isActive != true && coordinator?.state?.value?.blocksInteraction != true) checkIfNeeded()
            }
        }
        scope.launch {
            FirmwareInfoState.runProgramTypeFlow.collect {
                if (coordinator?.state?.value?.phase == "waiting") changed.trySend(Unit)
            }
        }
    }

    fun observe(callback: (UserFirmwareUiState) -> Unit): Job = scope.launch { mutableState.collect(callback) }

    fun setUserRole(enabled: Boolean) {
        val entered = enabled && !userRole
        userRole = enabled
        if (entered) checkIfNeeded()
        if (!enabled && coordinator?.state?.value?.phase in listOf("idle", "checking", "unavailable", "offered")) {
            operation?.cancel()
            coordinator = null
            forwarding?.cancel()
            mutableState.value = UserFirmwareUiState()
        }
    }

    /** Called by network availability and foreground callbacks, never by a retry timer. */
    fun environmentChanged() {
        changed.trySend(Unit)
        if (operation?.isActive != true && coordinator?.state?.value?.blocksInteraction != true) checkIfNeeded()
    }

    fun start() {
        val current = coordinator ?: return
        if (operation?.isActive == true) return
        operation = scope.launch { current.start() }
    }

    fun acknowledge() {
        if (operation?.isActive == true) return
        operation = scope.launch { coordinator?.acknowledge() }
    }

    fun close() { scope.cancel(); UserFirmwareActivity.isActive = false }

    private fun currentId(): String = runCatching { ConnectionState.connectedDeviceAddress }.getOrDefault("")

    private fun checkIfNeeded() {
        if (!userRole || operation?.isActive == true || BLEState.state.value != BLEState.State.READY) return
        val id = currentId().takeIf { it.isNotBlank() } ?: return
        if (coordinator?.state?.value?.blocksInteraction == true) return
        if (coordinator == null || id != deviceId) {
            deviceId = id
            coordinator = UserFirmwareCoordinator(id, Backend(id))
            forwarding?.cancel()
            forwarding = scope.launch { coordinator!!.state.collect {
                UserFirmwareActivity.isActive = it.blocksInteraction && it.phase != "offered" && it.phase != "complete"
                mutableState.value = it
            } }
        }
        val current = coordinator!!
        operation = scope.launch {
            current.check()
            if (current.needsResume()) current.start()
        }
    }

    private inner class Backend(private val expectedId: String) : UserFirmwareBackend {
        private val journalPath = "$directory/session-${firmwareSha256(expectedId.encodeToByteArray())}.json"
        override fun isSameDevice(): Boolean = currentId() == expectedId && BLEState.state.value == BLEState.State.READY
        private suspend fun ready() {
            if (!isSameDevice()) BLEState.state.first { it == BLEState.State.READY && currentId() == expectedId }
            check(currentId() == expectedId)
        }
        override suspend fun boards(): List<UserFirmwareBoard> {
            ready()
            return freshBoards()
        }
        override suspend fun targets(boards: List<UserFirmwareBoard>) = repository.targets(boards)
        override suspend fun validate(target: UserFirmwareTarget) = repository.validate(target)
        override suspend fun probe(address: Int): UserFirmwareBoard {
            ready()
            val type = coroutineScope {
                val response = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(5_000) { FirmwareInfoState.runProgramTypeFlow.first { it.first == address }.second }
                }
                PlatformFirmwareCommandSender.send(BLECommandsV3.requestRunProgramTypeFw(address), FirmwareTransportChannel.V3_SERIAL)
                response.await()
            }
            val board = freshBoards().firstOrNull { it.address == address } ?: error("Board is not responding")
            return board.copy(isMain = type == RunProgramType.MAIN_APP)
        }
        private suspend fun freshBoards(): List<UserFirmwareBoard> = coroutineScope {
            val response = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5_000) {
                    FirmwareInfoState.boardListUpdatedFlow.first()
                    GlobalParameters.baseSubDevicesInfoStructSet.map {
                        UserFirmwareBoard(it.deviceAddress, UserFirmwareVersion.parse(it.fwVersion), it.isBoot == 0)
                    }
                }
            }
            PlatformFirmwareCommandSender.send(BLECommandsV3.requestDeviceData(), FirmwareTransportChannel.V3_SERIAL)
            val result = response.await()
            check(isSameDevice()) { "Device changed during board read" }
            result
        }
        override suspend fun transfer(target: UserFirmwareTarget, progress: (Int) -> Unit) {
            ready()
            val prepared = suspendCancellableCoroutine { continuation ->
                host.prepareTransfer { if (continuation.isActive) continuation.resume(it) }
            }
            check(prepared) { "Firmware transport is not ready" }
            val archive = repository.read(target.path)
            // V3 updaters already inspect the mode and skip the jump when in a bootloader.
            run {
                val updater = FirmwareUpdateCoordinator(
                    Ubi4FirmwareUpdater(PlatformFirmwareCommandSender),
                    V3FirmwareUpdater(PlatformFirmwareCommandSender, PlatformFirmwareBulkTransport),
                    LegacyV3FirmwareUpdater(PlatformFirmwareCommandSender)
                )
                val result = updater.runFirmwareUpdate(FirmwareUpdateProtocol.V3, target.module.address,
                    archive.packageFor(target.module.file)) { offset, total -> progress(if (total > 0) (offset * 100 / total).coerceIn(0, 100) else 0) }
                check(result == FirmwareUpdateResult.Success) { "Firmware transfer failed: $result" }
            }
        }
        override suspend fun readJournal(): String? = sharedFile(journalPath).let {
            if (it.exists()) it.readBytes().decodeToString() else null
        }
        override suspend fun writeJournal(text: String) = writeFirmwareJournal(journalPath, text)
        override suspend fun awaitChange() { changed.receive() }
    }
}
