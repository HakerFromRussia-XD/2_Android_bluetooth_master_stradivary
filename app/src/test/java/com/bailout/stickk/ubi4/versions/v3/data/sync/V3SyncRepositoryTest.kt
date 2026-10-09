package com.bailout.stickk.ubi4.versions.v3.data.sync

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncEvent
import com.bailout.stickk.ubi4.versions.v3.domain.sync.ObserveSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sync.RequestSyncInitializationUseCaseV3
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3SyncRepositoryTest {
    @Test fun `initialization forwards each native request synchronously without resetting shared flags`() {
        val startup = UiState.startupInProgress.value
        val full = UiState.fullInitInProgress.value
        val progress = UiState.widgetsLoadingProgressFlow.value
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("initialization failed")
        var fail = false
        val request = RequestSyncInitializationUseCaseV3(V3SyncInitializationRepositoryImpl {
            calls += "native initialize"
            if (fail) throw failure
        })
        assertTrue(calls.isEmpty())
        request(); calls += "returned"; request()
        assertEquals(listOf("native initialize", "returned", "native initialize"), calls)
        fail = true
        assertSame(failure, assertThrows(IllegalStateException::class.java) { request() })
        assertEquals(4, calls.size)
        assertEquals(startup, UiState.startupInProgress.value)
        assertEquals(full, UiState.fullInitInProgress.value)
        assertEquals(progress, UiState.widgetsLoadingProgressFlow.value)
    }

    @Test fun `separate native callbacks retain raw replay repeats conflation and cancellation`() = runTest {
        val oldReady = UiState.widgetsLoadingFlow
        val oldInfo = UiState.initializationInfoFlow
        val oldProgress = UiState.widgetsLoadingProgressFlow.value
        val oldStartup = UiState.startupInProgress.value
        val oldFull = UiState.fullInitInProgress.value
        val jobs = mutableListOf<Job>()
        val received = mutableListOf<String>()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            // Construct before replacing the sources: they must be captured at collection time.
            val observe = ObserveSyncUseCaseV3(V3SyncRepositoryImpl())
            UiState.widgetsLoadingFlow = MutableSharedFlow()
            UiState.initializationInfoFlow = MutableSharedFlow(replay = 1)
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(current = -3, total = 0)
            val info = initializationInfo(-2, Int.MAX_VALUE)
            UiState.widgetsLoadingFlow.emit(Unit)
            UiState.initializationInfoFlow.emit(info)

            fun subscribe() {
                jobs += observe.observeWidgetsLoadCompletion { received += "ready" }
                jobs += observe.observeInitializationInfo { received += "info:${it.parametersNum}:${it.subDeviceNum}" }
                jobs += observe.observeWidgetsLoadingProgress { received += "progress:${it.current}:${it.total}" }
            }
            subscribe()
            runCurrent()
            assertEquals(listOf("info:-2:${Int.MAX_VALUE}", "progress:-3:0"), received)
            UiState.startupInProgress.value = !oldStartup
            UiState.fullInitInProgress.value = !oldFull
            runCurrent()
            assertEquals(2, received.size)

            repeat(2) {
                UiState.widgetsLoadingFlow.emit(Unit)
                UiState.initializationInfoFlow.emit(info)
                runCurrent()
            }
            assertEquals(listOf("ready", "info:-2:${Int.MAX_VALUE}", "ready", "info:-2:${Int.MAX_VALUE}"), received.drop(2))
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(current = -3, total = 0)
            runCurrent()
            assertEquals(6, received.size)
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(current = 4, total = 10)
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(current = 11, total = 10)
            runCurrent()
            assertEquals("progress:11:10", received.last())
            assertEquals(7, received.size)

            jobs.forEach { it.cancelAndJoin() }
            val size = received.size
            UiState.widgetsLoadingFlow.emit(Unit)
            UiState.initializationInfoFlow.emit(initializationInfo(3, 4))
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(current = 8, total = -1)
            runCurrent()
            assertEquals(size, received.size)

            UiState.widgetsLoadingFlow = MutableSharedFlow()
            UiState.initializationInfoFlow = MutableSharedFlow(replay = 1)
            UiState.initializationInfoFlow.emit(initializationInfo(7, 9))
            subscribe()
            runCurrent()
            assertEquals(listOf("info:7:9", "progress:8:-1"), received.drop(size))
            UiState.widgetsLoadingFlow.emit(Unit)
            runCurrent()
            assertEquals("ready", received.last())
        } finally {
            jobs.forEach { it.cancelAndJoin() }
            UiState.widgetsLoadingFlow = oldReady
            UiState.initializationInfoFlow = oldInfo
            UiState.widgetsLoadingProgressFlow.value = oldProgress
            UiState.startupInProgress.value = oldStartup
            UiState.fullInitInProgress.value = oldFull
            Dispatchers.resetMain()
        }
    }

    private fun initializationInfo(parameters: Int, subDevices: Int) = FullInicializeConnectionStruct(
        deviceName = "", deviceVersion = 0, deviceSubVersion = 0, deviceLabel = "", deviceType = 0,
        deviceCode = 0, deviceAddress = 0, deviceUUID_Prefix = "", deviceUUID = 0L,
        parametersNum = parameters, subDeviceNum = subDevices, programType = 0, defaultPort = 0,
    )

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
