package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3UserFirmwareViewModel(
    private val observeUpdates: ObserveUserFirmwareUpdatesUseCaseV3,
    private val refreshEnvironment: RefreshUserFirmwareEnvironmentUseCaseV3,
    private val startUpdate: StartUserFirmwareUpdateUseCaseV3,
    private val postponeUpdate: PostponeUserFirmwareUpdateUseCaseV3,
    private val acknowledgeCompletion: AcknowledgeUserFirmwareCompletionUseCaseV3,
    private val closeUpdates: CloseUserFirmwareUpdatesUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3UserFirmwareUiState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null

    fun onAction(action: V3UserFirmwareAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3UserFirmwareAction.ViewCreated -> {
                if (observation != null) return
                observation = viewModelScope.launch {
                    observeUpdates().collect { update ->
                        state.value = V3UserFirmwareUiState(
                            // BLE preparation and verification stay inside the same progress dialog.
                            phase = if (update.phase in setOf("preparing", "verifying")) "updating" else update.phase,
                            boardNumber = update.boardNumber,
                            boardCount = update.boardCount,
                            progress = update.progress,
                            isVisible = update.blocksInteraction,
                        )
                    }
                }
            }
            V3UserFirmwareAction.ViewDestroyed -> closeSession()
            else -> if (observation != null) when (action) {
                V3UserFirmwareAction.ViewResumed -> refreshEnvironment()
                V3UserFirmwareAction.InstallClicked -> startUpdate()
                V3UserFirmwareAction.RemindLaterClicked -> postponeUpdate()
                V3UserFirmwareAction.CompletionAcknowledged -> acknowledgeCompletion()
                else -> Unit
            }
        }
    }

    private fun closeSession() {
        observation?.cancel()
        observation = null
        // Close synchronously before the Activity releases its BLE controller.
        closeUpdates()
        state.value = V3UserFirmwareUiState()
    }

    override fun onCleared() {
        closeSession()
        super.onCleared()
    }
}
