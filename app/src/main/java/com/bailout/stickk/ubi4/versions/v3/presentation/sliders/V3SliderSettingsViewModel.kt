package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderSettingsChange
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.toSliderUiState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Keeps Slider support for screens still using BaseWidgetsFragment's binding. */
class V3SliderSettingsViewModel(
    private val getSliderSettings: GetSliderSettingsUseCaseV3,
    private val observeSliderSettings: ObserveSliderSettingsUseCaseV3,
    private val setSliderValue: SetSliderValueUseCaseV3,
    sliderParameterKeys: Set<String>,
) : ViewModel() {
    private val sliderKeys = sliderParameterKeys
    private val sliderSettings = V3SliderSettingsStateHolder(
        getSliderSettings(sliderKeys).toSliderUiState(), viewModelScope,
        onWriteRequested = { key, value ->
            if (viewModelScope.isActive) setSliderValue(key, value)
        },
    )
    val uiState = sliderSettings.uiState

    init {
        viewModelScope.launch {
            observeSliderSettings(sliderKeys).collect { change ->
                when (change) {
                    is V3SliderSettingsChange.ValueChanged -> sliderSettings.updateValue(change.parameterKey, change.value)
                    is V3SliderSettingsChange.InteractionChanged -> sliderSettings.setInteractionEnabled(change.enabled)
                }
            }
        }
    }

    fun onAction(action: V3SliderSettingsAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3SliderSettingsAction.ViewAttached -> setSliderSettingsActive(true)
            V3SliderSettingsAction.ViewDetached -> setSliderSettingsActive(false)
            is V3SliderSettingsAction.SliderAction -> {
                sliderSettings.setInteractionEnabled(getSliderSettings(sliderKeys).isInteractionEnabled)
                sliderSettings.onAction(action.action)
            }
        }
    }

    private fun setSliderSettingsActive(active: Boolean) {
        val settings = getSliderSettings(sliderKeys)
        sliderSettings.setInteractionEnabled(settings.isInteractionEnabled)
        sliderSettings.setActive(active, settings.values)
    }
}
