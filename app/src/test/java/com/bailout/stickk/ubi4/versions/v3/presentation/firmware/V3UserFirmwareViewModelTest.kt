package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3UserFirmwareViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val updates = MutableSharedFlow<V3UserFirmwareStatus>(extraBufferCapacity = 16)
    private val calls = mutableListOf<String>()
    private var observers = 0
    private val store = ViewModelStore()
    private lateinit var vm: V3UserFirmwareViewModel

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        val repository = object : V3UserFirmwareRepository {
            override fun observe() = flow {
                observers++
                try { emitAll(updates) } finally { observers-- }
            }
            override fun refreshEnvironment() { calls += "refresh" }
            override fun startUpdate() { calls += "start" }
            override fun postponeUpdate() { calls += "postpone" }
            override fun acknowledgeCompletion() { calls += "acknowledge" }
            override fun close() { calls += "close" }
        }
        vm = V3UserFirmwareViewModel(
            ObserveUserFirmwareUpdatesUseCaseV3(repository), RefreshUserFirmwareEnvironmentUseCaseV3(repository),
            StartUserFirmwareUpdateUseCaseV3(repository), PostponeUserFirmwareUpdateUseCaseV3(repository),
            AcknowledgeUserFirmwareCompletionUseCaseV3(repository), CloseUserFirmwareUpdatesUseCaseV3(repository),
        )
        store.put("firmware", vm)
        vm.onAction(V3UserFirmwareAction.ViewCreated)
        dispatcher.scheduler.runCurrent()
    }

    @AfterEach fun cleanup() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun send(phase: String, blocked: Boolean = true) {
        assertTrue(updates.tryEmit(V3UserFirmwareStatus(phase, 2, 3, 41, blocked)))
        dispatcher.scheduler.runCurrent()
    }

    @Test fun `offer progress and completion render without starting or acknowledging a transfer`() {
        for (phase in listOf("offered", "preparing", "updating", "verifying", "complete")) {
            send(phase)
            val displayPhase = if (phase in listOf("preparing", "verifying")) "updating" else phase
            assertEquals(V3UserFirmwareUiState(displayPhase, 2, 3, 41, true), vm.uiState.value)
        }
        assertTrue(calls.isEmpty())
        send("unavailable", false)
        assertFalse(vm.uiState.value.isVisible)
        send("idle", false)
        assertFalse(vm.uiState.value.isVisible)
    }

    @Test fun `buttons and foreground use their operations while repeat create keeps one observation`() {
        repeat(3) { vm.onAction(V3UserFirmwareAction.ViewCreated) }
        vm.onAction(V3UserFirmwareAction.ViewResumed)
        vm.onAction(V3UserFirmwareAction.InstallClicked)
        vm.onAction(V3UserFirmwareAction.RemindLaterClicked)
        vm.onAction(V3UserFirmwareAction.CompletionAcknowledged)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, observers)
        assertEquals(listOf("refresh", "start", "postpone", "acknowledge"), calls)
    }

    @Test fun `recreation closes before BLE release and restarts observation without replaying a button`() {
        send("updating")
        vm.onAction(V3UserFirmwareAction.ViewDestroyed)
        assertEquals(listOf("close"), calls)
        assertEquals(V3UserFirmwareUiState(), vm.uiState.value)
        vm.onAction(V3UserFirmwareAction.InstallClicked)
        send("complete")
        assertEquals(0, observers)
        assertEquals(V3UserFirmwareUiState(), vm.uiState.value)
        vm.onAction(V3UserFirmwareAction.ViewCreated)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, observers)
        send("verifying")
        assertEquals("updating", vm.uiState.value.phase)
        assertEquals(listOf("close"), calls)
    }

    @Test fun `cleared viewmodel cannot reopen a session or handle stale dialog clicks`() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        calls.clear()
        vm.onAction(V3UserFirmwareAction.ViewCreated)
        vm.onAction(V3UserFirmwareAction.ViewResumed)
        vm.onAction(V3UserFirmwareAction.InstallClicked)
        vm.onAction(V3UserFirmwareAction.RemindLaterClicked)
        vm.onAction(V3UserFirmwareAction.CompletionAcknowledged)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, observers)
        assertTrue(calls.isEmpty())
    }

    @Test fun `dialog use cases accept only actions and preserve repeated calls in order`() {
        val actions = mutableListOf<String>()
        val repository = object : V3UserFirmwareActionsRepository {
            override fun startUpdate() { actions += "start" }
            override fun postponeUpdate() { actions += "postpone" }
            override fun acknowledgeCompletion() { actions += "acknowledge" }
        }
        val start = StartUserFirmwareUpdateUseCaseV3(repository)
        val postpone = PostponeUserFirmwareUpdateUseCaseV3(repository)
        val acknowledge = AcknowledgeUserFirmwareCompletionUseCaseV3(repository)
        assertTrue(actions.isEmpty())
        start(); start(); postpone(); postpone(); acknowledge(); acknowledge()
        assertEquals(listOf("start", "start", "postpone", "postpone", "acknowledge", "acknowledge"), actions)
    }

    @Test fun `dialog use cases propagate source failure without retrying another action`() {
        var calls = 0
        val failure = IllegalStateException("source failure")
        val repository = object : V3UserFirmwareActionsRepository {
            override fun startUpdate() { calls++; throw failure }
            override fun postponeUpdate() { calls++; throw failure }
            override fun acknowledgeCompletion() { calls++; throw failure }
        }
        assertSame(failure, assertThrows<IllegalStateException> { StartUserFirmwareUpdateUseCaseV3(repository)() })
        assertSame(failure, assertThrows<IllegalStateException> { PostponeUserFirmwareUpdateUseCaseV3(repository)() })
        assertSame(failure, assertThrows<IllegalStateException> { AcknowledgeUserFirmwareCompletionUseCaseV3(repository)() })
        assertEquals(3, calls)
    }
}
