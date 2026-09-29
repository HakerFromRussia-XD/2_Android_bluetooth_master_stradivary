package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.firmware.V3UserFirmwareRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.*
import com.bailout.stickk.ubi4.versions.v3.presentation.firmware.V3UserFirmwareViewModel
import java.io.File

class V3UserFirmwareViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val context = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3UserFirmwareViewModel::class.java)
        val repository = V3UserFirmwareRepositoryImpl(
            preferences = context.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
            connectivity = context.getSystemService(ConnectivityManager::class.java),
            directory = File(context.filesDir, "user_firmware").apply { mkdirs() }.path,
            prepareTransfer = BleDependencies::prepareUserFirmwareTransfer,
            setSessionActive = BleDependencies::setUserFirmwareSessionActive,
            resumeAfterUpdate = BleDependencies::resumeAfterUserFirmwareUpdate,
        )
        @Suppress("UNCHECKED_CAST")
        return V3UserFirmwareViewModel(
            ObserveUserFirmwareUpdatesUseCaseV3(repository),
            RefreshUserFirmwareEnvironmentUseCaseV3(repository),
            StartUserFirmwareUpdateUseCaseV3(repository),
            PostponeUserFirmwareUpdateUseCaseV3(repository),
            AcknowledgeUserFirmwareCompletionUseCaseV3(repository),
            CloseUserFirmwareUpdatesUseCaseV3(repository),
        ) as T
    }
}
