package com.bailout.stickk.ubi4.versions.v3.domain.gestures

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.EditRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.GetGesturesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.GetRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.LoadRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.MoveGestureInRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.RemoveGestureFromRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.RequestActiveGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.SaveRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.SelectGestureUseCaseV3

@OptIn(ExperimentalCoroutinesApi::class)
class GesturesUseCasesV3Test {
    @Test
    fun `protocol selections include hidden gesture while editor targets only custom gestures`() {
        val repository = Repository()
        val select = SelectGestureUseCaseV3(repository)
        val request = RequestActiveGestureUseCaseV3(repository)
        val ids = (1..15).toList() + (64..77).toList()
        ids.forEach { assertTrue(select("device", it)) }
        assertEquals(ids, repository.selected)
        listOf(-1, 0, 16, 63, 78, 255).forEach { assertFalse(select("device", it)) }
        assertFalse(select("other", 4))
        assertFalse(request("other"))
        repository.current = repository.current.copy(isInteractionEnabled = false)
        assertFalse(select("device", 4))
        assertFalse(request("device"))
        assertEquals(ids, repository.selected)
        assertEquals(0, repository.activeRequests)
        repository.current = repository.current.copy(isInteractionEnabled = true)
        assertTrue(request("device"))
        assertEquals(1, repository.activeRequests)
        for (id in 64..77) {
            val target = requireNotNull(V3GestureSettingsTarget.fromGestureId(id))
            assertEquals(id, target.gestureId)
            assertEquals(id - 63, target.gestureNumber)
        }
        listOf(0, 1, 12, 15, 63, 78).forEach { assertNull(V3GestureSettingsTarget.fromGestureId(it)) }
    }

    @Test
    fun `selection preserves duplicates collection order checkbox limit and unchanged saves`() {
        val repository = Repository()
        val get = GetRotationGroupSelectionUseCaseV3(repository)
        assertNull(get())
        assertFalse(GetGesturesUseCaseV3(repository)().isRotationGroupAvailable)
        repository.group = emptyList()
        assertTrue(GetGesturesUseCaseV3(repository)().isRotationGroupAvailable)
        repository.group = listOf(64, 4, 64)
        val edit = EditRotationGroupSelectionUseCaseV3()
        val save = SaveRotationGroupSelectionUseCaseV3(repository)
        val original = requireNotNull(get())
        val updated = listOf(4, 77, 2, 1).fold(original) { draft, id -> edit(draft, id).selection }
        assertTrue(save("device", updated))
        assertEquals(listOf(64, 64, 1, 2, 77), repository.written.single())
        assertFalse(save("device", updated))

        repository.group = listOf(4, 4)
        val selection = requireNotNull(get())
        assertEquals(selection, edit(selection, 12).selection)
        val full = edit(selection.copy(selectedGestureIds = (1..7).toSet()), 8)
        assertFalse(full.isLimitReached)
        val excessive = edit(full.selection, 9)
        assertTrue(excessive.isLimitReached)
        assertEquals(full.selection, excessive.selection)
        assertTrue(save("device", full.selection))
        assertEquals(listOf(4, 4, 1, 2, 3, 5, 6, 7), repository.written.last())
        val writes = repository.written.size
        assertTrue(save("device", requireNotNull(get())))
        assertEquals(writes + 1, repository.written.size)
    }

