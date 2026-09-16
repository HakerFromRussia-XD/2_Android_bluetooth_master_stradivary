package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.utility.currentTimeMillis
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** GUI reboots behind FAM: boot entry, erase and finalization keep the BLE link. */
internal class GuiFirmwareUpdater(
    private val transport: FirmwareBulkTransport,
    private val logger: FirmwareUpdateLogger = NoOpFirmwareUpdateLogger,
    private val replies: SharedFlow<Pair<Int, ByteArray>> = FirmwareInfoState.addressedFirmwareResponseFlow,
    private val v2Replies: SharedFlow<ByteArray> = FirmwareInfoState.dfuV2ResponseFlow
) {
    suspend fun update(
        firmware: FirmwareUpdatePackage,
        onProgress: (Int, Int) -> Unit
    ): FirmwareUpdateResult? {
        val started = currentTimeMillis()
        val crc = firmware.descriptorFirmwareCrc.takeIf { it != 0L }
            ?: MotoricaCrc32.calculate(firmware.payload)
        require(firmware.payload.isNotEmpty() && MotoricaCrc32.calculate(firmware.payload) == crc) {
            "GUI image CRC mismatch before boot entry"
        }
        // Only the actual GUI v1 status selects legacy, before CAPS/BEGIN.
        if (ensureBootloader() == 2) return null
        val descriptor = firmware.descriptor.copyOf()
        FirmwareInfoDescriptorBuilder.patchFirmwareSizeInPlace(descriptor, firmware.payload.size)
        val checked = exchange(BLECommandsV3.requestCheckNewFw(ADDRESS, descriptor))
        val status = CheckNewFwStatus.from(checked[1].toInt() and 0xFF)
        if (status != CheckNewFwStatus.NEW_FW_ACCEPT)
            return FirmwareUpdateResult.CheckNewFirmwareRejected(status)

        val uploader = FastDfuUploaderV2(transport, v2Replies, logger)
        val caps = checkNotNull(uploader.negotiate(ADDRESS)) {
            "GUI bootloader v2 did not negotiate CAPS through FAM"
        }
        logger.info(TAG, "GUI v2 selected addr=9 caps=$caps")
        // Never silently turn a failed GUI v2 experiment into a successful v1 upload.
        uploader.upload(ADDRESS, firmware.payload, crc, caps, onProgress)
        val calculated = exchange(BLECommandsV3.requestCalculateCrcFw(ADDRESS))
        if ((calculated[1].toInt() and 0xFF) != 1) return FirmwareUpdateResult.CrcMismatch
        delay(150)
        val completed = exchange(BLECommandsV3.requestCompleteUpdateFw(ADDRESS))
        if ((completed[1].toInt() and 0xFF) != 0x2E) return FirmwareUpdateResult.CrcMismatch
        logger.info(TAG, "GUI GOOD_CRC_FIRMWARE; waiting for fresh MAIN_APP")
        repeat(12) {
            delay(250)
            if (readProgramType() == 1) {
                uploader.completeCurrent()
                logger.info(TAG, "GUI v2 SUCCESS addr=9 CRC=GOOD MAIN_APP total_ms=${currentTimeMillis() - started}")
                return FirmwareUpdateResult.Success
            }
        }
        error("GUI CRC passed but main did not start")
    }

    private suspend fun ensureBootloader(): Int {
        val initial = readProgramType()
        logger.info(TAG, "GUI initial_program_type=$initial addr=9")
        if (initial == 2 || initial == 3) return initial
        check(initial == 1) { "Unexpected GUI program type: $initial" }
        transport.writeControl(BLECommandsV3.jumpToBootloaderFw(ADDRESS).withDfuAddress(ADDRESS))
        repeat(12) {
            delay(250)
            val type = readProgramType()
            if (type == 2 || type == 3) {
                logger.info(TAG, "GUI boot ready program_type=$type; BLE bridge preserved")
                return type
            }
        }
        error("GUI did not enter bootloader")
    }

    private suspend fun readProgramType(): Int =
        exchange(BLECommandsV3.requestRunProgramTypeFw(ADDRESS))[1].toInt() and 0xFF

    private suspend fun exchange(packet: ByteArray): ByteArray = coroutineScope {
        val request = packet.withDfuAddress(ADDRESS)
        val opcode = request[if (request[0].toInt() and 0x80 != 0) 5 else 2]
        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(3000) {
                replies.filter { (address, bytes) ->
                    // FAM relay replaces the outer address with zero. Updates must be serialized.
                    address in setOf(0, ADDRESS) && bytes.size >= 2 && bytes[0] == opcode
                }.first().second
            }
        }
        transport.writeControl(request)
        waiter.await()
    }

    companion object {
        private const val ADDRESS = 9
        private const val TAG = "GUI_DFU_V2"
    }
}
