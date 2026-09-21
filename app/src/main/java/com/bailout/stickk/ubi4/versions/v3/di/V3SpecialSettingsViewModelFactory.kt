package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.di.BleDependencies
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3.SettingsProfileApplierV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.appsettings.V3AppSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceSessionRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles.V3SettingsProfilesRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetAutoLoginEnabledUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetSpecialSettingsSectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.ObserveAutoLoginEnabledUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetAutoLoginEnabledUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetSpecialSettingsSectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.EditToggleSliderUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSpinnerSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetToggleSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSpinnerSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveToggleSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SendToggleSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.CreateSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.GetSettingsProfilesDeviceSerialUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.LoadSettingsProfilesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.GetSettingsProfilesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.ObserveSettingsProfilesChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.RenameSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.SelectSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfilesRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.DataFactoryV3SpecialSettingsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource

class V3SpecialSettingsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3SpecialSettingsWidgetsSource,
    private val toggleSliderRepository: V3ToggleSliderSettingsRepository,
    private val spinnerRepository: V3SpinnerSettingsRepository,
    private val settingsProfilesRepository: V3SettingsProfilesRepository,
    private val appSettingsRepository: V3AppSettingsRepository,
    private val sessionRepository: V3DeviceSessionRepository,
) : ViewModelProvider.Factory {
    companion object {
        fun create(context: Context): V3SpecialSettingsViewModelFactory = create(context) { packet ->
            BleDependencies.v3CommandTransport.enqueue(packet)
        }

        fun create(context: Context, enqueuePacket: (ByteArray) -> Unit): V3SpecialSettingsViewModelFactory {
            val repository = V3DeviceSettingsRepositoryImpl(enqueuePacket)
            return V3SpecialSettingsViewModelFactory(
                repository = repository,
                widgetsSource = DataFactoryV3SpecialSettingsWidgetsSource(),
                toggleSliderRepository = repository,
                spinnerRepository = repository,
                settingsProfilesRepository = V3SettingsProfilesRepositoryImpl(SettingsProfileApplierV3::apply),
                appSettingsRepository = V3AppSettingsRepositoryImpl(context.applicationContext.getSharedPreferences(
                    PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE,
                )),
                sessionRepository = V3DeviceSessionRepositoryImpl(),
            )
        }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SpecialSettingsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SpecialSettingsViewModel(
            getSliderSettings = GetSliderSettingsUseCaseV3(repository),
            observeSliderSettings = ObserveSliderSettingsUseCaseV3(repository),
            setSliderValue = SetSliderValueUseCaseV3(repository),
            widgetsSource = widgetsSource,
            getDeviceSession = GetDeviceSessionUseCaseV3(sessionRepository),
            observeDeviceSessionChanges = ObserveDeviceSessionChangesUseCaseV3(sessionRepository),
            getToggleSliderSettings = GetToggleSliderSettingsUseCaseV3(toggleSliderRepository),
            observeToggleSliderSettings = ObserveToggleSliderSettingsUseCaseV3(toggleSliderRepository),
            editToggleSlider = EditToggleSliderUseCaseV3(toggleSliderRepository),
            sendToggleSliderValue = SendToggleSliderValueUseCaseV3(toggleSliderRepository),
            getSpinnerSettings = GetSpinnerSettingsUseCaseV3(spinnerRepository),
            observeSpinnerSettings = ObserveSpinnerSettingsUseCaseV3(spinnerRepository),
            setSpinnerValue = SetSpinnerValueUseCaseV3(spinnerRepository),
            observeSettingsProfilesChanges = ObserveSettingsProfilesChangesUseCaseV3(settingsProfilesRepository),
            getSettingsProfilesDeviceSerial = GetSettingsProfilesDeviceSerialUseCaseV3(settingsProfilesRepository),
            loadSettingsProfiles = LoadSettingsProfilesUseCaseV3(GetSettingsProfilesUseCaseV3(settingsProfilesRepository), settingsProfilesRepository),
            selectSettingsProfile = SelectSettingsProfileUseCaseV3(settingsProfilesRepository),
            createSettingsProfile = CreateSettingsProfileUseCaseV3(settingsProfilesRepository),
            renameSettingsProfile = RenameSettingsProfileUseCaseV3(settingsProfilesRepository),
            getAutoLoginEnabled = GetAutoLoginEnabledUseCaseV3(appSettingsRepository),
            observeAutoLoginEnabled = ObserveAutoLoginEnabledUseCaseV3(appSettingsRepository),
            getSpecialSettingsSection = GetSpecialSettingsSectionUseCaseV3(appSettingsRepository),
            setAutoLoginEnabled = SetAutoLoginEnabledUseCaseV3(appSettingsRepository),
            setSpecialSettingsSection = SetSpecialSettingsSectionUseCaseV3(appSettingsRepository),
        ) as T
    }
}
