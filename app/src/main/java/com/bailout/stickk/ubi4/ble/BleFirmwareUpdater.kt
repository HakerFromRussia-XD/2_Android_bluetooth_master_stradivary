package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.firmware.Ubi4FirmwareUpdater
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.StartSystemUpdateStatus
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import java.io.File

// Android-фасад совместимости: основной сценарий теперь выполняет common FirmwareUpdateCoordinator.
@Suppress("unused")
class BleFirmwareUpdater(
    internal val sharedUpdater: Ubi4FirmwareUpdater = Ubi4FirmwareUpdater(
        sender = AndroidFirmwareCommandSender,
        logger = AndroidFirmwareUpdateLogger
    )
) {
    suspend fun startSystemUpdate(): StartSystemUpdateStatus =
        sharedUpdater.startSystemUpdate()

    suspend fun ensureBootloader(addr: Int) {
        sharedUpdater.ensureBootloader(addr)
    }

    suspend fun getBootloaderInfo(addr: Int): List<Int> =
        sharedUpdater.getBootloaderInfo(addr)

    suspend fun checkNewFirmware(addr: Int, fileItem: FirmwareFileItem): CheckNewFwStatus =
        sharedUpdater.checkNewFirmware(addr, FirmwareUpdateUtils.readFirmwarePackage(fileItem.file))

    suspend fun getMaxChunkSize(addr: Int): MaxChunkSizeInfo =
        sharedUpdater.getMaxChunkSize(addr)

    suspend fun preloadFlash(addr: Int): PreferenceKeysUbi4.BootloaderStatus =
        sharedUpdater.preloadFlash(addr)

    suspend fun waitForDoneClear(addr: Int): PreferenceKeysUbi4.BootloaderStatus =
        sharedUpdater.waitForDoneClear(addr)

    suspend fun sendFirmwareWithProgress(
        addr: Int,
        zipFile: File,
        maxInfo: MaxChunkSizeInfo,
        onProgress: (offset: Int, total: Int) -> Unit
    ) {
        sharedUpdater.sendFirmwareWithProgress(
            addr = addr,
            firmware = FirmwareUpdateUtils.readFirmwarePackage(zipFile),
            maxInfo = maxInfo,
            onProgress = onProgress
        )
    }

    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean =
        sharedUpdater.checkFirmwareCrcAndCompleteUpdate(addr)

    suspend fun finishSystemUpdate(addr: Int) {
        sharedUpdater.finishSystemUpdate(addr)
    }
}
