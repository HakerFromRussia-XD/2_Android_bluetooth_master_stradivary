package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountStatisticsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountStatisticsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = Repository()
    private val store = ViewModelStore()
    private lateinit var vm: V3AccountStatisticsViewModel

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3AccountStatisticsViewModelFactory(repository).create(V3AccountStatisticsViewModel::class.java)
        store.put("statistics", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun `one request per view and updates only render state`() = runTest(dispatcher) {
        vm.onAction(V3AccountStatisticsAction.ViewAttached)
        vm.onAction(V3AccountStatisticsAction.ViewAttached)
        runCurrent()
        assertEquals(listOf(V3GestureUsage(1, 5)), vm.uiState.value.gestures)
        assertEquals(1, repository.requests)
        repository.values.value = V3AccountStatistics(customGestureCounts = listOf(7), customGestureNames = listOf("Name"))
        runCurrent()
        assertEquals(listOf(V3GestureUsage(64, 7, 0, "Name")), vm.uiState.value.gestures)
        repository.values.value = repository.values.value.copy(customGestureNames = listOf("Renamed"))
        runCurrent()
        assertEquals("Renamed", vm.uiState.value.gestures.single().customName)
        assertEquals(1, repository.requests)
    }

    @Test fun `UI collectors can stop and resume without additional requests`() = runTest(dispatcher) {
        vm.onAction(V3AccountStatisticsAction.ViewAttached)
        val first = launch { vm.uiState.collect {} }
        runCurrent(); first.cancel(); runCurrent()
        repository.values.value = V3AccountStatistics(baseGestureCounts = listOf(0, 9))
        runCurrent()
        val second = launch { vm.uiState.collect {} }
        runCurrent(); second.cancel()
        assertEquals(9L, vm.uiState.value.gestures.single().count)
        assertEquals(1, repository.requests)
    }

    @Test fun `destroyed view unsubscribes and recreated view reads latest snapshot with one new request`() = runTest(dispatcher) {
        vm.onAction(V3AccountStatisticsAction.ViewAttached); runCurrent()
        val before = vm.uiState.value
        vm.onAction(V3AccountStatisticsAction.ViewDetached)
        vm.onAction(V3AccountStatisticsAction.ViewDetached)
        runCurrent()
        assertEquals(0, repository.values.subscriptionCount.value)
        repository.values.value = V3AccountStatistics(baseGestureCounts = listOf(0, 17))
        runCurrent()
        assertEquals(before, vm.uiState.value)
        vm.onAction(V3AccountStatisticsAction.ViewAttached); runCurrent()
        assertEquals(17L, vm.uiState.value.gestures.single().count)
        assertEquals(1, repository.values.subscriptionCount.value)
        assertEquals(2, repository.requests)
    }

    @Test fun `cleared viewmodel ignores late actions and data`() = runTest(dispatcher) {
        vm.onAction(V3AccountStatisticsAction.ViewAttached); runCurrent()
        val before = vm.uiState.value
        store.clear(); runCurrent()
        repository.values.value = V3AccountStatistics()
        vm.onAction(V3AccountStatisticsAction.ViewDetached)
        vm.onAction(V3AccountStatisticsAction.ViewAttached); runCurrent()
        assertEquals(before, vm.uiState.value)
        assertEquals(0, repository.values.subscriptionCount.value)
        assertEquals(1, repository.requests)
    }

    private class Repository : V3AccountStatisticsRepository {
        val values = MutableStateFlow(V3AccountStatistics(baseGestureCounts = listOf(0, 5)))
        var requests = 0
        override fun observeStatistics() = values
        override fun requestStatistics() { requests++ }
    }
}
