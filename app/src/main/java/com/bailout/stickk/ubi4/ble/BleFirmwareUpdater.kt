package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.bootloaderStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.chunkWrittenFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.maxChunkSizeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.preloadInfoFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.startSystemUpdateFlow
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.StartSystemUpdateStatus
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.zip.ZipFile

class BleFirmwareUpdater {

    private var lastMaxChunkInfo: MaxChunkSizeInfo? = null
    // Фиксированный таймаут ожидания подтверждения записи чанка (мс)
    private val FIXED_WRITE_TIMEOUT_MS = 500L

    suspend fun startSystemUpdate(): StartSystemUpdateStatus {
        Log.d("FW_FLOW", "TX START_SYSTEM_UPDATE")
        main?.bleCommandWithQueue(
            BLECommands.requestStartSystemUpdate(),
            MAIN_CHANNEL, WRITE
        ) {}
        FirmwareInfoState.checkNewFwFlow.resetReplayCache()
        val raw = startSystemUpdateFlow.first()
        val status = StartSystemUpdateStatus.from(raw)
        Log.d("FW_FLOW", "RX START_SYSTEM_UPDATE status=$status")
        return status
    }

    suspend fun ensureBootloader(addr: Int) {
        suspend fun readRunType() = runProgramTypeFlow
            .filter { it.first == addr }
            .map { it.second }
            .first()

        Log.d("FW_FLOW", "TX GET_RUN_PROGRAM_TYPE")
        main?.bleCommandWithQueue(
            BLECommands.requestRunProgramType(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        val initial = readRunType()
        Log.d("FW_FLOW", "RX initial status = $initial")

        if (initial != PreferenceKeysUBI4.RunProgramType.BOOTLOADER) {
            Log.d("FW_FLOW", "TX JUMP_TO_BOOTLOADER")
            main?.bleCommandWithQueue(
                BLECommands.jumpToBootloader(addr.toByte()),
                MAIN_CHANNEL, WRITE
            ) {}
            delay(800)
            Log.d("FW_FLOW", "TX GET_RUN_PROGRAM_TYPE (после delay)")
            main?.bleCommandWithQueue(
                BLECommands.requestRunProgramType(addr.toByte()),
                MAIN_CHANNEL, WRITE
            ) {}
            readRunType()
            Log.d("FW_FLOW", "BOOTLOADER готов")
        } else {
            Log.d("FW_FLOW", "Плата уже в bootloader — получаем инфо")
        }
    }

    suspend fun getBootloaderInfo(addr: Int): List<Int> {
        Log.d("FW_FLOW", "TX GET_BOOTLOADER_INFO")
        main?.bleCommandWithQueue(
            BLECommands.getBootloaderInfo(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        val payload = FirmwareInfoState.bootloaderInfoFlow.first()
        Log.d("FW_FLOW", "RX GET_BOOTLOADER_INFO payload=$payload")
        return payload
    }

    suspend fun checkNewFirmware(addr: Int, fileItem: FirmwareFileItem): CheckNewFwStatus {
        Log.d("FW_FLOW", "TX CHECK_NEW_FW")
        val descriptor = FirmwareUpdateUtils.buildFwInfoDescriptor(fileItem.file)
        Log.d("FW_DESC", descriptor.joinToString(" ") { "%02X".format(it) })
        main?.bleCommandWithQueue(
            BLECommands.requestCheckNewFw(addr.toByte(), descriptor),
            MAIN_CHANNEL, WRITE
        ) {}
        val raw = FirmwareInfoState.checkNewFwFlow.first()
        val status = CheckNewFwStatus.from(raw)
        Log.i("FW_FLOW", "CHECK_NEW_FW ← raw=$raw mapped=$status")
        return status
    }

    suspend fun getMaxChunkSize(addr: Int): MaxChunkSizeInfo {
        Log.d("FW_FLOW", "TX GET_MAX_CHANK_SIZE")
        main?.bleCommandWithQueue(
            BLECommands.requestMaxChunkSize(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        val (_, info) = maxChunkSizeFlow
            .filter { it.first == addr }
            .map { it }
            .first()
        Log.d("FW_FLOW", "RX GET_MAX_CHANK_SIZE → $info")
        return info
    }

    suspend fun preloadFlash(addr: Int): PreferenceKeysUBI4.BootloaderStatus {
        Log.d("FW_FLOW", "TX PRELOAD_INFO")
        main?.bleCommandWithQueue(
            BLECommands.requestPreloadInfo(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        val status = preloadInfoFlow.first()
        Log.d("FW_FLOW", "RX PRELOAD_INFO status=$status")
        return status
    }

    suspend fun waitForDoneClear(addr: Int): PreferenceKeysUBI4.BootloaderStatus {
        Log.d("FW_FLOW", "TX GET_BOOTLOADER_STATUS")
        main?.bleCommandWithQueue(
            BLECommands.requestBootloaderStatus(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        val status = bootloaderStatusFlow.first { it == PreferenceKeysUBI4.BootloaderStatus.DONE_CLEAR }
        Log.d("FW_FLOW", "RX DONE_CLEAR status=$status")
        return status
    }

    suspend fun sendFirmwareWithProgress(
        addr: Int,
        zipFile: File,
        maxInfo: MaxChunkSizeInfo,
        onProgress: (offset: Int, total: Int) -> Unit
    ) {
        // 1) Открываем ZIP и извлекаем .bin
        val fwBytes: ByteArray = ZipFile(zipFile).use { zip ->
            val entry = zip.entries().toList()
                .first { !it.isDirectory && it.name.endsWith(".bin", ignoreCase = true) }
            zip.getInputStream(entry).use { it.readBytes() }
        }

        // 2) Размер прошивки
        val totalSize = FirmwareUpdateUtils.lastFwSize.takeIf { it > 0 }?.toInt() ?: fwBytes.size

        // Начинаем цикл отправки
        var offset = 0
        chunkWrittenFlow.resetReplayCache()

        while (offset < fwBytes.size) {
            val partSize = minOf(maxInfo.chunkSize, fwBytes.size - offset)
            val chunk = fwBytes.copyOfRange(offset, offset + partSize)
            val packet = BLECommands.sendLoadNewFw(addr.toByte(), offset, chunk)

            // первая отправка
            main?.bleCommandWithQueue(packet, MAIN_CHANNEL, WRITE) {}
            Log.d("FW_FLOW", "→ отправили LOAD_NEW_FW payload size=$partSize, offset=$offset")

            // ждём ответ в течение FIXED_WRITE_TIMEOUT_MS
            val written: Int = try {
                withTimeout(FIXED_WRITE_TIMEOUT_MS) {
                    chunkWrittenFlow
                        .filter { it.first == addr }
                        .first()
                        .second
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("FW_FLOW", "Timeout на offset=$offset, переотправляем chunk")
                main?.bleCommandWithQueue(packet, MAIN_CHANNEL, WRITE) {}
                // ждём без таймаута
                chunkWrittenFlow
                    .filter { it.first == addr }
                    .first()
                    .second
            }

            // ограничиваем до partSize
            val actualWritten = written.coerceAtMost(partSize)
            if (actualWritten <= 0) {
                throw IllegalStateException("При offset=$offset ничего не записалось")
            }

            offset += actualWritten
            onProgress(offset.coerceAtMost(totalSize), totalSize)

            // пауза каждые bytesInterval
            if (offset % maxInfo.bytesInterval == 0) {
                delay(maxInfo.timeoutMs.toLong())
            }
        }

        Log.d("FW_FLOW", "Все $offset байт прошивки отправлены успешно")
    }

    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean {
        Log.d("FW_FLOW", "TX CALCULATE_CRC → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestCalculateCrc(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        Log.d("FW_FLOW", "TX GET_BOOTLOADER_STATUS for CRC → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestBootloaderStatus(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}

        val delayMs = lastMaxChunkInfo?.flashClearDelayMs?.toLong() ?: 0L
        Log.d("FW_FLOW", "Waiting $delayMs ms for CRC calculation to settle")
        delay(delayMs)

        bootloaderStatusFlow
            .onEach { status ->
                if (status != PreferenceKeysUBI4.BootloaderStatus.DONE_CRC) {
                    Log.d("FW_FLOW", "RX $status — повтор TX GET_BOOTLOADER_STATUS → addr=$addr")
                    main?.bleCommandWithQueue(
                        BLECommands.requestBootloaderStatus(addr.toByte()),
                        MAIN_CHANNEL, WRITE
                    ) {}
                }
            }
            .first { it == PreferenceKeysUBI4.BootloaderStatus.DONE_CRC }
            .also { Log.d("FW_FLOW", "RX DONE_CRC for addr=$addr") }

        Log.d("FW_FLOW", "TX COMPLETE_UPDATE → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestCompleteUpdate(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        val ok = FirmwareInfoState.completeCrcFlow.first()
        Log.i("FW_FLOW", "CRC verification result for addr=$addr → $ok")
        return ok
    }

    suspend fun finishSystemUpdate(addr: Int) {
        Log.d("FW_FLOW", "TX FINISH_SYSTEM_UPDATE → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestFinishSystemUpdate(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        FirmwareInfoState.finishSystemUpdateFlow.first()
        Log.d("FW_FLOW", "SYSTEM UPDATE COMPLETE on addr=$addr")
    }
}