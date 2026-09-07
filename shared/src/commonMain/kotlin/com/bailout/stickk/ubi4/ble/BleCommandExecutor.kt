package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4

interface BleCommandExecutor {
    fun getQueueUBI4(): BlockingQueueUbi4
    fun getRemainingTasksCount(): Int
    fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit)
    fun sendWidgetsArray()
    fun updateSerialNumber(deviceInfo: DeviceInfoStructs)

    /** Common legacy/v2 main-to-boot handoff; independent of bulk DFU. */
    suspend fun firmwareJumpToBootloader(packet: ByteArray) {
        error("Firmware program-switch transport is unavailable")
    }

    /** Dedicated DFU v2 path. Defaults keep older hosts on legacy DFU. */
    suspend fun dfuMaximumWriteWithoutResponseSize(): Int = 20
    suspend fun dfuSupportsWriteWithoutResponse(): Boolean = false
    suspend fun dfuSetHighPerformanceMode() = Unit
    suspend fun dfuWriteControl(packet: ByteArray) {
        error("DFU bulk control transport is unavailable")
    }
    suspend fun dfuWriteControlExpectDisconnect(packet: ByteArray) =
        dfuWriteControl(packet)
    suspend fun dfuWriteWithoutResponse(packet: ByteArray): Boolean = false
    suspend fun dfuAwaitWritable() = Unit
    suspend fun dfuReconnect() {
        error("DFU reconnect transport is unavailable")
    }
    suspend fun dfuAwaitReconnect() = dfuReconnect()
//    fun updateFirmwareInfo(info: FirmwareInfoStruct)
}
