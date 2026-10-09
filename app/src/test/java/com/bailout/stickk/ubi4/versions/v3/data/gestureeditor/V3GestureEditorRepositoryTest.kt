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
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettings
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.RequestGestureSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.ObserveGestureSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettingsResponse
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

    @ParameterizedTest @ValueSource(ints = [-1, 0, 1, 7])
    fun `authoritative hand side callback bypasses cache metadata and rereads without side effects`(side: Int) {
        val previousValues = ParameterStoreV3.values.value
        val info = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        ParameterStoreV3.put(info, ParameterTypedValueV3.Spinner(SpinnerV3(0)))
        var currentSide = side
        var reads = 0
        val repository = V3GestureEditorRepositoryImpl(
            readSavedHandSide = { reads++; currentSide },
            subscribeSettingsUpdates = { error("Reading hand side must not subscribe") },
            enqueuePacket = { error("Reading hand side must not send packets") },
            saveProfile = { _, _ -> error("Reading hand side must not save a profile") },
            readTypedHandSideFirst = false,
        )
        mockkObject(ParameterInfoRegistry, ParameterStoreV3)
        try {
            every { ParameterInfoRegistry.require(any()) } throws IllegalStateException("Native reading must not require metadata")
            every { ParameterStoreV3.get(any()) } throws IllegalStateException("Native reading must not read typed cache")
            val get = GetGestureEditorHandSideUseCaseV3(repository)
            assertEquals(listOf(side, side), List(2) { get() })
            currentSide = when (side) { -1 -> 0; 0 -> 1; 1 -> 7; else -> -1 }
            assertEquals(currentSide, get())
            assertEquals(3, reads)
            verify { ParameterInfoRegistry wasNot Called; ParameterStoreV3 wasNot Called }
        } finally {
            unmockkObject(ParameterInfoRegistry, ParameterStoreV3)
            ParameterStoreV3.clear()
            previousValues.forEach { (key, value) ->
                ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
            }
        }
    }

    @Test fun `request uses existing codec without changing gesture ID`() {
        val packets = mutableListOf<ByteArray>()
        val repository = createRepository(enqueuePacket = { packets.add(it) })
        repository.requestSettings(17)
        assertEquals(1, packets.size)
        assertArrayEquals(BLECommandsV3.requestGestureInfo(17), packets.single())
    }

    @Test fun `immediate request queues raw repeated GET offline while suspend request still waits and cancels`() = runTest {
        val previous = BLEState.state.value
        val values = ParameterStoreV3.values.value
        val packets = mutableListOf<ByteArray>()
        val repository = createRepository(enqueuePacket = { packets += it })
        val request = RequestGestureSettingsUseCaseV3(repository)
        var waiting: Deferred<Unit>? = null
        var cancelled: Deferred<Unit>? = null
        try {
            BLEState.publishDisconnect()
            waiting = async { request(64) }
            runCurrent()
            assertFalse(waiting.isCompleted)
            assertTrue(packets.isEmpty())
            val ids = listOf(64, 64, 0, -1, 256, Int.MIN_VALUE, Int.MAX_VALUE)
            ids.forEachIndexed { index, id ->
                request.requestNow(id)
                assertEquals(index + 1, packets.size, "Queue before the synchronous request returns")
                assertArrayEquals(BLECommandsV3.requestGestureInfo(id), packets.last())
            }
            val expected = listOf(
                listOf(0, 15, 38, 64, 114), listOf(0, 15, 38, 64, 114),
                listOf(0, 15, 38, 0, 52), listOf(0, 15, 38, 255, 1),
                listOf(0, 15, 38, 0, 52), listOf(0, 15, 38, 0, 52), listOf(0, 15, 38, 255, 1),
            )
            expected.zip(packets).forEach { (bytes, packet) -> assertArrayEquals(bytes.map(Int::toByte).toByteArray(), packet) }
            assertFalse(waiting.isCompleted)
            BLEState.publishConnecting()
            runCurrent()
            assertFalse(waiting.isCompleted)
            assertEquals(ids.size, packets.size)
            BLEState.publishReady()
            runCurrent()
            assertTrue(waiting.isCompleted)
            assertEquals(ids.size + 1, packets.size)
            assertArrayEquals(byteArrayOf(0, 15, 38, 64, 114), packets.last())
            BLEState.publishDisconnect()
            cancelled = async { request(65) }
            runCurrent()
            cancelled.cancelAndJoin()
            BLEState.publishReady()
            runCurrent()
            assertTrue(cancelled.isCancelled)
            assertEquals(ids.size + 1, packets.size)
            assertEquals(values, ParameterStoreV3.values.value)
        } finally {
            waiting?.cancel()
            cancelled?.cancel()
            when (previous) {
                BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
                BLEState.State.CONNECTING -> BLEState.publishConnecting()
                BLEState.State.READY -> BLEState.publishReady()
                BLEState.State.ERROR -> BLEState.publishError()
            }
        }
    }

    @Test fun `opaque responses read exact bridge once and fall back only when absent preserving blank wrong codec and nullable metadata guards`() {
        val base = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val originalInfo = base.copy(dataOffsets = 70)
        val previousDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        val previousValues = ParameterStoreV3.values.value
        val parameter = BaseParameterInfoStruct(ID = base.parameterID, dataCode = base.dataCode, data = "not JSON")
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = base.deviceAddress, parametersList = arrayListOf(parameter)))
        ParameterStoreV3.clear()
        val preferences = mockk<SharedPreferences>()
        val repository = createRepository(preferences, enqueuePacket = { error("RX must not enqueue") })
        val responses = mutableListOf<V3GestureSettingsResponse>()
        val snapshotKeys = mutableListOf<Triple<Int, Int, Int>>()
        val fallbackInfos = mutableListOf<ParameterInfo<Int, Int, Int, Int>>()
        var unsubscribe: (() -> Unit)? = null
        mockkObject(WidgetStateBridgeV3, ParameterProvider)
        try {
            every { WidgetStateBridgeV3.getCurrent(any(), any(), any()) } answers {
                snapshotKeys += Triple(firstArg<Int>(), secondArg<Int>(), thirdArg<Int>())
                callOriginal()
            }
            every { ParameterProvider.getParameterV3(any<ParameterInfo<Int, Int, Int, Int>>()) } answers {
                fallbackInfos += firstArg<ParameterInfo<Int, Int, Int, Int>>()
                callOriginal()
            }
            unsubscribe = ObserveGestureSettingsUseCaseV3(repository).observeResponses { responses += it }
            assertTrue(responses.isEmpty())
            fun emit(info: ParameterInfo<Int, Int, Int, Int>) {
                val values = ParameterStoreV3.values.value
                RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(info)
                assertEquals(values, ParameterStoreV3.values.value)
            }
            emit(originalInfo)
            assertEquals(V3GestureSettingsResponse(base.deviceAddress, base.parameterID, base.dataCode, "not JSON"), responses.single())
            assertEquals(listOf(originalInfo), fallbackInfos)
            parameter.data = "new cache"
            ParameterStoreV3.put(base, ParameterTypedValueV3.GestureSettings(GestureV3(gestureId = 70, openPosition1 = 255)))
            emit(originalInfo)
            assertEquals("{\"gestureId\":70,\"openPosition1\":255}", responses.last().serializedSettings)
            ParameterStoreV3.put(base, ParameterTypedValueV3.Slider(SliderV3(sliderValue = 4)))
            emit(originalInfo)
            assertEquals("", responses.last().serializedSettings, "Present but unencodable snapshot must not fall back")
            val wrongCodec = ParameterInfo(base.parameterID, 0x25, base.deviceAddress, 99)
            ParameterStoreV3.put(wrongCodec, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(4)))
            emit(wrongCodec)
            assertEquals(V3GestureSettingsResponse(base.deviceAddress, base.parameterID, 0x25, "{\"currentGesture\":4}"), responses.last())
            val foreign = ParameterInfo(213, 244, 206, 137)
            ParameterStoreV3.put(foreign, ParameterTypedValueV3.Text("foreign"))
            emit(foreign)
            assertEquals(V3GestureSettingsResponse(206, 213, 244, ""), responses.last())
            @Suppress("UNCHECKED_CAST")
            val missing = listOf(
                ParameterInfo(null, base.dataCode, base.deviceAddress, 0),
                ParameterInfo(base.parameterID, null, base.deviceAddress, 0),
                ParameterInfo(base.parameterID, base.dataCode, null, 0),
            ) as List<ParameterInfo<Int, Int, Int, Int>>
            missing.forEach(::emit)
            assertEquals(5, responses.size)
            assertEquals(listOf(
                Triple(base.deviceAddress, base.parameterID, base.dataCode),
                Triple(base.deviceAddress, base.parameterID, base.dataCode),
                Triple(base.deviceAddress, base.parameterID, base.dataCode),
                Triple(base.deviceAddress, base.parameterID, 0x25), Triple(206, 213, 244),
            ), snapshotKeys)
            assertEquals(listOf(originalInfo), fallbackInfos)
            verify { preferences wasNot Called }
        } finally {
            unsubscribe?.invoke()
            unmockkObject(WidgetStateBridgeV3, ParameterProvider)
            GlobalParameters.baseSubDevicesInfoStructSetV3 = previousDevices
            ParameterStoreV3.clear()
            previousValues.forEach { (key, value) ->
                ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
            }
        }
    }

    @Test fun `response callbacks copy every payload synchronously retain repeats and unsubscribe while Android typed flow stays cache based`() = runTest {
        val base = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val previousDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        val previousValues = ParameterStoreV3.values.value
        val parameter = BaseParameterInfoStruct(ID = base.parameterID, dataCode = base.dataCode)
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = base.deviceAddress, parametersList = arrayListOf(parameter)))
        ParameterStoreV3.clear()
        val repository = createRepository(enqueuePacket = { error("RX must not enqueue") })
        val observe = ObserveGestureSettingsUseCaseV3(repository)
        val responses = mutableListOf<V3GestureSettingsResponse>()
        val typed = mutableListOf<V3GestureSettings?>()
        val order = mutableListOf<String>()
        val sourceThread = Thread.currentThread()
        var unsubscribe: (() -> Unit)? = null
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observe().collect { typed += it }
        }
        try {
            unsubscribe = observe.observeResponses {
                assertSame(sourceThread, Thread.currentThread())
                responses += it
                order += "callback"
            }
            assertTrue(responses.isEmpty())
            assertTrue(typed.isEmpty())
            fun emit(data: String) {
                parameter.data = data
                val count = responses.size
                RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(base)
                assertEquals(count + 1, responses.size, "Callback before producer returns")
                order += "returned"
            }
            listOf("{\"gestureId\":70}", "invalid", "", "").forEach(::emit)
            assertEquals(listOf("{\"gestureId\":70}", "invalid", "", ""), responses.map { it.serializedSettings })
            assertEquals(List(4) { listOf("callback", "returned") }.flatten(), order)
            ParameterStoreV3.put(base, ParameterTypedValueV3.GestureSettings(GestureV3(gestureId = 71)))
            emit("{\"gestureId\":99}")
            assertEquals("{\"gestureId\":71}", responses.last().serializedSettings)
            runCurrent()
            assertEquals(listOf(V3GestureSettings(gestureId = 70), null, null, null, V3GestureSettings(gestureId = 99)), typed)
            assertEquals("{\"gestureId\":70}", responses.first().serializedSettings)
            assertEquals("{\"gestureId\":99}", parameter.data)
            unsubscribe.invoke()
            unsubscribe.invoke()
            RxUpdateMainEventUbi4.getInstance().updateUiGestureSettingsV3(base)
            runCurrent()
            assertEquals(5, responses.size)
            assertEquals(6, typed.size)
            unsubscribe = observe.observeResponses { responses += it }
            assertEquals(5, responses.size, "Resubscribe must not replay latest state")
            emit("{\"gestureId\":100}")
            assertEquals("{\"gestureId\":71}", responses.last().serializedSettings)
            assertEquals(6, responses.size)
        } finally {
            unsubscribe?.invoke()
            job.cancelAndJoin()
            GlobalParameters.baseSubDevicesInfoStructSetV3 = previousDevices
            ParameterStoreV3.clear()
            previousValues.forEach { (key, value) ->
                ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
            }
        }
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
