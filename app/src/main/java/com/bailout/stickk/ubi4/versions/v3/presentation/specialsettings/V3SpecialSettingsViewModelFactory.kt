package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetSpecialSettingsSectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetAutoLoginEnabledUseCaseV3
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.EditToggleSliderUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SendToggleSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfilesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.GetSettingsProfilesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.RenameSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.CreateSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.SelectSettingsProfileUseCaseV3

class V3SpecialSettingsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3SpecialSettingsWidgetsSource,
    private val toggleSliderRepository: V3ToggleSliderSettingsRepository,
    private val spinnerRepository: V3SpinnerSettingsRepository,
    private val settingsProfilesRepository: V3SettingsProfilesRepository,
    private val appSettingsRepository: V3AppSettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SpecialSettingsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SpecialSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository), widgetsSource, toggleSliderRepository,
            EditToggleSliderUseCaseV3(toggleSliderRepository), SendToggleSliderValueUseCaseV3(toggleSliderRepository),
            spinnerRepository, SetSpinnerValueUseCaseV3(spinnerRepository),
            settingsProfilesRepository, GetSettingsProfilesUseCaseV3(settingsProfilesRepository),
            SelectSettingsProfileUseCaseV3(settingsProfilesRepository),
            CreateSettingsProfileUseCaseV3(settingsProfilesRepository),
            RenameSettingsProfileUseCaseV3(settingsProfilesRepository),
            appSettingsRepository, SetAutoLoginEnabledUseCaseV3(appSettingsRepository),
            SetSpecialSettingsSectionUseCaseV3(appSettingsRepository),
        ) as T
    }
}
