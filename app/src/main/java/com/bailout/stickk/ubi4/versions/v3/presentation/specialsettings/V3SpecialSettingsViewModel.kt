package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.EditToggleSliderUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SendToggleSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.V3ToggleSliderAction
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsController
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class V3SpecialSettingsViewModel(
    repository: V3DeviceSettingsRepository,
    setSliderValue: SetSliderValueUseCaseV3,
    private val widgetsSource: V3SpecialSettingsWidgetsSource,
    private val toggleSliderRepository: V3ToggleSliderSettingsRepository,
    private val editToggleSlider: EditToggleSliderUseCaseV3,
    private val sendToggleSliderValue: SendToggleSliderValueUseCaseV3,
) : ViewModel() {
    companion object {
        val toggleSliderParameterKeys: Set<String> = setOf(
            P_KEY_EMG_CHANGE_GESTURE, P_KEY_EMG_MOVEMENT_LOCK, P_KEY_SCREEN_TIMEOUT,
        )
    }

    private val sliderSettings = V3SliderSettingsController(
        repository = repository,
        setSliderValue = setSliderValue,
        sliderParameterKeys = setOf(P_KEY_SPEED_SETTINGS, P_KEY_FORCE_SETTINGS, P_KEY_EMG_MAX_GAIN_VALUE),
        scope = viewModelScope,
    )
    private val initialWidgets = widgetsSource.snapshot(V3SpecialSettingsSection.PROSTHESIS)
    private var deviceAddress = initialWidgets.deviceAddress
    private val _uiState = MutableStateFlow(
        V3SpecialSettingsUiState(
            selectedSection = V3SpecialSettingsSection.PROSTHESIS,
            sliders = sliderSettings.uiState.value.sliders,
            deviceProfile = initialWidgets.deviceProfile,
            widgets = initialWidgets.widgets,
            toggleSliders = toggleSliderParameterKeys.associateWith { key ->
                ToggleSliderUiStateV3(
                    value = toggleSliderRepository.getToggleSliderValue(key) ?: V3ToggleSliderValue(),
                    allowedTimeRange = V3ToggleSliderSettingsRules.allowedTimeRange(key),
                )
            },
        )
    )
    val uiState = _uiState.asStateFlow()
    private var isViewAttached = false
    private var activeToggleSliderKeys = emptySet<String>()
    private val pendingToggleSliderWrites = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            sliderSettings.uiState.collect { sliders ->
                _uiState.update { it.copy(sliders = sliders.sliders) }
            }
        }
        viewModelScope.launch {
            widgetsSource.updates.collect { refreshWidgets(_uiState.value.selectedSection) }
        }
        toggleSliderParameterKeys.forEach { key ->
            viewModelScope.launch {
                toggleSliderRepository.observeToggleSliderValue(key).collect { value ->
                    updateToggleSlider(key) { state ->
                        val next = value ?: V3ToggleSliderValue()
                        state.copy(value = next, animateValueChange = state.value != next)
                    }
                }
            }
        }
        viewModelScope.launch {
            toggleSliderRepository.toggleSliderInteractionEnabled.collect { enabled ->
                if (!enabled) cancelAllScheduledToggleSliderWrites()
                updateToggleSliderActivity()
            }
        }
    }

    @MainThread
    fun onAction(action: V3SpecialSettingsAction) {
        when (action) {
            V3SpecialSettingsAction.ViewAttached -> {
                isViewAttached = true
                refreshWidgets(_uiState.value.selectedSection)
            }
            V3SpecialSettingsAction.ViewDetached -> {
                isViewAttached = false
                updateSliderActivity()
                updateToggleSliderActivity()
            }
            is V3SpecialSettingsAction.SettingsSectionSelected -> {
                refreshWidgets(action.section)
            }
            is V3SpecialSettingsAction.SliderAction -> {
                if (action.action.parameterKey in visibleSliderKeys(_uiState.value.widgets)) {
                    sliderSettings.onAction(action.action)
                }
            }
            is V3SpecialSettingsAction.ToggleSliderAction -> onToggleSliderAction(action.action)
        }
    }

    private fun refreshWidgets(section: V3SpecialSettingsSection) {
        val snapshot = widgetsSource.snapshot(section)
        val previous = _uiState.value
        val deviceChanged = snapshot.deviceAddress != deviceAddress || snapshot.deviceProfile != previous.deviceProfile
        if (deviceChanged ||
            visibleSliderKeys(snapshot.widgets) != visibleSliderKeys(previous.widgets)
        ) {
            sliderSettings.setActive(false)
        }
        if (deviceChanged) {
            cancelAllScheduledToggleSliderWrites()
            activeToggleSliderKeys = emptySet()
        }
        deviceAddress = snapshot.deviceAddress
        _uiState.update { it.copy(selectedSection = section, deviceProfile = snapshot.deviceProfile, widgets = snapshot.widgets) }
        updateSliderActivity()
        updateToggleSliderActivity()
    }

    private fun visibleSliderKeys(widgets: List<V3SpecialSettingsWidget>) =
        widgets.filterIsInstance<V3SpecialSettingsWidget.Slider>().map { it.info.key }.toSet()

    private fun updateSliderActivity() {
        val state = _uiState.value
        sliderSettings.setActive(
            isViewAttached && state.selectedSection == V3SpecialSettingsSection.PROSTHESIS &&
                state.deviceProfile != V3DeviceProfile.NOT_V3 && visibleSliderKeys(state.widgets).isNotEmpty()
        )
        _uiState.update { it.copy(sliders = sliderSettings.uiState.value.sliders) }
    }

    private fun updateToggleSliderActivity() {
        val state = _uiState.value
        val keys = if (isViewAttached && state.selectedSection == V3SpecialSettingsSection.PROSTHESIS &&
            state.deviceProfile != V3DeviceProfile.NOT_V3
        ) {
            state.widgets.filterIsInstance<V3SpecialSettingsWidget.ToggleSlider>()
                .map { it.info.key }.toSet().intersect(toggleSliderParameterKeys)
        } else emptySet()
        (activeToggleSliderKeys - keys).forEach { pendingToggleSliderWrites.remove(it)?.cancel() }
        val restoredValues = (keys - activeToggleSliderKeys).associateWith {
            toggleSliderRepository.getToggleSliderValue(it) ?: V3ToggleSliderValue()
        }
        activeToggleSliderKeys = keys
        val interactionEnabled = toggleSliderRepository.toggleSliderInteractionEnabled.value
        _uiState.update { current ->
            current.copy(toggleSliders = current.toggleSliders.mapValues { (key, slider) ->
                slider.copy(
                    value = restoredValues[key] ?: slider.value,
                    isInteractionEnabled = key in keys && interactionEnabled,
                    animateValueChange = if (key in restoredValues || key !in keys) false else slider.animateValueChange,
                )
            })
        }
    }

    private fun onToggleSliderAction(action: V3ToggleSliderAction) {
        val key = action.parameterKey
        if (!canChangeToggleSlider(key)) return
        val state = _uiState.value.toggleSliders.getValue(key)
        when (action) {
            is V3ToggleSliderAction.ToggleSliderValueChanged -> {
                if (!state.isSliderEnabled) return
                updateToggleSlider(key) { it.copy(
                    value = it.value.copy(timeTenths = action.timeTenths.coerceIn(it.allowedTimeRange)),
                    animateValueChange = false,
                ) }
            }
            is V3ToggleSliderAction.ToggleSliderChangeCommitted -> {
                applyToggleSliderEdit(key, editToggleSlider.setTime(key, action.timeTenths.coerceIn(state.allowedTimeRange)))
            }
            is V3ToggleSliderAction.ToggleSliderStepClicked -> {
                if (action.step != -1 && action.step != 1) return
                val value = (state.value.timeTenths.toLong() + action.step)
                    .coerceIn(state.allowedTimeRange.first.toLong(), state.allowedTimeRange.last.toLong()).toInt()
                applyToggleSliderEdit(key, editToggleSlider.setTime(key, value))
            }
            is V3ToggleSliderAction.ToggleSliderEnabledChanged -> {
                applyToggleSliderEdit(key, editToggleSlider.setEnabled(key, action.enabled))
            }
        }
    }

    private fun applyToggleSliderEdit(key: String, value: V3ToggleSliderValue?) {
        if (value == null) return
        updateToggleSlider(key) { it.copy(value = value, animateValueChange = false) }
        pendingToggleSliderWrites.remove(key)?.cancel()
        pendingToggleSliderWrites[key] = viewModelScope.launch {
            delay(300)
            pendingToggleSliderWrites.remove(key)
            if (canChangeToggleSlider(key)) sendToggleSliderValue(key)
        }
    }

    private fun canChangeToggleSlider(key: String) =
        key in activeToggleSliderKeys && toggleSliderRepository.toggleSliderInteractionEnabled.value

    private fun updateToggleSlider(key: String, transform: (ToggleSliderUiStateV3) -> ToggleSliderUiStateV3) {
        _uiState.update { state ->
            state.copy(toggleSliders = state.toggleSliders + (key to transform(state.toggleSliders.getValue(key))))
        }
    }

    private fun cancelAllScheduledToggleSliderWrites() {
        pendingToggleSliderWrites.values.forEach { it.cancel() }
        pendingToggleSliderWrites.clear()
    }
}
