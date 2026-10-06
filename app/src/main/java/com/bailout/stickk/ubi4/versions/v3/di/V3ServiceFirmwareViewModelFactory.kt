package com.bailout.stickk.ubi4.versions.v3.di

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.ubi4.ble.AndroidFirmwareCommandSender
import com.bailout.stickk.ubi4.ble.AndroidFirmwareUpdateLogger
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.firmware.*
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import com.bailout.stickk.ubi4.versions.v3.data.firmware.V3ServiceFirmwareUpdateRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareForDebugUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.firmware.V3ServiceFirmwareViewModel
import java.io.File

class V3ServiceFirmwareViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3ServiceFirmwareViewModel::class.java)
        val legacy = LegacyV3FirmwareUpdater(AndroidFirmwareCommandSender, AndroidFirmwareUpdateLogger)
        val coordinator = FirmwareUpdateCoordinator(
            Ubi4FirmwareUpdater(AndroidFirmwareCommandSender, AndroidFirmwareUpdateLogger),
            V3FirmwareUpdater(AndroidFirmwareCommandSender, PlatformFirmwareBulkTransport, AndroidFirmwareUpdateLogger),
            legacy,
            AndroidFirmwareUpdateLogger,
        )
        val repository = V3ServiceFirmwareUpdateRepositoryImpl(
            readPackage = { FirmwareUpdateUtils.readFirmwarePackage(File(it)) },
            prepareTransfer = BleDependencies::prepareUserFirmwareTransfer,
            setSessionActive = BleDependencies::setUserFirmwareSessionActive,
            runUpdate = { address, firmware, progress -> coordinator.runFirmwareUpdate(FirmwareUpdateProtocol.V3, address, firmware, progress) },
            ensureBootloader = { legacy.ensureBootloader(it); Unit },
            elapsedRealtime = SystemClock::elapsedRealtime,
            probeOnly = BuildConfig.DFU_BOOT_ENTRY_PROBE_ONLY,
            logger = AndroidFirmwareUpdateLogger,
            debugLogger = object : FirmwareUpdateLogger {
                override fun info(tag: String, message: String) { Log.i(tag, message) }
                override fun error(tag: String, message: String, throwable: Throwable?) { Log.e(tag, message, throwable) }
            },
        )
        @Suppress("UNCHECKED_CAST")
        return V3ServiceFirmwareViewModel(
            InstallServiceFirmwareUseCaseV3(repository), InstallServiceFirmwareForDebugUseCaseV3(repository),
        ) as T
    }
}
