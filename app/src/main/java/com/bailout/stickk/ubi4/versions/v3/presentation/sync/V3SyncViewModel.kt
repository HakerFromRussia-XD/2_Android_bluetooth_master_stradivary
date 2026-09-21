package com.bailout.stickk.ubi4.versions.v3.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.sync.ObserveSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3SyncViewModel(private val observeSync: ObserveSyncUseCaseV3) : ViewModel() {
    private val state = MutableStateFlow(V3SyncUiState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null

    fun onAction(action: V3SyncAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3SyncAction.ViewCreated -> {
                observation?.cancel()
                state.value = V3SyncUiState()
            }
            V3SyncAction.StartupShown -> state.value = state.value.copy(
                isShowing = true, isProgressVisible = false, isIndeterminate = false, progress = 0,
            )
            V3SyncAction.ViewAttached -> {
                observation?.cancel()
                observation = viewModelScope.launch {
                    observeSync().collect { event ->
                        if (isActive) state.value = when (event) {
                            V3SyncEvent.Ready -> closedState()
                            is V3SyncEvent.Progress -> progressState(event)
                        }
                    }
                }
            }
            V3SyncAction.ViewDetached -> { observation?.cancel(); observation = null }
        }
    }

    private fun closedState(forceChrome: Boolean = false) = V3SyncUiState(
        chromeVisible = if (forceChrome || state.value.chromeVisible == false) true else state.value.chromeVisible,
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
            progress = if (total <= 0) 0 else maxOf(if (state.value.isShowing) state.value.progress else 0, percent),
            chromeVisible = false,
        )
    }
}
