package com.bailout.stickk.ubi4.versions.v3.presentation.sync

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.data.phonesync.V3PhoneSyncStateStore
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.ObservePhoneSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncState
import com.bailout.stickk.ubi4.versions.v3.domain.sync.ObserveSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncEvent
import com.bailout.stickk.ubi4.versions.v3.domain.sync.V3SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3PhoneSyncScreenStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val phone = V3PhoneSyncStateStore()
    private val events = MutableSharedFlow<V3SyncEvent>(extraBufferCapacity = 16)
    private val models = ViewModelStore()
    private fun create() = V3SyncViewModel(
        ObserveSyncUseCaseV3(object : V3SyncRepository { override fun observe() = events }),
        ObservePhoneSyncUseCaseV3(phone),
    )
    private lateinit var vm: V3SyncViewModel
    private fun runCurrent() = dispatcher.scheduler.runCurrent()
    private fun phoneState(value: V3PhoneSyncState) { phone.publish(value); runCurrent() }
    private fun event(value: V3SyncEvent) { assertTrue(events.tryEmit(value)); runCurrent() }

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = create()
        models.put("sync", vm)
        vm.onAction(V3SyncAction.StartupShown)
        vm.onAction(V3SyncAction.ViewAttached)
        runCurrent()
    }
    @AfterEach fun cleanup() { models.clear(); runCurrent(); Dispatchers.resetMain() }

    @Test fun `missing echo selects instructions while existing show and hide triggers remain unchanged`() {
        val startup = vm.uiState.value
        phoneState(V3PhoneSyncState.Probing)
        assertEquals(startup, vm.uiState.value)
        phoneState(V3PhoneSyncState.Checking)
        assertEquals(startup.copy(mode = V3SyncMode.PhoneConfirmation), vm.uiState.value)
        event(V3SyncEvent.Ready)
        assertEquals(V3SyncUiState(mode = V3SyncMode.PhoneConfirmation), vm.uiState.value)
    }

    @Test fun `start selects instructions before ack while the original events still control visibility`() {
        phoneState(V3PhoneSyncState.Starting)
        val instruction = vm.uiState.value
        assertEquals(V3SyncMode.PhoneConfirmation, instruction.mode)
        assertTrue(instruction.isShowing)
        assertFalse(instruction.isProgressVisible)
        event(V3SyncEvent.Ready)
        assertFalse(vm.uiState.value.isShowing)
        assertEquals(V3SyncMode.PhoneConfirmation, vm.uiState.value.mode)
        event(V3SyncEvent.Progress(true, true, 0, 0))
        assertTrue(vm.uiState.value.isShowing)
        val shownAgain = vm.uiState.value
        phoneState(V3PhoneSyncState.WaitingForUser)
        assertEquals(shownAgain, vm.uiState.value)
    }

    @Test fun `success switches the same screen to normal polling and ready closes it`() {
        phoneState(V3PhoneSyncState.WaitingForUser)
        phoneState(V3PhoneSyncState.Authorized)
        assertEquals(V3SyncMode.Standard, vm.uiState.value.mode)
        assertTrue(vm.uiState.value.isShowing)
        event(V3SyncEvent.Progress(false, true, 3, 17))
        assertTrue(vm.uiState.value.isProgressVisible)
        assertEquals(17, vm.uiState.value.progress)
        event(V3SyncEvent.Ready)
        assertFalse(vm.uiState.value.isShowing)
        assertFalse(vm.uiState.value.returnToScanning)
    }

    @Test fun `failure requests scanning without replacing the existing synchronization state`() {
        phoneState(V3PhoneSyncState.WaitingForUser)
        phoneState(V3PhoneSyncState.Failed)
        val failure = V3SyncUiState(isShowing = true, returnToScanning = true)
        assertEquals(failure, vm.uiState.value)
        event(V3SyncEvent.Ready)
        assertFalse(vm.uiState.value.isShowing)
        assertTrue(vm.uiState.value.returnToScanning)
        event(V3SyncEvent.Progress(true, true, 0, 0))
        assertTrue(vm.uiState.value.isShowing)
        assertTrue(vm.uiState.value.returnToScanning)
        val recreated = create()
        models.put("recreated", recreated)
        assertTrue(recreated.uiState.value.returnToScanning)
    }

    @Test fun `returning from background restores instruction or a pending failure`() {
        phoneState(V3PhoneSyncState.Starting)
        vm.onAction(V3SyncAction.ViewDetached); runCurrent()
        phoneState(V3PhoneSyncState.Failed)
        assertEquals(V3SyncMode.PhoneConfirmation, vm.uiState.value.mode)
        vm.onAction(V3SyncAction.ViewAttached); runCurrent()
        assertTrue(vm.uiState.value.returnToScanning)
        assertEquals(V3SyncMode.Standard, vm.uiState.value.mode)
    }

    @Test fun `legacy and an already bound phone proceed without confirmation instructions`() {
        phoneState(V3PhoneSyncState.Probing)
        phoneState(V3PhoneSyncState.LegacyAllowed)
        assertEquals(V3SyncMode.Standard, vm.uiState.value.mode)
        phoneState(V3PhoneSyncState.Checking)
        phoneState(V3PhoneSyncState.Authorized)
        assertEquals(V3SyncMode.Standard, vm.uiState.value.mode)
        assertFalse(vm.uiState.value.returnToScanning)
    }

    @Test fun `legacy detection preserves the old progress and ready behavior exactly`() {
        event(V3SyncEvent.Progress(false, true, 4, 17))
        phoneState(V3PhoneSyncState.Probing)
        event(V3SyncEvent.Progress(false, true, 5, 17))
        phoneState(V3PhoneSyncState.LegacyAllowed)
        assertEquals(V3SyncUiState(
            isShowing = true, isProgressVisible = true, progress = 29, chromeVisible = false,
        ), vm.uiState.value)

        phoneState(V3PhoneSyncState.Probing)
        event(V3SyncEvent.Ready)
        phoneState(V3PhoneSyncState.LegacyAllowed)
        assertEquals(V3SyncUiState(chromeVisible = true), vm.uiState.value)
    }

    @Test fun `phone states change only content mode and the failure destination`() {
        event(V3SyncEvent.Progress(false, true, 4, 17))
        val original = vm.uiState.value
        listOf(V3PhoneSyncState.Probing, V3PhoneSyncState.Checking, V3PhoneSyncState.Starting,
            V3PhoneSyncState.WaitingForUser, V3PhoneSyncState.Authorized,
            V3PhoneSyncState.LegacyAllowed, V3PhoneSyncState.Failed).forEach { phase ->
            phoneState(phase)
            assertEquals(original, vm.uiState.value.copy(mode = V3SyncMode.Standard, returnToScanning = false), phase.name)
        }
    }
}
