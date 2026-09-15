package com.bailout.stickk.ubi4.versions.v3.presentation.service

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsController
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3ServiceViewModel(
    repository: V3DeviceSettingsRepository,
    setSliderValue: SetSliderValueUseCaseV3,
    private val widgetsSource: V3ServiceWidgetsSource,
    private val spinnerRepository: V3SpinnerSettingsRepository,
    private val setSpinnerValue: SetSpinnerValueUseCaseV3,
    private val roleRepository: V3DeviceRoleRepository,
    private val restoreDeviceRole: RestoreDeviceRoleUseCaseV3,
    private val changeDeviceRole: ChangeDeviceRoleUseCaseV3,
    private val deviceInfoRepository: V3DeviceInfoRepository,
    private val editDeviceInfoText: EditDeviceInfoTextUseCaseV3,
    private val setDeviceInfoText: SetDeviceInfoTextUseCaseV3,
) : ViewModel() {
    companion object {
        val spinnerParameterKeys: Set<String> = setOf(P_KEY_EMG_CONTROL_MODE, P_KEY_LEFT_RIGHT_HAND)
    }

    private val sliderSettings = V3SliderSettingsController(repository, setSliderValue,
        setOf(P_KEY_GLOBAL_THUMB_CLOSED_POSITION, P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION), viewModelScope)
    private val initialSnapshot = widgetsSource.snapshot()
    private var deviceAddress = initialSnapshot.deviceAddress
    private var isViewAttached = false
    private var nextPinRequestId = 0L
    private var nextTextFeedbackId = 0L
    private val _uiState = MutableStateFlow(V3ServiceUiState(
        initialSnapshot.deviceProfile, initialSnapshot.widgets,
        sliderSettings.uiState.value.sliders.filterKeys { it in visibleSliderKeys(initialSnapshot.widgets) },
        initialSnapshot.animationsEnabled,
    ))
    val uiState = _uiState.asStateFlow()

    init {
        updateSpinnerState()
        updateRoleState()
        updateTextInputState()
        viewModelScope.launch { sliderSettings.uiState.collect { updateSliderState() } }
        viewModelScope.launch { widgetsSource.updates.collect { refreshWidgets() } }
        spinnerParameterKeys.forEach { key ->
            viewModelScope.launch {
                spinnerRepository.observeSpinnerValue(key).collect { updateSpinnerState() }
            }
        }
        viewModelScope.launch {
            spinnerRepository.spinnerInteractionEnabled.collect { updateSpinnerState() }
        }
        viewModelScope.launch {
            roleRepository.interactionEnabled.collect { updateRoleState() }
        }
        viewModelScope.launch {
            deviceInfoRepository.interactionEnabled.collect { updateTextInputState() }
        }
    }

    @MainThread
    fun onAction(action: V3ServiceAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3ServiceAction.ViewAttached -> {
                isViewAttached = true
                refreshWidgets()
            }
            V3ServiceAction.ViewDetached, V3ServiceAction.ViewDestroyed -> {
                isViewAttached = false
                updateSliderActivity()
                updateSpinnerState()
                updateRoleState(clearPin = true)
                updateTextInputState(resetDrafts = action == V3ServiceAction.ViewDestroyed)
            }
            is V3ServiceAction.SliderAction -> {
                if (action.action.parameterKey in visibleSliderKeys(_uiState.value.widgets)) {
                    sliderSettings.onAction(action.action)
                    updateSliderState()
                }
            }
            is V3ServiceAction.SpinnerAction -> onSpinnerAction(action.action)
            is V3ServiceAction.RoleSelected -> onRoleSelected(action.role)
            is V3ServiceAction.RolePinSubmitted -> onRolePinSubmitted(action)
            is V3ServiceAction.RolePinCancelled -> {
                if (_uiState.value.role?.pinRequest?.id == action.requestId) updateRoleState(clearPin = true)
            }
            is V3ServiceAction.RolePinFeedbackShown -> {
                val role = _uiState.value.role
                if (role?.pinFeedback?.requestId == action.requestId) {
                    _uiState.update { it.copy(role = role.copy(pinFeedback = null)) }
                }
            }
            is V3ServiceAction.TextInputChanged -> {
                if (canUseTextInput(action.field)) editTextInput(action.field, action.text)
            }
            is V3ServiceAction.TextInputPrefillRequested -> {
                if (canUseTextInput(action.field)) {
                    deviceInfoRepository.getTextForInput(action.field)?.takeUnless { it.isBlank() }?.let {
                        editTextInput(action.field, it, moveCursorToEnd = true)
                    }
                }
            }
            is V3ServiceAction.TextInputSendClicked -> sendTextInput(action.field)
            is V3ServiceAction.TextInputFeedbackShown -> {
                if (_uiState.value.textInputFeedback?.id == action.id) _uiState.update { it.copy(textInputFeedback = null) }
            }
        }
    }

    private fun refreshWidgets() {
        val snapshot = widgetsSource.snapshot()
        val previous = _uiState.value
        val resetTextDrafts = deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            textWidgets(previous.widgets) != textWidgets(snapshot.widgets)
        val roleContextChanged = deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            roleWidget(previous.widgets) != roleWidget(snapshot.widgets)
        if (deviceAddress != snapshot.deviceAddress || previous.deviceProfile != snapshot.deviceProfile ||
            visibleSliderKeys(previous.widgets) != visibleSliderKeys(snapshot.widgets)
        ) sliderSettings.setActive(false)
        deviceAddress = snapshot.deviceAddress
        _uiState.update { it.copy(deviceProfile = snapshot.deviceProfile, widgets = snapshot.widgets,
            animationsEnabled = snapshot.animationsEnabled) }
        updateSliderActivity()
        updateSpinnerState()
        updateRoleState(clearPin = roleContextChanged)
        updateTextInputState(resetDrafts = resetTextDrafts)
    }

    private fun textWidgets(widgets: List<V3ServiceWidget>) = widgets.filterIsInstance<V3ServiceWidget.TextInput>()

    private fun updateTextInputState(resetDrafts: Boolean = false) {
        val screen = _uiState.value
        val fields = if (screen.deviceProfile == V3DeviceProfile.NOT_V3) emptyList() else textWidgets(screen.widgets).map { it.field }
        val enabled = isViewAttached && deviceInfoRepository.interactionEnabled.value
        val inputs = fields.associateWith {
            (screen.textInputs[it]?.takeUnless { resetDrafts } ?: V3ServiceTextInputUiState()).copy(canSend = enabled)
        }
        _uiState.update { it.copy(textInputs = inputs,
            textInputFeedback = it.textInputFeedback.takeIf { feedback ->
                isViewAttached && !resetDrafts && feedback?.field in fields
            }) }
    }

    private fun canUseTextInput(field: V3DeviceInfoField): Boolean {
        if (!isViewAttached || field !in _uiState.value.textInputs) return false
        val state = _uiState.value
        val snapshot = widgetsSource.snapshot()
        if (deviceAddress != snapshot.deviceAddress || state.deviceProfile != snapshot.deviceProfile ||
            textWidgets(state.widgets).firstOrNull { it.field == field } != textWidgets(snapshot.widgets).firstOrNull { it.field == field }
        ) {
            refreshWidgets()
            return false
        }
        return true
    }

    private fun editTextInput(field: V3DeviceInfoField, text: String, moveCursorToEnd: Boolean = false) {
        val previous = _uiState.value.textInputs[field] ?: return
        val edit = editDeviceInfoText(field, text)
        val state = previous.copy(text = edit.text,
            cursorRevision = previous.cursorRevision + if (moveCursorToEnd || edit.limitReached) 1 else 0)
        _uiState.update { it.copy(textInputs = it.textInputs + (field to state)) }
        if (edit.limitReached) showTextInputFeedback(field, V3TextInputMessage.LIMIT_REACHED)
    }

    private fun sendTextInput(field: V3DeviceInfoField) {
        if (!canUseTextInput(field)) return
        val text = _uiState.value.textInputs[field]?.text ?: return
        val message = when (setDeviceInfoText(field, text)) {
            V3DeviceInfoWriteResult.SENT -> V3TextInputMessage.SENT
            V3DeviceInfoWriteResult.EMPTY -> V3TextInputMessage.ENTER_TEXT
            V3DeviceInfoWriteResult.PREPARATION_FAILED -> V3TextInputMessage.PREPARATION_FAILED
            V3DeviceInfoWriteResult.BLOCKED -> return
        }
        showTextInputFeedback(field, message)
    }

    private fun showTextInputFeedback(field: V3DeviceInfoField, message: V3TextInputMessage) {
        val feedback = V3TextInputFeedback(++nextTextFeedbackId, field, message)
        _uiState.update { it.copy(textInputFeedback = feedback) }
    }

    private fun roleWidget(widgets: List<V3ServiceWidget>) = widgets.filterIsInstance<V3ServiceWidget.Spinner>()
        .firstOrNull { it.parameterKey == P_KEY_DEVICE_ROLE }

    private fun updateRoleState(clearPin: Boolean = false) {
        val screen = _uiState.value
        val previous = screen.role
        val role = if (screen.deviceProfile == V3DeviceProfile.NOT_V3 || roleWidget(screen.widgets) == null) null else {
            val selected = restoreDeviceRole()
            val enabled = isViewAttached && roleRepository.interactionEnabled.value
            val retainPin = !clearPin && enabled && previous?.selectedRole == selected
            V3ServiceRoleUiState(selected, enabled,
                previous?.pinRequest.takeIf { retainPin }, previous?.pinFeedback.takeIf { retainPin })
        }
        _uiState.update { it.copy(role = role) }
    }

    private fun canChangeRole(): Boolean {
        if (!isViewAttached || !roleRepository.interactionEnabled.value) {
            updateRoleState(clearPin = true)
            return false
        }
        val state = _uiState.value
        val role = state.role ?: return false
        val snapshot = widgetsSource.snapshot()
        if (deviceAddress != snapshot.deviceAddress || state.deviceProfile != snapshot.deviceProfile ||
            roleWidget(state.widgets) != roleWidget(snapshot.widgets)
        ) {
            refreshWidgets()
            return false
        }
        if (roleRepository.getSelectedRole() != role.selectedRole) {
            updateRoleState(clearPin = true)
            return false
        }
        return role.isEnabled
    }

    private fun onRoleSelected(selected: V3DeviceRole) {
        if (!canChangeRole()) return
        val role = _uiState.value.role ?: return
        if (role.pinRequest != null) return
        when (changeDeviceRole(selected)) {
            V3DeviceRoleChangeResult.PIN_REQUIRED -> _uiState.update {
                it.copy(role = role.copy(pinRequest = V3RolePinRequest(++nextPinRequestId, selected), pinFeedback = null))
            }
            else -> updateRoleState(clearPin = true)
        }
    }

    private fun onRolePinSubmitted(action: V3ServiceAction.RolePinSubmitted) {
        val request = _uiState.value.role?.pinRequest?.takeIf { it.id == action.requestId } ?: return
        if (!canChangeRole()) return
        val result = changeDeviceRole(request.role, action.pin)
        updateRoleState(clearPin = true)
        val feedback = when (result) {
            V3DeviceRoleChangeResult.APPLIED -> V3RolePinFeedback(request.id, accessGranted = true)
            V3DeviceRoleChangeResult.INVALID_PIN -> V3RolePinFeedback(request.id, accessGranted = false)
            else -> null
        }
        _uiState.update { it.copy(role = it.role?.copy(pinFeedback = feedback)) }
    }

    private fun updateSpinnerState() {
        val screen = _uiState.value
        val canSelect = isViewAttached && spinnerRepository.spinnerInteractionEnabled.value
        val spinners = visibleSpinners(screen).associate { widget ->
            val index = if (widget.options.isEmpty()) null else {
                (spinnerRepository.getSpinnerValue(widget.parameterKey) ?: widget.initialSelectedIndex)
                    .coerceIn(widget.options.indices)
            }
            widget.parameterKey to SpinnerUiStateV3(index, canSelect && index != null)
        }
        _uiState.update { it.copy(spinners = spinners) }
    }

    private fun onSpinnerAction(action: V3SpinnerAction) {
        when (action) {
            is V3SpinnerAction.SpinnerValueSelected -> {
                if (!isViewAttached || !spinnerRepository.spinnerInteractionEnabled.value) return
                val widget = visibleSpinners(_uiState.value).firstOrNull { it.parameterKey == action.parameterKey } ?: return
                // Reject callbacks from the previous device/composition before its update is collected.
                val snapshot = widgetsSource.snapshot()
                if (deviceAddress != snapshot.deviceAddress || _uiState.value.deviceProfile != snapshot.deviceProfile ||
                    widget != snapshot.widgets.filterIsInstance<V3ServiceWidget.Spinner>().firstOrNull { it.parameterKey == action.parameterKey }
                ) {
                    refreshWidgets()
                    return
                }
                if (action.value !in widget.options.indices || action.value !in V3SpinnerSettingsRules.allowedValues(action.parameterKey)) return
                setSpinnerValue(action.parameterKey, action.value)
                updateSpinnerState()
            }
        }
    }

    private fun visibleSpinners(state: V3ServiceUiState): List<V3ServiceWidget.Spinner> {
        if (state.deviceProfile == V3DeviceProfile.NOT_V3) return emptyList()
        return state.widgets.filterIsInstance<V3ServiceWidget.Spinner>().filter {
            it.parameterKey in spinnerParameterKeys &&
                (it.parameterKey != P_KEY_LEFT_RIGHT_HAND || state.deviceProfile == V3DeviceProfile.STANDARD_V3)
        }
    }

    private fun updateSliderActivity() {
        val screen = _uiState.value
        sliderSettings.setActive(isViewAttached && screen.deviceProfile != V3DeviceProfile.NOT_V3 &&
            visibleSliderKeys(screen.widgets).isNotEmpty())
        updateSliderState()
    }

    private fun updateSliderState() {
        val keys = visibleSliderKeys(_uiState.value.widgets)
        val sliders = sliderSettings.uiState.value.sliders.filterKeys { it in keys }
        _uiState.update { it.copy(sliders = sliders) }
    }

    private fun visibleSliderKeys(widgets: List<V3ServiceWidget>): Set<String> =
        widgets.filterIsInstance<V3ServiceWidget.Slider>().map { it.parameterKey }.toSet()
}
