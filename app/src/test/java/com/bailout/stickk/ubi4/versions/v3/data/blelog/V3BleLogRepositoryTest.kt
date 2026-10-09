package com.bailout.stickk.ubi4.versions.v3.data.blelog

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.blelog.BleLogDirection
import com.bailout.stickk.ubi4.blelog.BleLogEntry
import com.bailout.stickk.ubi4.blelog.BleLogStore
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.di.createBleLogRepository
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.V3BleLogEntry
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.ObserveBleLogUseCaseV3
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3BleLogRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val repository = createBleLogRepository(preferences)
    private val version = MutableStateFlow(0L)
    private val history = mutableListOf<BleLogEntry>()
    @BeforeEach fun setup() {
        mockkObject(BleLogStore)
        every { BleLogStore.snapshot() } answers { history.toList() }
        every { BleLogStore.entriesAfter(any()) } answers { history.filter { it.id > firstArg<Long>() } }
        every { BleLogStore.version } returns version
        every { BleLogStore.setHideGraphStream(any()) } just Runs
    }
    @AfterEach fun cleanup() { unmockkObject(BleLogStore) }
    private fun entry(id: Long) = BleLogEntry(id, id * 1000, BleLogDirection.INCOMING, "00 0A FF")

    @Test fun `explicit log reads preserve snapshots exact cursor mapping and fresh additions without filter writes`() {
        history += entry(1).copy(direction = BleLogDirection.OUTGOING)
        history += entry(2)
        val observe = ObserveBleLogUseCaseV3(repository)
        val initial = observe.snapshot()
        assertEquals(listOf(V3BleLogEntry(1, 1000, true, "00 0A FF"), V3BleLogEntry(2, 2000, false, "00 0A FF")), initial)
        history += entry(3)
        assertEquals(listOf(V3BleLogEntry(3, 3000, false, "00 0A FF")), observe.entriesAfter(2))
        assertTrue(observe.entriesAfter(Long.MAX_VALUE).isEmpty())
        assertEquals(listOf(1L, 2L, 3L), observe.entriesAfter(-1).map { it.id })
        assertEquals(listOf(1L, 2L), initial.map { it.id })
        verifySequence {
            BleLogStore.snapshot()
            BleLogStore.entriesAfter(2L)
            BleLogStore.entriesAfter(Long.MAX_VALUE)
            BleLogStore.entriesAfter(-1L)
        }
        verify { preferences wasNot Called }
        verify(exactly = 0) { BleLogStore.setHideGraphStream(any()) }
    }

    @Test fun `version callbacks retain current replay conflation and cancellation without reading batches`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val observe = ObserveBleLogUseCaseV3(repository)
        val received = mutableListOf<Long>()
        var job: Job? = null
        var reopened: Job? = null
        try {
            job = observe.observeVersion(received::add)
            assertTrue(received.isEmpty())
            runCurrent()
            assertEquals(listOf(0L), received)
            history += entry(1)
            version.value = 1
            history += entry(2)
            version.value = 2
            runCurrent()
            assertEquals(listOf(0L, 2L), received)
            version.value = 2
            runCurrent()
            assertEquals(listOf(0L, 2L), received)
            version.value = 3 // Notifications without new entries still reach the native timer.
            runCurrent()
            assertEquals(listOf(0L, 2L, 3L), received)
            job.cancel()
            version.value = 4
            runCurrent()
            assertEquals(listOf(0L, 2L, 3L), received)
            val current = mutableListOf<Long>()
            reopened = observe.observeVersion(current::add)
            runCurrent()
            assertEquals(listOf(4L), current)
            verify(exactly = 0) { BleLogStore.snapshot(); BleLogStore.entriesAfter(any()); BleLogStore.setHideGraphStream(any()) }
            verify { preferences wasNot Called }
        } finally {
            job?.cancel()
            reopened?.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test fun `snapshot then new batches preserve bytes directions and IDs and stop on cancellation`() = runTest {
        history += entry(1).copy(direction = BleLogDirection.OUTGOING)
        val received = mutableListOf<List<V3BleLogEntry>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeEntryBatches().toList(received)
        }
        assertEquals(listOf(V3BleLogEntry(1, 1000, true, "00 0A FF")), received.single())
        history += entry(2)
        version.value = 2
        version.value = 3 // A notification without new entries must not duplicate the log.
        assertEquals(listOf(1L, 2L), received.flatten().map { it.id })
        assertFalse(received.last().single().isOutgoing)
        job.cancel()
        history += entry(3)
        version.value = 4
        assertEquals(2, received.size)
        val reopened = repository.observeEntryBatches().first()
        assertEquals(listOf(1L, 2L, 3L), reopened.map { it.id })
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `entry arriving between initial snapshot and version subscription is not lost`() = runTest {
        val received = mutableListOf<List<V3BleLogEntry>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeEntryBatches().collect {
                received += it
                if (received.size == 1) {
                    assertTrue(it.isEmpty())
                    history += entry(1)
                    version.value = 1
                }
            }
        }
        assertEquals(listOf(1L), received.flatten().map { it.id })
        job.cancel()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `restoring filter uses existing key and default without saving or rebuilding history`(hidden: Boolean) {
        every { preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true) } returns hidden
        assertEquals(hidden, repository.restoreGraphStreamFilter())
        verifySequence {
            preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true)
            BleLogStore.setHideGraphStream(hidden)
        }
        verify(exactly = 0) { preferences.edit(); BleLogStore.snapshot() }
    }

    @Test fun `restoring filter reads current preference on every call`() {
        every { preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true) } returnsMany listOf(true, false)
        assertTrue(repository.restoreGraphStreamFilter())
        assertFalse(repository.restoreGraphStreamFilter())
        verifySequence {
            preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true)
            BleLogStore.setHideGraphStream(true)
            preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true)
            BleLogStore.setHideGraphStream(false)
        }
        verify(exactly = 0) { preferences.edit(); BleLogStore.snapshot() }
    }

    @Test fun `read failure propagates without applying filter or saving`() {
        val failure = IllegalStateException("Preference read failed")
        every { preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true) } throws failure
        assertSame(failure, assertThrows(IllegalStateException::class.java) { repository.restoreGraphStreamFilter() })
        verify(exactly = 0) { BleLogStore.setHideGraphStream(any()); preferences.edit(); BleLogStore.snapshot() }
    }

    @Test fun `filter change saves before applying to future graph packets`() {
        val editor = mockk<SharedPreferences.Editor>()
        every { preferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
        repository.setGraphStreamHidden(false)
        verifySequence {
            preferences.edit()
            editor.putBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, false)
            editor.apply()
            BleLogStore.setHideGraphStream(false)
        }
        verify(exactly = 0) { BleLogStore.snapshot() }
    }

    @Test fun `save failure propagates before applying filter`() {
        val failure = IllegalStateException("Preference save failed")
        val editor = mockk<SharedPreferences.Editor>()
        every { preferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } throws failure
        assertSame(failure, assertThrows(IllegalStateException::class.java) { repository.setGraphStreamHidden(false) })
        verifySequence {
            preferences.edit()
            editor.putBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, false)
            editor.apply()
        }
        verify(exactly = 0) { BleLogStore.setHideGraphStream(any()); BleLogStore.snapshot() }
    }
}
