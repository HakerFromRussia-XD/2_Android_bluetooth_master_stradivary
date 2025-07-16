package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.bootloaderStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.chunkWrittenFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.maxChunkSizeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.preloadInfoFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.startSystemUpdateFlow
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.StartSystemUpdateStatus
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream


class BleFirmwareUpdater {

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
            readRunType()  // ждём BOOTLOADER
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
        return preloadInfoFlow.first()
    }

    suspend fun waitForDoneClear(addr: Int): PreferenceKeysUBI4.BootloaderStatus {
        Log.d("FW_FLOW", "TX GET_BOOTLOADER_STATUS")
        main?.bleCommandWithQueue(
            BLECommands.requestBootloaderStatus(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}
        return bootloaderStatusFlow
            .first { it == PreferenceKeysUBI4.BootloaderStatus.DONE_CLEAR }
    }
    suspend fun sendFirmware(
        addr: Int,
        zipFile: File,
        maxInfo: MaxChunkSizeInfo
    ) {
        // 1) Открываем ZIP и извлекаем .bin
        val fwBytes: ByteArray = ZipFile(zipFile).use { zip ->
            val entry = zip.entries().toList()
                .first { !it.isDirectory && it.name.endsWith(".bin", ignoreCase = true) }
            zip.getInputStream(entry).use { it.readBytes() }
        }
        // 2) Отправка чанков
        var offset = 0
        chunkWrittenFlow.resetReplayCache()
        while (offset < fwBytes.size) {
            // размер текущего чанк
            val partSize = minOf(maxInfo.chunkSize, fwBytes.size - offset)
            val chunk = fwBytes.copyOfRange(offset, offset + partSize)
            // отправляем чистый payload: [CMD, offsetLo, offsetHi, ...chunk...]
            val packet = BLECommands.sendLoadNewFw(addr.toByte(), offset, chunk)
            main?.bleCommandWithQueue(packet, MAIN_CHANNEL, WRITE) {}
            Log.d("FW_FLOW", "→ отправили LOAD_NEW_FW payload size=$partSize, offset=$offset")
            // ждём, сколько реально записано (парсер эмитит именно количество байт)
            val written = FirmwareInfoState.chunkWrittenFlow
                .filter { it.first == addr }
                .map { it.second }
                .first()
            if (written <= 0) {
                throw IllegalStateException("При offset=$offset ничего не записалось")
            }
            offset += written
            // пауза каждые bytesInterval
            if (offset % maxInfo.bytesInterval == 0) {
                delay(maxInfo.timeoutMs.toLong())
            }
        }

        Log.d("FW_FLOW", "Все $offset байт прошивки отправлены успешно")
    }
    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean {
        // 1) Запускаем расчёт CRC
        Log.d("FW_FLOW", "TX CALCULATE_CRC → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestCalculateCrc(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}

        // 2) Сразу запрашиваем статус загрузчика для отслеживания DONE_CRC
        Log.d("FW_FLOW", "TX GET_BOOTLOADER_STATUS for CRC → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestBootloaderStatus(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}

        // 3) Ждём в потоке bootloaderStatusFlow статус DONE_CRC,
        //    переотправляя GET_BOOTLOADER_STATUS каждый раз, когда приходит не тот статус
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

        // 4) Шлём COMPLETE_UPDATE, чтобы плата вернула GOOD/BAD
        Log.d("FW_FLOW", "TX COMPLETE_UPDATE → addr=$addr")
        main?.bleCommandWithQueue(
            BLECommands.requestCompleteUpdate(addr.toByte()),
            MAIN_CHANNEL, WRITE
        ) {}

        // 5) Ждём финального результата в completeCrcFlow
        val ok = FirmwareInfoState.completeCrcFlow.first()
        Log.i("FW_FLOW", "CRC verification result for addr=$addr → $ok")
        return ok
    }

}