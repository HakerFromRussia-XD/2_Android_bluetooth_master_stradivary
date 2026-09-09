package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3

class V3SliderSettingsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val sliderParameterKeys: Set<String>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SliderSettingsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SliderSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository), sliderParameterKeys
        ) as T
    }
}
