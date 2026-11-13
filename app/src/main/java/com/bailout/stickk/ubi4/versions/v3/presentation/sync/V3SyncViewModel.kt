package com.bailout.stickk.ubi4.versions.v3.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.sync.ObserveSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncEvent
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.ObservePhoneSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3SyncViewModel(
    private val observeSync: ObserveSyncUseCaseV3,
    private val observePhoneSync: ObservePhoneSyncUseCaseV3? = null,
) : ViewModel() {
    private var standardState = V3SyncUiState()
    private var phoneState = observePhoneSync?.invoke()?.value ?: V3PhoneSyncState.Idle
    private val state = MutableStateFlow(V3SyncUiState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    private var phoneObservation: Job? = null

    init { publishState() }

    fun onAction(action: V3SyncAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3SyncAction.ViewCreated -> {
                observation?.cancel()
                phoneObservation?.cancel()
                standardState = V3SyncUiState()
                phoneState = observePhoneSync?.invoke()?.value ?: V3PhoneSyncState.Idle
                publishState()
            }
            V3SyncAction.StartupShown -> {
                standardState = standardState.copy(
                    isShowing = true, isProgressVisible = false, isIndeterminate = false, progress = 0,
                )
                publishState()
            }
            V3SyncAction.ViewAttached -> {
                observation?.cancel()
                phoneObservation?.cancel()
                observePhoneSync?.let { observe ->
                    updatePhoneState(observe().value)
                    phoneObservation = viewModelScope.launch {
                        observe().collect { updatePhoneState(it) }
                    }
                }
                observation = viewModelScope.launch {
                    observeSync().collect { event ->
                        if (isActive) {
                            standardState = when (event) {
                                V3SyncEvent.Ready -> closedState()
                                is V3SyncEvent.Progress -> progressState(event)
                            }
                            publishState()
                        }
                    }
                }
            }
            V3SyncAction.ViewDetached -> {
                observation?.cancel(); observation = null
                phoneObservation?.cancel(); phoneObservation = null
            }
        }
    }

    private fun updatePhoneState(value: V3PhoneSyncState) {
        phoneState = value
        publishState()
    }

    private fun publishState() {
        state.value = standardState.copy(
            mode = if (phoneState == V3PhoneSyncState.Checking || phoneState == V3PhoneSyncState.Starting ||
                phoneState == V3PhoneSyncState.WaitingForUser)
                V3SyncMode.PhoneConfirmation else V3SyncMode.Standard,
            returnToScanning = phoneState == V3PhoneSyncState.Failed,
        )
    }

    private fun closedState(forceChrome: Boolean = false) = V3SyncUiState(
        chromeVisible = if (forceChrome || standardState.chromeVisible == false) true else standardState.chromeVisible,
    )

    private fun progressState(event: V3SyncEvent.Progress): V3SyncUiState {
        if (event.startup) return V3SyncUiState(isShowing = true, isIndeterminate = true, chromeVisible = false)
        if (!event.fullInit) return closedState()
        val total = event.total.coerceAtLeast(0)
        val current = event.current.coerceAtLeast(0).coerceAtMost(total)
        if (total > 0 && current >= total) return closedState(forceChrome = true)
        val percent = if (total > 0) (current * 100 / total).coerceIn(0, 100) else 0
        return V3SyncUiState(
            isShowing = true,
            isProgressVisible = total > 0,
            isIndeterminate = total <= 0,
            progress = if (total <= 0) 0 else maxOf(if (standardState.isShowing) standardState.progress else 0, percent),
            chromeVisible = false,
        )
    }
}
