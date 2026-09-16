package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.IsPlotPausedUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.GetPlotSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObservePlotAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObservePlotSamplesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObservePlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObserveSensorsCommandsAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StopProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderSettingsChange
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotFrame
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotSmoother
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsStateHolder
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.toSliderUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3SensorsViewModel(
    private val getSliderSettings: GetSliderSettingsUseCaseV3,
    private val observeSliderSettings: ObserveSliderSettingsUseCaseV3,
    private val setSliderValue: SetSliderValueUseCaseV3,
    private val widgetsSource: V3SensorsWidgetsSource,
    private val getDeviceSession: GetDeviceSessionUseCaseV3,
    private val observeDeviceSessionChanges: ObserveDeviceSessionChangesUseCaseV3,
    private val getPlotSettings: GetPlotSettingsUseCaseV3,
    private val isPlotPaused: IsPlotPausedUseCaseV3,
    private val observePlotAvailability: ObservePlotAvailabilityUseCaseV3,
    private val observePlotSamples: ObservePlotSamplesUseCaseV3,
    private val observePlotThresholds: ObservePlotThresholdsUseCaseV3,
    private val editPlotThreshold: EditPlotThresholdUseCaseV3,
    private val setPlotThresholds: SetPlotThresholdsUseCaseV3,
    private val observeCommandsAvailability: ObserveSensorsCommandsAvailabilityUseCaseV3,
    private val startMovement: StartProsthesisMovementUseCaseV3,
    private val stopMovement: StopProsthesisMovementUseCaseV3,
    private val refreshSensors: RefreshSensorsUseCaseV3,
) : ViewModel() {
    private val sliderKeys = setOf(P_KEY_EMG_GAIN_OPEN_VALUE, P_KEY_EMG_GAIN_CLOSE_VALUE)
    private val sliderSettings = V3SliderSettingsStateHolder(
        getSliderSettings(sliderKeys).toSliderUiState(), viewModelScope,
        onWriteRequested = { key, value ->
            if (viewModelScope.isActive) setSliderValue(key, value)
        },
    )
    private var plotUiState = V3PlotUiState()
    private var isPlotActive = false
    private var plotObservation: Job? = null
    private var plotFrameSequence = 0L
    private var hasPlotThresholdDraft = false
    private val initialSnapshot = readWidgets()
    private var deviceAddress = initialSnapshot.deviceAddress
    private var isViewAttached = false
    private data class ButtonPress(val movement: V3ProsthesisMovement, val deviceAddress: String)
    private val buttonPresses = mutableMapOf<Long, ButtonPress>()
    private val _uiState = MutableStateFlow(V3SensorsUiState(
        initialSnapshot.deviceProfile, initialSnapshot.widgets, sliderSettings.uiState.value.sliders,
        animationsEnabled = initialSnapshot.animationsEnabled,
    ))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSliderSettings(sliderKeys).collect { change ->
                when (change) {
                    is V3SliderSettingsChange.ValueChanged -> sliderSettings.updateValue(change.parameterKey, change.value)
                    is V3SliderSettingsChange.InteractionChanged -> sliderSettings.setInteractionEnabled(change.enabled)
                }
            }
        }
        viewModelScope.launch {
            observeCommandsAvailability().collect { enabled ->
                if (!enabled) releaseAllButtons()
                updateButtonsState()
            }
        }
        viewModelScope.launch {
            sliderSettings.uiState.collect { updateSliderState() }
        }
        viewModelScope.launch {
            observeDeviceSessionChanges().collect {
                refreshWidgets()
                // Preserve Sensors' previous local indicator behavior on updateFlow.
                _uiState.update { it.copy(isRefreshIndicatorVisible = false) }
            }
        }
    }

    @MainThread
    fun onAction(action: V3SensorsAction) {
        // A queued view callback must not revive a cleared screen or send a command.
        if (!viewModelScope.isActive) return
        when (action) {
            V3SensorsAction.ViewAttached -> {
                isViewAttached = true
                refreshWidgets()
            }
            V3SensorsAction.ViewDetached -> {
                isViewAttached = false
                releaseAllButtons()
                updateButtonsState()
                updateSliderActivity()
                updatePlotActivity()
                _uiState.update { it.copy(isRefreshIndicatorVisible = false) }
            }
            V3SensorsAction.RefreshRequested -> {
                if (isViewAttached && _uiState.value.deviceProfile != V3DeviceProfile.NOT_V3 &&
                    !_uiState.value.isRefreshIndicatorVisible
                ) {
                    releaseAllButtons()
                    updateButtonsState()
                    _uiState.update { it.copy(isRefreshIndicatorVisible = true) }
                    if (!refreshSensors(deviceAddress)) _uiState.update { it.copy(isRefreshIndicatorVisible = false) }
                }
            }
            is V3SensorsAction.ButtonsAction -> onButtonsAction(action.action)
            is V3SensorsAction.PlotAction -> {
                if (_uiState.value.widgets.any { it is V3SensorsWidget.Plot }) {
                    onPlotAction(action.action)
                    updatePlotState()
                }
            }
            is V3SensorsAction.SliderAction -> {
                if (action.action.parameterKey in visibleSliderKeys(_uiState.value.widgets)) {
                    sliderSettings.setInteractionEnabled(getSliderSettings(sliderKeys).isInteractionEnabled)
                    sliderSettings.onAction(action.action)
                    updateSliderState()
                }
            }
        }
    }

    private fun refreshWidgets() {
        val snapshot = readWidgets()
        val previous = _uiState.value
        if (deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            visibleSliderKeys(previous.widgets) != visibleSliderKeys(snapshot.widgets)
        ) {
            setSliderSettingsActive(false)
        }
        if (deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            previous.widgets.filterIsInstance<V3SensorsWidget.Plot>() != snapshot.widgets.filterIsInstance<V3SensorsWidget.Plot>()
        ) setPlotActive(false)
        if (deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            availableMovements(previous.widgets) != availableMovements(snapshot.widgets)
        ) releaseAllButtons()
        deviceAddress = snapshot.deviceAddress
        _uiState.update { it.copy(
            deviceProfile = snapshot.deviceProfile, widgets = snapshot.widgets,
            animationsEnabled = snapshot.animationsEnabled,
        ) }
        updateSliderActivity()
        updatePlotActivity()
        updateButtonsState()
    }

    private fun updateSliderActivity() {
        val state = _uiState.value
        setSliderSettingsActive(isViewAttached && state.deviceProfile != V3DeviceProfile.NOT_V3 &&
            visibleSliderKeys(state.widgets).isNotEmpty())
        updateSliderState()
    }

    private fun updateSliderState() {
        val keys = visibleSliderKeys(_uiState.value.widgets)
        val sliders = sliderSettings.uiState.value.sliders.mapValues { (key, slider) ->
            if (key in keys) slider else slider.copy(isEnabled = false)
        }
        _uiState.update { it.copy(sliders = sliders) }
    }

    private fun updatePlotActivity() {
        val screen = _uiState.value
        setPlotActive(isViewAttached && screen.deviceProfile != V3DeviceProfile.NOT_V3 &&
            screen.widgets.any { it is V3SensorsWidget.Plot })
        updatePlotState()
    }

    private fun updatePlotState() {
        _uiState.update { it.copy(plot = plotUiState.takeIf { _ ->
            it.deviceProfile != V3DeviceProfile.NOT_V3 && it.widgets.any { widget -> widget is V3SensorsWidget.Plot }
        }) }
    }

    private fun onButtonsAction(action: V3SensorsButtonsAction) {
        when (action) {
            is V3SensorsButtonsAction.ButtonPressed -> {
                if (!isViewAttached || _uiState.value.deviceProfile == V3DeviceProfile.NOT_V3 ||
                    action.pressId in buttonPresses || action.movement !in availableMovements(_uiState.value.widgets)
                ) return
                if (startMovement(deviceAddress, action.movement)) {
                    buttonPresses[action.pressId] = ButtonPress(action.movement, deviceAddress)
                }
            }
            is V3SensorsButtonsAction.ButtonReleased -> {
                buttonPresses.remove(action.pressId)?.let { stopMovement(it.deviceAddress) }
            }
        }
        updateButtonsState()
    }

    private fun releaseAllButtons() {
        val presses = buttonPresses.values.toList()
        buttonPresses.clear()
        presses.forEach { stopMovement(it.deviceAddress) }
    }

    private fun updateButtonsState() {
        val screen = _uiState.value
        val movements = availableMovements(screen.widgets)
        _uiState.update { it.copy(buttons = V3SensorsButtonsUiState(
            availableMovements = movements,
            pressedMovements = buttonPresses.values.map { press -> press.movement }.toSet(),
            isEnabled = isViewAttached && screen.deviceProfile != V3DeviceProfile.NOT_V3 &&
                movements.isNotEmpty() && observeCommandsAvailability().value,
        )) }
    }

    private fun availableMovements(widgets: List<V3SensorsWidget>): Set<V3ProsthesisMovement> =
        widgets.filterIsInstance<V3SensorsWidget.Buttons>().flatMap { it.movements }.toSet()

    override fun onCleared() {
        releaseAllButtons()
        super.onCleared()
    }

    private fun visibleSliderKeys(widgets: List<V3SensorsWidget>): Set<String> =
        widgets.filterIsInstance<V3SensorsWidget.Slider>().map { it.parameterKey }.toSet()

    private fun setSliderSettingsActive(active: Boolean) {
        val settings = getSliderSettings(sliderKeys)
        sliderSettings.setInteractionEnabled(settings.isInteractionEnabled)
        sliderSettings.setActive(active, settings.values)
    }

    private fun setPlotActive(value: Boolean) {
        if (isPlotActive == value) {
            if (isPlotActive) updatePlotUiState { it.copy(channelCount = getPlotSettings().channelCount) }
            return
        }
        isPlotActive = value
        plotObservation?.cancel()
        plotObservation = null
        hasPlotThresholdDraft = false
        if (!isPlotActive) {
            updatePlotUiState { V3PlotUiState() }
            return
        }
        restorePlotThresholds(animate = true)
        updatePlotUiState { it.copy(channelCount = getPlotSettings().channelCount) }
        plotObservation = viewModelScope.launch {
            var samples = List(6) { 0 }
            val smoother = V3PlotSmoother()
            launch { observePlotSamples().collect { samples = it } }
            launch {
                observePlotThresholds().collect { thresholds ->
                    if (!observePlotAvailability().value) restorePlotThresholds(animate = false)
                    else if (thresholds != null) {
                        hasPlotThresholdDraft = false
                        updatePlotUiState { it.copy(thresholds = thresholds, animateThresholdChanges = true) }
                    }
                }
            }
            launch {
                observePlotAvailability().collect { restorePlotThresholds(animate = it) }
            }
            while (isActive) {
                val paused = isPlotPaused()
                val frame = if (paused) plotUiState.frame else V3PlotFrame(++plotFrameSequence, smoother.next(samples))
                updatePlotUiState { it.copy(isPaused = paused, frame = frame) }
                delay(25)
            }
        }
    }

    private fun onPlotAction(action: V3PlotAction) {
        if (!isPlotActive || !viewModelScope.isActive || !observePlotAvailability().value) {
            hasPlotThresholdDraft = false
            return
        }
        when (action) {
            is V3PlotAction.ThresholdValueChanged -> {
                hasPlotThresholdDraft = true
                updatePlotUiState { it.copy(
                    thresholds = editPlotThreshold(it.thresholds, action.threshold, action.value),
                    animateThresholdChanges = false,
                ) }
            }
            V3PlotAction.ThresholdChangeCommitted -> {
                if (!hasPlotThresholdDraft) return
                hasPlotThresholdDraft = false
                setPlotThresholds(plotUiState.thresholds)
            }
        }
    }

    private fun restorePlotThresholds(animate: Boolean) {
        hasPlotThresholdDraft = false
        val enabled = isPlotActive && observePlotAvailability().value
        val thresholds = if (enabled) getPlotSettings().thresholds ?: plotUiState.thresholds else V3PlotThresholds()
        updatePlotUiState { it.copy(thresholds = thresholds, isEnabled = enabled, animateThresholdChanges = animate && enabled) }
    }

    private fun updatePlotUiState(transform: (V3PlotUiState) -> V3PlotUiState) {
        plotUiState = transform(plotUiState)
        updatePlotState()
    }

    private fun readWidgets(): V3SensorsWidgetsSnapshot {
        val session = getDeviceSession()
        return V3SensorsWidgetsSnapshot(
            session.profile, session.address, widgetsSource.widgets(session.profile),
            animationsEnabled = !session.restoredFromSnapshot,
        )
    }
}
