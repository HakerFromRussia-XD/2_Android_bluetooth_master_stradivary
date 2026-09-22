package com.bailout.stickk.ubi4.versions.v3.presentation.gestureeditor

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.*
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3GestureEditorWritingTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = MutableSharedFlow<V3GestureSettings?>(extraBufferCapacity = 8)
    private data class Write(val settings: V3GestureSettings, val command: V3GestureCommand, val name: String)
    private val writes = mutableListOf<Write>()
    private val order = mutableListOf<String>()
    private val repository = object : V3GestureEditorRepository {
        override fun getHandSide() = 1
        override fun observeSettings() = events
        override suspend fun awaitReady() = awaitCancellation()
        override fun requestSettings(gestureId: Int) = error("No read expected")
        override fun writeSettings(settings: V3GestureSettings, command: V3GestureCommand, name: String) {
            writes.add(Write(settings, command, name))
            order.add("write:${command.code}:$name")
        }
    }
    private val preferences = mockk<V3AppSettingsRepository> {
        every { getGestureEditorNames() } returns V3GestureEditorNames(1, listOf("Old", "Other"))
        every { saveGestureEditorNames(any()) } answers { order.add("name:${firstArg<List<String>>()[0]}"); Unit }
    }
    private val vm = V3GestureEditorViewModel(GetGestureEditorNamesUseCaseV3(preferences),
        SaveGestureEditorNamesUseCaseV3(preferences), ObserveGestureSettingsUseCaseV3(repository),
        RequestGestureSettingsUseCaseV3(repository), EditGestureSettingsUseCaseV3(), WriteGestureSettingsUseCaseV3(repository), GetGestureEditorHandSideUseCaseV3(repository))
    private val store = ViewModelStore()
    private val clock get() = dispatcher.scheduler
    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        store.put("editor", vm)
        send(V3GestureEditorAction.ViewCreated(7))
    }
    @AfterEach fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun send(action: V3GestureEditorAction) = vm.onAction(action)
    private fun finger(number: Int, value: Int) = send(V3GestureEditorAction.FingerPositionChanged(number, value))

    @Test fun `renderer finger order and immediate writes are preserved including repeated values`() {
        for (number in 1..4) finger(number, number * 10)
        assertEquals(listOf(40, 30, 20, 10, 0, 0), vm.uiState.value.settings.closePositions)
        assertEquals(4, writes.size)
        assertTrue(writes.all { it.command == V3GestureCommand.CLOSE && it.settings.gestureId == 7 })
        finger(4, 40)
        finger(55, 50)
        assertEquals(5, writes.size)
        assertEquals(List(6) { 0 }, vm.uiState.value.settings.openPositions)
    }

    @Test fun `thumb restarts twelve ms wait and sixth axis cancels it without duplicate write`() {
        finger(5, 20); clock.runCurrent(); clock.advanceTimeBy(11)
        assertTrue(writes.isEmpty())
        finger(5, 30); clock.runCurrent(); clock.advanceTimeBy(11)
        assertTrue(writes.isEmpty())
        finger(6, 40)
        assertEquals(listOf(30, 40), writes.single().settings.closePositions.takeLast(2))
        clock.advanceTimeBy(20); clock.runCurrent()
        assertEquals(1, writes.size)
    }

    @Test fun `pending thumb sends current pose and command and other fingers do not cancel it`() {
        finger(5, 60); clock.runCurrent()
        finger(1, 35)
        send(V3GestureEditorAction.PoseTransitionStarted(true))
        assertEquals(listOf(V3GestureCommand.CLOSE, V3GestureCommand.OPEN_WITH_DELAY), writes.map { it.command })
        clock.advanceTimeBy(12); clock.runCurrent()
        assertEquals(V3GestureCommand.OPEN, writes.last().command)
        assertEquals(35, writes.last().settings.closePositions[3])
        assertEquals(60, writes.last().settings.closePositions[4])
    }

    @Test fun `delay direction and device index remain unchanged across pose transitions`() {
        send(V3GestureEditorAction.FingerDelayChanged(1, 17))
        assertEquals(17, writes.last().settings.openToCloseDelays[0])
        assertEquals(V3GestureCommand.CLOSE, writes.last().command)
        send(V3GestureEditorAction.PoseTransitionStarted(true))
        assertEquals(V3GestureCommand.OPEN_WITH_DELAY, writes.last().command)
        send(V3GestureEditorAction.FingerDelayChanged(4, 23))
        assertEquals(23, writes.last().settings.closeToOpenDelays[3])
        assertEquals(V3GestureCommand.OPEN, writes.last().command)
        send(V3GestureEditorAction.PoseTransitionStarted(false))
        assertEquals(V3GestureCommand.CLOSE_WITH_DELAY, writes.last().command)
        assertEquals(V3GestureCommand.CLOSE, vm.uiState.value.command)
    }

    @Test fun `save writes gesture then name then closes without waiting for pending thumb`() {
        finger(5, 80); clock.runCurrent()
        send(V3GestureEditorAction.EditNameClicked)
        send(V3GestureEditorAction.NameChanged("New"))
        send(V3GestureEditorAction.SaveClicked)
        assertEquals(listOf("write:255:Old", "name:New"), order)
        assertTrue(vm.uiState.value.closeRequested)
        assertTrue(vm.uiState.value.isEditingName)
        clock.advanceTimeBy(12); clock.runCurrent()
        assertEquals("write:255:New", order.last())
    }

    @Test fun `existing STOP timing is preserved but destroy and recreation cancel delayed writes`() {
        finger(5, 50); clock.runCurrent()
        send(V3GestureEditorAction.ViewStopped)
        clock.advanceTimeBy(12); clock.runCurrent()
        assertEquals(1, writes.size)
        finger(5, 70); clock.runCurrent()
        send(V3GestureEditorAction.ViewDestroyed)
        clock.advanceTimeBy(12); clock.runCurrent()
        finger(6, 10)
        assertEquals(1, writes.size)
        send(V3GestureEditorAction.ViewCreated(9))
        finger(5, 40); clock.runCurrent()
        send(V3GestureEditorAction.ViewCreated(11))
        clock.advanceTimeBy(12); clock.runCurrent()
        assertEquals(1, writes.size)
        assertEquals(11, vm.uiState.value.settings.gestureId)
    }

    @Test fun `cleared viewmodel cannot send queued or newly received actions`() {
        finger(5, 90); clock.runCurrent()
        store.clear()
        clock.advanceTimeBy(12); clock.runCurrent()
        send(V3GestureEditorAction.SaveClicked)
        finger(6, 10)
        assertTrue(writes.isEmpty())
    }

    @Test fun `loaded positions clamp and do not replace selected ID or send commands`() {
        events.tryEmit(V3GestureSettings(99, listOf(-1, 2, 3, 4, 5, 110), closePositions = List(6) { 120 }))
        clock.runCurrent()
        send(V3GestureEditorAction.SettingsApplied(vm.uiState.value.pendingSettings.single().id))
        assertEquals(7, vm.uiState.value.settings.gestureId)
        assertEquals(listOf(0,2,3,4,5,100), vm.uiState.value.settings.openPositions)
        assertEquals(List(6) { 100 }, vm.uiState.value.settings.closePositions)
        assertTrue(writes.isEmpty())
        finger(1, -20)
        assertEquals(-20, vm.uiState.value.settings.closePositions[3])
        assertEquals(0, writes.last().settings.closePositions[3])
        finger(6, 130)
        assertEquals(100, vm.uiState.value.settings.closePositions[5])
    }
}
