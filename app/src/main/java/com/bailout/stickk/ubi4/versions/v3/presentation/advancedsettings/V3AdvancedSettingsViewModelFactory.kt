package com.bailout.stickk.ubi4.versions.v3.presentation.advancedsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3

class V3AdvancedSettingsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val sliderRanges: Map<String, IntRange>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3AdvancedSettingsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3AdvancedSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository), sliderRanges
        ) as T
    }
}
