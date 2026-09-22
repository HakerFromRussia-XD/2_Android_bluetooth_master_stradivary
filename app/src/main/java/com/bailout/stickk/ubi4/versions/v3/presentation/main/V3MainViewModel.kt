package com.bailout.stickk.ubi4.versions.v3.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.main.ManageMainConnectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.GetMainVisibleDisplaysUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.ObserveMainUpdatesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainUpdate
import com.bailout.stickk.ubi4.versions.v3.domain.main.UpdateMainDeviceIdentityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.ScheduleProfileUploadUseCaseV3
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3MainViewModel(
    private val getVisibleDisplays: GetMainVisibleDisplaysUseCaseV3,
    private val observeUpdates: ObserveMainUpdatesUseCaseV3,
    private val updateDeviceIdentity: UpdateMainDeviceIdentityUseCaseV3,
    private val scheduleProfileUpload: ScheduleProfileUploadUseCaseV3,
    private val manageConnection: ManageMainConnectionUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3MainUiState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    private var attached = false

    fun onAction(action: V3MainAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3MainAction.ViewCreated -> {
                scheduleProfileUpload.resetRequest()
                observation?.cancel()
                attached = true
                // Device identity is initialized before the screen subscriptions are bound.
                state.update { V3MainUiState(deviceIdentity = it.deviceIdentity) }
                refreshNavigation()
                // Match the previous Activity subscriptions: keep observing through STOP.
                observation = viewModelScope.launch {
                    launch {
                        observeUpdates.deviceIdentity().collect { identity ->
                            ensureActive()
                            state.update { it.copy(deviceIdentity = identity) }
                        }
                    }
                    observeUpdates().collect { update ->
                        ensureActive()
                        when (update) {
                            V3MainUpdate.WidgetsChanged -> refreshNavigation()
                            is V3MainUpdate.BatteryChanged -> state.update { it.copy(batteryPercent = update.percent) }
                        }
                    }
                }
            }
            V3MainAction.ViewResumed -> scheduleProfileUpload.cancelPendingUpload()
            is V3MainAction.ViewStopped -> scheduleProfileUpload(
                action.openingScanAfterDisconnect, action.deviceConnected, action.language,
            )
            V3MainAction.NavigationRefreshRequested -> if (attached) refreshNavigation()
            V3MainAction.DeviceConnected -> {
                val identity = updateDeviceIdentity.loadConnectedDevice()
                state.update { it.copy(deviceIdentity = identity) }
                manageConnection.rememberLastConnection()
            }
            V3MainAction.WidgetStorageInitializationRequested -> manageConnection.initializeWidgetStorage()
            V3MainAction.SavedConnectionRestoreRequested -> manageConnection.restoreSavedConnection()
            V3MainAction.LastConnectionResetRequested -> manageConnection.resetLastConnection()
            is V3MainAction.SerialNumberReceived -> {
                val identity = updateDeviceIdentity.applySerialNumber(action.serial)
                state.update { it.copy(deviceIdentity = identity) }
            }
            V3MainAction.ViewDestroyed -> {
                attached = false
                observation?.cancel()
                observation = null
            }
        }
    }

    private fun refreshNavigation() {
        val displays = getVisibleDisplays()
        state.update { it.copy(visibleDisplays = displays, navigationRevision = it.navigationRevision + 1) }
    }
}
