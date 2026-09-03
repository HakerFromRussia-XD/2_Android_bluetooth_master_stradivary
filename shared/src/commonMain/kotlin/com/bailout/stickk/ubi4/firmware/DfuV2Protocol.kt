package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.WRITE_FW_COMMAND

enum class DfuTransferMode {
    LEGACY_STOP_AND_WAIT,
    FAST_WINDOWED_V2
}

/** Hidden diagnostics switch. Production defaults to negotiated v2. */
object DfuDiagnostics {
    var forceLegacy: Boolean = false
    var requireMainStart: Boolean = false
}

enum class DfuV2Command(val code: Int) {
    CAPS(0x20),
    BEGIN(0x21),
    DATA(0x22),
    STATUS(0x23),
    ABORT(0x24)
}

enum class DfuV2Status(val code: Int) {
    OK(0x00),
    READY(0x01),
    ERASE_RECONNECT(0x02),
    ERROR(0x80),
    BAD_STATE(0x81),
    BAD_SESSION(0x82),
    BAD_OFFSET(0x83),
    QUEUE_FULL(0x84),
    FLASH_ERROR(0x85),
    CRC_ERROR(0x86),
    UNSUPPORTED(0xFF);

    companion object {
        fun from(code: Int): DfuV2Status? = entries.firstOrNull { it.code == code }
    }
}

object DfuV2Flags {
    const val WRITE_WITHOUT_RESPONSE = 1 shl 0
    const val CUMULATIVE_ACK = 1 shl 1
    const val PREFIX_CRC32 = 1 shl 2
    const val RESUME_AFTER_DISCONNECT = 1 shl 3
    const val ERASE_RECONNECT = 1 shl 4
    const val REQUIRED = WRITE_WITHOUT_RESPONSE or CUMULATIVE_ACK or
        PREFIX_CRC32 or RESUME_AFTER_DISCONNECT or ERASE_RECONNECT
}

data class DfuCapabilitiesV2(
    val status: DfuV2Status,
    val major: Int,
    val minor: Int,
    val flags: Int,
    val maxFrame: Int,
    val maxWindow: Int,
    val ackEvery: Int,
    val programUnit: Int
) {
    val supportsRequiredFeatures: Boolean
        get() = status == DfuV2Status.OK &&
            major == 2 &&
            flags and DfuV2Flags.REQUIRED == DfuV2Flags.REQUIRED &&
            maxFrame in (DfuV2Protocol.DATA_FRAME_OVERHEAD + 1)..DfuV2Protocol.MAX_GATT_FRAME &&
            maxWindow > 0 && ackEvery in 1..maxWindow && programUnit > 0
}

data class DfuSessionV2(
    val status: DfuV2Status,
    val sessionId: Long,
    val dataLength: Int,
    val window: Int,
    val ackEvery: Int,
    val committedOffset: Long,
    val prefixCrc32: Long,
    val ackTimeoutMs: Int
)

data class DfuAckV2(
    val status: DfuV2Status,
    val sessionId: Long,
    val committedOffset: Long,
    val acceptedOffset: Long,
    val sendLimitOffset: Long,
    val prefixCrc32: Long
)

data class DfuAbortAckV2(
    val status: DfuV2Status,
    val sessionId: Long
)

interface FirmwareBulkTransport {
    suspend fun maximumWriteWithoutResponseSize(): Int
    suspend fun supportsWriteWithoutResponse(): Boolean
    suspend fun setHighPerformanceMode()
    suspend fun writeControl(packet: ByteArray)
    /**
     * Starts a control write whose successful handling immediately resets the
     * peripheral.  A write callback is therefore not a valid completion gate;
     * the following reconnect is the acknowledgement.
     */
    suspend fun writeControlExpectDisconnect(packet: ByteArray) = writeControl(packet)
    suspend fun writeWithoutResponse(packet: ByteArray): Boolean
    suspend fun awaitWritable()
    suspend fun reconnect()
    /** Wait for the peripheral's automatic post-reset reconnect without forcing another disconnect. */
    suspend fun awaitReconnect() = reconnect()
}

object DfuV2Protocol {
    const val MAX_GATT_FRAME = 250
    const val UBI_LONG_OVERHEAD = 6
    const val DATA_HEADER_SIZE = 10
    const val DATA_FRAME_OVERHEAD = UBI_LONG_OVERHEAD + DATA_HEADER_SIZE
    const val DEFAULT_WINDOW = 16
    const val DEFAULT_ACK_EVERY = 8

    fun caps(address: Int): ByteArray = BLECommandsV3.sendCommand(
        WRITE_FW_COMMAND.number.toInt(),
        DfuV2Command.CAPS.code,
        address
    )

    fun begin(
        address: Int,
        flags: Int,
        imageSize: Long,
        imageCrc32: Long,
        clientMaxFrame: Int,
        requestedWindow: Int = DEFAULT_WINDOW,
        requestedAckEvery: Int = DEFAULT_ACK_EVERY
    ): ByteArray = longCommand(
        DfuV2Command.BEGIN,
        byteArrayOf(address.toByte()) +
            flags.toUInt16Le() +
            imageSize.toUInt32Le() +
            imageCrc32.toUInt32Le() +
            clientMaxFrame.toUInt16Le() +
            byteArrayOf(requestedWindow.toByte(), requestedAckEvery.toByte())
    )

