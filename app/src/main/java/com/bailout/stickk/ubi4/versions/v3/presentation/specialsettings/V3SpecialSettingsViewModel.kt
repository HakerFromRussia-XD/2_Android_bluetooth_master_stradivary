package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfilesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.RenameSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfileNameEditorUiState
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.GetSettingsProfilesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.CreateSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfileOperation
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.SelectSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfilesUiState

class V3SpecialSettingsViewModel(
    repository: V3DeviceSettingsRepository,
    setSliderValue: SetSliderValueUseCaseV3,
    private val widgetsSource: V3SpecialSettingsWidgetsSource,
    private val toggleSliderRepository: V3ToggleSliderSettingsRepository,
    private val editToggleSlider: EditToggleSliderUseCaseV3,
    private val sendToggleSliderValue: SendToggleSliderValueUseCaseV3,
    private val spinnerRepository: V3SpinnerSettingsRepository,
    private val setSpinnerValue: SetSpinnerValueUseCaseV3,
    private val settingsProfilesRepository: V3SettingsProfilesRepository,
    private val getSettingsProfiles: GetSettingsProfilesUseCaseV3,
    private val selectSettingsProfile: SelectSettingsProfileUseCaseV3,
    private val createSettingsProfile: CreateSettingsProfileUseCaseV3,
    private val renameSettingsProfile: RenameSettingsProfileUseCaseV3,
) : ViewModel() {
    companion object {
        val spinnerParameterKeys: Set<String> = setOf(P_KEY_HAND_CONTROL_MODE, P_KEY_GESTURE_CHANGE_MODE)
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
    private var spinnerValues = spinnerParameterKeys.associateWith(spinnerRepository::getSpinnerValue)
    private val _uiState = MutableStateFlow(
        V3SpecialSettingsUiState(
            selectedSection = V3SpecialSettingsSection.PROSTHESIS,
            sliders = sliderSettings.uiState.value.sliders,
            deviceProfile = initialWidgets.deviceProfile,
            widgets = initialWidgets.widgets,
            spinners = spinnerParameterKeys.associateWith { SpinnerUiStateV3(selectedIndex = null) },
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
    private var settingsProfilesSerial: String? = null
    private var settingsProfilesLoadJob: Job? = null
    private var settingsProfilesRequest = 0L
    private var settingsProfileOperationJob: Job? = null
    private var settingsProfileOperationRequest = 0L
    private var settingsProfileNameRequest = 0L
    private val isApplyingProfile get() = _uiState.value.settingsProfiles?.operation.let {
        it == V3SettingsProfileOperation.SELECT || it == V3SettingsProfileOperation.CREATE
    }
    private val isProfileOperationRunning get() = _uiState.value.settingsProfiles?.operation != null

    init {
        viewModelScope.launch {
            settingsProfilesRepository.updates.collect { updateSettingsProfiles(reload = true) }
        }
        updateSpinnerState()
        spinnerParameterKeys.forEach { key ->
            viewModelScope.launch {
                spinnerRepository.observeSpinnerValue(key).collect { value ->
                    spinnerValues = spinnerValues + (key to value)
                    updateSpinnerState()
                }
            }
        }
        viewModelScope.launch {
            spinnerRepository.spinnerInteractionEnabled.collect { enabled ->
                if (!enabled) cancelSettingsProfileOperation()
                updateSpinnerState()
                updateSettingsProfiles()
            }
        }
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
                refreshWidgets(_uiState.value.selectedSection, reloadProfiles = true)
            }
            V3SpecialSettingsAction.ViewDetached -> {
                isViewAttached = false
                cancelSettingsProfileOperation()
                updateSliderActivity()
                updateToggleSliderActivity()
                updateSpinnerState()
                updateSettingsProfiles()
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
            is V3SpecialSettingsAction.SpinnerAction -> onSpinnerAction(action.action)
            is V3SpecialSettingsAction.SettingsProfileSelected -> onSettingsProfileSelected(action.profileId)
            V3SpecialSettingsAction.SettingsProfileCreateRequested -> onSettingsProfileCreateRequested()
            is V3SpecialSettingsAction.SettingsProfileRenameRequested -> onSettingsProfileRenameRequested(action.profileId)
            is V3SpecialSettingsAction.SettingsProfileNameSubmitted -> onSettingsProfileNameSubmitted(action.requestId, action.name)
            is V3SpecialSettingsAction.SettingsProfileNameDismissed -> dismissSettingsProfileName(action.requestId)
        }
    }

    private fun refreshWidgets(section: V3SpecialSettingsSection, reloadProfiles: Boolean = false) {
        val snapshot = widgetsSource.snapshot(section)
        val previous = _uiState.value
        val deviceChanged = snapshot.deviceAddress != deviceAddress || snapshot.deviceProfile != previous.deviceProfile
        if (deviceChanged ||
            visibleSliderKeys(snapshot.widgets) != visibleSliderKeys(previous.widgets)
        ) {
            sliderSettings.setActive(false)
        }
        if (deviceChanged) {
            cancelSettingsProfileOperation()
            cancelAllScheduledToggleSliderWrites()
            activeToggleSliderKeys = emptySet()
        }
        deviceAddress = snapshot.deviceAddress
        _uiState.update { it.copy(selectedSection = section, deviceProfile = snapshot.deviceProfile, widgets = snapshot.widgets) }
        updateSliderActivity()
        updateToggleSliderActivity()
        spinnerValues = spinnerParameterKeys.associateWith(spinnerRepository::getSpinnerValue)
        updateSpinnerState()
        if (deviceChanged) clearSettingsProfiles()
        updateSettingsProfiles(reloadProfiles || previous.widgets != snapshot.widgets || previous.selectedSection != section)
    }

    private fun clearSettingsProfiles() {
        settingsProfilesRequest++
        settingsProfilesLoadJob?.cancel()
        settingsProfilesLoadJob = null
        settingsProfilesSerial = null
        _uiState.update { it.copy(settingsProfiles = null) }
    }

    private fun updateSettingsProfiles(reload: Boolean = false) {
        val screen = _uiState.value
        if (screen.deviceProfile == V3DeviceProfile.NOT_V3 ||
            screen.selectedSection != V3SpecialSettingsSection.PROSTHESIS ||
            screen.widgets.none { it is V3SpecialSettingsWidget.SettingsProfile }
        ) {
            cancelSettingsProfileOperation()
            clearSettingsProfiles()
            return
        }
        val serial = settingsProfilesRepository.currentSerial()
        if (serial != settingsProfilesSerial) {
            cancelSettingsProfileOperation()
            clearSettingsProfiles()
        }
        if (isProfileOperationRunning) return
        val missing = _uiState.value.settingsProfiles == null
        settingsProfilesSerial = serial
        val current = _uiState.value.settingsProfiles ?: V3SettingsProfilesUiState()
        if (!isViewAttached) {
            settingsProfilesRequest++
            settingsProfilesLoadJob?.cancel()
            settingsProfilesLoadJob = null
            _uiState.update { it.copy(settingsProfiles = current.copy(isEnabled = false, isLoading = false, nameEditor = null)) }
            return
        }
        _uiState.update { it.copy(settingsProfiles = current.copy(
            isEnabled = !current.isLoading && !current.loadFailed && current.profiles.isNotEmpty() && spinnerRepository.spinnerInteractionEnabled.value,
            nameEditor = current.nameEditor.takeIf { spinnerRepository.spinnerInteractionEnabled.value },
        )) }
        if (!reload && !missing) return
        settingsProfilesLoadJob?.cancel()
        val request = ++settingsProfilesRequest
        _uiState.update { it.copy(settingsProfiles = current.copy(isLoading = true, isEnabled = false, loadFailed = false, nameEditor = null)) }
        settingsProfilesLoadJob = viewModelScope.launch {
            try {
                val profiles = getSettingsProfiles(serial)
                if (!isActive || request != settingsProfilesRequest || serial != settingsProfilesRepository.currentSerial()) return@launch
                settingsProfilesRepository.cacheSelection(serial, profiles)
                _uiState.update { it.copy(settingsProfiles = V3SettingsProfilesUiState(
                    profiles = profiles.profiles,
                    activeProfileId = profiles.activeProfileId,
                    canCreate = profiles.canCreate,
                    isEnabled = isViewAttached && spinnerRepository.spinnerInteractionEnabled.value,
                    isLoading = false,
                    failedOperation = _uiState.value.settingsProfiles?.failedOperation,
                )) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!isActive || request != settingsProfilesRequest || serial != settingsProfilesRepository.currentSerial()) return@launch
                _uiState.update { state -> state.copy(settingsProfiles = state.settingsProfiles?.copy(
                    isLoading = false, isEnabled = false, loadFailed = true,
                )) }
                platformLog("V3SpecialSettingsViewModel", "Cannot read settings profiles: ${error.message}")
            }
        }
    }

    private fun onSettingsProfileSelected(profileId: Int) {
        if (_uiState.value.settingsProfiles?.profiles?.none { it.profileId == profileId } != false) return
        runSettingsProfileOperation(V3SettingsProfileOperation.SELECT) { serial -> selectSettingsProfile(serial, profileId) }
    }

    private fun onSettingsProfileCreateRequested() {
        if (_uiState.value.settingsProfiles?.canCreate != true) return
        runSettingsProfileOperation(V3SettingsProfileOperation.CREATE) { serial -> createSettingsProfile(serial) }
    }

    private fun onSettingsProfileRenameRequested(profileId: Int) {
        val state = _uiState.value.settingsProfiles ?: return
        if (!isViewAttached || isProfileOperationRunning || !state.isEnabled || !spinnerRepository.spinnerInteractionEnabled.value) return
        if (settingsProfilesSerial != settingsProfilesRepository.currentSerial()) {
            updateSettingsProfiles(reload = true)
            return
        }
        val profile = state.profiles.firstOrNull { it.profileId == profileId } ?: return
        if (state.nameEditor?.profile?.profileId == profileId) return
        _uiState.update { it.copy(settingsProfiles = state.copy(
            nameEditor = V3SettingsProfileNameEditorUiState(++settingsProfileNameRequest, profile),
        )) }
    }

    private fun onSettingsProfileNameSubmitted(requestId: Long, name: String) {
        val editor = _uiState.value.settingsProfiles?.nameEditor ?: return
        if (editor.requestId != requestId) return
        runSettingsProfileOperation(V3SettingsProfileOperation.RENAME) { serial ->
            renameSettingsProfile(serial, editor.profile.profileId, name)
        }
    }

    private fun dismissSettingsProfileName(requestId: Long) {
        _uiState.update { state ->
            if (state.settingsProfiles?.nameEditor?.requestId != requestId) state
            else state.copy(settingsProfiles = state.settingsProfiles.copy(nameEditor = null))
        }
    }

    private fun runSettingsProfileOperation(
        operation: V3SettingsProfileOperation,
        execute: suspend (String) -> Unit,
    ) {
        val state = _uiState.value.settingsProfiles ?: return
        if (!isViewAttached || isProfileOperationRunning || !state.isEnabled || !spinnerRepository.spinnerInteractionEnabled.value) return
        val serial = settingsProfilesSerial ?: return
        if (serial != settingsProfilesRepository.currentSerial()) {
            updateSettingsProfiles(reload = true)
            return
        }
        settingsProfilesRequest++
        settingsProfilesLoadJob?.cancel()
        val request = ++settingsProfileOperationRequest
        _uiState.update { it.copy(settingsProfiles = state.copy(operation = operation, isEnabled = false, failedOperation = null, nameEditor = null)) }
        updateSliderActivity()
        updateToggleSliderActivity()
        updateSpinnerState()
        settingsProfileOperationJob = viewModelScope.launch {
            try {
                execute(serial)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (isActive && request == settingsProfileOperationRequest && serial == settingsProfilesRepository.currentSerial()) {
                    _uiState.update { it.copy(settingsProfiles = it.settingsProfiles?.copy(failedOperation = operation)) }
                    platformLog("V3SpecialSettingsViewModel", "Settings profile operation $operation failed: ${error.message}")
                }
            } finally {
                if (isActive && request == settingsProfileOperationRequest) {
                    _uiState.update { it.copy(settingsProfiles = it.settingsProfiles?.copy(operation = null)) }
                    updateSliderActivity()
                    updateToggleSliderActivity()
                    updateSpinnerState()
                    updateSettingsProfiles(reload = true)
                }
            }
        }
    }

    private fun cancelSettingsProfileOperation() {
        if (!isProfileOperationRunning) return
        settingsProfileOperationRequest++
        settingsProfileOperationJob?.cancel()
        settingsProfileOperationJob = null
        _uiState.update { it.copy(settingsProfiles = it.settingsProfiles?.copy(operation = null)) }
        updateSliderActivity()
        updateToggleSliderActivity()
        updateSpinnerState()
    }

    private fun updateSpinnerState() {
        val state = _uiState.value
        val widgets = state.widgets.filterIsInstance<V3SpecialSettingsWidget.Spinner>().associateBy { it.info.key }
        val canSelect = !isApplyingProfile && isViewAttached && state.selectedSection == V3SpecialSettingsSection.PROSTHESIS &&
            state.deviceProfile != V3DeviceProfile.NOT_V3 && spinnerRepository.spinnerInteractionEnabled.value
        val spinners = spinnerParameterKeys.associateWith { key ->
            val widget = widgets[key]
            val selectedIndex = if (widget == null || widget.options.isEmpty()) null else {
                (spinnerValues[key] ?: widget.initialSelectedIndex).coerceIn(widget.options.indices)
            }
            SpinnerUiStateV3(selectedIndex = selectedIndex, isEnabled = canSelect && selectedIndex != null)
        }
        _uiState.update { it.copy(spinners = spinners) }
    }

    private fun onSpinnerAction(action: V3SpinnerAction) {
        when (action) {
            is V3SpinnerAction.SpinnerValueSelected -> {
                val state = _uiState.value.spinners[action.parameterKey] ?: return
                if (!state.isEnabled || !spinnerRepository.spinnerInteractionEnabled.value) return
                val widget = _uiState.value.widgets.filterIsInstance<V3SpecialSettingsWidget.Spinner>()
                    .firstOrNull { it.info.key == action.parameterKey } ?: return
                if (action.value !in widget.options.indices || action.value !in V3SpinnerSettingsRules.allowedValues(action.parameterKey)) return
                setSpinnerValue(action.parameterKey, action.value)
                spinnerValues = spinnerValues + (action.parameterKey to spinnerRepository.getSpinnerValue(action.parameterKey))
                updateSpinnerState()
            }
        }
    }

    private fun visibleSliderKeys(widgets: List<V3SpecialSettingsWidget>) =
        widgets.filterIsInstance<V3SpecialSettingsWidget.Slider>().map { it.info.key }.toSet()

    private fun updateSliderActivity() {
        val state = _uiState.value
        sliderSettings.setActive(
            !isApplyingProfile && isViewAttached && state.selectedSection == V3SpecialSettingsSection.PROSTHESIS &&
                state.deviceProfile != V3DeviceProfile.NOT_V3 && visibleSliderKeys(state.widgets).isNotEmpty()
        )
        _uiState.update { it.copy(sliders = sliderSettings.uiState.value.sliders) }
    }

    private fun updateToggleSliderActivity() {
        val state = _uiState.value
        val keys = if (!isApplyingProfile && isViewAttached && state.selectedSection == V3SpecialSettingsSection.PROSTHESIS &&
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
