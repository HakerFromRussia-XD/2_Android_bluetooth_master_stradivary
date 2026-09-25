package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountProfileLocalRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountProfileRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.service.V3DeviceRoleRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.ObserveServiceEngineerAccessUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.RestoreDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileViewModel

class V3AccountProfileViewModelFactory(
    private val remote: V3AccountProfileRepository,
    private val local: V3AccountProfileLocalRepository,
    private val role: V3DeviceRoleRepository,
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
        ) as T
    }
    companion object {
        fun from(context: Context) = V3AccountProfileViewModelFactory(
            V3AccountProfileRepositoryImpl(),
            createAccountProfileLocalRepository(context),
            V3DeviceRoleRepositoryImpl(
                context.applicationContext.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
                V3DeviceSettingsRepositoryImpl(enqueuePacket = { BleDependencies.v3CommandTransport.enqueue(it) {} }),
            ),
        )
    }
}

internal fun createAccountProfileLocalRepository(context: Context) =
    V3AccountProfileLocalRepositoryImpl(context.applicationContext.getSharedPreferences(
        PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE,
    ), deviceContext = {
        val host = MainActivityUBI4.mainOrNull
        V3AccountProfileDeviceContext(host?.mDeviceName, host?.locate, host?.mDeviceAddress,
            host?.mDeviceType, host?.driverVersionS)
    })
