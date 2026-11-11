package com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3BaseSettingsUpdatesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val environment = MutableStateFlow(V3BaseSettingsEnvironment("serial", true, true, 10, 0))
    private val responses = mutableListOf<Pair<V3BaseSettingsOffer, Boolean>>()
    private val refreshes = mutableListOf<String>()
    private val draftCutoffs = mutableListOf<Long?>()
    private var wallTimeMs = 100L
    private var release: CompletableDeferred<Unit>? = null
    private var failResponse = false
    private val repository = object : V3BaseSettingsUpdatesRepository {
        override fun observeEnvironment() = environment
        override suspend fun refresh(serial: String, discardEngineerDraftsBefore: Long?) {
            refreshes += serial
            draftCutoffs += discardEngineerDraftsBefore
        }
        override suspend fun respond(offer: V3BaseSettingsOffer, accept: Boolean): Boolean {
            responses += offer to accept
            release?.await()
            if (failResponse) error("BLE unavailable")
            environment.value = environment.value.copy(handledRevision = offer.revision)
            return true
        }
    }
    private val model = V3BaseSettingsUpdatesViewModel(
        ObserveBaseSettingsUpdatesUseCaseV3(repository), RefreshBaseSettingsUpdatesUseCaseV3(repository),
        RespondToBaseSettingsUpdateUseCaseV3(repository),
        currentTimeMillis = { wallTimeMs },
    )
    private val store = ViewModelStore()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher); store.put("base", model) }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun attach() = model.onAction(V3BaseSettingsUpdatesAction.ViewCreated)
    private fun respond(accept: Boolean) = model.onAction(V3BaseSettingsUpdatesAction.Respond(requireNotNull(model.uiState.value.offer), accept))

    @Test fun `user sees an offer and refreshes on resume`() = runTest(dispatcher) {
        attach(); runCurrent()
        assertEquals(V3BaseSettingsOffer("serial", 10), model.uiState.value.offer)
        assertEquals(listOf("serial"), refreshes)
        model.onAction(V3BaseSettingsUpdatesAction.ViewResumed); runCurrent()
        assertEquals(2, refreshes.size)
    }

    @Test fun `network restoration refreshes baseline without requiring a new activity`() = runTest(dispatcher) {
        environment.value = environment.value.copy(online = false)
        attach(); runCurrent()
        assertTrue(refreshes.isEmpty())
        environment.value = environment.value.copy(online = true); runCurrent()
        assertEquals(listOf("serial"), refreshes)
    }

    @Test fun `engineer keeps profiles on resume and after widgets become available again`() = runTest(dispatcher) {
        environment.value = environment.value.copy(isUser = false, isEngineer = true, isConnected = true)
        attach(); runCurrent()
        model.onAction(V3BaseSettingsUpdatesAction.ViewResumed); runCurrent()

        environment.value = environment.value.copy(canApply = false); runCurrent()
        wallTimeMs = 200
        model.onAction(V3BaseSettingsUpdatesAction.ViewResumed); runCurrent()
        environment.value = environment.value.copy(canApply = true); runCurrent()

        assertEquals(listOf("serial"), refreshes)
        assertEquals(listOf(100L), draftCutoffs)
        assertNull(model.uiState.value.offer)
    }

    @Test fun `reconnecting to the same device reloads engineer profiles with a new draft cutoff`() = runTest(dispatcher) {
        environment.value = environment.value.copy(isUser = false, isEngineer = true, isConnected = true)
        attach(); runCurrent()

        environment.value = environment.value.copy(isEngineer = false, isConnected = false, canApply = false); runCurrent()
        wallTimeMs = 200
        environment.value = environment.value.copy(isEngineer = true, isConnected = true); runCurrent()
        assertEquals(listOf(100L), draftCutoffs)
        environment.value = environment.value.copy(canApply = true); runCurrent()

        assertEquals(listOf("serial", "serial"), refreshes)
        assertEquals(listOf(100L, 200L), draftCutoffs)
    }

    @Test fun `engineer connecting offline loads once when online with the original cutoff`() = runTest(dispatcher) {
        environment.value = environment.value.copy(isUser = false, isEngineer = true, isConnected = true, online = false)
        attach(); runCurrent()
        assertTrue(refreshes.isEmpty())

        wallTimeMs = 200
        model.onAction(V3BaseSettingsUpdatesAction.ViewResumed); runCurrent()
        environment.value = environment.value.copy(online = true); runCurrent()
        environment.value = environment.value.copy(online = false); runCurrent()
        environment.value = environment.value.copy(online = true); runCurrent()

        assertEquals(listOf("serial"), refreshes)
        assertEquals(listOf(100L), draftCutoffs)
    }

    @Test fun `engineer does not see a dialog but switching to user offers the pending baseline`() = runTest(dispatcher) {
        environment.value = environment.value.copy(isUser = false, isEngineer = true, isConnected = true)
        attach(); runCurrent()
        assertNull(model.uiState.value.offer)
        assertEquals(listOf("serial"), refreshes)
        environment.value = environment.value.copy(isUser = true, isEngineer = false); runCurrent()
        assertNotNull(model.uiState.value.offer)
        assertEquals(listOf("serial", "serial"), refreshes)
        environment.value = environment.value.copy(isUser = false, isEngineer = true); runCurrent()
        assertNull(model.uiState.value.offer)
        assertEquals(listOf("serial", "serial"), refreshes)
    }

    @Test fun `skip is remembered for this revision and the next engineer change is offered`() = runTest(dispatcher) {
        attach(); runCurrent(); respond(false); runCurrent()
        assertEquals(listOf(V3BaseSettingsOffer("serial", 10) to false), responses)
        assertNull(model.uiState.value.offer)
        model.onAction(V3BaseSettingsUpdatesAction.ViewDestroyed); attach(); runCurrent()
        assertNull(model.uiState.value.offer)
        environment.value = environment.value.copy(revision = 11); runCurrent()
        assertEquals(V3BaseSettingsOffer("serial", 11), model.uiState.value.offer)
    }

    @Test fun `accept is submitted once even after repeated clicks`() = runTest(dispatcher) {
        release = CompletableDeferred()
        attach(); runCurrent(); respond(true); respond(true); runCurrent()
        assertTrue(model.uiState.value.isApplying)
        assertEquals(1, responses.size)
        release?.complete(Unit); runCurrent()
        assertNull(model.uiState.value.offer)
    }

    @Test fun `stale dialog cannot apply the previous revision`() = runTest(dispatcher) {
        attach(); runCurrent()
        val old = requireNotNull(model.uiState.value.offer)
        environment.value = environment.value.copy(revision = 11); runCurrent()
        model.onAction(V3BaseSettingsUpdatesAction.Respond(old, true)); runCurrent()
        assertTrue(responses.isEmpty())
    }

    @Test fun `changing device cancels pending response and shows only the new device offer`() = runTest(dispatcher) {
        release = CompletableDeferred()
        attach(); runCurrent(); respond(true); runCurrent()
        environment.value = environment.value.copy(serial = "other", revision = 20); runCurrent()
        release?.complete(Unit); runCurrent()
        assertEquals(V3BaseSettingsOffer("other", 20), model.uiState.value.offer)
        assertFalse(model.uiState.value.isApplying)
        assertEquals(0, environment.value.handledRevision)
    }

    @Test fun `apply error keeps offer available for retry`() = runTest(dispatcher) {
        failResponse = true
        attach(); runCurrent(); respond(true); runCurrent()
        assertTrue(model.uiState.value.failed)
        assertNotNull(model.uiState.value.offer)
        assertFalse(model.uiState.value.isApplying)
    }

    @Test fun `no offer is shown during initialization or after activity is destroyed`() = runTest(dispatcher) {
        environment.value = environment.value.copy(canApply = false)
        attach(); runCurrent()
        assertNull(model.uiState.value.offer)
        environment.value = environment.value.copy(canApply = true); runCurrent()
        assertNotNull(model.uiState.value.offer)
        model.onAction(V3BaseSettingsUpdatesAction.ViewDestroyed)
        environment.value = environment.value.copy(revision = 20); runCurrent()
        assertNull(model.uiState.value.offer)
    }
}
