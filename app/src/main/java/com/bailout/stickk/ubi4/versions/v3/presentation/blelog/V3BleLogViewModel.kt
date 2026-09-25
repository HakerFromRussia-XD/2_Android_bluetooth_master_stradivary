package com.bailout.stickk.ubi4.versions.v3.presentation.blelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.ManageBleLogFilterUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.ObserveBleLogUseCaseV3
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3BleLogViewModel(
    private val observeLog: ObserveBleLogUseCaseV3,
    private val filter: ManageBleLogFilterUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3BleLogUiState())
    val uiState = state.asStateFlow()
    private var observing: Job? = null
    private var viewCreated = false

    fun onAction(action: V3BleLogAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3BleLogAction.ViewCreated -> if (!viewCreated) {
                state.value = V3BleLogUiState(hideGraphStream = filter.restore())
                viewCreated = true
            }
            V3BleLogAction.ViewStarted -> if (viewCreated && observing == null) {
                state.value = state.value.copy(entries = emptyList())
                observing = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    observeLog().collect { added ->
                        state.value = state.value.copy(entries = state.value.entries + added)
                    }
                }
            }
            V3BleLogAction.ViewStopped, V3BleLogAction.ViewDestroyed -> {
                observing?.cancel()
                observing = null
                if (action == V3BleLogAction.ViewDestroyed) viewCreated = false
            }
            is V3BleLogAction.GraphStreamFilterChanged -> if (viewCreated && action.hidden != state.value.hideGraphStream) {
                filter.setGraphStreamHidden(action.hidden)
                state.value = state.value.copy(hideGraphStream = action.hidden)
            }
        }
    }
}
