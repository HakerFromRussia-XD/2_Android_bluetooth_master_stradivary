package com.bailout.stickk.ubi4.versions.v3.presentation.gestures

import com.bailout.stickk.ubi4.versions.v3.di.V3GesturesViewModelFactory
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3GesturesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = Repository()
    private lateinit var vm: V3GesturesViewModel
    private fun attach() = vm.onAction(V3GesturesAction.ViewAttached)
    private fun select(id: Int) = vm.onAction(V3GesturesAction.GestureSelected(id))

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3GesturesViewModelFactory(repository).create(V3GesturesViewModel::class.java)
        store.put("gestures", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun `attach requests once and repeated state updates never select a gesture`() = runTest(dispatcher) {
        select(2)
        assertEquals(V3GesturesUiState(), vm.uiState.value)
        attach(); runCurrent()
        repeat(3) { attach(); repository.updates.emit(Unit); runCurrent() }
        assertEquals(V3GesturesUiState(1, true), vm.uiState.value)
        assertEquals(listOf("first"), repository.requests)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `selection updates state immediately and each explicit repeat remains one command`() = runTest(dispatcher) {
        attach(); runCurrent()
        select(64)
        assertEquals(V3GesturesUiState(64, true), vm.uiState.value)
        select(64)
        repository.updates.emit(Unit); runCurrent()
        assertEquals(listOf(64, 64), repository.selections)
        assertEquals(1, repository.requests.size)
    }

    @Test fun `device updates replace optimistic selection without writing back`() = runTest(dispatcher) {
        attach(); runCurrent(); select(4)
        repository.current = repository.current.copy(gestureId = 77)
        repository.updates.emit(Unit); runCurrent()
        assertEquals(77, vm.uiState.value.activeGestureId)
        assertEquals(listOf(4), repository.selections)
    }

    @ParameterizedTest @ValueSource(ints = [-1, 0, 16, 63, 78, 255])
    fun `invalid gesture identifiers do not reach repository`(id: Int) = runTest(dispatcher) {
        attach(); runCurrent(); select(id)
        assertEquals(1, vm.uiState.value.activeGestureId)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `lock wins over a click before its flow is collected and incoming data stays hidden`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.current = repository.current.copy(isInteractionEnabled = false)
        select(2)
        assertEquals(V3GesturesUiState(), vm.uiState.value)
        repository.current = repository.current.copy(gestureId = 4)
        repository.updates.emit(Unit); runCurrent()
        assertEquals(V3GesturesUiState(), vm.uiState.value)
        repository.current = repository.current.copy(isInteractionEnabled = true)
        repository.updates.emit(Unit); runCurrent()
        assertEquals(V3GesturesUiState(4, true), vm.uiState.value)
        assertEquals(2, repository.requests.size)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `detached view ignores clicks and reads until reattached`() = runTest(dispatcher) {
        attach(); runCurrent()
        vm.onAction(V3GesturesAction.ViewDetached)
        repository.current = repository.current.copy(gestureId = 3)
        repository.updates.emit(Unit); runCurrent(); select(2)
        assertEquals(V3GesturesUiState(), vm.uiState.value)
        assertEquals(1, repository.requests.size)
        attach(); runCurrent()
        assertEquals(V3GesturesUiState(3, true), vm.uiState.value)
        assertEquals(2, repository.requests.size)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `cleared viewmodel cannot be reattached by late callbacks`() = runTest(dispatcher) {
        attach(); runCurrent(); store.clear()
        attach(); select(2); repository.updates.emit(Unit); runCurrent()
        assertEquals(V3GesturesUiState(), vm.uiState.value)
        assertEquals(1, repository.requests.size)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `device replacement rejects click from the old screen state`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.current = V3ActiveGesture("second", 7, true)
        select(2)
        assertTrue(repository.selections.isEmpty())
        assertEquals(V3GesturesUiState(7, true), vm.uiState.value)
        assertEquals(listOf("first", "second"), repository.requests)
    }

    @Test fun `missing device gesture is represented as unknown without invented selection`() = runTest(dispatcher) {
        repository.current = repository.current.copy(gestureId = null)
        attach(); runCurrent()
        assertEquals(V3GesturesUiState(null, true), vm.uiState.value)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `cached group and incoming order render without requests or writes in collection`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(5, 2, 5, 64)
        attach(); runCurrent()
        assertEquals(listOf(5, 2, 5, 64), vm.uiState.value.rotationGroupGestureIds)
        repository.rotationGroup = emptyList()
        repository.updates.emit(Unit); runCurrent()
        assertTrue(vm.uiState.value.rotationGroupGestureIds.isEmpty())
        assertTrue(repository.rotationRequests.isEmpty())
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `visible group loads once and equal response stops retries`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(1, 2)
        attach(); showGroup(); runCurrent()
        repeat(3) { showGroup(); repository.updates.emit(Unit); runCurrent() }
        assertEquals(listOf("first"), repository.rotationRequests)
        repository.rotationGroupUpdates.emit(Unit); runCurrent()
        advanceTimeBy(3000); runCurrent()
        assertEquals(listOf("first"), repository.rotationRequests)
        assertEquals(listOf(1, 2), vm.uiState.value.rotationGroupGestureIds)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `no response keeps original six GETs at 400 ms and stops`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
        advanceTimeBy(399); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
        advanceTimeBy(1); runCurrent()
        assertEquals(2, repository.rotationRequests.size)
        advanceTimeBy(3000); runCurrent()
        assertEquals(6, repository.rotationRequests.size)
        repository.updates.emit(Unit); runCurrent()
        assertEquals(6, repository.rotationRequests.size)
    }

    @Test fun `leaving group cancels retries and reopening requests again`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGroupVisibilityChanged(false))
        advanceTimeBy(3000); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
        showGroup(); runCurrent()
        assertEquals(2, repository.rotationRequests.size)
    }

    @Test fun `detaching cancels group request and late data sends nothing`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.ViewDetached)
        repository.rotationGroup = listOf(7)
        repository.rotationGroupUpdates.emit(Unit)
        repository.updates.emit(Unit)
        advanceTimeBy(3000); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
        assertEquals(V3GesturesUiState(), vm.uiState.value)
        attach(); runCurrent()
        assertEquals(2, repository.rotationRequests.size)
        assertEquals(listOf(7), vm.uiState.value.rotationGroupGestureIds)
    }

    @Test fun `changing device cancels the old request and starts one for the new device`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent()
        repository.current = V3ActiveGesture("second", 3, true)
        repository.rotationGroup = listOf(3, 4)
        repository.updates.emit(Unit); runCurrent()
        repository.rotationGroupUpdates.emit(Unit); runCurrent()
        advanceTimeBy(3000); runCurrent()
        assertEquals(listOf("first", "second"), repository.rotationRequests)
        assertEquals(listOf(3, 4), vm.uiState.value.rotationGroupGestureIds)
    }

    @Test fun `lock stops next retry even before a state update is delivered`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent()
        repository.current = repository.current.copy(isInteractionEnabled = false)
        advanceTimeBy(3000); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
    }

    @Test fun `clearing viewmodel cancels retries and ignores late group actions`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent(); store.clear()
        showGroup(); attach()
        advanceTimeBy(3000); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
        assertEquals(V3GesturesUiState(), vm.uiState.value)
    }

    @Test fun `unavailable group request stops without retries`() = runTest(dispatcher) {
        repository.acceptRotationRequests = false
        attach(); showGroup(); advanceTimeBy(3000); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
    }

    private fun showGroup() = vm.onAction(V3GesturesAction.RotationGroupVisibilityChanged(true))

    @Test fun `explicit group clicks reload while visibility and rebind events do not duplicate requests`() = runTest(dispatcher) {
        attach(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGroupRequested)
        showGroup(); runCurrent()
        assertEquals(1, repository.rotationRequests.size)
        repository.rotationGroupUpdates.emit(Unit); runCurrent()
        vm.onAction(V3GesturesAction.RotationGroupRequested); runCurrent()
        repository.rotationGroupUpdates.emit(Unit); runCurrent()
        advanceTimeBy(3000); runCurrent()
        assertEquals(2, repository.rotationRequests.size)
        vm.onAction(V3GesturesAction.ViewDetached)
        vm.onAction(V3GesturesAction.RotationGroupRequested); runCurrent()
        assertEquals(2, repository.rotationRequests.size)
    }

    private fun requestRemoval(position: Int = 0): Long {
        vm.onAction(V3GesturesAction.RotationGestureRemovalRequested(position, repository.rotationGroup.orEmpty()))
        return requireNotNull(vm.uiState.value.rotationGestureRemoval).requestId
    }

    @Test fun `confirmed removal targets one duplicate slot and repeats never write twice`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 64, 4)
        attach(); showGroup(); runCurrent()
        val requestId = requestRemoval()
        assertEquals(V3RotationGestureRemovalUiState(requestId, 4), vm.uiState.value.rotationGestureRemoval)
        assertTrue(repository.rotationWrites.isEmpty())
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(requestId))
        assertEquals(listOf(64, 4), vm.uiState.value.rotationGroupGestureIds)
        assertNull(vm.uiState.value.rotationGestureRemoval)
        repeat(3) {
            vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(requestId))
            attach(); repository.updates.emit(Unit); runCurrent()
        }
        assertEquals(listOf(listOf(64, 4)), repository.rotationWrites)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `cancellation and callbacks from previous dialog cannot affect a new request`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 4)
        attach(); showGroup(); runCurrent()
        val cancelled = requestRemoval()
        vm.onAction(V3GesturesAction.RotationGestureRemovalCancelled(cancelled))
        val current = requestRemoval(1)
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(cancelled))
        vm.onAction(V3GesturesAction.RotationGestureRemovalCancelled(cancelled))
        assertEquals(current, vm.uiState.value.rotationGestureRemoval?.requestId)
        assertTrue(repository.rotationWrites.isEmpty())
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(current))
        assertEquals(listOf(listOf(4)), repository.rotationWrites)
    }

    @ParameterizedTest @ValueSource(strings = ["detach", "hide", "lock", "device", "group", "clear"])
    fun `invalidated dialog never removes after returning to the same screen`(reason: String) = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 64, 4)
        attach(); showGroup(); runCurrent()
        val requestId = requestRemoval()
        when (reason) {
            "detach" -> vm.onAction(V3GesturesAction.ViewDetached)
            "hide" -> vm.onAction(V3GesturesAction.RotationGroupVisibilityChanged(false))
            "lock" -> repository.current = repository.current.copy(isInteractionEnabled = false)
            "device" -> repository.current = repository.current.copy(deviceAddress = "other")
            "group" -> repository.rotationGroup = listOf(64, 4, 4)
            "clear" -> store.clear()
        }
        repository.updates.emit(Unit); runCurrent()
        assertNull(vm.uiState.value.rotationGestureRemoval)
        repository.current = V3ActiveGesture("first", 1, true)
        repository.rotationGroup = listOf(4, 64, 4)
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(requestId))
        assertTrue(repository.rotationWrites.isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["lock", "device", "group", "missing"])
    fun `confirmation checks fresh data before change flow is delivered`(reason: String) = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 64, 4)
        attach(); showGroup(); runCurrent()
        val requestId = requestRemoval()
        when (reason) {
            "lock" -> repository.current = repository.current.copy(isInteractionEnabled = false)
            "device" -> repository.current = repository.current.copy(deviceAddress = "other")
            "group" -> repository.rotationGroup = listOf(64, 4, 4)
            "missing" -> repository.rotationGroup = null
        }
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(requestId))
        assertTrue(repository.rotationWrites.isEmpty())
        assertNull(vm.uiState.value.rotationGestureRemoval)
    }

    @Test fun `stale displayed order and invalid position do not open confirmation`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 64, 4)
        attach(); showGroup(); runCurrent()
        listOf(-1, 3).forEach {
            vm.onAction(V3GesturesAction.RotationGestureRemovalRequested(it, listOf(4, 64, 4)))
            assertNull(vm.uiState.value.rotationGestureRemoval)
        }
        vm.onAction(V3GesturesAction.RotationGestureRemovalRequested(0, listOf(64, 4, 4)))
        assertNull(vm.uiState.value.rotationGestureRemoval)
        assertTrue(repository.rotationWrites.isEmpty())
    }

    @Test fun `request snapshots caller values and deleting the final slot leaves an empty group`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(64)
        attach(); showGroup(); runCurrent()
        val displayed = mutableListOf(64)
        vm.onAction(V3GesturesAction.RotationGestureRemovalRequested(0, displayed))
        val requestId = requireNotNull(vm.uiState.value.rotationGestureRemoval).requestId
        displayed.clear()
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(requestId))
        assertTrue(vm.uiState.value.rotationGroupGestureIds.isEmpty())
        assertEquals(listOf(emptyList<Int>()), repository.rotationWrites)
    }

    private fun openSelection(): Long {
        vm.onAction(V3GesturesAction.RotationGroupSelectionRequested)
        return requireNotNull(vm.uiState.value.rotationGroupSelection).requestId
    }

    @Test fun `selection is a draft until save and preserves old order and duplicates`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(64, 4, 64)
        attach(); showGroup(); runCurrent()
        val request = openSelection()
        assertEquals(setOf(64, 4), vm.uiState.value.rotationGroupSelection?.selectedGestureIds)
        listOf(4, 77, 2, 1).forEach { vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, it)) }
        assertEquals(listOf(64, 4, 64), vm.uiState.value.rotationGroupGestureIds)
        assertTrue(repository.rotationWrites.isEmpty())
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(request))
        assertEquals(listOf(64, 64, 1, 2, 77), vm.uiState.value.rotationGroupGestureIds)
        repeat(3) { vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(request)); attach() }
        assertEquals(listOf(listOf(64, 64, 1, 2, 77)), repository.rotationWrites)
        assertNull(vm.uiState.value.rotationGroupSelection)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `cancel discards draft and callbacks from old dialog cannot change new one`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4)
        attach(); showGroup(); runCurrent()
        val old = openSelection()
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(old, 1))
        vm.onAction(V3GesturesAction.RotationGroupSelectionCancelled(old))
        val current = openSelection()
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(old, 2))
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(old))
        vm.onAction(V3GesturesAction.RotationGroupSelectionCancelled(old))
        assertEquals(current, vm.uiState.value.rotationGroupSelection?.requestId)
        assertEquals(setOf(4), vm.uiState.value.rotationGroupSelection?.selectedGestureIds)
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(current))
        assertEquals(listOf(listOf(4)), repository.rotationWrites, "Existing Save always sends the group")
    }

    @Test fun `limit counts eight checked gestures and retains existing first eight packet slots`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 4, 64)
        attach(); showGroup(); runCurrent()
        val request = openSelection()
        listOf(1, 2, 3, 5, 6).forEach { vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, it)) }
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 7))
        val eightChecks = vm.uiState.value.rotationGroupSelection!!.selectedGestureIds
        assertEquals(8, eightChecks.size)
        assertNull(vm.uiState.value.rotationGroupSelection?.limitMessageId)
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 8))
        val message = requireNotNull(vm.uiState.value.rotationGroupSelection?.limitMessageId)
        assertEquals(eightChecks, vm.uiState.value.rotationGroupSelection?.selectedGestureIds)
        vm.onAction(V3GesturesAction.RotationGroupLimitMessageShown(request, message))
        repository.updates.emit(Unit); runCurrent()
        assertNull(vm.uiState.value.rotationGroupSelection?.limitMessageId)
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 8))
        val nextMessage = requireNotNull(vm.uiState.value.rotationGroupSelection?.limitMessageId)
        assertTrue(nextMessage > message)
        vm.onAction(V3GesturesAction.RotationGroupLimitMessageShown(request, message))
        assertEquals(nextMessage, vm.uiState.value.rotationGroupSelection?.limitMessageId)
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 4))
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 8))
        assertNull(vm.uiState.value.rotationGroupSelection?.limitMessageId)
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(request))
        assertEquals(listOf(listOf(64, 1, 2, 3, 5, 6, 7, 8)), repository.rotationWrites)
    }

    @ParameterizedTest @ValueSource(strings = ["detach", "hide", "lock", "device", "group", "missing", "clear"])
    fun `selection invalidation discards draft before returning to the same device`(reason: String) = runTest(dispatcher) {
        repository.rotationGroup = emptyList()
        attach(); showGroup(); runCurrent()
        val request = openSelection()
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 4))
        when (reason) {
            "detach" -> vm.onAction(V3GesturesAction.ViewDetached)
            "hide" -> vm.onAction(V3GesturesAction.RotationGroupVisibilityChanged(false))
            "lock" -> repository.current = repository.current.copy(isInteractionEnabled = false)
            "device" -> repository.current = repository.current.copy(deviceAddress = "other")
            "group" -> repository.rotationGroup = listOf(64)
            "missing" -> repository.rotationGroup = null
            "clear" -> store.clear()
        }
        repository.updates.emit(Unit); runCurrent()
        assertNull(vm.uiState.value.rotationGroupSelection)
        repository.current = V3ActiveGesture("first", 1, true)
        repository.rotationGroup = emptyList()
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(request))
        assertTrue(repository.rotationWrites.isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["lock", "device", "group", "missing"])
    fun `selection save rechecks device and source group before flow delivery`(reason: String) = runTest(dispatcher) {
        repository.rotationGroup = listOf(4)
        attach(); showGroup(); runCurrent()
        val request = openSelection()
        vm.onAction(V3GesturesAction.RotationGroupGestureToggled(request, 64))
        when (reason) {
            "lock" -> repository.current = repository.current.copy(isInteractionEnabled = false)
            "device" -> repository.current = repository.current.copy(deviceAddress = "other")
            "group" -> repository.rotationGroup = listOf(77)
            "missing" -> repository.rotationGroup = null
        }
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(request))
        assertNull(vm.uiState.value.rotationGroupSelection)
        assertTrue(repository.rotationWrites.isEmpty())
    }

    @Test fun `selection cannot open for missing data hidden group or detached view`() = runTest(dispatcher) {
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGroupSelectionRequested)
        assertNull(vm.uiState.value.rotationGroupSelection)
        repository.rotationGroup = emptyList()
        vm.onAction(V3GesturesAction.RotationGroupVisibilityChanged(false))
        vm.onAction(V3GesturesAction.RotationGroupSelectionRequested)
        assertNull(vm.uiState.value.rotationGroupSelection)
        showGroup(); vm.onAction(V3GesturesAction.ViewDetached)
        vm.onAction(V3GesturesAction.RotationGroupSelectionRequested)
        assertNull(vm.uiState.value.rotationGroupSelection)
        assertTrue(repository.rotationWrites.isEmpty())
    }

    @Test fun `selection and removal dialogs cannot act on each others pending requests`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4)
        attach(); showGroup(); runCurrent()
        val removal = requestRemoval()
        val selection = openSelection()
        assertNull(vm.uiState.value.rotationGestureRemoval)
        vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(removal))
        assertEquals(selection, vm.uiState.value.rotationGroupSelection?.requestId)
        requestRemoval()
        assertNull(vm.uiState.value.rotationGroupSelection)
        vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(selection))
        assertTrue(repository.rotationWrites.isEmpty())
    }

    @Test fun `drag changes group through state and rendering or reattach never repeats the write`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 64, 4)
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGestureMoved(0, 2, listOf(4, 64, 4)))
        assertEquals(listOf(64, 4, 4), vm.uiState.value.rotationGroupGestureIds)
        repeat(3) { attach(); repository.updates.emit(Unit); runCurrent() }
        assertEquals(listOf(listOf(64, 4, 4)), repository.rotationWrites)
        assertTrue(repository.selections.isEmpty())
    }

    @Test fun `same-position drag is ignored but distinct positions with equal gestures always write`() = runTest(dispatcher) {
        repository.rotationGroup = listOf(4, 4)
        attach(); showGroup(); runCurrent()
        vm.onAction(V3GesturesAction.RotationGestureMoved(0, 0, listOf(4, 4)))
        assertTrue(repository.rotationWrites.isEmpty())
        repeat(2) { vm.onAction(V3GesturesAction.RotationGestureMoved(0, 1, listOf(4, 4))) }
        assertEquals(listOf(listOf(4, 4), listOf(4, 4)), repository.rotationWrites)
        assertEquals(listOf(4, 4), vm.uiState.value.rotationGroupGestureIds)
    }

    @ParameterizedTest @ValueSource(strings = ["detach", "hide", "lock", "device", "group", "missing", "clear"])
    fun `drag completion respects current screen and device before flow delivery`(reason: String) = runTest(dispatcher) {
        val original = listOf(4, 64, 4)
        repository.rotationGroup = original
        attach(); showGroup(); runCurrent()
        when (reason) {
            "detach" -> vm.onAction(V3GesturesAction.ViewDetached)
            "hide" -> vm.onAction(V3GesturesAction.RotationGroupVisibilityChanged(false))
            "lock" -> repository.current = repository.current.copy(isInteractionEnabled = false)
            "device" -> repository.current = repository.current.copy(deviceAddress = "other")
            "group" -> repository.rotationGroup = listOf(64, 4, 4)
            "missing" -> repository.rotationGroup = null
            "clear" -> store.clear()
        }
        vm.onAction(V3GesturesAction.RotationGestureMoved(0, 2, original))
        assertTrue(repository.rotationWrites.isEmpty())
        assertTrue(repository.selections.isEmpty())
    }

    private class Repository : V3GesturesRepository {
        var current = V3ActiveGesture("first", 1, true)
        override val updates = MutableSharedFlow<Unit>()
        override val rotationGroupUpdates = MutableSharedFlow<Unit>()
        var rotationGroup: List<Int>? = null
        var acceptRotationRequests = true
        val rotationRequests = mutableListOf<String>()
        val rotationWrites = mutableListOf<List<Int>>()
        override fun setRotationGroup(deviceAddress: String, gestureIds: List<Int>): Boolean {
            rotationWrites.add(gestureIds.toList())
            rotationGroup = gestureIds.toList()
            return true
        }
        override fun getRotationGroupGestureIds() = rotationGroup
        override fun requestRotationGroup(deviceAddress: String): Boolean {
            rotationRequests.add(deviceAddress)
            return acceptRotationRequests
        }
        val requests = mutableListOf<String>()
        val selections = mutableListOf<Int>()
        override fun getActiveGesture() = current
        override fun requestActiveGesture(deviceAddress: String): Boolean {
            requests.add(deviceAddress); return true
        }
        override fun selectGesture(deviceAddress: String, gestureId: Int): Boolean {
            selections.add(gestureId)
            current = current.copy(gestureId = gestureId)
            return true
        }
    }
}
