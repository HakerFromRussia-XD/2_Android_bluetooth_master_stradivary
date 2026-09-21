package com.bailout.stickk.ubi4.versions.v3.presentation.sync

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.sync.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3SyncViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = MutableSharedFlow<V3SyncEvent>(extraBufferCapacity = 16)
    private var subscriptions = 0
    private lateinit var vm: V3SyncViewModel
    private val store = ViewModelStore()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = V3SyncViewModel(ObserveSyncUseCaseV3(object : V3SyncRepository {
            override fun observe() = flow {
                subscriptions++
                try { emitAll(events) } finally { subscriptions-- }
            }
        }))
        store.put("sync", vm)
        vm.onAction(V3SyncAction.ViewCreated)
        vm.onAction(V3SyncAction.ViewAttached)
        dispatcher.scheduler.runCurrent()
    }
    @AfterEach fun cleanup() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun send(event: V3SyncEvent) {
        assertTrue(events.tryEmit(event))
        dispatcher.scheduler.runCurrent()
    }
    private fun progress(current: Int, total: Int, startup: Boolean = false, full: Boolean = true) =
        send(V3SyncEvent.Progress(startup, full, current, total))

    @Test fun `startup takes priority over complete progress and full init`() {
        vm.onAction(V3SyncAction.StartupShown)
        assertEquals(V3SyncUiState(isShowing = true), vm.uiState.value)
        progress(10, 10, startup = true)
        assertEquals(V3SyncUiState(isShowing = true, isIndeterminate = true, chromeVisible = false), vm.uiState.value)
        progress(10, 10)
        assertEquals(V3SyncUiState(chromeVisible = true), vm.uiState.value)
    }
    @Test fun `unknown total stays open and progress is monotonic until a reset or new dialog`() {
        progress(8, 10)
        assertEquals(80, vm.uiState.value.progress)
        progress(2, 10)
        assertEquals(80, vm.uiState.value.progress)
        progress(-1, -1)
        assertEquals(V3SyncUiState(isShowing = true, isIndeterminate = true, chromeVisible = false), vm.uiState.value)
        progress(-1, 10)
        assertEquals(0, vm.uiState.value.progress)
        assertTrue(vm.uiState.value.isProgressVisible)
        progress(11, 10)
        assertFalse(vm.uiState.value.isShowing)
        progress(2, 10)
        assertEquals(20, vm.uiState.value.progress)
    }
    @Test fun `ready closes even during startup and later progress can reopen`() {
        progress(0, 0, startup = true)
        send(V3SyncEvent.Ready)
        assertEquals(V3SyncUiState(chromeVisible = true), vm.uiState.value)
        progress(0, 0, startup = true)
        assertTrue(vm.uiState.value.isShowing)
        assertEquals(false, vm.uiState.value.chromeVisible)
    }
    @Test fun `warm ready does not change chrome before sync has hidden it`() {
        progress(0, 0, full = false)
        send(V3SyncEvent.Ready)
        assertEquals(V3SyncUiState(), vm.uiState.value)
        progress(1, 10)
        progress(1, 10, full = false)
        assertEquals(V3SyncUiState(chromeVisible = true), vm.uiState.value)
    }
    @Test fun `detach preserves display and repeat attach replaces subscription`() {
        progress(6, 10)
        vm.onAction(V3SyncAction.ViewDetached)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, subscriptions)
        send(V3SyncEvent.Ready)
        assertEquals(60, vm.uiState.value.progress)
        repeat(2) { vm.onAction(V3SyncAction.ViewAttached); dispatcher.scheduler.runCurrent() }
        assertEquals(1, subscriptions)
        progress(3, 10)
        assertEquals(60, vm.uiState.value.progress)
        send(V3SyncEvent.Ready)
        assertFalse(vm.uiState.value.isShowing)
    }
    @Test fun `new activity resets display and cleared model cannot subscribe again`() {
        progress(6, 10)
        vm.onAction(V3SyncAction.ViewCreated)
        dispatcher.scheduler.runCurrent()
        assertEquals(V3SyncUiState(), vm.uiState.value)
        assertEquals(0, subscriptions)
        store.clear()
        vm.onAction(V3SyncAction.ViewAttached)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, subscriptions)
    }
}
