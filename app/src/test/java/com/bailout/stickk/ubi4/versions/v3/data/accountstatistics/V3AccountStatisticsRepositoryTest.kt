package com.bailout.stickk.ubi4.versions.v3.data.accountstatistics

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.RequestAccountStatisticsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3AccountStatistics
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountStatisticsRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val saved = mutableMapOf<String, String?>()
    private val counters = MutableStateFlow(TelemetryGestureCounters())
    private val updates = MutableSharedFlow<Unit>()
    private var requests = 0
    private val macKey = PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4
    private val nameKey = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM
    private val repository = V3AccountStatisticsRepositoryImpl(preferences, { requests++ }, counters, updates)

    @BeforeEach fun setUp() {
        every { preferences.getString(any(), any()) } answers {
            if (saved.containsKey(firstArg<String>())) saved[firstArg()] else secondArg<String?>()
        }
    }

    @Test fun `initial telemetry needs no update event and preserves all name slots null empty and MAC defaults`() = runTest {
        counters.value = TelemetryGestureCounters(customGestureMovementCount = List(17) { 1L })
        saved[nameKey + 0] = "No MAC"
        saved[nameKey + 1] = ""
        saved[nameKey + 2] = null
        saved[nameKey + 14] = "Fifteenth"
        saved[nameKey + 16] = "Beyond defaults"
        val initial = repository.observeStatistics().first()
        assertEquals(17, initial.customGestureNames.size)
        assertEquals(listOf("No MAC", "", null, null), initial.customGestureNames.take(4))
        assertEquals("Fifteenth", initial.customGestureNames[14])
        assertEquals("Beyond defaults", initial.customGestureNames[16])
        saved[macKey] = null
        assertEquals(initial, repository.observeStatistics().first())
        saved[macKey] = "AA:BB"
        saved[nameKey + "AA:BB0"] = "Device name"
        saved[nameKey + "AA:BB64"] = "Wrong index"
        assertEquals("Device name", repository.observeStatistics().first().customGestureNames[0])
        assertEquals(0, requests)
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `updates reread renamed or switched device names without discarding telemetry or requesting BLE`() = runTest {
        counters.value = TelemetryGestureCounters(baseGestureMovementCount = listOf(0, 3), customGestureMovementCount = listOf(4))
        saved[macKey] = "first"
        saved[nameKey + "first0"] = "Before"
        val states = mutableListOf<V3AccountStatistics>()
        val job = launch { repository.observeStatistics().collect(states::add) }
        runCurrent()
        saved[nameKey + "first0"] = "Renamed"
        updates.emit(Unit); runCurrent()
        saved[macKey] = "second"
        saved[nameKey + "second0"] = "Other"
        updates.emit(Unit); runCurrent()
        assertEquals(listOf("Before", "Renamed", "Other"), states.map { it.customGestureNames.single() })
        assertTrue(states.all { it.baseGestureCounts == listOf(0L, 3L) })
        assertEquals(0, requests)
        job.cancel(); runCurrent()
        assertEquals(0, counters.subscriptionCount.value)
        assertEquals(0, updates.subscriptionCount.value)
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `unused custom slots do not read preference keys which the previous screen never accessed`() = runTest {
        counters.value = TelemetryGestureCounters(baseGestureMovementCount = listOf(0, 1), customGestureMovementCount = listOf(0, -1))
        assertEquals(listOf(null, null), repository.observeStatistics().first().customGestureNames)
        verify { preferences wasNot Called }
    }

    @Test fun `snapshots detach from mutable source lists and counters refresh without writes`() = runTest {
        val rawBase = mutableListOf(0L, 7L)
        val rawCustom = mutableListOf(2L)
        counters.value = TelemetryGestureCounters(rawBase, rawCustom)
        val initial = repository.observeStatistics().first()
        rawBase[1] = 100L
        rawCustom[0] = 200L
        assertEquals(listOf(0L, 7L), initial.baseGestureCounts)
        assertEquals(listOf(2L), initial.customGestureCounts)
        assertEquals(0, requests)
    }

    @Test fun `request preserves the original V3 gate for both profiles and delegates exactly once`() {
        val oldV3 = UiState.isInterfaceV3Activated
        val oldProfile = UiState.activeV3DeviceProfile
        try {
            val request = RequestAccountStatisticsUseCaseV3(repository)
            UiState.isInterfaceV3Activated = false
            request()
            assertEquals(0, requests)
            UiState.isInterfaceV3Activated = true
            listOf(V3DeviceProfile.STANDARD_V3, V3DeviceProfile.INDY3).forEach {
                UiState.activeV3DeviceProfile = it
                request()
            }
            assertEquals(2, requests)
            UiState.isInterfaceV3Activated = false
            request()
            assertEquals(2, requests)
        } finally {
            UiState.isInterfaceV3Activated = oldV3
            UiState.activeV3DeviceProfile = oldProfile
        }
    }
}
