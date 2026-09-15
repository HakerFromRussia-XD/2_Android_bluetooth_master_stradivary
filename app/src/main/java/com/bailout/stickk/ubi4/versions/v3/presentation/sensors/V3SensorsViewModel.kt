package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3SensorsPlotController
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsCommandsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StopProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsUiState
import kotlinx.coroutines.isActive
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class V3SensorsViewModel(
    repository: V3DeviceSettingsRepository,
    setSliderValue: SetSliderValueUseCaseV3,
    private val widgetsSource: V3SensorsWidgetsSource,
    plotRepository: V3SensorsPlotRepository,
    editPlotThreshold: EditPlotThresholdUseCaseV3,
    setPlotThresholds: SetPlotThresholdsUseCaseV3,
    private val commandsRepository: V3SensorsCommandsRepository,
    private val startMovement: StartProsthesisMovementUseCaseV3,
    private val stopMovement: StopProsthesisMovementUseCaseV3,
    private val refreshSensors: RefreshSensorsUseCaseV3,
) : ViewModel() {
    private val sliderSettings = V3SliderSettingsController(
        repository, setSliderValue, setOf(P_KEY_EMG_GAIN_OPEN_VALUE, P_KEY_EMG_GAIN_CLOSE_VALUE), viewModelScope,
    )
    private val plotSettings = V3SensorsPlotController(plotRepository, editPlotThreshold, setPlotThresholds, viewModelScope)
    private val initialSnapshot = widgetsSource.snapshot()
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
            commandsRepository.interactionEnabled.collect { enabled ->
                if (!enabled) releaseAllButtons()
                updateButtonsState()
            }
        }
        viewModelScope.launch { plotSettings.uiState.collect { updatePlotState() } }
        viewModelScope.launch {
            sliderSettings.uiState.collect { updateSliderState() }
        }
        viewModelScope.launch {
            widgetsSource.updates.collect {
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
                    plotSettings.onAction(action.action)
                    updatePlotState()
                }
            }
            is V3SensorsAction.SliderAction -> {
                if (action.action.parameterKey in visibleSliderKeys(_uiState.value.widgets)) {
                    sliderSettings.onAction(action.action)
                    updateSliderState()
                }
            }
        }
    }

    private fun refreshWidgets() {
        val snapshot = widgetsSource.snapshot()
        val previous = _uiState.value
        if (deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            visibleSliderKeys(previous.widgets) != visibleSliderKeys(snapshot.widgets)
        ) {
            sliderSettings.setActive(false)
        }
        if (deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            previous.widgets.filterIsInstance<V3SensorsWidget.Plot>() != snapshot.widgets.filterIsInstance<V3SensorsWidget.Plot>()
        ) plotSettings.setActive(false)
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
        sliderSettings.setActive(isViewAttached && state.deviceProfile != V3DeviceProfile.NOT_V3 &&
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
        plotSettings.setActive(isViewAttached && screen.deviceProfile != V3DeviceProfile.NOT_V3 &&
            screen.widgets.any { it is V3SensorsWidget.Plot })
        updatePlotState()
    }

    private fun updatePlotState() {
        _uiState.update { it.copy(plot = plotSettings.uiState.value.takeIf { _ ->
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
                movements.isNotEmpty() && commandsRepository.interactionEnabled.value,
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
}
