package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.main.ManageMainConnectionUseCaseV3
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.main.V3MainRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.main.GetMainVisibleDisplaysUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.ObserveMainUpdatesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.UpdateMainDeviceIdentityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.ScheduleProfileUploadUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.main.V3MainViewModel

class V3MainViewModelFactory(private val context: Context, private val owner: ViewModelStoreOwner) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3MainViewModel::class.java)
        val repository = V3MainRepositoryImpl(
            BleDependencies.deviceIdentity(owner), context,
            context.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
        )
        @Suppress("UNCHECKED_CAST")
        return V3MainViewModel(
            GetMainVisibleDisplaysUseCaseV3(repository), ObserveMainUpdatesUseCaseV3(repository),
            UpdateMainDeviceIdentityUseCaseV3(repository),
            ScheduleProfileUploadUseCaseV3(repository),
            ManageMainConnectionUseCaseV3(repository),
        ) as T
    }
}
