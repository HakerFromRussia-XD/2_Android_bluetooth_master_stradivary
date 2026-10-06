package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.firmware.FirmwareCompatibility
import com.bailout.stickk.ubi4.firmware.FirmwareVersionCatalog
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountProfileLocalRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountProfileRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountBoardsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.toAccountProfileDeviceContext
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.data.firmware.V3ServiceFirmwareCatalogRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ObserveServiceEngineerAccessUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.RestoreDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileViewModel

class V3AccountProfileViewModelFactory(
    private val remote: V3AccountProfileRepository,
    private val local: V3AccountProfileLocalRepository,
    private val role: V3DeviceRoleRepository,
    private val firmware: V3ServiceFirmwareCatalogRepository,
    private val boards: V3AccountBoardsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3AccountProfileViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3AccountProfileViewModel(
            getViewData = GetAccountProfileViewDataUseCaseV3(local),
            loadProfile = LoadAccountProfileUseCaseV3(remote, local),
            cacheHeader = CacheAccountProfileHeaderUseCaseV3(local),
            restoreDeviceRole = RestoreDeviceRoleUseCaseV3(role),
            observeServiceEngineerAccess = ObserveServiceEngineerAccessUseCaseV3(role),
            loadFirmwareCatalog = LoadServiceFirmwareCatalogUseCaseV3(firmware),
            getFirmwareForBoard = createServiceFirmwareForBoardUseCaseV3(),
            downloadFirmwareFiles = DownloadServiceFirmwareFilesUseCaseV3(firmware),
            getBoards = GetAccountBoardsUseCaseV3(boards, FirmwareVersionCatalog::isZeroVersion),
            cacheBoards = CacheAccountBoardsUseCaseV3(boards),
            observeBoards = ObserveAccountBoardsUseCaseV3(boards),
        ) as T
    }
    companion object {
        fun from(context: Context): V3AccountProfileViewModelFactory {
            val applicationContext = context.applicationContext
            return V3AccountProfileViewModelFactory(
                V3AccountProfileRepositoryImpl(),
                createAccountProfileLocalRepository(context),
                createDeviceRoleRepository(
                    applicationContext.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
                    V3DeviceSettingsRepositoryImpl(enqueuePacket = { BleDependencies.v3CommandTransport.enqueue(it) {} }),
                ),
                V3ServiceFirmwareCatalogRepositoryImpl(cacheDirectory = { applicationContext.cacheDir.absolutePath }),
                V3AccountBoardsRepositoryImpl(),
            )
        }
    }
}

internal fun createServiceFirmwareForBoardUseCaseV3() = GetServiceFirmwareForBoardUseCaseV3(
    familyForAddress = { FirmwareBoardFamily.fromDeviceAddress(it).name },
    isCompatible = FirmwareCompatibility::isCompatible,
    versionForDevice = FirmwareCompatibility::versionForDevice,
    isVersionNewer = FirmwareVersionCatalog::isLocalVersionNewer,
    isUpdateAvailable = FirmwareCompatibility::isUpdateAvailable,
)

internal fun createAccountProfileLocalRepository(context: Context) =
    V3AccountProfileLocalRepositoryImpl(context.applicationContext.getSharedPreferences(
        PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE,
    ), deviceContext = { BleDependencies.currentDeviceIdentity.toAccountProfileDeviceContext() })
