package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.EditToggleSliderUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SendToggleSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource

class V3SpecialSettingsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3SpecialSettingsWidgetsSource,
    private val toggleSliderRepository: V3ToggleSliderSettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SpecialSettingsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SpecialSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository), widgetsSource, toggleSliderRepository,
            EditToggleSliderUseCaseV3(toggleSliderRepository), SendToggleSliderValueUseCaseV3(toggleSliderRepository),
        ) as T
    }
}