    @Test
    fun `move and removal use positions and reject stale order device and lock`() {
        val repository = Repository()
        val move = MoveGestureInRotationGroupUseCaseV3(repository)
        val remove = RemoveGestureFromRotationGroupUseCaseV3(repository)
        val original = listOf(4, 64, 4, 77)
        repository.group = original
        assertTrue(move("device", 0, 3, original))
        val moved = listOf(64, 4, 77, 4)
        assertEquals(moved, repository.written.single())
        assertFalse(remove("device", 0, original))
        assertFalse(move("device", 0, 1, original))
        assertTrue(remove("device", 3, moved))
        assertEquals(listOf(64, 4, 77), repository.written.last())
        val duplicate = listOf(4, 4)
        repository.group = duplicate
        assertTrue(move("device", 0, 1, duplicate))
        assertEquals(duplicate, repository.written.last())
        val writes = repository.written.size
        assertFalse(move("device", 0, 0, duplicate))
        assertFalse(move("other", 0, 1, duplicate))
        assertFalse(remove("device", 2, duplicate))
        repository.current = repository.current.copy(isInteractionEnabled = false)
        assertFalse(move("device", 0, 1, duplicate))
        assertFalse(remove("device", 0, duplicate))
        assertEquals(writes, repository.written.size)
    }

    @Test
    fun `loading subscribes before request so an immediate equal response completes it`() = runTest {
        val repository = Repository()
        repository.group = listOf(4, 4)
        repository.onRotationRequest = {
            assertEquals(1, repository.rotationGroupUpdates.subscriptionCount.value)
            assertTrue(repository.rotationGroupUpdates.tryEmit(Unit))
        }
        assertTrue(LoadRotationGroupUseCaseV3(repository)("device"))
        assertEquals(1, repository.rotationRequests)
        assertTrue(repository.written.isEmpty())
        assertEquals(0, repository.rotationGroupUpdates.subscriptionCount.value)
    }

    @Test
    fun `no response preserves six requests spaced 400 milliseconds apart`() = runTest {
        val repository = Repository()
        val times = mutableListOf<Long>()
        repository.onRotationRequest = { times.add(testScheduler.currentTime) }
        assertFalse(LoadRotationGroupUseCaseV3(repository)("device"))
        assertEquals(listOf(0L, 400L, 800L, 1200L, 1600L, 2000L), times)
        assertEquals(2400L, testScheduler.currentTime)
        assertEquals(0, repository.rotationGroupUpdates.subscriptionCount.value)
    }

    @Test
    fun `cancellation stops retries and a reply after device change cannot succeed`() = runTest {
        val repository = Repository()
        val load = LoadRotationGroupUseCaseV3(repository)
        val pending = async { load("device") }
        runCurrent()
        assertEquals(1, repository.rotationRequests)
        pending.cancelAndJoin()
        repository.rotationGroupUpdates.tryEmit(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.rotationRequests)
        assertEquals(0, repository.rotationGroupUpdates.subscriptionCount.value)

        repository.onRotationRequest = {
            repository.current = repository.current.copy(deviceAddress = "other")
            repository.rotationGroupUpdates.tryEmit(Unit)
        }
        assertFalse(load("device"))
        assertEquals(2, repository.rotationRequests)
        assertEquals(0, repository.rotationGroupUpdates.subscriptionCount.value)
        assertTrue(repository.written.isEmpty())
    }

    private class Repository : V3GesturesRepository {
        override val updates = MutableSharedFlow<Unit>()
        override val rotationGroupUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var current = V3ActiveGesture("device", 4, true)
        var group: List<Int>? = null
        val selected = mutableListOf<Int>()
        val written = mutableListOf<List<Int>>()
        var activeRequests = 0
        var rotationRequests = 0
        var onRotationRequest: () -> Unit = {}
        override fun getActiveGesture() = current
        override fun getRotationGroupGestureIds() = group
        override fun requestActiveGesture(deviceAddress: String): Boolean {
            activeRequests++
            return true
        }
        override fun requestRotationGroup(deviceAddress: String): Boolean {
            rotationRequests++
            onRotationRequest()
            return true
        }
        override fun setRotationGroup(deviceAddress: String, gestureIds: List<Int>): Boolean {
            written.add(gestureIds.toList())
            group = gestureIds.toList()
            return true
        }
        override fun selectGesture(deviceAddress: String, gestureId: Int): Boolean {
            selected.add(gestureId)
            return true
        }
    }
}
