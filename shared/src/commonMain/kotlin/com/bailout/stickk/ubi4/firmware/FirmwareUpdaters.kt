package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.bootloaderStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.chunkWrittenFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.firmwareCommandStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.maxChunkSizeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.preloadInfoFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.startSystemUpdateFlow
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.FirmwareManagerCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.StartSystemUpdateStatus
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class Ubi4FirmwareUpdater(
    private val sender: FirmwareCommandSender,
    private val logger: FirmwareUpdateLogger = NoOpFirmwareUpdateLogger
) {
    // START_SYSTEM_UPDATE: переводит систему в режим обновления прошивки.
    suspend fun startSystemUpdate(): StartSystemUpdateStatus {
        logger.debug(TAG, "TX START_SYSTEM_UPDATE")
        send(BLECommands.requestStartSystemUpdate())
        FirmwareInfoState.checkNewFwFlow.resetReplayCache()
        val raw = startSystemUpdateFlow.first()
        val status = StartSystemUpdateStatus.from(raw)
        logger.debug(TAG, "RX START_SYSTEM_UPDATE status=$status")
        return status
    }

    // GET_RUN_PROGRAM_TYPE/JUMP_TO_BOOTLOADER: проверяет режим платы и переводит ее в bootloader.
    suspend fun ensureBootloader(addr: Int) {
        suspend fun readRunType() = runProgramTypeFlow
            .filter { it.first == addr }
            .map { it.second }
            .first()

        logger.debug(TAG, "TX GET_RUN_PROGRAM_TYPE")
        send(BLECommands.requestRunProgramType(addr.toByte()))
        val initial = readRunType()
        logger.debug(TAG, "RX initial status = $initial")

        if (initial != PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
            logger.debug(TAG, "TX JUMP_TO_BOOTLOADER")
            send(BLECommands.jumpToBootloader(addr.toByte()))
            delay(BOOTLOADER_RECONNECT_DELAY_MS)
            logger.debug(TAG, "TX GET_RUN_PROGRAM_TYPE after delay")
            send(BLECommands.requestRunProgramType(addr.toByte()))
            readRunType()
            logger.debug(TAG, "BOOTLOADER ready")
        } else {
            logger.debug(TAG, "Board already in bootloader")
        }
    }

    // GET_BOOTLOADER_INFO: запрашивает информацию bootloader платы.
    suspend fun getBootloaderInfo(addr: Int): List<Int> {
        logger.debug(TAG, "TX GET_BOOTLOADER_INFO")
        send(BLECommands.getBootloaderInfo(addr.toByte()))
        val payload = FirmwareInfoState.bootloaderInfoFlow.first()
        logger.debug(TAG, "RX GET_BOOTLOADER_INFO payload=$payload")
        return payload
    }

    // CHECK_NEW_FW: отправляет дескриптор прошивки и ждет решение платы о записи.
    suspend fun checkNewFirmware(
        addr: Int,
        firmware: FirmwareUpdatePackage
    ): CheckNewFwStatus {
        logger.debug(TAG, "TX CHECK_NEW_FW")
        logger.debug(DESC_TAG, firmware.descriptor.toHexString())
        send(BLECommands.requestCheckNewFw(addr.toByte(), firmware.descriptor))
        val raw = FirmwareInfoState.checkNewFwFlow.first()
        val status = CheckNewFwStatus.from(raw)
        logger.info(TAG, "CHECK_NEW_FW raw=$raw mapped=$status")
        return status
    }

    // GET_MAX_CHANK_SIZE: получает размер чанка и тайминги записи прошивки.
    suspend fun getMaxChunkSize(addr: Int): MaxChunkSizeInfo {
        logger.debug(TAG, "TX GET_MAX_CHANK_SIZE")
        send(BLECommands.requestMaxChunkSize(addr.toByte()))
        val (_, info) = maxChunkSizeFlow
            .filter { it.first == addr }
            .first()
        logger.debug(TAG, "RX GET_MAX_CHANK_SIZE $info")
        return info
    }

    // PRELOAD_INFO: запускает подготовку flash-памяти перед записью прошивки.
    suspend fun preloadFlash(addr: Int): PreferenceKeysUbi4.BootloaderStatus {
        logger.debug(TAG, "TX PRELOAD_INFO")
        send(BLECommands.requestPreloadInfo(addr.toByte()))
        val status = preloadInfoFlow.first()
        logger.debug(TAG, "RX PRELOAD_INFO status=$status")
        return status
    }

    // GET_BOOTLOADER_STATUS: ждет DONE_CLEAR после очистки flash-памяти.
    suspend fun waitForDoneClear(addr: Int): PreferenceKeysUbi4.BootloaderStatus {
        logger.debug(TAG, "TX GET_BOOTLOADER_STATUS")
        send(BLECommands.requestBootloaderStatus(addr.toByte()))
        val status = bootloaderStatusFlow.first {
            it == PreferenceKeysUbi4.BootloaderStatus.DONE_CLEAR
        }
        logger.debug(TAG, "RX DONE_CLEAR status=$status")
        return status
    }

    // LOAD_NEW_FW: отправляет bin-прошивку чанками и ждет подтверждение записи.
    suspend fun sendFirmwareWithProgress(
        addr: Int,
        firmware: FirmwareUpdatePackage,
        maxInfo: MaxChunkSizeInfo,
        onProgress: (offset: Int, total: Int) -> Unit
    ) {
        val fwBytes = firmware.payload
        val totalSize = firmware.descriptorFirmwareSize
            .takeIf { it > 0 }
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: fwBytes.size
        var offset = 0
        chunkWrittenFlow.resetReplayCache()

        while (offset < fwBytes.size) {
            val partSize = minOf(maxInfo.chunkSize, fwBytes.size - offset)
            val chunk = fwBytes.copyOfRange(offset, offset + partSize)
            val packet = BLECommands.sendLoadNewFw(addr.toByte(), offset, chunk)

            send(packet)
            logger.debug(TAG, "Sent LOAD_NEW_FW payload size=$partSize, offset=$offset")

            val written = try {
                withTimeout(FIXED_WRITE_TIMEOUT_MS) {
                    chunkWrittenFlow
                        .filter { it.first == addr }
                        .first()
                        .second
                }
            } catch (e: TimeoutCancellationException) {
                logger.error(TAG, "Timeout on offset=$offset, retrying chunk", e)
                send(packet)
                chunkWrittenFlow
                    .filter { it.first == addr }
                    .first()
                    .second
            }

            val actualWritten = written.coerceAtMost(partSize)
            if (actualWritten <= 0) {
                throw IllegalStateException("Nothing was written at offset=$offset")
            }

            offset += actualWritten
            onProgress(offset.coerceAtMost(totalSize), totalSize)

            if (maxInfo.bytesInterval > 0 && offset % maxInfo.bytesInterval == 0) {
                delay(maxInfo.timeoutMs.toLong())
            }
        }

        logger.debug(TAG, "All $offset firmware bytes sent")
    }

    // CALCULATE_CRC/COMPLETE_UPDATE: запускает CRC-проверку и подтверждает завершение записи.
    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean {
        logger.debug(TAG, "TX CALCULATE_CRC addr=$addr")
        send(BLECommands.requestCalculateCrc(addr.toByte()))
        logger.debug(TAG, "TX GET_BOOTLOADER_STATUS for CRC addr=$addr")
        send(BLECommands.requestBootloaderStatus(addr.toByte()))

        val delayMs = 0L
        logger.debug(TAG, "Waiting $delayMs ms for CRC calculation")
        delay(delayMs)

        bootloaderStatusFlow
            .onEach { status ->
                if (status != PreferenceKeysUbi4.BootloaderStatus.DONE_CRC) {
                    logger.debug(TAG, "RX $status, repeating GET_BOOTLOADER_STATUS addr=$addr")
                    send(BLECommands.requestBootloaderStatus(addr.toByte()))
                }
            }
            .first { it == PreferenceKeysUbi4.BootloaderStatus.DONE_CRC }

        logger.debug(TAG, "TX COMPLETE_UPDATE addr=$addr")
        send(BLECommands.requestCompleteUpdate(addr.toByte()))
        val ok = FirmwareInfoState.completeCrcFlow.first()
        logger.info(TAG, "CRC verification result for addr=$addr -> $ok")
        return ok
    }

    // FINISH_SYSTEM_UPDATE: завершает системное обновление после успешной CRC-проверки.
    suspend fun finishSystemUpdate(addr: Int) {
        logger.debug(TAG, "TX FINISH_SYSTEM_UPDATE addr=$addr")
        send(BLECommands.requestFinishSystemUpdate(addr.toByte()))
        FirmwareInfoState.finishSystemUpdateFlow.first()
        logger.debug(TAG, "SYSTEM UPDATE COMPLETE on addr=$addr")
    }

    private suspend fun send(packet: ByteArray) {
        sender.send(packet, FirmwareTransportChannel.UBI4_MAIN)
    }

    private companion object {
        const val TAG = "FW_FLOW"
        const val DESC_TAG = "FW_DESC"
        const val FIXED_WRITE_TIMEOUT_MS = 500L
        const val BOOTLOADER_RECONNECT_DELAY_MS = 800L
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class V3FirmwareUpdater(
    private val sender: FirmwareCommandSender,
    private val logger: FirmwareUpdateLogger = NoOpFirmwareUpdateLogger
) {
    private var lastMaxChunkInfo: MaxChunkSizeInfo? = null

    // GET_RUN_PROGRAM_TYPE/JUMP_TO_BOOTLOADER: проверяет режим V3-платы и переводит ее в bootloader.
    suspend fun ensureBootloader(addr: Int) {
        logger.debug(TAG, "TX GET_RUN_PROGRAM_TYPE")
        val initial = requestRunType(addr)
            ?: throw IllegalStateException("Не удалось прочитать режим платы")
        logger.debug(TAG, "RX initial status = $initial")

        if (initial != PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
            logger.debug(TAG, "TX JUMP_TO_BOOTLOADER")
            send(BLECommandsV3.jumpToBootloaderFw(addr))

            delay(DEFAULT_RECONNECT_DELAY_MS)
            repeat(BOOTLOADER_CHECK_ATTEMPTS) { attempt ->
                logger.debug(
                    TAG,
                    "TX GET_RUN_PROGRAM_TYPE after reboot attempt=${attempt + 1}/$BOOTLOADER_CHECK_ATTEMPTS"
                )
                val runType = requestRunType(addr)
                logger.debug(TAG, "RX reboot status=$runType")
                if (runType == PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
                    logger.debug(TAG, "BOOTLOADER ready")
                    return
                }
                delay(BOOTLOADER_CHECK_INTERVAL_MS)
            }

            throw IllegalStateException("Плата не перешла в bootloader после перезапуска")
        } else {
            logger.debug(TAG, "Board already in bootloader")
        }
    }

    // GET_UP_LOAD_ATRIBUTE: получает V3-размер чанка и тайминги записи прошивки.
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

    // CHECK_NEW_FW: отправляет V3-дескриптор с фактическим размером bin-прошивки.
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

    // PRELOAD_INFO: передает размер bin-прошивки и готовит flash-память к записи.
    suspend fun preloadFlash(addr: Int, fwSize: Int): Boolean {
        logger.debug(TAG, "TX PRELOAD_INFO fwSize=$fwSize")
        send(BLECommandsV3.requestPreloadInfoFw(addr, fwSize))

        val status = waitFirmwareStatus(FirmwareManagerCommand.PRELOAD_INFO)
        val ok = status == FW_ACK_OK
        logger.debug(TAG, "RX PRELOAD_INFO status=0x${status.toString(16)} ok=$ok")
        return ok
    }

    // LOAD_NEW_FW: отправляет bin-прошивку чанками по V3 long-command протоколу.
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

    // Возвращает фактический размер bin-прошивки из пакета для PRELOAD_INFO.
    fun getFirmwarePayloadSize(firmware: FirmwareUpdatePackage): Int = firmware.payload.size

    // CALCULATE_CRC/COMPLITE_CRC: запускает CRC-проверку и читает итоговый результат.
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

class FirmwareUpdateCoordinator(
    private val ubi4Updater: Ubi4FirmwareUpdater,
    private val v3Updater: V3FirmwareUpdater,
    private val logger: FirmwareUpdateLogger = NoOpFirmwareUpdateLogger
) {
    suspend fun runFirmwareUpdate(
        protocol: FirmwareUpdateProtocol,
        addr: Int,
        firmware: FirmwareUpdatePackage,
        onProgress: (offset: Int, total: Int) -> Unit
    ): FirmwareUpdateResult =
        when (protocol) {
            FirmwareUpdateProtocol.UBI4 -> runUbi4Update(addr, firmware, onProgress)
            FirmwareUpdateProtocol.V3 -> runV3Update(addr, firmware, onProgress)
        }

    private suspend fun runUbi4Update(
        addr: Int,
        firmware: FirmwareUpdatePackage,
        onProgress: (offset: Int, total: Int) -> Unit
    ): FirmwareUpdateResult {
        val startStatus = ubi4Updater.startSystemUpdate()
        if (startStatus != StartSystemUpdateStatus.NEW_FW_ACCEPT) {
            return FirmwareUpdateResult.StartSystemUpdateRejected(startStatus)
        }

        ubi4Updater.ensureBootloader(addr)
        ubi4Updater.getBootloaderInfo(addr)

        val checkStatus = ubi4Updater.checkNewFirmware(addr, firmware)
        if (checkStatus != CheckNewFwStatus.NEW_FW_ACCEPT) {
            return FirmwareUpdateResult.CheckNewFirmwareRejected(checkStatus)
        }

        val maxInfo = ubi4Updater.getMaxChunkSize(addr)
        val preloadStatus = ubi4Updater.preloadFlash(addr)
        logger.debug("FW_FLOW", "RX PRELOAD_INFO $preloadStatus")

        val delayMs = maxInfo.flashClearDelayMs.toLong()
        logger.debug("FW_FLOW", "Waiting $delayMs ms for flash clear")
        delay(delayMs)

        val doneClear = ubi4Updater.waitForDoneClear(addr)
        logger.info("FW_FLOW", "Firmware ready, status=$doneClear")

        ubi4Updater.sendFirmwareWithProgress(addr, firmware, maxInfo, onProgress)

        val crcOk = ubi4Updater.checkFirmwareCrcAndCompleteUpdate(addr)
        if (!crcOk) {
            return FirmwareUpdateResult.CrcMismatch
        }

        ubi4Updater.finishSystemUpdate(addr)
        return FirmwareUpdateResult.Success
    }

    private suspend fun runV3Update(
        addr: Int,
        firmware: FirmwareUpdatePackage,
        onProgress: (offset: Int, total: Int) -> Unit
    ): FirmwareUpdateResult {
        v3Updater.ensureBootloader(addr)

        val maxInfo = v3Updater.getUploadAttribute(addr)
        val checkStatus = v3Updater.checkNewFirmware(addr, firmware)
        if (checkStatus != CheckNewFwStatus.NEW_FW_ACCEPT) {
            return FirmwareUpdateResult.CheckNewFirmwareRejected(checkStatus)
        }

        val fwSize = v3Updater.getFirmwarePayloadSize(firmware)
        val preloadOk = v3Updater.preloadFlash(addr, fwSize)
        if (!preloadOk) {
            return FirmwareUpdateResult.PreloadFailed
        }

        val delayMs = maxInfo.flashClearDelayMs.toLong()
        logger.debug("FW_FLOW_V3", "Waiting $delayMs ms for flash clear")
        delay(delayMs)

        v3Updater.sendFirmwareWithProgress(addr, firmware, maxInfo, onProgress)

        val crcOk = v3Updater.checkFirmwareCrcAndCompleteUpdate(addr)
        if (!crcOk) {
            return FirmwareUpdateResult.CrcMismatch
        }

        return FirmwareUpdateResult.Success
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
