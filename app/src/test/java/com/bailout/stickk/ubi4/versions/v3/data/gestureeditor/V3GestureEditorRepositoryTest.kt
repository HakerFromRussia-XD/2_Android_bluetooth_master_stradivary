package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import android.content.SharedPreferences
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.GetGestureEditorHandSideUseCaseV3
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettings
import io.mockk.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3GestureEditorRepositoryTest {
    private fun createRepository(
        preferences: SharedPreferences = mockk(),
        enqueuePacket: (ByteArray) -> Unit = {},
        callbackScheduler: Scheduler = Schedulers.trampoline(),
    ): V3GestureEditorRepositoryImpl {
        val source = V3GestureEditorAndroidSource(preferences, callbackScheduler)
        return V3GestureEditorRepositoryImpl(source::readSavedHandSide, source::subscribeSettingsUpdates, enqueuePacket)
    }

    @ParameterizedTest @ValueSource(ints = [-2, 0, 1, 3])
    fun `device hand side takes priority and is limited to zero or one`(value: Int) {
        val info = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        val preferences = mockk<SharedPreferences>()
        mockkObject(ParameterStoreV3)
        try {
            every { ParameterStoreV3.get(info) } returns ParameterTypedValueV3.Spinner(SpinnerV3(value))
            val repository = createRepository(preferences, { error("Reading must not send commands") })
            assertEquals(value.coerceIn(0, 1), GetGestureEditorHandSideUseCaseV3(repository)())
            verify { preferences wasNot Called }
        } finally { unmockkObject(ParameterStoreV3) }
    }

    @ParameterizedTest @ValueSource(ints = [0, 1, 7])
    fun `missing or mismatched device value uses address preference without clamping`(saved: Int) {
        val info = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        val preferences = mockk<SharedPreferences> {
            every { getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") } returns "device-address"
            every { getInt("device-address" + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) } returns saved
        }
        mockkObject(ParameterStoreV3)
        try {
            val repository = createRepository(preferences, { error("Reading must not send commands") })
            for (stored in listOf(null, ParameterTypedValueV3.Text("1"))) {
                every { ParameterStoreV3.get(info) } returns stored
                assertEquals(saved, GetGestureEditorHandSideUseCaseV3(repository)())
            }
            verify(exactly = 2) { preferences.getInt("device-address" + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) }
            verify(exactly = 0) { preferences.edit() }
        } finally { unmockkObject(ParameterStoreV3) }
    }

    @Test fun `hand side fallback reads current address each time and keeps the default`() {
        val info = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        val preferences = mockk<SharedPreferences> {
            every { getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") } returnsMany listOf("first", "second", "")
            every { getInt("first" + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) } returns 0
            every { getInt("second" + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) } returns 7
            every { getInt(PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) } returns 1
        }
        mockkObject(ParameterStoreV3)
        try {
            every { ParameterStoreV3.get(info) } returns null
            val repository = createRepository(preferences)
            assertEquals(listOf(0, 7, 1), List(3) { repository.getHandSide() })
            verify(exactly = 3) { preferences.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") }
            verify(exactly = 0) { preferences.edit() }
        } finally { unmockkObject(ParameterStoreV3) }
    }

    @Test fun `request uses existing codec without changing gesture ID`() {
        val packets = mutableListOf<ByteArray>()
        val repository = createRepository(enqueuePacket = { packets.add(it) })
        repository.requestSettings(17)
        assertEquals(1, packets.size)
        assertArrayEquals(BLECommandsV3.requestGestureInfo(17), packets.single())
    }

    @Test fun `responses preserve every position delay ID and malformed event and unsubscribe on cancel`() = runTest {
        val info = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val previousDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        val parameter = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode)
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(parameter)))
        val packets = mutableListOf<ByteArray>()
        val repository = createRepository(enqueuePacket = { packets.add(it) })
        val received = mutableListOf<V3GestureSettings?>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSettings().collect { received.add(it) }
        }
        try {
            val gesture = GestureV3(99,
                -1,2,3,4,5,106, 11,12,13,14,15,16,
                21,22,23,24,25,26, 31,32,33,34,35,36)
            parameter.data = Json.encodeToString(gesture)
            RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(info)
            parameter.data = ""
            RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(info)
            parameter.data = "invalid"
            RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(info)
            runCurrent()
            assertEquals(listOf(V3GestureSettings(99, listOf(-1,2,3,4,5,106),
                listOf(11,12,13,14,15,16), listOf(21,22,23,24,25,26), listOf(31,32,33,34,35,36)), null, null), received)
            job.cancelAndJoin()
            RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(info)
            runCurrent()
            assertEquals(3, received.size)
            assertTrue(packets.isEmpty())
        } finally { job.cancel(); GlobalParameters.baseSubDevicesInfoStructSetV3 = previousDevices }
    }

    @Test fun `scheduled responses are read before buffering without dropping repeats and cancellation disposes pending reads`() = runTest {
        val info = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val parameter = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode)
        val scheduler = TestScheduler()
        val repository = createRepository(callbackScheduler = scheduler)
        val received = mutableListOf<V3GestureSettings?>()
        val events = RxUpdateMainEventUbi4.getInstance()
        mockkObject(ParameterProvider)
        val job = backgroundScope.launch { repository.observeSettings().collect { received.add(it) } }
        try {
            every { ParameterProvider.getParameterV3(info) } returns parameter
            runCurrent()
            parameter.data = Json.encodeToString(GestureV3(gestureId = 1))
            events.updateUiGestureSettingsV3(info)
            // Read at callback delivery, as before; neither at event emission nor later in collect.
            parameter.data = Json.encodeToString(GestureV3(gestureId = 2))
            repeat(80) { events.updateUiGestureSettingsV3(info) }
            verify(exactly = 0) { ParameterProvider.getParameterV3(info) }
            assertTrue(received.isEmpty())
            scheduler.triggerActions()
            verify(exactly = 81) { ParameterProvider.getParameterV3(info) }
            parameter.data = "invalid"
            runCurrent()
            assertEquals(List(81) { V3GestureSettings(gestureId = 2) }, received)

            events.updateUiGestureSettingsV3(info)
            job.cancelAndJoin()
            scheduler.triggerActions()
            events.updateUiGestureSettingsV3(info)
            scheduler.triggerActions()
            runCurrent()
            verify(exactly = 81) { ParameterProvider.getParameterV3(info) }
            assertEquals(81, received.size)
        } finally { job.cancelAndJoin(); unmockkObject(ParameterProvider) }
    }

    @Test fun `ready wait completes only on READY and cancelled wait stays cancelled`() = runTest {
        val previous = BLEState.state.value
        val repository = createRepository()
        try {
            BLEState.publishDisconnect()
            val waiting = async { repository.awaitReady() }
            runCurrent()
            assertFalse(waiting.isCompleted)
            BLEState.publishConnecting(); runCurrent()
            assertFalse(waiting.isCompleted)
            BLEState.publishReady(); runCurrent()
            assertTrue(waiting.isCompleted)
            BLEState.publishDisconnect()
            val cancelled = async { repository.awaitReady() }
            runCurrent(); cancelled.cancelAndJoin()
            BLEState.publishReady(); runCurrent()
            assertTrue(cancelled.isCancelled)
        } finally {
            when (previous) {
                BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
                BLEState.State.CONNECTING -> BLEState.publishConnecting()
                BLEState.State.READY -> BLEState.publishReady()
                BLEState.State.ERROR -> BLEState.publishError()
            }
        }
    }
}
