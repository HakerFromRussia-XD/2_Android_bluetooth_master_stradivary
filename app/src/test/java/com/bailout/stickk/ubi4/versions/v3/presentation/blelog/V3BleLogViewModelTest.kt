package com.bailout.stickk.ubi4.versions.v3.presentation.blelog

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3BleLogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val additions = MutableSharedFlow<List<V3BleLogEntry>>(extraBufferCapacity = 8)
    private val history = mutableListOf<V3BleLogEntry>()
    private val writes = mutableListOf<Boolean>()
    private var hidden = false
    private var restores = 0
    private var subscriptions = 0
    private val repository = object : V3BleLogRepository {
        override fun observeEntryBatches() = flow {
            subscriptions++
            try {
                emit(history.toList())
                emitAll(additions)
            } finally { subscriptions-- }
        }
        override fun restoreGraphStreamFilter(): Boolean { restores++; return hidden }
        override fun setGraphStreamHidden(hidden: Boolean) { writes += hidden; this@V3BleLogViewModelTest.hidden = hidden }
    }
    private val vm = V3BleLogViewModel(ObserveBleLogUseCaseV3(repository), ManageBleLogFilterUseCaseV3(repository))
    private val store = ViewModelStore()
    @BeforeEach fun setup() { Dispatchers.setMain(dispatcher); store.put("log", vm) }
    @AfterEach fun cleanup() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun entry(id: Long) = V3BleLogEntry(id, id * 1000, id % 2 == 0L, "00 FF")
    private fun open() {
        vm.onAction(V3BleLogAction.ViewCreated)
        vm.onAction(V3BleLogAction.ViewStarted)
    }
    private fun append(id: Long) {
        val value = entry(id)
        history += value
        assertTrue(additions.tryEmit(listOf(value)))
        dispatcher.scheduler.runCurrent()
    }

    @Test fun `opening restores filter once and exposes existing log without preference writes`() {
        history += entry(1)
        open()
        open()
        assertFalse(vm.uiState.value.hideGraphStream)
        assertEquals(history, vm.uiState.value.entries)
        assertEquals(1, restores)
        assertEquals(1, subscriptions)
        assertTrue(writes.isEmpty())
    }

    @Test fun `empty log and successive batches produce complete immutable snapshots`() {
        open()
        val empty = vm.uiState.value
        append(1)
        val first = vm.uiState.value
        append(2)
        append(3)
        // A collector may skip intermediate StateFlow emissions; the latest state must still contain every row.
        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.entries.map { it.id })
        assertTrue(empty.entries.isEmpty())
        assertEquals(listOf(entry(1)), first.entries)
    }

    @Test fun `filter changes persist once without removing historical rows`() {
        history += entry(1)
        open()
        vm.onAction(V3BleLogAction.GraphStreamFilterChanged(true))
        vm.onAction(V3BleLogAction.GraphStreamFilterChanged(true))
        append(2)
        assertEquals(listOf(true), writes)
        assertTrue(vm.uiState.value.hideGraphStream)
        assertEquals(history, vm.uiState.value.entries)
    }

    @Test fun `stop cancels observation and return reloads all missed entries without duplicates`() {
        open()
        append(1)
        vm.onAction(V3BleLogAction.ViewStopped)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, subscriptions)
        append(2)
        assertEquals(listOf(entry(1)), vm.uiState.value.entries)
        vm.onAction(V3BleLogAction.ViewStarted)
        assertEquals(history, vm.uiState.value.entries)
        assertEquals(1, subscriptions)
        assertEquals(1, restores)
        assertTrue(writes.isEmpty())
    }

    @Test fun `destroy ignores late actions and recreation restores current preference`() {
        open()
        vm.onAction(V3BleLogAction.ViewDestroyed)
        vm.onAction(V3BleLogAction.GraphStreamFilterChanged(true))
        vm.onAction(V3BleLogAction.ViewStarted)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, subscriptions)
        assertTrue(writes.isEmpty())
        hidden = true
        history += entry(1)
        open()
        assertTrue(vm.uiState.value.hideGraphStream)
        assertEquals(history, vm.uiState.value.entries)
        assertEquals(2, restores)
    }

    @Test fun `cleared viewmodel cannot restart observations or save filter`() {
        open()
        store.clear()
        dispatcher.scheduler.runCurrent()
        open()
        vm.onAction(V3BleLogAction.GraphStreamFilterChanged(true))
        assertEquals(0, subscriptions)
        assertEquals(1, restores)
        assertTrue(writes.isEmpty())
    }
}
