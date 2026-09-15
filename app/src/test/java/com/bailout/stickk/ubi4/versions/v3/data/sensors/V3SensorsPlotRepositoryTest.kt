package com.bailout.stickk.ubi4.versions.v3.data.sensors

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.ble.ThresholdsV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_PLOT
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3SensorsPlotRepositoryTest {
    private val info = ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD)
    private val plotInfo = ParameterInfoRegistry.require(P_KEY_PLOT)
    private val originalSubDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalValues = ParameterStoreV3.values.value
    private val originalSample = WidgetState.plotArrayFlow.value
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalPause = WidgetState.pausePlotPointsDuringTransition
    private val packets = mutableListOf<ByteArray>()
    private val saved = mutableListOf<ParameterTypedValueV3>()
    private val events = mutableListOf<String>()
    private val repository = V3SensorsPlotRepositoryImpl(
        enqueuePacket = {
            assertNull(ParameterStoreV3.get(info))
            packets += it
            events += "send"
        },
        saveBleValue = { key, value ->
            assertEquals(info, key)
            assertEquals(value, ParameterStoreV3.get(info))
            saved += value
            events += "save"
        },
    )

    @BeforeEach
    fun setUp() {
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress,
            parametersList = arrayListOf(BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode,
                data = "{\"openThreshold\":158,\"closeThreshold\":109}")),
        ))
        WidgetState.plotArrayFlow.value = PlotParameterRef(0, 0, arrayListOf())
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        originalValues.forEach { (key, value) ->
            ParameterStoreV3.put(com.bailout.stickk.ubi4.models.commonModels.ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
        }
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalSubDevices
        WidgetState.plotArrayFlow.value = originalSample
        WidgetState.pausePlotPointsDuringTransition = originalPause
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
    }

    @Test
    fun `cache and incoming thresholds are detached values and observation sends nothing`() = runTest {
        assertEquals(V3PlotThresholds(158, 109), repository.getThresholds())
        val observed = mutableListOf<V3PlotThresholds?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeThresholds().collect(observed::add) }
        val incoming = ThresholdsV3(180, 125)
        ParameterStoreV3.put(info, ParameterTypedValueV3.Thresholds(incoming))
        assertEquals(listOf(V3PlotThresholds(158, 109), V3PlotThresholds(180, 125)), observed)
        incoming.openThreshold = 200
        assertEquals(V3PlotThresholds(180, 125), observed.last())
        assertTrue(packets.isEmpty())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `samples filter parameter identity copy arrays and preserve omitted channels`() = runTest {
        val observed = mutableListOf<List<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeSamples().collect(observed::add) }
        val input = arrayListOf(10, 20, 30, 40, 50, 60, 70)
        WidgetState.plotArrayFlow.value = PlotParameterRef(plotInfo.deviceAddress, plotInfo.parameterID, input)
        assertEquals(listOf(10, 20, 30, 40, 50, 60), observed.last())
        input[0] = 250
        assertEquals(10, observed.last()[0])
        WidgetState.plotArrayFlow.value = PlotParameterRef(99, plotInfo.parameterID, arrayListOf(99))
        WidgetState.plotArrayFlow.value = PlotParameterRef(plotInfo.deviceAddress, 99, arrayListOf(99))
        assertEquals(2, observed.size)
        WidgetState.plotArrayFlow.value = PlotParameterRef(plotInfo.deviceAddress, plotInfo.parameterID, arrayListOf(11))
        assertEquals(listOf(11, 20, 30, 40, 50, 60), observed.last())
        assertTrue(packets.isEmpty())
    }

    @Test
    fun `release keeps command payload order and updates store profile and serialized cache`() {
        SetPlotThresholdsUseCaseV3(repository)(V3PlotThresholds(255, 109))
        val packet = packets.single().map { it.toInt() and 255 }
        assertEquals(listOf(0x80, 0x0f, 3, 0), packet.take(4))
        assertEquals(listOf(0x2f, 255, 109), packet.subList(5, 8))
        assertEquals(BLECommandsV3.calculationCRCRange(packets.single(), 0, 4), packet[4])
        assertEquals(BLECommandsV3.calculationCRCRange(packets.single(), 5, 3), packet[8])
        assertEquals(listOf("send", "save"), events)
        assertEquals(ParameterTypedValueV3.Thresholds(ThresholdsV3(255, 109)), saved.single())
        assertEquals("{\"openThreshold\":255,\"closeThreshold\":109}", ParameterProvider.getParameterV3(info).data)
    }

    @Test
    fun `locked or invalid writes do not reach the existing queue or stores`() {
        val set = SetPlotThresholdsUseCaseV3(repository)
        UiState.v3WidgetsInteractionEnabled.value = false
        set(V3PlotThresholds(200, 100))
        UiState.v3WidgetsInteractionEnabled.value = true
        assertThrows(IllegalArgumentException::class.java) { set(V3PlotThresholds(256, 100)) }
        assertThrows(IllegalArgumentException::class.java) { set(V3PlotThresholds(20, -1)) }
        assertTrue(packets.isEmpty())
        assertTrue(saved.isEmpty())
        assertNull(ParameterStoreV3.get(info))
    }

    @Test
    fun `hardcoded V3 plot retains two curves and existing transition pause`() {
        assertEquals(2, repository.getChannelCount())
        WidgetState.pausePlotPointsDuringTransition = true
        assertTrue(repository.arePlotPointsPaused())
        WidgetState.pausePlotPointsDuringTransition = false
        assertFalse(repository.arePlotPointsPaused())
    }
}
