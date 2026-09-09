package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3SliderSettingsIntegrationTest {
    private val initialValues = linkedMapOf(
        P_KEY_SPEED_SETTINGS to 17,
        P_KEY_FORCE_SETTINGS to 70,
        P_KEY_EMG_MAX_GAIN_VALUE to 225,
        P_KEY_EMG_GAIN_OPEN_VALUE to 20,
        P_KEY_EMG_GAIN_CLOSE_VALUE to 40,
        P_KEY_GLOBAL_THUMB_CLOSED_POSITION to 35,
        P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION to 55,
    )
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val packets = mutableListOf<ByteArray>()
    private val savedValues = mutableListOf<Pair<ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3>>()
    private lateinit var originalSubDevices: MutableSet<BaseSubDeviceInfoStruct>
    private var originalInteractionEnabled = true
    private lateinit var repository: V3DeviceSettingsRepositoryImpl
    private lateinit var viewModel: V3SliderSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        originalSubDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        originalInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
        UiState.v3WidgetsInteractionEnabled.value = true
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(
            BaseSubDeviceInfoStruct(
                deviceAddress = 1,
                parametersList = ArrayList(initialValues.keys
                    .distinctBy { ParameterStoreV3.toKey(ParameterInfoRegistry.require(it)) }
                    .map { key ->
                        val info = ParameterInfoRegistry.require(key)
                        BaseParameterInfoStruct(
                            ID = info.parameterID,
                            dataCode = info.dataCode,
                            data = if (key == P_KEY_EMG_GAIN_OPEN_VALUE) {
                                "{\"openGain\":20,\"closeGain\":40}"
                            } else "{\"sliderValue\":${initialValues.getValue(key)}}",
                        )
                    }),
            )
        )
        repository = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packets.add(it) },
            saveBleValue = { info, value ->
                assertEquals(value, ParameterStoreV3.get(info))
                savedValues.add(info to value)
            },
        )
        viewModel = V3SliderSettingsViewModel(
            repository,
            SetSliderValueUseCaseV3(repository),
            initialValues.keys,
        )
        store.put("settings", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalSubDevices
        UiState.v3WidgetsInteractionEnabled.value = originalInteractionEnabled
        Dispatchers.resetMain()
    }

    private fun stateValues() = viewModel.uiState.value.sliders.mapValues { it.value.value }
    private fun attach() = viewModel.onViewAttached()
    private fun step(key: String) = viewModel.onAction(V3SliderAction.SliderStepClicked(key, 1))
    private fun cached(key: String) = ParameterProvider.getParameterV3(ParameterInfoRegistry.require(key))

    @Test
    fun `all seven sliders restore cache and observe incoming data without sending`() = runTest(dispatcher) {
        attach()
        runCurrent()
        assertEquals(initialValues, stateValues())
        initialValues.keys.filter { it != P_KEY_EMG_GAIN_OPEN_VALUE && it != P_KEY_EMG_GAIN_CLOSE_VALUE }
            .forEach { key ->
                ParameterStoreV3.put(ParameterInfoRegistry.require(key), ParameterTypedValueV3.Slider(SliderV3(63)))
            }
        ParameterStoreV3.put(
            ParameterInfoRegistry.require(P_KEY_EMG_GAIN_OPEN_VALUE),
            ParameterTypedValueV3.EmgGains(EMGGainsV3(61, 73)),
        )
        runCurrent()
        assertEquals(initialValues.mapValues { (key, _) ->
            when (key) {
                P_KEY_EMG_GAIN_OPEN_VALUE -> 61
                P_KEY_EMG_GAIN_CLOSE_VALUE -> 73
                else -> 63
            }
        }, stateValues())
        viewModel.onViewDetached()
        attach()
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `clearing parameter store refreshes slider state from cache without a widget event or write`() = runTest(dispatcher) {
        attach()
        runCurrent()
        ParameterStoreV3.put(
            ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS), ParameterTypedValueV3.Slider(SliderV3(63)),
        )
        runCurrent()
        assertEquals(63, stateValues()[P_KEY_SPEED_SETTINGS])
        ParameterStoreV3.clear()
        runCurrent()
        assertEquals(initialValues, stateValues())
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `writing either gain first preserves the latest other gain and uses the paired packet`() {
        val open = P_KEY_EMG_GAIN_OPEN_VALUE
        val close = P_KEY_EMG_GAIN_CLOSE_VALUE
        listOf(listOf(open to 22, close to 44), listOf(close to 44, open to 22)).forEach { writes ->
            packets.clear()
            savedValues.clear()
            cached(open).data = "{\"openGain\":10,\"closeGain\":30}"
            ParameterStoreV3.put(ParameterInfoRegistry.require(open), ParameterTypedValueV3.EmgGains(EMGGainsV3(20, 40)))
            writes.forEach { (key, value) -> repository.setSliderValue(key, value) }

            val firstOpen = writes.first().first == open
            val firstValue = if (firstOpen) EMGGainsV3(22, 40) else EMGGainsV3(20, 44)
            assertEquals(listOf(
                ParameterInfoRegistry.require(writes[0].first) to ParameterTypedValueV3.EmgGains(firstValue),
                ParameterInfoRegistry.require(writes[1].first) to ParameterTypedValueV3.EmgGains(EMGGainsV3(22, 44)),
            ), savedValues)
            assertEquals(2, packets.size)
            assertArrayEquals(
                if (firstOpen) byteArrayOf(0x80.toByte(), 0x12, 0x03, 0x00, 0x89.toByte(), 0x01, 0x16, 0x28, 0x0C)
                else byteArrayOf(0x80.toByte(), 0x12, 0x03, 0x00, 0x89.toByte(), 0x01, 0x14, 0x2C, 0xFC.toByte()),
                packets[0],
            )
            assertArrayEquals(byteArrayOf(0x80.toByte(), 0x12, 0x03, 0x00, 0x89.toByte(), 0x01, 0x16, 0x2C, 0x6D), packets[1])
            assertEquals("{\"openGain\":22,\"closeGain\":44}", cached(open).data)
            assertEquals(cached(open), cached(close))
            assertEquals(22, repository.getSliderValue(open))
            assertEquals(44, repository.getSliderValue(close))
            initialValues.filterKeys { it != open && it != close }.forEach { (key, value) ->
                assertEquals(value, repository.getSliderValue(key))
            }
        }
    }

    @Test
    fun `finger positions use independent packets profile keys and caches at both boundaries`() {
        val cases = listOf(
            Triple(P_KEY_GLOBAL_THUMB_CLOSED_POSITION, 0, byteArrayOf(0, 0x0F, 0x44, 0, 0xFF.toByte())),
            Triple(P_KEY_GLOBAL_THUMB_CLOSED_POSITION, 100, byteArrayOf(0, 0x0F, 0x44, 0x64, 0xFB.toByte())),
            Triple(P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION, 0, byteArrayOf(0, 0x0F, 0x46, 0, 0x6E)),
            Triple(P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION, 100, byteArrayOf(0, 0x0F, 0x46, 0x64, 0x6A)),
        )
        cases.forEach { (key, value, packet) ->
            val before = initialValues.keys.associateWith(repository::getSliderValue)
            repository.setSliderValue(key, value)
            assertArrayEquals(packet, packets.last())
            assertEquals(ParameterInfoRegistry.require(key) to ParameterTypedValueV3.Slider(SliderV3(value)), savedValues.last())
            // The existing codec omits the default sliderValue=0 from JSON.
            assertEquals(if (value == 0) "{}" else "{\"sliderValue\":$value}", cached(key).data)
            assertEquals(before + (key to value), initialValues.keys.associateWith(repository::getSliderValue))
        }
        assertEquals(4, packets.size)
        assertEquals(4, savedValues.size)
    }

    @Test
    fun `paired button timers preserve the other draft when the first write updates the shared store`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step(P_KEY_EMG_GAIN_OPEN_VALUE)
        step(P_KEY_EMG_GAIN_CLOSE_VALUE)
        advanceTimeBy(100)
        step(P_KEY_EMG_GAIN_OPEN_VALUE)
        advanceTimeBy(200)
        runCurrent()
        assertEquals(22, stateValues()[P_KEY_EMG_GAIN_OPEN_VALUE])
        assertEquals(41, stateValues()[P_KEY_EMG_GAIN_CLOSE_VALUE])
        assertEquals(listOf(ParameterTypedValueV3.EmgGains(EMGGainsV3(20, 41))), savedValues.map { it.second })
        advanceTimeBy(100)
        runCurrent()
        assertEquals(ParameterTypedValueV3.EmgGains(EMGGainsV3(22, 41)), savedValues.last().second)
        assertEquals(2, packets.size)
    }

    @Test
    fun `all sliders clamp input to their own range and send only on release`() = runTest(dispatcher) {
        attach()
        runCurrent()
        initialValues.keys.forEach { key ->
            val before = packets.size
            val upper = if (key == P_KEY_EMG_MAX_GAIN_VALUE) 250 else 100
            viewModel.onAction(V3SliderAction.SliderValueChanged(key, upper + 10))
            assertEquals(upper, stateValues()[key])
            assertEquals(before, packets.size)
            viewModel.onAction(V3SliderAction.SliderChangeCommitted(key, upper + 10))
            runCurrent()
            assertEquals(upper, repository.getSliderValue(key))
            viewModel.onAction(V3SliderAction.SliderChangeCommitted(key, -1))
            runCurrent()
            assertEquals(0, repository.getSliderValue(key))
        }
        assertEquals(14, packets.size)
    }

    @Test
    fun `lock cancels all seven pending writes and rejects releases`() = runTest(dispatcher) {
        attach()
        runCurrent()
        initialValues.keys.forEach(::step)
        UiState.v3WidgetsInteractionEnabled.value = false
        initialValues.keys.forEach { viewModel.onAction(V3SliderAction.SliderChangeCommitted(it, 50)) }
        runCurrent()
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.isEnabled) }
        advanceTimeBy(301)
        runCurrent()
        UiState.v3WidgetsInteractionEnabled.value = true
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `detaching cancels all seven pending writes and reattach restores stored values`() = runTest(dispatcher) {
        attach()
        runCurrent()
        initialValues.keys.forEach(::step)
        viewModel.onViewDetached()
        advanceTimeBy(301)
        runCurrent()
        attach()
        assertEquals(initialValues, stateValues())
        advanceTimeBy(301)
        runCurrent()
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }

    @Test
    fun `clearing viewmodel cancels all seven pending writes`() = runTest(dispatcher) {
        attach()
        runCurrent()
        initialValues.keys.forEach(::step)
        store.clear()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(0, packets.size)
        assertEquals(0, savedValues.size)
    }
}
