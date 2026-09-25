package com.bailout.stickk.ubi4.versions.v3.data.blelog

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.blelog.BleLogDirection
import com.bailout.stickk.ubi4.blelog.BleLogEntry
import com.bailout.stickk.ubi4.blelog.BleLogStore
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.V3BleLogEntry
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3BleLogRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val repository = V3BleLogRepositoryImpl(preferences)
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

    @Test fun `restoring filter uses existing key and default without saving or rebuilding history`() {
        every { preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true) } returns true
        assertTrue(repository.restoreGraphStreamFilter())
        verifySequence {
            preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true)
            BleLogStore.setHideGraphStream(true)
        }
        verify(exactly = 0) { preferences.edit(); BleLogStore.snapshot() }
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
}
