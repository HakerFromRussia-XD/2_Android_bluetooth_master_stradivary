package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3

/** Keeps Slider support for screens still using BaseWidgetsFragment's binding. */
class V3SliderSettingsViewModel(
    repository: V3DeviceSettingsRepository,
    setSliderValue: SetSliderValueUseCaseV3,
    sliderParameterKeys: Set<String>,
) : ViewModel() {
    private val sliderSettings = V3SliderSettingsController(
        repository, setSliderValue, sliderParameterKeys, viewModelScope,
    )
    val uiState = sliderSettings.uiState

    fun onViewAttached() = sliderSettings.setActive(true)
    fun onViewDetached() = sliderSettings.setActive(false)
    fun onAction(action: V3SliderAction) = sliderSettings.onAction(action)
}
