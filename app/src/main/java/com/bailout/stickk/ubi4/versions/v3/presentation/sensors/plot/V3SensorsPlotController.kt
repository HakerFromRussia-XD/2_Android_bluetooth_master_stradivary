package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Main-thread presentation state, owned and activated by the Sensors ViewModel. */
class V3SensorsPlotController(
    private val repository: V3SensorsPlotRepository,
    private val editThreshold: EditPlotThresholdUseCaseV3,
    private val setThresholds: SetPlotThresholdsUseCaseV3,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(V3PlotUiState())
    val uiState = _uiState.asStateFlow()
    private var active = false
    private var observation: Job? = null
    private var sequence = 0L
    private var hasThresholdDraft = false

    fun setActive(value: Boolean) {
        if (active == value) {
            if (active) _uiState.update { it.copy(channelCount = repository.getChannelCount()) }
            return
        }
        active = value
        observation?.cancel()
        observation = null
        hasThresholdDraft = false
        if (!active) {
            _uiState.value = V3PlotUiState()
            return
        }
        restoreThresholds(animate = true)
        _uiState.update { it.copy(channelCount = repository.getChannelCount()) }
        observation = scope.launch {
            var samples = List(6) { 0 }
            val smoother = V3PlotSmoother()
            launch { repository.observeSamples().collect { samples = it } }
            launch {
                repository.observeThresholds().collect { thresholds ->
                    if (!repository.interactionEnabled.value) restoreThresholds(animate = false)
                    else if (thresholds != null) {
                        hasThresholdDraft = false
                        _uiState.update { it.copy(thresholds = thresholds, animateThresholdChanges = true) }
                    }
                }
            }
            launch {
                repository.interactionEnabled.collect { restoreThresholds(animate = it) }
            }
            while (isActive) {
                val paused = repository.arePlotPointsPaused()
                val frame = if (paused) _uiState.value.frame else V3PlotFrame(++sequence, smoother.next(samples))
                _uiState.update { it.copy(isPaused = paused, frame = frame) }
                delay(25)
            }
        }
    }

    fun onAction(action: V3PlotAction) {
        if (!active || !scope.isActive || !repository.interactionEnabled.value) {
            hasThresholdDraft = false
            return
        }
        when (action) {
            is V3PlotAction.ThresholdValueChanged -> {
                hasThresholdDraft = true
                _uiState.update { it.copy(
                    thresholds = editThreshold(it.thresholds, action.threshold, action.value),
                    animateThresholdChanges = false,
                ) }
            }
            V3PlotAction.ThresholdChangeCommitted -> {
                if (!hasThresholdDraft) return
                hasThresholdDraft = false
                setThresholds(_uiState.value.thresholds)
            }
        }
    }

    private fun restoreThresholds(animate: Boolean) {
        hasThresholdDraft = false
        val enabled = active && repository.interactionEnabled.value
        val thresholds = if (enabled) repository.getThresholds() ?: _uiState.value.thresholds else V3PlotThresholds()
        _uiState.update { it.copy(thresholds = thresholds, isEnabled = enabled, animateThresholdChanges = animate && enabled) }
    }
}
