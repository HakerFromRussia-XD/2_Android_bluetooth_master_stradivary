package com.bailout.stickk.ubi4.versions.v3.presentation.dashboard

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.state.DashboardSlotInfo
import com.bailout.stickk.ubi4.data.state.DashboardSlotsState
import com.bailout.stickk.ubi4.ui.dashboard.toDashboardSlotUiItem
import com.bailout.stickk.ubi4.ui.fragments.dashboard.toUiItem
import com.bailout.stickk.ubi4.versions.v3.data.dashboard.V3DashboardSlotsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.di.V3DashboardSlotsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Exercises the production ViewModel, use case, repository and unchanged parser state together. */
@OptIn(ExperimentalCoroutinesApi::class)
class V3DashboardSlotsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val original = DashboardSlotsState.stateFlow.value
    private val packets = mutableListOf<ByteArray>()
    private val store = ViewModelStore()
    private val slot = DashboardSlotInfo(8, 0x21, 9, 3, 2, 1024, 16, 0xABCD)
    private lateinit var viewModel: V3DashboardSlotsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val repository = V3DashboardSlotsRepositoryImpl { packet ->
            // Request state must be reset before anything is handed to the BLE queue.
            assertTrue(DashboardSlotsState.stateFlow.value.isLoading)
            assertTrue(DashboardSlotsState.stateFlow.value.slots.isEmpty())
            assertNull(DashboardSlotsState.stateFlow.value.errorMessage)
            packets += packet
        }
        viewModel = V3DashboardSlotsViewModelFactory(repository).create(V3DashboardSlotsViewModel::class.java)
        store.put("slots", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        DashboardSlotsState.requestStarted(original.deviceAddress)
        if (original.errorMessage != null) DashboardSlotsState.requestFailed(original.deviceAddress, original.errorMessage!!)
        else if (!original.isLoading) DashboardSlotsState.updateSlots(original.deviceAddress, original.slots)
    }

    @Test
    fun `one view sends the existing read packet once and replaces the previous list`() = runTest(dispatcher) {
        DashboardSlotsState.updateSlots(7, listOf(slot.copy(deviceAddress = 7)))
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(8, state.deviceAddress)
        assertTrue(state.isLoading)
        assertTrue(state.slots.isEmpty())
        assertNull(state.errorMessage)
        assertArrayEquals(byteArrayOf(0, 2, 1, 8, 0x49), packets.single())
    }

    @Test
    fun `parser response preserves every slot field and the existing UI title and navigation data`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        DashboardSlotsState.updateSlots(8, listOf(slot))
        runCurrent()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(slot.dataType, state.slots.single().dataType)
        assertEquals(slot.toDashboardSlotUiItem(), state.slots.single().toUiItem())
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(state, viewModel.uiState.value)
        assertEquals(1, packets.size)
    }

    @Test
    fun `timeout is exactly five seconds and a late response still replaces the same error`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Плата не ответила на запрос слотов", viewModel.uiState.value.errorMessage)
        DashboardSlotsState.updateSlots(8, listOf(slot))
        runCurrent()
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.slots.size)
        assertEquals(1, packets.size)
    }

    @Test
    fun `empty response is success and timeout does not replace another board's state`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        DashboardSlotsState.updateSlots(8, emptyList())
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.slots.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
        // Preserve shared-state behavior; this migration does not invent response filtering.
        DashboardSlotsState.requestStarted(9)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(9, viewModel.uiState.value.deviceAddress)
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `destroyed view cancels observation and timeout and recreation makes a fresh request`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        advanceTimeBy(4_000)
        viewModel.onAction(V3DashboardSlotsAction.ViewDestroyed)
        runCurrent()
        val detached = viewModel.uiState.value
        DashboardSlotsState.updateSlots(8, listOf(slot))
        runCurrent()
        assertEquals(detached, viewModel.uiState.value)
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        assertEquals(2, packets.size)
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading) // The first view's timeout was cancelled.
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals("Плата не ответила на запрос слотов", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `cleared viewmodel cannot restart loading or change shared state later`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(8))
        runCurrent()
        store.clear()
        runCurrent()
        viewModel.onAction(V3DashboardSlotsAction.ViewCreated(9))
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, packets.size)
        assertTrue(DashboardSlotsState.stateFlow.value.isLoading)
        assertNull(DashboardSlotsState.stateFlow.value.errorMessage)
    }
}
