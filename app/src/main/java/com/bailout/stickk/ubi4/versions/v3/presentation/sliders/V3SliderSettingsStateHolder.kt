package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** UI drafts and debounce only. The owning ViewModel supplies values and handles write requests. */
class V3SliderSettingsStateHolder(
    initialState: V3SliderSettingsUiState,
    private val scope: CoroutineScope,
    private val onWriteRequested: (parameterKey: String, value: Int) -> Unit,
) {
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()
    private var isActive = false
    private var isInteractionEnabled = false
    private val pendingSliderWrites = mutableMapOf<String, Job>()

    fun updateValue(parameterKey: String, value: Int?) {
        if (parameterKey in _uiState.value.sliders) {
            updateSlider(parameterKey) { it.copy(value = value, animateValueChange = true) }
        }
    }

    fun setInteractionEnabled(enabled: Boolean) {
        isInteractionEnabled = enabled
        if (!enabled) cancelAllScheduledSliderWrites()
        _uiState.update { state -> state.copy(sliders = state.sliders.mapValues { (_, slider) ->
            slider.copy(isEnabled = enabled && isActive)
        }) }
    }

    /** A stopped view or hidden settings section cannot schedule or finish a write. */
    @MainThread
    fun setActive(active: Boolean, values: Map<String, Int?> = emptyMap()) {
        if (isActive == active) return
        isActive = active
        if (!active) cancelAllScheduledSliderWrites()
        val enabled = active && isInteractionEnabled
        _uiState.update { state ->
            state.copy(sliders = state.sliders.mapValues { (key, slider) ->
                slider.copy(
                    value = if (active) values[key] else slider.value,
                    isEnabled = enabled,
                    animateValueChange = false,
                )
            })
        }
    }

    /** UI callbacks and delayed writes run on Main; rendering never calls this method. */
    @MainThread
    fun onAction(action: V3SliderAction) {
        when (action) {
            is V3SliderAction.SliderValueChanged -> {
                if (!canChangeSliderValue(action.parameterKey)) return
                cancelScheduledSliderWrite(action.parameterKey)
                updateSliderDraft(action.parameterKey, action.value)
            }
            is V3SliderAction.SliderChangeCommitted -> {
                if (!canChangeSliderValue(action.parameterKey)) return
                cancelScheduledSliderWrite(action.parameterKey)
                updateSliderDraft(action.parameterKey, action.value)
                applySliderValue(action.parameterKey)
            }
            is V3SliderAction.SliderStepClicked -> {
                if (!canChangeSliderValue(action.parameterKey) || action.step !in listOf(-1, 1)) return
                val slider = _uiState.value.sliders.getValue(action.parameterKey)
                val allowedRange = slider.allowedRange
                val current = slider.value ?: allowedRange.first
                val next = (current.toLong() + action.step).coerceIn(
                    allowedRange.first.toLong(), allowedRange.last.toLong()
                ).toInt()
                updateSliderDraft(action.parameterKey, next)
                cancelScheduledSliderWrite(action.parameterKey)
                pendingSliderWrites[action.parameterKey] = scope.launch {
                    delay(300)
                    pendingSliderWrites.remove(action.parameterKey)
                    if (canChangeSliderValue(action.parameterKey)) applySliderValue(action.parameterKey)
                }
            }
        }
    }

    private fun canChangeSliderValue(key: String) =
        key in _uiState.value.sliders && isActive && scope.isActive && isInteractionEnabled

    private fun updateSliderDraft(parameterKey: String, value: Int) {
        updateSlider(parameterKey) {
            it.copy(value = value.coerceIn(it.allowedRange), animateValueChange = false)
        }
    }

    private fun applySliderValue(parameterKey: String) {
        val slider = _uiState.value.sliders.getValue(parameterKey)
        val value = slider.value ?: return
        onWriteRequested(parameterKey, value)
    }

    private fun updateSlider(parameterKey: String, transform: (SliderUiStateV3) -> SliderUiStateV3) {
        _uiState.update { state ->
            state.copy(sliders = state.sliders + (parameterKey to transform(state.sliders.getValue(parameterKey))))
        }
    }

    private fun cancelScheduledSliderWrite(parameterKey: String) {
        pendingSliderWrites.remove(parameterKey)?.cancel()
    }

    private fun cancelAllScheduledSliderWrites() {
        pendingSliderWrites.values.forEach { it.cancel() }
        pendingSliderWrites.clear()
    }
}
