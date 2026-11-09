package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles.V3BaseSettingsUpdatesRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.*
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3BaseSettingsUpdatesViewModel

class V3BaseSettingsUpdatesViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = V3BaseSettingsUpdatesRepositoryImpl(
        context.applicationContext,
        context.applicationContext.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
        createSettingsProfileValueApplier(context.applicationContext),
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3BaseSettingsUpdatesViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3BaseSettingsUpdatesViewModel(
            ObserveBaseSettingsUpdatesUseCaseV3(repository), RefreshBaseSettingsUpdatesUseCaseV3(repository),
            RespondToBaseSettingsUpdateUseCaseV3(repository),
        ) as T
    }
}
