package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettings
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3GestureEditorRepositoryTest {
    @Test fun `request uses existing codec without changing gesture ID`() {
        val packets = mutableListOf<ByteArray>()
        val repository = V3GestureEditorRepositoryImpl({ packets.add(it) }, Schedulers.trampoline())
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
        val repository = V3GestureEditorRepositoryImpl({ packets.add(it) }, Schedulers.trampoline())
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

    @Test fun `ready wait completes only on READY and cancelled wait stays cancelled`() = runTest {
        val previous = BLEState.state.value
        val repository = V3GestureEditorRepositoryImpl({}, Schedulers.trampoline())
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
