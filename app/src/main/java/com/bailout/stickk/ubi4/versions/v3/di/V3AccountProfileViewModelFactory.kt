package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountProfileLocalRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.accountprofile.V3AccountProfileRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile.V3AccountProfileViewModel

class V3AccountProfileViewModelFactory(
    private val remote: V3AccountProfileRepository,
    private val local: V3AccountProfileLocalRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3AccountProfileViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3AccountProfileViewModel(GetAccountProfileViewDataUseCaseV3(local),
            LoadAccountProfileUseCaseV3(remote, local), CacheAccountProfileHeaderUseCaseV3(local)) as T
    }
    companion object {
        fun from(context: Context) = V3AccountProfileViewModelFactory(
            V3AccountProfileRepositoryImpl(),
            createAccountProfileLocalRepository(context),
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
