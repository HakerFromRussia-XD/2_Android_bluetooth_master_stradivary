package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsViewModel

class V3SliderSettingsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val sliderParameterKeys: Set<String>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SliderSettingsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SliderSettingsViewModel(
            getSliderSettings = GetSliderSettingsUseCaseV3(repository),
            observeSliderSettings = ObserveSliderSettingsUseCaseV3(repository),
            setSliderValue = SetSliderValueUseCaseV3(repository),
            sliderParameterKeys = sliderParameterKeys,
        ) as T
    }
}
