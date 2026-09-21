package com.bailout.stickk.ubi4.versions.v3.data.sync

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3SyncRepositoryTest {
    @Test fun `ready remains an event and restart samples current progress and flags`() = runTest {
        val oldStartup = UiState.startupInProgress.value
        val oldFull = UiState.fullInitInProgress.value
        val oldProgress = UiState.widgetsLoadingProgressFlow.value
        val oldReady = UiState.widgetsLoadingFlow
        UiState.widgetsLoadingFlow = MutableSharedFlow()
        val received = mutableListOf<V3SyncEvent>()
        var observation: Job? = null
        try {
            UiState.startupInProgress.value = true
            UiState.fullInitInProgress.value = true
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(10, 2)
            val repository = V3SyncRepositoryImpl()
            observation = backgroundScope.launch { repository.observe().toList(received) }
            runCurrent()
            assertTrue(received.isNotEmpty())
            assertTrue(received.all { it == V3SyncEvent.Progress(true, true, 2, 10) })
            UiState.widgetsLoadingFlow.emit(Unit)
            runCurrent()
            assertEquals(V3SyncEvent.Ready, received.last())
            UiState.startupInProgress.value = false
            runCurrent()
            assertEquals(V3SyncEvent.Progress(false, true, 2, 10), received.last())
            observation.cancelAndJoin()
            val size = received.size
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(10, 7)
            runCurrent()
            assertEquals(size, received.size)
            observation = backgroundScope.launch { repository.observe().toList(received) }
            runCurrent()
            assertEquals(V3SyncEvent.Progress(false, true, 7, 10), received.last())
        } finally {
            observation?.cancelAndJoin()
            UiState.startupInProgress.value = oldStartup
            UiState.fullInitInProgress.value = oldFull
            UiState.widgetsLoadingProgressFlow.value = oldProgress
            UiState.widgetsLoadingFlow = oldReady
        }
    }
}
