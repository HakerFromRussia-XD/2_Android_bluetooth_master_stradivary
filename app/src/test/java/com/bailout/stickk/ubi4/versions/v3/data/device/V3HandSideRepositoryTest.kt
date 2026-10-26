package com.bailout.stickk.ubi4.versions.v3.data.device

import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetHandSideUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveHandSideResponsesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.StoreHandSideUseCaseV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3HandSideRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val info = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
    private val oldDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "cached side")
    private val repository = V3HandSideRepositoryImpl()
    private val get = GetHandSideUseCaseV3(repository)
    private val observe = ObserveHandSideResponsesUseCaseV3(repository)
    private val store = StoreHandSideUseCaseV3(repository)
    private val jobs = mutableListOf<Job>()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(
            BaseSubDeviceInfoStruct(deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache)),
        )
    }

    @AfterEach fun cleanup() {
        jobs.forEach { it.cancel() }
        dispatcher.scheduler.runCurrent()
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = oldDevices
        Dispatchers.resetMain()
    }

    private fun incoming(side: Int) = ParameterStoreV3.put(info, ParameterTypedValueV3.Spinner(SpinnerV3(side)))

    @Test fun `current side uses typed values including zero without JSON omission or cache fallback`() {
        assertNull(get())
        for (side in listOf(0, 1, -1, 2, Int.MIN_VALUE, Int.MAX_VALUE)) {
            incoming(side)
            val previous = ParameterStoreV3.values.value
            assertEquals(side.takeIf { it == 0 || it == 1 }, get())
            assertEquals(previous, ParameterStoreV3.values.value)
        }
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("wrong codec"))
        assertNull(get())
        assertEquals("cached side", ParameterProvider.getParameterV3(info).data)
    }

    @Test fun `local selection writes only valid typed sides without saving the profile and preserves repeated RX events`() = runTest(dispatcher) {
        val received = mutableListOf<Int>()
        jobs += observe { received += it }
        runCurrent()
        assertTrue(received.isEmpty())
        store(0)
        assertEquals(0, get())
        runCurrent()
        assertTrue(received.isEmpty(), "Default zero remains omitted from JSON responses")
        repeat(2) { store(1); runCurrent() }
        assertEquals(listOf(1, 1), received)
        val previous = ParameterStoreV3.values.value
        for (side in listOf(-1, 2, Int.MIN_VALUE, Int.MAX_VALUE)) store(side)
        runCurrent()
        assertEquals(previous, ParameterStoreV3.values.value)
        assertEquals(listOf(1, 1), received)
        assertEquals("cached side", ParameterProvider.getParameterV3(info).data)
    }

    @Test fun `responses match original bridge routing omissions repeats and cancellation`() = runTest(dispatcher) {
        incoming(1)
        val original = mutableListOf<Int>()
        val current = mutableListOf<Int>()
        jobs += WidgetStateBridgeV3.observeUpdates { snapshot ->
            if (snapshot.addressDevice == 1 && snapshot.parameterID == 0x10 && snapshot.dataCode == 0x0E) {
                val side = runCatching {
                    Json.parseToJsonElement(snapshot.serializedValue).jsonObject["spinnerValue"]?.jsonPrimitive?.intOrNull
                }.getOrNull()
                if (side == 0 || side == 1) original += requireNotNull(side)
            }
        }
        jobs += observe { current += it }
        runCurrent()
        assertTrue(current.isEmpty())
        for (side in listOf(1, 1, 0, 0, 2, -1, 1)) { incoming(side); runCurrent() }
        for (other in listOf(info.copy(deviceAddress = 2), info.copy(parameterID = 0x11), info.copy(dataCode = 0x0F))) {
            ParameterStoreV3.put(other, ParameterTypedValueV3.Spinner(SpinnerV3(1)))
            runCurrent()
        }
        ParameterStoreV3.put(info.copy(dataOffsets = 9), ParameterTypedValueV3.Spinner(SpinnerV3(1)))
        runCurrent()
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("wrong codec"))
        runCurrent()
        assertEquals(listOf(1, 1, 1, 1), current)
        assertEquals(original, current)
        jobs.forEach { it.cancel() }
        runCurrent()
        incoming(1)
        runCurrent()
        assertEquals(listOf(1, 1, 1, 1), current)
        assertEquals("cached side", ParameterProvider.getParameterV3(info).data)
    }

    @Test fun `queued responses read current store while clear cancellation and reopen do not synthesize initial events`() = runTest(dispatcher) {
        val received = mutableListOf<Int>()
        jobs += observe { received += it }
        runCurrent()
        incoming(1); incoming(0)
        runCurrent()
        assertTrue(received.isEmpty())
        incoming(1); incoming(1)
        runCurrent()
        assertEquals(listOf(1, 1), received)
        incoming(1); ParameterStoreV3.clear()
        runCurrent()
        assertEquals(listOf(1, 1), received)
        jobs.last().cancel()
        runCurrent()
        incoming(1)
        jobs += observe { received += it }
        runCurrent()
        assertEquals(listOf(1, 1), received)
        incoming(1)
        runCurrent()
        assertEquals(listOf(1, 1, 1), received)
    }
}
