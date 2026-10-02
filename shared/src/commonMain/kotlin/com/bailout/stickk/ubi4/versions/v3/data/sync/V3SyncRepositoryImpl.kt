package com.bailout.stickk.ubi4.versions.v3.data.sync

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncEvent
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class V3SyncRepositoryImpl : V3SyncRepository {
    override fun observe() = flow<V3SyncEvent> {
        var progress = UiState.widgetsLoadingProgressFlow.value
        fun snapshot() = V3SyncEvent.Progress(
            UiState.startupInProgress.value, UiState.fullInitInProgress.value,
            progress.current, progress.total,
        )
        // Keep ready as a separate event and sample flags at the source event,
        // as the original dialog did; combine would change ready semantics.
        merge(
            UiState.widgetsLoadingProgressFlow.map { progress = it; snapshot() },
            UiState.startupInProgress.map { snapshot() },
            UiState.fullInitInProgress.map { snapshot() },
            UiState.widgetsLoadingFlow.map { V3SyncEvent.Ready },
        ).collect { emit(it) }
    }
}
