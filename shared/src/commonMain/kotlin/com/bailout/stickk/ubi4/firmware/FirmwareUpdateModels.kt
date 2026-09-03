package com.bailout.stickk.ubi4.firmware

import kotlinx.coroutines.delay

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4

data class FirmwareUpdatePackage(
    val name: String,
    val descriptor: ByteArray,
    val payload: ByteArray,
    val descriptorFirmwareSize: Long,
    val descriptorFirmwareCrc: Long,
    val localVersionString: String?
)

data class FirmwareInfoDescriptor(
    val bytes: ByteArray,
    val firmwareSize: Long,
    val firmwareCrc: Long,
    val localVersionString: String?
)

enum class FirmwareUpdateProtocol {
    UBI4,
    V3
}

sealed class FirmwareUpdateResult {
    data object Success : FirmwareUpdateResult()

    data class StartSystemUpdateRejected(
        val status: PreferenceKeysUbi4.StartSystemUpdateStatus
    ) : FirmwareUpdateResult()

    data class CheckNewFirmwareRejected(
        val status: PreferenceKeysUbi4.CheckNewFwStatus
    ) : FirmwareUpdateResult()

    data object PreloadFailed : FirmwareUpdateResult()
    data object CrcMismatch : FirmwareUpdateResult()
}

enum class FirmwareTransportChannel {
    UBI4_MAIN,
    V3_SERIAL
}

fun interface FirmwareCommandSender {
    suspend fun send(packet: ByteArray, channel: FirmwareTransportChannel)

    /**
     * Sends the legacy JUMP_TO_BOOTLOADER command. Platforms that need an
     * explicit BLE handoff may override this without coupling legacy DFU to
     * the v2 bulk transport.
     */
    suspend fun sendBootloaderJump(packet: ByteArray, channel: FirmwareTransportChannel) {
        send(packet, channel)
        delay(1_500L)
    }
}

interface FirmwareUpdateLogger {
    fun debug(tag: String, message: String) = Unit
    fun info(tag: String, message: String) = Unit
    fun warn(tag: String, message: String) = Unit
    fun error(tag: String, message: String, throwable: Throwable? = null) = Unit
}

object NoOpFirmwareUpdateLogger : FirmwareUpdateLogger
