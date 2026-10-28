package com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.GetInitializationCommandsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ObserveInitializationCommandsAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.SendInitializationCommandUseCaseV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3InitializationCommandsViewModel(
    private val getCommands: GetInitializationCommandsUseCaseV3,
    private val observeAvailability: ObserveInitializationCommandsAvailabilityUseCaseV3,
    private val sendCommand: SendInitializationCommandUseCaseV3,
    private val getDeviceSession: GetDeviceSessionUseCaseV3,
    private val observeDeviceSessionChanges: ObserveDeviceSessionChangesUseCaseV3,
) : ViewModel() {
    private var session = getDeviceSession()
    private var isAttached = false
    private var isAvailable = false
    private val _uiState = MutableStateFlow(V3InitializationCommandsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { observeAvailability().collect { isAvailable = it; refresh() } }
        viewModelScope.launch { observeDeviceSessionChanges().collect { refresh() } }
    }

    fun onAction(action: V3InitializationCommandsAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3InitializationCommandsAction.ViewAttached -> { isAttached = true; refresh() }
            V3InitializationCommandsAction.ViewDetached -> { isAttached = false; refresh() }
            is V3InitializationCommandsAction.CommandClicked -> enqueue(action.command)
        }
    }

    private fun refresh() {
        val currentSession = getDeviceSession()
        val previous = _uiState.value.commands.takeIf { currentSession.address == session.address && currentSession.profile == session.profile }
        session = currentSession
        val enabled = isAttached && isAvailable
        val packets = if (session.profile == V3DeviceProfile.NOT_V3) emptyList() else getCommands()
        _uiState.value = V3InitializationCommandsUiState(packets.mapIndexed { index, packet ->
            // Keep the exact date/time packet visible after it has been queued.
            val lastQueued = previous?.firstOrNull { it.packet.command == packet.command && it.isLastQueued }
            V3InitializationCommandUiState(index + 1, lastQueued?.packet ?: packet, enabled, lastQueued != null)
        })
    }

    private fun enqueue(command: V3InitializationCommand) {
        if (!isAttached || !isAvailable) return
        val currentSession = getDeviceSession()
        if (session.address != currentSession.address || session.profile != currentSession.profile) { refresh(); return }
        if (_uiState.value.commands.none { it.packet.command == command && it.isEnabled }) return
        val packet = sendCommand(session.address, command) ?: return
        _uiState.value = V3InitializationCommandsUiState(_uiState.value.commands.map {
            it.copy(packet = if (it.packet.command == command) packet else it.packet, isLastQueued = it.packet.command == command)
        })
    }
}
