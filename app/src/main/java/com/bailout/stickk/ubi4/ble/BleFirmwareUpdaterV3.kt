package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.firmwareCommandStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.maxChunkSizeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.FirmwareManagerCommand
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

class BleFirmwareUpdaterV3 {

    private var lastMaxChunkInfo: MaxChunkSizeInfo? = null

    suspend fun ensureBootloader(addr: Int) {
        Log.d("FW_FLOW_V3", "TX GET_RUN_PROGRAM_TYPE")
        val initial = requestRunType(addr)
            ?: throw IllegalStateException("Не удалось прочитать режим платы")
        Log.d("FW_FLOW_V3", "RX initial status = $initial")

        if (initial != PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
            Log.d("FW_FLOW_V3", "TX JUMP_TO_BOOTLOADER")
            send(BLECommandsV3.jumpToBootloaderFw(addr))

            delay(DEFAULT_RECONNECT_DELAY_MS)
            repeat(BOOTLOADER_CHECK_ATTEMPTS) { attempt ->
                Log.d(
                    "FW_FLOW_V3",
                    "TX GET_RUN_PROGRAM_TYPE after reboot attempt=${attempt + 1}/$BOOTLOADER_CHECK_ATTEMPTS"
                )
                val runType = requestRunType(addr)
                Log.d("FW_FLOW_V3", "RX reboot status=$runType")
                if (runType == PreferenceKeysUbi4.RunProgramType.BOOTLOADER) {
                    Log.d("FW_FLOW_V3", "BOOTLOADER готов")
                    return
                }
                delay(BOOTLOADER_CHECK_INTERVAL_MS)
            }

            throw IllegalStateException("Плата не перешла в bootloader после перезапуска")
        } else {
            Log.d("FW_FLOW_V3", "Плата уже в bootloader")
        }
    }

    suspend fun getUploadAttribute(addr: Int): MaxChunkSizeInfo {
        Log.d("FW_FLOW_V3", "TX GET_UP_LOAD_ATRIBUTE")
        send(BLECommandsV3.requestUploadAttributeFw(addr))

        val (_, info) = maxChunkSizeFlow
            .filter { it.first == addr || it.first == 0 }
            .first()

        lastMaxChunkInfo = info
        Log.d("FW_FLOW_V3", "RX GET_UP_LOAD_ATRIBUTE → $info")
        return info
    }

    suspend fun checkNewFirmware(addr: Int, fileItem: FirmwareFileItem): CheckNewFwStatus {
        Log.d("FW_FLOW_V3", "TX CHECK_NEW_FW")
        val fwSize = getFirmwarePayloadSize(fileItem.file)
        val descriptor = FirmwareUpdateUtils.buildFwInfoDescriptor(fileItem.file)
        patchDescriptorFirmwareSize(descriptor, fwSize)
        Log.d(
            "FW_FLOW_V3",
            "CHECK_NEW_FW descriptor fwSize=$fwSize iniFwSize=${FirmwareUpdateUtils.lastFwSize} " +
                "iniFwCrc=${FirmwareUpdateUtils.lastFwCrc}"
        )
        Log.d("FW_DESC_V3", descriptor.joinToString(" ") { "%02X".format(it) })
        send(BLECommandsV3.requestCheckNewFw(addr, descriptor))

        val raw = FirmwareInfoState.checkNewFwFlow.first()
        val status = CheckNewFwStatus.from(raw)
        Log.i("FW_FLOW_V3", "CHECK_NEW_FW ← raw=$raw mapped=$status")
        return status
    }

    suspend fun preloadFlash(addr: Int, fwSize: Int): Boolean {
        Log.d("FW_FLOW_V3", "TX PRELOAD_INFO fwSize=$fwSize")
        send(BLECommandsV3.requestPreloadInfoFw(addr, fwSize))

        val status = waitFirmwareStatus(FirmwareManagerCommand.PRELOAD_INFO)
        val ok = status == FW_ACK_OK
        Log.d("FW_FLOW_V3", "RX PRELOAD_INFO status=0x${status.toString(16)} ok=$ok")
        return ok
    }

    suspend fun sendFirmwareWithProgress(
        addr: Int,
        zipFile: File,
        maxInfo: MaxChunkSizeInfo,
        onProgress: (offset: Int, total: Int) -> Unit
    ) {
        val fwBytes = readFirmwareBytes(zipFile)
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

        Log.d("FW_FLOW_V3", "Все $offset байт прошивки отправлены успешно")
    }

    fun getFirmwarePayloadSize(zipFile: File): Int = readFirmwareBytes(zipFile).size

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean {
        Log.d("FW_FLOW_V3", "TX CALCULATE_CRC")
        send(BLECommandsV3.requestCalculateCrcFw(addr))

        val calculateStatus = waitFirmwareStatus(FirmwareManagerCommand.CALCULATE_CRC)
        if (calculateStatus != FW_ACK_OK) {
            Log.w("FW_FLOW_V3", "CALCULATE_CRC не стартовал: status=0x${calculateStatus.toString(16)}")
            return false
        }

        val delayMs = lastMaxChunkInfo
            ?.flashClearDelayMs
            ?.takeIf { it > 0 }
            ?.toLong()
            ?: DEFAULT_CRC_DELAY_MS
        delay(delayMs)

        FirmwareInfoState.completeCrcFlow.resetReplayCache()
        Log.d("FW_FLOW_V3", "TX COMPLITE_CRC")
        send(BLECommandsV3.requestCompleteUpdateFw(addr))

        val ok = FirmwareInfoState.completeCrcFlow.first()
        Log.i("FW_FLOW_V3", "CRC verification result for addr=$addr → $ok")
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
            Log.w(
                "FW_FLOW_V3",
                "LOAD_NEW_FW writtenBytes=$writtenBytes expected=$expectedWrittenBytes, переотправляем chunk"
            )
            send(packet)
            writtenBytes = waitFirmwareStatusOrNull(FirmwareManagerCommand.LOAD_NEW_FW, timeoutMs)
        }
        return isLoadChunkAckOk(writtenBytes, expectedWrittenBytes)
    }

    private fun isLoadChunkAckOk(writtenBytes: Int?, expectedWrittenBytes: Int): Boolean {
        return writtenBytes == expectedWrittenBytes || writtenBytes == FW_ACK_OK
    }

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

    private fun send(packet: ByteArray) {
        main?.bleCommandWithQueue(packet, SERIALPORTCHAR_UUID, WRITE) {}
    }

    private fun readFirmwareBytes(zipFile: File): ByteArray =
        ZipFile(zipFile).use { zip ->
            val entry = zip.entries().toList()
                .first { !it.isDirectory && it.name.endsWith(".bin", ignoreCase = true) }
            zip.getInputStream(entry).use { it.readBytes() }
        }

    private fun patchDescriptorFirmwareSize(descriptor: ByteArray, fwSize: Int) {
        ByteBuffer.wrap(descriptor)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(FW_DESCRIPTOR_SIZE_OFFSET, fwSize)
    }

    private companion object {
        const val FW_ACK_OK = 0x01
        const val FW_DESCRIPTOR_SIZE_OFFSET = 103
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
