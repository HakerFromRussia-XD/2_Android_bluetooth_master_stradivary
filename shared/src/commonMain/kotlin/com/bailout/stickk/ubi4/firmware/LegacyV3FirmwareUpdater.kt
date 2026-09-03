package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.firmwareCommandStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.maxChunkSizeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.FirmwareManagerCommand
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Legacy FAM DFU v1 implementation copied from the main application.
 *
 * Keep protocol behavior in this class identical to the main application:
 * diagnostics may observe calls from the coordinator, but must not add BLE
 * commands, reconnect gates, retries, or timing changes here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LegacyV3FirmwareUpdater(
    private val sender: FirmwareCommandSender,
    private val logger: FirmwareUpdateLogger = NoOpFirmwareUpdateLogger
) {
    private var lastMaxChunkInfo: MaxChunkSizeInfo? = null

    // GET_RUN_PROGRAM_TYPE/JUMP_TO_BOOTLOADER: checks the V3 board mode and enters bootloader.
    suspend fun ensureBootloader(addr: Int) {
        logger.info(TRACE_TAG, "legacy boot_entry start addr=$addr")
        logger.debug(TAG, "TX GET_RUN_PROGRAM_TYPE")
        val initial = requestRunType(addr)
            ?: throw IllegalStateException("Не удалось прочитать режим платы")
        logger.debug(TAG, "RX initial status = $initial")
        logger.info(TRACE_TAG, "legacy boot_entry initial_run_type=$initial")

        if (initial != PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
            logger.info(TRACE_TAG, "legacy boot_entry jump_to_bootloader TX")
            logger.debug(TAG, "TX JUMP_TO_BOOTLOADER")
            sender.sendBootloaderJump(
                BLECommandsV3.jumpToBootloaderFw(addr),
                FirmwareTransportChannel.V3_SERIAL
            )
            repeat(BOOTLOADER_CHECK_ATTEMPTS) { attempt ->
                logger.debug(
                    TAG,
                    "TX GET_RUN_PROGRAM_TYPE after reboot attempt=${attempt + 1}/$BOOTLOADER_CHECK_ATTEMPTS"
                )
                val runType = requestRunType(addr)
                logger.debug(TAG, "RX reboot status=$runType")
                logger.info(
                    TRACE_TAG,
                    "legacy boot_entry probe attempt=${attempt + 1}/$BOOTLOADER_CHECK_ATTEMPTS run_type=$runType"
                )
                if (runType == PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
                    logger.debug(TAG, "BOOTLOADER ready")
                    logger.info(TRACE_TAG, "legacy boot_entry complete mode=bootloader")
                    return
                }
                delay(BOOTLOADER_CHECK_INTERVAL_MS)
            }

            throw IllegalStateException("Плата не перешла в bootloader после перезапуска")
        } else {
            logger.debug(TAG, "Board already in bootloader")
            logger.info(TRACE_TAG, "legacy boot_entry complete mode=already_bootloader")
        }
    }

    // GET_UP_LOAD_ATRIBUTE: gets the V3 chunk size and firmware write timings.
    suspend fun getUploadAttribute(addr: Int): MaxChunkSizeInfo {
        logger.debug(TAG, "TX GET_UP_LOAD_ATRIBUTE")
        send(BLECommandsV3.requestUploadAttributeFw(addr))

        val (_, info) = maxChunkSizeFlow
            .filter { it.first == addr || it.first == 0 }
            .first()

        lastMaxChunkInfo = info
        logger.debug(TAG, "RX GET_UP_LOAD_ATRIBUTE $info")
        return info
    }

    // CHECK_NEW_FW: sends the V3 descriptor with the actual firmware payload size.
    suspend fun checkNewFirmware(
        addr: Int,
        firmware: FirmwareUpdatePackage
    ): CheckNewFwStatus {
        logger.debug(TAG, "TX CHECK_NEW_FW")
        val fwSize = firmware.payload.size
        val descriptor = firmware.descriptor.copyOf()
        FirmwareInfoDescriptorBuilder.patchFirmwareSizeInPlace(descriptor, fwSize)
        logger.debug(
            TAG,
            "CHECK_NEW_FW descriptor fwSize=$fwSize iniFwSize=${firmware.descriptorFirmwareSize} " +
                "iniFwCrc=${firmware.descriptorFirmwareCrc}"
        )
        logger.debug(DESC_TAG, descriptor.toHexString())
        send(BLECommandsV3.requestCheckNewFw(addr, descriptor))

        val raw = FirmwareInfoState.checkNewFwFlow.first()
        val status = CheckNewFwStatus.from(raw)
        logger.info(TAG, "CHECK_NEW_FW raw=$raw mapped=$status")
        return status
    }

    // PRELOAD_INFO: sends payload size and prepares flash for writing.
    suspend fun preloadFlash(addr: Int, fwSize: Int): Boolean {
        logger.debug(TAG, "TX PRELOAD_INFO fwSize=$fwSize")
        send(BLECommandsV3.requestPreloadInfoFw(addr, fwSize))

        val status = waitFirmwareStatus(FirmwareManagerCommand.PRELOAD_INFO)
        val ok = status == FW_ACK_OK
        logger.debug(TAG, "RX PRELOAD_INFO status=0x${status.toString(16)} ok=$ok")
        return ok
    }

    // LOAD_NEW_FW: sends firmware chunks over the V3 long-command protocol.
    suspend fun sendFirmwareWithProgress(
        addr: Int,
        firmware: FirmwareUpdatePackage,
        maxInfo: MaxChunkSizeInfo,
        onProgress: (offset: Int, total: Int) -> Unit
    ) {
        val fwBytes = firmware.payload
        val totalSize = fwBytes.size
        val chunkSize = maxInfo.chunkSize
            .takeIf { it > 0 }
            ?.coerceAtMost(MAX_V3_CHUNK_DATA_SIZE)
            ?: MAX_V3_CHUNK_DATA_SIZE
        val timeoutMs = maxInfo.timeoutMs.takeIf { it > 0 } ?: DEFAULT_WRITE_TIMEOUT_MS
        val responseTimeoutMs = maxOf(timeoutMs, MIN_LOAD_NEW_FW_RESPONSE_TIMEOUT_MS)

        var offset = 0
        while (offset < fwBytes.size) {
            val partSize = minOf(chunkSize, fwBytes.size - offset)
            val chunk = fwBytes.copyOfRange(offset, offset + partSize)
            val packet = BLECommandsV3.sendLoadNewFw(addr, offset, chunk)

            val ackOk = sendChunkAndAwait(
                packet = packet,
                expectedWrittenBytes = partSize,
                timeoutMs = responseTimeoutMs
            )
            if (!ackOk) {
                throw IllegalStateException("LOAD_NEW_FW не подтвердился при offset=$offset")
            }

            offset += partSize
            onProgress(offset.coerceAtMost(totalSize), totalSize)

            if (maxInfo.bytesInterval > 0 && offset % maxInfo.bytesInterval == 0) {
                delay(maxInfo.timeoutMs.toLong())
            }
        }

        logger.debug(TAG, "All $offset firmware bytes sent")
    }

    // Returns the actual firmware payload size for PRELOAD_INFO.
    fun getFirmwarePayloadSize(firmware: FirmwareUpdatePackage): Int = firmware.payload.size

    // CALCULATE_CRC/COMPLITE_CRC: starts CRC and reads its final result.
    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean {
        logger.debug(TAG, "TX CALCULATE_CRC")
        send(BLECommandsV3.requestCalculateCrcFw(addr))

        val calculateStatus = waitFirmwareStatus(FirmwareManagerCommand.CALCULATE_CRC)
        if (calculateStatus != FW_ACK_OK) {
            logger.warn(TAG, "CALCULATE_CRC did not start: status=0x${calculateStatus.toString(16)}")
            return false
        }

        val delayMs = lastMaxChunkInfo
            ?.flashClearDelayMs
            ?.takeIf { it > 0 }
            ?.toLong()
            ?: DEFAULT_CRC_DELAY_MS
        delay(delayMs)

        FirmwareInfoState.completeCrcFlow.resetReplayCache()
        logger.debug(TAG, "TX COMPLITE_CRC")
        send(BLECommandsV3.requestCompleteUpdateFw(addr))

        val ok = FirmwareInfoState.completeCrcFlow.first()
        logger.info(TAG, "CRC verification result for addr=$addr -> $ok")
        return ok
    }

    private suspend fun sendChunkAndAwait(
        packet: ByteArray,
        expectedWrittenBytes: Int,
        timeoutMs: Int
    ): Boolean {
        send(packet)
        var writtenBytes = waitFirmwareStatusOrNull(FirmwareManagerCommand.LOAD_NEW_FW, timeoutMs)

        if (!isLoadChunkAckOk(writtenBytes, expectedWrittenBytes)) {
            logger.warn(
                TAG,
                "LOAD_NEW_FW writtenBytes=$writtenBytes expected=$expectedWrittenBytes, retrying chunk"
            )
            send(packet)
            writtenBytes = waitFirmwareStatusOrNull(FirmwareManagerCommand.LOAD_NEW_FW, timeoutMs)
        }
        return isLoadChunkAckOk(writtenBytes, expectedWrittenBytes)
    }

    private fun isLoadChunkAckOk(writtenBytes: Int?, expectedWrittenBytes: Int): Boolean =
        writtenBytes == expectedWrittenBytes || writtenBytes == FW_ACK_OK

    private suspend fun waitFirmwareStatus(command: FirmwareManagerCommand): Int =
        firmwareCommandStatusFlow
            .filter { it.first == command.number.toInt() }
            .map { it.second }
            .first()

    private suspend fun waitFirmwareStatusOrNull(
        command: FirmwareManagerCommand,
        timeoutMs: Int
    ): Int? = withTimeoutOrNull(timeoutMs.toLong()) {
        waitFirmwareStatus(command)
    }

    private suspend fun requestRunType(addr: Int): PreferenceKeysUbi4.RunProgramType? {
        send(BLECommandsV3.requestRunProgramTypeFw(addr))
        return withTimeoutOrNull(RUN_TYPE_RESPONSE_TIMEOUT_MS) {
            runProgramTypeFlow
                .filter { it.first == addr || it.first == 0 }
                .map { it.second }
                .first()
        }
    }

    private suspend fun send(packet: ByteArray) {
        sender.send(packet, FirmwareTransportChannel.V3_SERIAL)
    }

    private companion object {
        const val TAG = "FW_FLOW_V3"
        const val TRACE_TAG = "DFU_V2_TRACE"
        const val DESC_TAG = "FW_DESC_V3"
        const val FW_ACK_OK = 0x01
        const val MAX_V3_CHUNK_DATA_SIZE = 238
        const val DEFAULT_WRITE_TIMEOUT_MS = 500
        const val MIN_LOAD_NEW_FW_RESPONSE_TIMEOUT_MS = 500
        const val DEFAULT_RECONNECT_DELAY_MS = 1_500L
        const val RUN_TYPE_RESPONSE_TIMEOUT_MS = 1_000L
        const val BOOTLOADER_CHECK_ATTEMPTS = 8
        const val BOOTLOADER_CHECK_INTERVAL_MS = 750L
        const val DEFAULT_CRC_DELAY_MS = 1_500L
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
