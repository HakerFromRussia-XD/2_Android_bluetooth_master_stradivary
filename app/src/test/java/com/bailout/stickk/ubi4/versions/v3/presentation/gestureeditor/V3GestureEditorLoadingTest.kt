package com.bailout.stickk.ubi4.versions.v3.presentation.gestureeditor

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.*
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3GestureEditorLoadingTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = MutableSharedFlow<V3GestureSettings?>(extraBufferCapacity = 16)
    private val ready = MutableStateFlow(false)
    private val requests = mutableListOf<Int>()
    private val repository = object : V3GestureEditorRepository {
        override fun observeSettings() = events
        override suspend fun awaitReady() { ready.first { it } }
        override fun requestSettings(gestureId: Int) { requests.add(gestureId) }
    }
    private val preferences = mockk<V3AppSettingsRepository> {
        every { getGestureEditorNames() } returns V3GestureEditorNames(1, listOf("One"))
    }
    private val vm = V3GestureEditorViewModel(GetGestureEditorNamesUseCaseV3(preferences),
        SaveGestureEditorNamesUseCaseV3(preferences), ObserveGestureSettingsUseCaseV3(repository),
        RequestGestureSettingsUseCaseV3(repository))
    private val store = ViewModelStore()
    private val settings = V3GestureSettings(7, listOf(1,2,3,4,5,6), listOf(11,12,13,14,15,16),
        listOf(21,22,23,24,25,26), listOf(31,32,33,34,35,36))
    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        store.put("editor", vm)
        vm.onAction(V3GestureEditorAction.ViewCreated)
    }
    @AfterEach fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun drain() = dispatcher.scheduler.runCurrent()
    private fun receive(value: V3GestureSettings?) { assertTrue(events.tryEmit(value)); drain() }
    private fun applyFirst() { vm.onAction(V3GestureEditorAction.SettingsApplied(vm.uiState.value.pendingSettings.first().id)) }

    @Test fun `request waits for started and ready and does not repeat on resume`() {
        ready.value = true
        drain()
        assertTrue(requests.isEmpty())
        vm.onAction(V3GestureEditorAction.ViewStarted(7))
        drain()
        assertEquals(listOf(7), requests)
        vm.onAction(V3GestureEditorAction.ViewStopped)
        vm.onAction(V3GestureEditorAction.ViewStarted(7))
        drain()
        assertEquals(listOf(7), requests)
    }

    @Test fun `ready while stopped does not request until next start`() {
        vm.onAction(V3GestureEditorAction.ViewStarted(9))
        drain()
        vm.onAction(V3GestureEditorAction.ViewStopped)
        ready.value = true
        drain()
        assertTrue(requests.isEmpty())
        vm.onAction(V3GestureEditorAction.ViewStarted(9))
        vm.onAction(V3GestureEditorAction.ViewStarted(9))
        drain()
        assertEquals(listOf(9), requests)
    }

    @Test fun `data before first frame opens only after applying settings and frame`() {
        receive(settings)
        assertEquals(settings, vm.uiState.value.loadedSettings)
        assertFalse(vm.uiState.value.initialOpenRequested)
        applyFirst()
        assertFalse(vm.uiState.value.initialOpenRequested)
        vm.onAction(V3GestureEditorAction.RendererReady)
        assertTrue(vm.uiState.value.initialOpenRequested)
        vm.onAction(V3GestureEditorAction.InitialOpenHandled)
        receive(settings)
        applyFirst()
        vm.onAction(V3GestureEditorAction.RendererReady)
        vm.onAction(V3GestureEditorAction.EditNameClicked)
        assertFalse(vm.uiState.value.initialOpenRequested)
        assertTrue(requests.isEmpty())
    }

    @Test fun `frame before data opens once after successful application not malformed response`() {
        vm.onAction(V3GestureEditorAction.RendererReady)
        receive(null)
        applyFirst()
        assertFalse(vm.uiState.value.initialOpenRequested)
        receive(settings)
        assertFalse(vm.uiState.value.initialOpenRequested)
        applyFirst()
        assertTrue(vm.uiState.value.initialOpenRequested)
    }

    @Test fun `repeated and malformed responses remain ordered until consumed without ID filtering`() {
        events.tryEmit(settings)
        events.tryEmit(null)
        events.tryEmit(settings.copy(gestureId = 99))
        drain()
        val updates = vm.uiState.value.pendingSettings
        assertEquals(listOf(settings, null, settings.copy(gestureId = 99)), updates.map { it.settings })
        assertEquals(3, updates.map { it.id }.toSet().size)
        vm.onAction(V3GestureEditorAction.SettingsApplied(updates.last().id))
        assertEquals(3, vm.uiState.value.pendingSettings.size)
        applyFirst()
        vm.onAction(V3GestureEditorAction.SettingsApplied(updates.first().id))
        assertEquals(2, vm.uiState.value.pendingSettings.size)
        applyFirst(); applyFirst()
        assertTrue(vm.uiState.value.pendingSettings.isEmpty())
    }

    @Test fun `stop preserves response subscription but destroy cancels it and resets next activity`() {
        vm.onAction(V3GestureEditorAction.ViewStopped)
        receive(settings)
        assertEquals(1, events.subscriptionCount.value)
        vm.onAction(V3GestureEditorAction.ViewDestroyed)
        drain()
        assertEquals(0, events.subscriptionCount.value)
        vm.onAction(V3GestureEditorAction.ViewCreated)
        assertNull(vm.uiState.value.loadedSettings)
        assertTrue(vm.uiState.value.pendingSettings.isEmpty())
        assertEquals(1, events.subscriptionCount.value)
    }

    @Test fun `destroy and cleared VM cancel readiness wait and cannot issue late request`() {
        vm.onAction(V3GestureEditorAction.ViewStarted(7))
        drain()
        vm.onAction(V3GestureEditorAction.ViewDestroyed)
        ready.value = true
        drain()
        assertTrue(requests.isEmpty())
        store.clear()
        vm.onAction(V3GestureEditorAction.ViewCreated)
        vm.onAction(V3GestureEditorAction.ViewStarted(7))
        drain()
        assertEquals(0, events.subscriptionCount.value)
        assertTrue(requests.isEmpty())
    }
}
