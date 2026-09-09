package com.bailout.stickk.ubi4.versions.v3.presentation.advancedsettings

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class V3AdvancedSettingsViewModel(
    private val repository: V3DeviceSettingsRepository,
    private val setSliderValue: SetSliderValueUseCaseV3,
    sliderRanges: Map<String, IntRange>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        V3AdvancedSettingsUiState(
            sliders = sliderRanges.mapValues { (parameterKey, allowedRange) ->
                require(!allowedRange.isEmpty())
                SliderUiStateV3(
                    parameterKey = parameterKey,
                    value = repository.getSliderValue(parameterKey),
                    allowedRange = allowedRange,
                    isEnabled = repository.sliderInteractionEnabled.value,
                )
            }
        )
    )
    val uiState = _uiState.asStateFlow()
    private var isViewAttached = false
    private val pendingSliderWrites = mutableMapOf<String, Job>()

    init {
        _uiState.value.sliders.keys.forEach { parameterKey ->
            viewModelScope.launch {
                repository.observeSliderValue(parameterKey).collect { value ->
                    updateSlider(parameterKey) { it.copy(value = value, animateValueChange = true) }
                }
            }
        }
        viewModelScope.launch {
            repository.sliderInteractionEnabled.collect { enabled ->
                if (!enabled) cancelAllPendingWrites()
                _uiState.update { state ->
                    state.copy(sliders = state.sliders.mapValues { (_, slider) -> slider.copy(isEnabled = enabled) })
                }
            }
        }
    }

    /** UI callbacks and delayed writes run on Main; rendering must never call this method. */
    @MainThread
    fun onAction(action: V3AdvancedSettingsAction) {
        when (action) {
            V3AdvancedSettingsAction.ViewAttached -> {
                if (isViewAttached) return
                isViewAttached = true
                val values = _uiState.value.sliders.keys.associateWith(repository::getSliderValue)
                val enabled = repository.sliderInteractionEnabled.value
                _uiState.update { state ->
                    state.copy(sliders = state.sliders.mapValues { (key, slider) ->
                        slider.copy(value = values[key], isEnabled = enabled, animateValueChange = false)
                    })
                }
            }
            V3AdvancedSettingsAction.ViewDetached -> {
                isViewAttached = false
                cancelAllPendingWrites()
            }
            is V3AdvancedSettingsAction.SliderValueChanged -> {
                if (!canInteract(action.parameterKey)) return
                cancelPendingWrite(action.parameterKey)
                showUserValue(action.parameterKey, action.value)
            }
            is V3AdvancedSettingsAction.SliderChangeCommitted -> {
                if (!canInteract(action.parameterKey)) return
                cancelPendingWrite(action.parameterKey)
                showUserValue(action.parameterKey, action.value)
                commitSliderValue(action.parameterKey)
            }
            is V3AdvancedSettingsAction.SliderStepClicked -> {
                if (!canInteract(action.parameterKey) || action.step !in listOf(-1, 1)) return
                val slider = _uiState.value.sliders.getValue(action.parameterKey)
                val allowedRange = slider.allowedRange
                val current = slider.value ?: allowedRange.first
                val next = (current.toLong() + action.step).coerceIn(
                    allowedRange.first.toLong(), allowedRange.last.toLong()
                ).toInt()
                showUserValue(action.parameterKey, next)
                cancelPendingWrite(action.parameterKey)
                pendingSliderWrites[action.parameterKey] = viewModelScope.launch {
                    delay(300)
                    pendingSliderWrites.remove(action.parameterKey)
                    if (canInteract(action.parameterKey)) commitSliderValue(action.parameterKey)
                }
            }
        }
    }

    private fun canInteract(key: String) =
        key in _uiState.value.sliders && isViewAttached && repository.sliderInteractionEnabled.value

    private fun showUserValue(parameterKey: String, value: Int) {
        updateSlider(parameterKey) {
            it.copy(value = value.coerceIn(it.allowedRange), animateValueChange = false)
        }
    }

    private fun commitSliderValue(parameterKey: String) {
        val slider = _uiState.value.sliders.getValue(parameterKey)
        val value = slider.value ?: return
        setSliderValue(parameterKey, value.coerceIn(slider.allowedRange), slider.allowedRange)
    }

    private fun updateSlider(parameterKey: String, transform: (SliderUiStateV3) -> SliderUiStateV3) {
        _uiState.update { state ->
            state.copy(sliders = state.sliders + (parameterKey to transform(state.sliders.getValue(parameterKey))))
        }
    }

    private fun cancelPendingWrite(parameterKey: String) {
        pendingSliderWrites.remove(parameterKey)?.cancel()
    }

    private fun cancelAllPendingWrites() {
        pendingSliderWrites.values.forEach { it.cancel() }
        pendingSliderWrites.clear()
    }
}
