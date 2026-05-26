package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.firmware.V3FirmwareUpdater
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import java.io.File

// Android-фасад совместимости: основной сценарий теперь выполняет common FirmwareUpdateCoordinator.
@Suppress("unused")
class BleFirmwareUpdaterV3(
    internal val sharedUpdater: V3FirmwareUpdater = V3FirmwareUpdater(
        sender = AndroidFirmwareCommandSender,
        logger = AndroidFirmwareUpdateLogger
    )
) {
    suspend fun ensureBootloader(addr: Int) {
        sharedUpdater.ensureBootloader(addr)
    }

    suspend fun getUploadAttribute(addr: Int): MaxChunkSizeInfo =
        sharedUpdater.getUploadAttribute(addr)

    suspend fun checkNewFirmware(addr: Int, fileItem: FirmwareFileItem): CheckNewFwStatus =
        sharedUpdater.checkNewFirmware(addr, FirmwareUpdateUtils.readFirmwarePackage(fileItem.file))

    suspend fun preloadFlash(addr: Int, fwSize: Int): Boolean =
        sharedUpdater.preloadFlash(addr, fwSize)

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

    fun getFirmwarePayloadSize(zipFile: File): Int =
        FirmwareUpdateUtils.readFirmwarePackage(zipFile).payload.size

    suspend fun checkFirmwareCrcAndCompleteUpdate(addr: Int): Boolean =
        sharedUpdater.checkFirmwareCrcAndCompleteUpdate(addr)
}
