package com.bailout.stickk.ubi4.versions.v3.data.settings

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class V3DeviceSettingsRepositoryImplTest {
    private val parameterInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private val forceParameterInfo = ParameterInfoRegistry.require(P_KEY_FORCE_SETTINGS)
    private val maxGainParameterInfo = ParameterInfoRegistry.require(P_KEY_EMG_MAX_GAIN_VALUE)
    private lateinit var cachedParameter: BaseParameterInfoStruct
    private lateinit var cachedForceParameter: BaseParameterInfoStruct
    private lateinit var cachedMaxGainParameter: BaseParameterInfoStruct
    private lateinit var originalSubDevices: MutableSet<BaseSubDeviceInfoStruct>
    private val packets = mutableListOf<ByteArray>()
    private val savedValues = mutableListOf<ParameterTypedValueV3>()

    @BeforeEach
    fun setUp() {
        originalSubDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        cachedParameter = BaseParameterInfoStruct(
            ID = parameterInfo.parameterID,
            dataCode = parameterInfo.dataCode,
            data = "{\"sliderValue\":17}",
        )
        cachedForceParameter = BaseParameterInfoStruct(
            ID = forceParameterInfo.parameterID,
            dataCode = forceParameterInfo.dataCode,
            data = "{\"sliderValue\":70}",
        )
        cachedMaxGainParameter = BaseParameterInfoStruct(
            ID = maxGainParameterInfo.parameterID,
            dataCode = maxGainParameterInfo.dataCode,
            data = "{\"sliderValue\":225}",
        )
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(
            BaseSubDeviceInfoStruct(
                deviceAddress = parameterInfo.deviceAddress,
                parametersList = arrayListOf(cachedParameter, cachedForceParameter, cachedMaxGainParameter),
            )
        )
        ParameterStoreV3.clear()
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalSubDevices
    }

    private fun repository() = V3DeviceSettingsRepositoryImpl(
        enqueuePacket = { packets.add(it) },
        saveBleValue = { info, value ->
            assertEquals(parameterInfo, info)
            savedValues.add(value)
        },
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observation restores cache and delivers store changes without commands`() = runTest {
        val values = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository().observeSliderValue(P_KEY_SPEED_SETTINGS).toList(values)
        }
        ParameterStoreV3.put(parameterInfo, ParameterTypedValueV3.Slider(SliderV3(63)))
        ParameterStoreV3.put(parameterInfo, ParameterTypedValueV3.Slider(SliderV3(63)))

        assertEquals(listOf(17, 63), values)
        assertEquals(emptyList<ByteArray>(), packets)
        assertEquals(emptyList<ParameterTypedValueV3>(), savedValues)
    }

    @Test
    fun `reading prefers store over serialized cache without saving or sending`() {
        ParameterStoreV3.put(parameterInfo, ParameterTypedValueV3.Slider(SliderV3(63)))
        val repository = repository()

        assertEquals(63, repository.getSliderValue(P_KEY_SPEED_SETTINGS))
        assertEquals(63, repository.getSliderValue(P_KEY_SPEED_SETTINGS))
        assertEquals("{\"sliderValue\":17}", cachedParameter.data)
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `reading falls back to serialized cache without filling store or sending`() {
        assertEquals(17, repository().getSliderValue(P_KEY_SPEED_SETTINGS))
        assertNull(ParameterStoreV3.get(parameterInfo))
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `missing value remains unknown without sending`() {
        cachedParameter.data = ""

        assertNull(repository().getSliderValue(P_KEY_SPEED_SETTINGS))
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `write preserves optimistic store profile cache and queue order`() {
        val events = mutableListOf<String>()
        val expectedValue = ParameterTypedValueV3.Slider(SliderV3(42))
        val repository = V3DeviceSettingsRepositoryImpl(
            saveBleValue = { info, value ->
                assertEquals(parameterInfo, info)
                assertEquals(expectedValue, value)
                assertEquals(expectedValue, ParameterStoreV3.get(parameterInfo))
                assertEquals("{\"sliderValue\":17}", cachedParameter.data)
                events.add("save")
            },
            enqueuePacket = { packet ->
                assertEquals(expectedValue, ParameterStoreV3.get(parameterInfo))
                assertEquals("{\"sliderValue\":42}", cachedParameter.data)
                events.add("enqueue")
                packets.add(packet)
            },
        )

        repository.setSliderValue(P_KEY_SPEED_SETTINGS, 42)

        assertEquals(listOf("save", "enqueue"), events)
        assertEquals(1, packets.size)
        assertArrayEquals(byteArrayOf(0x00, 0x0F, 0x3D, 0x2A, 0xA6.toByte()), packets.single())
        assertEquals(42, repository.getSliderValue(P_KEY_SPEED_SETTINGS))
        assertEquals(1, packets.size)
    }

    @Test
    fun `unsupported key cannot change state profile or queue`() {
        val repository = repository()

        assertThrows(IllegalArgumentException::class.java) {
            repository.setSliderValue("unsupported_parameter", 42)
        }
        assertThrows(IllegalArgumentException::class.java) {
            repository.getSliderValue("unsupported_parameter")
        }
        assertNull(ParameterStoreV3.get(parameterInfo))
        assertEquals("{\"sliderValue\":17}", cachedParameter.data)
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `force write uses its own packet profile key and cache without changing speed`() {
        val events = mutableListOf<String>()
        val forceValue = ParameterTypedValueV3.Slider(SliderV3(42))
        val speedValue = ParameterTypedValueV3.Slider(SliderV3(17))
        ParameterStoreV3.put(parameterInfo, speedValue)
        val repository = V3DeviceSettingsRepositoryImpl(
            saveBleValue = { info, value ->
                assertEquals(forceParameterInfo, info)
                assertEquals(forceValue, value)
                assertEquals(forceValue, ParameterStoreV3.get(forceParameterInfo))
                assertEquals("{\"sliderValue\":70}", cachedForceParameter.data)
                events.add("save")
            },
            enqueuePacket = { packet ->
                assertEquals("{\"sliderValue\":42}", cachedForceParameter.data)
                assertEquals(speedValue, ParameterStoreV3.get(parameterInfo))
                assertEquals("{\"sliderValue\":17}", cachedParameter.data)
                packets.add(packet)
                events.add("enqueue")
            },
        )

        assertEquals(70, repository.getSliderValue(P_KEY_FORCE_SETTINGS))
        repository.setSliderValue(P_KEY_FORCE_SETTINGS, 42)

        assertEquals(listOf("save", "enqueue"), events)
        assertArrayEquals(byteArrayOf(0x00, 0x0F, 0x3F, 0x2A, 0x37), packets.single())
        assertEquals(42, repository.getSliderValue(P_KEY_FORCE_SETTINGS))
        assertEquals(17, repository.getSliderValue(P_KEY_SPEED_SETTINGS))
        assertEquals(1, packets.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `slider observers do not mix cache or incoming values`() = runTest {
        val speedValues = mutableListOf<Int?>()
        val forceValues = mutableListOf<Int?>()
        val maxGainValues = mutableListOf<Int?>()
        val repository = repository()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSliderValue(P_KEY_SPEED_SETTINGS).toList(speedValues)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSliderValue(P_KEY_FORCE_SETTINGS).toList(forceValues)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSliderValue(P_KEY_EMG_MAX_GAIN_VALUE).toList(maxGainValues)
        }
        ParameterStoreV3.put(forceParameterInfo, ParameterTypedValueV3.Slider(SliderV3(83)))
        ParameterStoreV3.put(parameterInfo, ParameterTypedValueV3.Slider(SliderV3(63)))
        ParameterStoreV3.put(maxGainParameterInfo, ParameterTypedValueV3.Slider(SliderV3(250)))

        assertEquals(listOf(17, 63), speedValues)
        assertEquals(listOf(70, 83), forceValues)
        assertEquals(listOf(225, 250), maxGainValues)
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `maximum sensor sensitivity writes 250 with its own packet profile and cache`() {
        val events = mutableListOf<String>()
        val expectedValue = ParameterTypedValueV3.Slider(SliderV3(250))
        val speedValue = ParameterTypedValueV3.Slider(SliderV3(17))
        val forceValue = ParameterTypedValueV3.Slider(SliderV3(70))
        ParameterStoreV3.put(parameterInfo, speedValue)
        ParameterStoreV3.put(forceParameterInfo, forceValue)
        val repository = V3DeviceSettingsRepositoryImpl(
            saveBleValue = { info, value ->
                assertEquals(maxGainParameterInfo, info)
                assertEquals(expectedValue, value)
                assertEquals(expectedValue, ParameterStoreV3.get(maxGainParameterInfo))
                assertEquals("{\"sliderValue\":225}", cachedMaxGainParameter.data)
                events.add("save")
            },
            enqueuePacket = { packet ->
                assertEquals("{\"sliderValue\":250}", cachedMaxGainParameter.data)
                packets.add(packet)
                events.add("enqueue")
            },
        )

        assertEquals(225, repository.getSliderValue(P_KEY_EMG_MAX_GAIN_VALUE))
        repository.setSliderValue(P_KEY_EMG_MAX_GAIN_VALUE, 250)

        assertEquals(listOf("save", "enqueue"), events)
        assertArrayEquals(byteArrayOf(0x00, 0x12, 0x03, 0xFA.toByte(), 0x5A), packets.single())
        assertEquals(250, repository.getSliderValue(P_KEY_EMG_MAX_GAIN_VALUE))
        assertEquals(speedValue, ParameterStoreV3.get(parameterInfo))
        assertEquals(forceValue, ParameterStoreV3.get(forceParameterInfo))
        assertEquals("{\"sliderValue\":17}", cachedParameter.data)
        assertEquals("{\"sliderValue\":70}", cachedForceParameter.data)
    }
}