    fun data(
        address: Int,
        sessionId: Long,
        offset: Long,
        bytes: ByteArray
    ): ByteArray = longCommand(
        DfuV2Command.DATA,
        byteArrayOf(address.toByte()) + sessionId.toUInt32Le() + offset.toUInt32Le() + bytes
    )

    fun status(
        address: Int,
        sessionId: Long,
        imageSize: Long,
        imageCrc32: Long
    ): ByteArray = longCommand(
        DfuV2Command.STATUS,
        byteArrayOf(address.toByte()) + sessionId.toUInt32Le() +
            imageSize.toUInt32Le() + imageCrc32.toUInt32Le()
    )

    fun abort(address: Int, sessionId: Long): ByteArray = longCommand(
        DfuV2Command.ABORT,
        byteArrayOf(address.toByte()) + sessionId.toUInt32Le()
    )

    fun parseCapabilities(payload: ByteArray): DfuCapabilitiesV2 {
        requireCommandAndSize(payload, DfuV2Command.CAPS, 12)
        return DfuCapabilitiesV2(
            status = payload.statusAt(1),
            major = payload.u8(2),
            minor = payload.u8(3),
            flags = payload.u16Le(4),
            maxFrame = payload.u16Le(6),
            maxWindow = payload.u8(8),
            ackEvery = payload.u8(9),
            programUnit = payload.u16Le(10)
        )
    }

    fun parseSession(payload: ByteArray): DfuSessionV2 {
        requireCommandAndSize(payload, DfuV2Command.BEGIN, 20)
        return DfuSessionV2(
            status = payload.statusAt(1),
            sessionId = payload.u32Le(2),
            dataLength = payload.u16Le(6),
            window = payload.u8(8),
            ackEvery = payload.u8(9),
            committedOffset = payload.u32Le(10),
            prefixCrc32 = payload.u32Le(14),
            ackTimeoutMs = payload.u16Le(18)
        )
    }

    fun parseAck(payload: ByteArray): DfuAckV2 {
        requireCommandAndSize(payload, DfuV2Command.STATUS, 22)
        return DfuAckV2(
            status = payload.statusAt(1),
            sessionId = payload.u32Le(2),
            committedOffset = payload.u32Le(6),
            acceptedOffset = payload.u32Le(10),
            sendLimitOffset = payload.u32Le(14),
            prefixCrc32 = payload.u32Le(18)
        )
    }

    fun parseAbort(payload: ByteArray): DfuAbortAckV2 {
        require(payload.size == 2 || payload.size == 6) {
            "ABORT response must contain 2 or 6 bytes, got ${payload.size}"
        }
        require(payload.u8(0) == DfuV2Command.ABORT.code) { "Not an ABORT response" }
        return DfuAbortAckV2(
            status = payload.statusAt(1),
            sessionId = if (payload.size == 6) payload.u32Le(2) else 0
        )
    }

    fun maxDataLength(platformFrame: Int, bootloaderFrame: Int): Int =
        minOf(platformFrame, bootloaderFrame, MAX_GATT_FRAME) - DATA_FRAME_OVERHEAD

    private fun longCommand(command: DfuV2Command, data: ByteArray): ByteArray =
        BLECommandsV3.sendLongCommand(
            WRITE_FW_COMMAND.number.toInt(),
            command.code,
            data
        )

    private fun requireCommandAndSize(payload: ByteArray, command: DfuV2Command, size: Int) {
        require(payload.size == size) {
            "${command.name} response must contain $size bytes, got ${payload.size}"
        }
        require(payload.u8(0) == command.code) { "Not a ${command.name} response" }
    }

    private fun ByteArray.statusAt(index: Int): DfuV2Status =
        DfuV2Status.from(u8(index)) ?: error("Unknown DFU v2 status 0x${u8(index).toString(16)}")

    private fun ByteArray.u8(index: Int): Int = this[index].toInt() and 0xFF
    private fun ByteArray.u16Le(index: Int): Int = u8(index) or (u8(index + 1) shl 8)
    private fun ByteArray.u32Le(index: Int): Long =
        u8(index).toLong() or
            (u8(index + 1).toLong() shl 8) or
            (u8(index + 2).toLong() shl 16) or
            (u8(index + 3).toLong() shl 24)

    private fun Int.toUInt16Le(): ByteArray = byteArrayOf(
        (this and 0xFF).toByte(),
        ((this ushr 8) and 0xFF).toByte()
    )

    private fun Long.toUInt32Le(): ByteArray = byteArrayOf(
        (this and 0xFF).toByte(),
        ((this ushr 8) and 0xFF).toByte(),
        ((this ushr 16) and 0xFF).toByte(),
        ((this ushr 24) and 0xFF).toByte()
    )
}
