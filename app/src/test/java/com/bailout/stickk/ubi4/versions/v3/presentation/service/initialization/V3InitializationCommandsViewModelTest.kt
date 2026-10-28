package com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.device.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3InitializationCommandsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = Commands()
    private val session = Session()
    private lateinit var viewModel: V3InitializationCommandsViewModel

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3InitializationCommandsViewModel(
            GetInitializationCommandsUseCaseV3(repository), ObserveInitializationCommandsAvailabilityUseCaseV3(repository),
            SendInitializationCommandUseCaseV3(repository), GetDeviceSessionUseCaseV3(session), ObserveDeviceSessionChangesUseCaseV3(session),
        )
        store.put("commands", viewModel)
    }
    @AfterEach fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun `attach and refresh do not send and one click enqueues once without timeout retries`() = runTest(dispatcher) {
        runCurrent()
        viewModel.onAction(V3InitializationCommandsAction.ViewAttached)
        session.updates.emit(Unit)
        runCurrent()
        assertEquals((1..18).toList(), viewModel.uiState.value.commands.map { it.step })
        assertTrue(repository.sent.isEmpty())
        click()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(listOf(V3InitializationCommand.THRESHOLD), repository.sent)
        assertEquals(V3InitializationCommand.THRESHOLD, viewModel.uiState.value.commands.single { it.isLastQueued }.packet.command)
    }

    @Test fun `detached unavailable switched and cleared screens cannot send stale clicks`() = runTest(dispatcher) {
        runCurrent()
        click()
        viewModel.onAction(V3InitializationCommandsAction.ViewAttached)
        session.current = session.current.copy(address = "new-device")
        click()
        assertTrue(repository.sent.isEmpty())
        repository.availability.value = false
        runCurrent()
        click()
        assertTrue(viewModel.uiState.value.commands.none { it.isEnabled })
        repository.availability.value = true
        runCurrent()
        viewModel.onAction(V3InitializationCommandsAction.ViewDetached)
        click()
        store.clear()
        viewModel.onAction(V3InitializationCommandsAction.ViewAttached)
        click()
        assertTrue(repository.sent.isEmpty())
    }

    @Test fun `both V3 profiles show screenshot commands while UBI4 hides them`() = runTest(dispatcher) {
        runCurrent()
        for (profile in listOf(V3DeviceProfile.STANDARD_V3, V3DeviceProfile.INDY3, V3DeviceProfile.NOT_V3)) {
            session.current = session.current.copy(profile = profile)
            viewModel.onAction(V3InitializationCommandsAction.ViewAttached)
            assertEquals(if (profile == V3DeviceProfile.NOT_V3) 0 else 18, viewModel.uiState.value.commands.size)
        }
        assertTrue(repository.sent.isEmpty())
    }

    private fun click() = viewModel.onAction(V3InitializationCommandsAction.CommandClicked(V3InitializationCommand.THRESHOLD))
    private class Commands : V3InitializationCommandsRepository {
        override val availability = MutableStateFlow(true)
        val sent = mutableListOf<V3InitializationCommand>()
        override fun getCommands() = V3InitializationCommand.entries.map { V3InitializationCommandPacket(it, "00") }
        override fun enqueue(deviceAddress: String, command: V3InitializationCommand): V3InitializationCommandPacket {
            sent += command
            return V3InitializationCommandPacket(command, "01")
        }
    }
    private class Session : V3DeviceSessionRepository {
        override val updates = MutableSharedFlow<Unit>()
        var current = V3DeviceSession(V3DeviceProfile.STANDARD_V3, "test-device", false)
        override fun getSession() = current
    }
}
