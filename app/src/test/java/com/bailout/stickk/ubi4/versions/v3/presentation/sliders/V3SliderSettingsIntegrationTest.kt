package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderResponsesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.RequestSliderValueUseCaseV3
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceSessionRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.sensors.V3SensorsCommandsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.device.UpdateDeviceInteractionUseCaseV3
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val scalarSliderMaxValues = linkedMapOf(
        P_KEY_EMG_MAX_GAIN_VALUE to 250,
        P_KEY_SPEED_SETTINGS to 100,
        P_KEY_FORCE_SETTINGS to 100,
        P_KEY_GLOBAL_THUMB_CLOSED_POSITION to 100,
        P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION to 100,
    )
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val packets = mutableListOf<ByteArray>()
    private val savedValues = mutableListOf<Pair<ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3>>()
    private lateinit var originalSubDevices: MutableSet<BaseSubDeviceInfoStruct>
    private var originalInteractionEnabled = true
    private var originalInterfaceV3Activated = false
    private var originalBleState = BLEState.State.DISCONNECTED
    private lateinit var repository: V3DeviceSettingsRepositoryImpl
    private lateinit var viewModel: V3SliderSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        originalSubDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        originalInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
        originalInterfaceV3Activated = UiState.isInterfaceV3Activated
        originalBleState = BLEState.state.value
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
            GetSliderSettingsUseCaseV3(repository), ObserveSliderSettingsUseCaseV3(repository),
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
        UiState.isInterfaceV3Activated = originalInterfaceV3Activated
        publishBleState(originalBleState)
        UiState.v3WidgetsInteractionEnabled.value = originalInteractionEnabled
        Dispatchers.resetMain()
    }

    private fun stateValues() = viewModel.uiState.value.sliders.mapValues { it.value.value }
    private fun attach() = viewModel.onAction(V3SliderSettingsAction.ViewAttached)
    private fun step(key: String) = viewModel.onAction(V3SliderSettingsAction.SliderAction(V3SliderAction.SliderStepClicked(key, 1)))
    private fun cached(key: String) = ParameterProvider.getParameterV3(ParameterInfoRegistry.require(key))

    private fun responseRepository() = V3DeviceSettingsRepositoryImpl(
        enqueuePacket = { packets.add(it) },
        saveBleValue = { info, value -> savedValues.add(info to value) },
        readCachedValues = false,
        saveValueBeforeSending = false,
    )

    private fun publishBleState(state: BLEState.State) = when (state) {
        BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
        BLEState.State.CONNECTING -> BLEState.publishConnecting()
        BLEState.State.READY -> BLEState.publishReady()
        BLEState.State.ERROR -> BLEState.publishError()
    }

    @Test
    fun `interaction gate only enables an explicitly ready V3 connection`() {
        val updateInteraction = UpdateDeviceInteractionUseCaseV3(V3DeviceSessionRepositoryImpl())
        UiState.isInterfaceV3Activated = true
        listOf(BLEState.State.CONNECTING, BLEState.State.DISCONNECTED, BLEState.State.ERROR).forEach { state ->
            BLEState.publishReady()
            assertTrue(updateInteraction(true))
            publishBleState(state)
            assertFalse(updateInteraction(true))
            assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        }

        BLEState.publishReady()
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        assertFalse(updateInteraction(false))
        assertTrue(updateInteraction(true))
        assertTrue(UiState.v3WidgetsInteractionEnabled.value)

        UiState.isInterfaceV3Activated = false
        assertFalse(updateInteraction(true))
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        UiState.isInterfaceV3Activated = true
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        assertTrue(updateInteraction(true))
        assertFalse(updateInteraction(false))
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
    }

    @Test
    fun `updating interaction gate leaves slider store cache packets and persistence untouched`() {
        val updateInteraction = UpdateDeviceInteractionUseCaseV3(V3DeviceSessionRepositoryImpl())
        val getSettings = GetSliderSettingsUseCaseV3(repository)
        val originalValues = ParameterStoreV3.values.value
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        UiState.isInterfaceV3Activated = true
        BLEState.publishReady()

        listOf(false, true, false).forEach { enabled ->
            assertEquals(enabled, updateInteraction(enabled))
            val snapshot = getSettings(initialValues.keys)
            assertEquals(enabled, snapshot.isInteractionEnabled)
            assertEquals(initialValues, snapshot.values)
            assertEquals(originalValues, ParameterStoreV3.values.value)
            assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
        }
        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
    }

    @Test
    fun `slider requests always enqueue old read packets without changing received values or interaction`() {
        val requestValue = RequestSliderValueUseCaseV3(repository)
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        val bleState = BLEState.state.value
        val interfaceActivated = UiState.isInterfaceV3Activated
        UiState.v3WidgetsInteractionEnabled.value = false

        listOf(false, true).forEach { hasReceivedValues ->
            if (hasReceivedValues) {
                scalarSliderMaxValues.keys.forEach { key ->
                    ParameterStoreV3.put(
                        ParameterInfoRegistry.require(key), ParameterTypedValueV3.Slider(SliderV3(initialValues.getValue(key) + 1)),
                    )
                }
                ParameterStoreV3.put(
                    ParameterInfoRegistry.require(P_KEY_EMG_GAIN_OPEN_VALUE), ParameterTypedValueV3.EmgGains(EMGGainsV3(61, 73)),
                )
            }
            val receivedStore = ParameterStoreV3.values.value
            initialValues.keys.forEach { key ->
                val info = ParameterInfoRegistry.require(key)
                val expectedPacket = requireNotNull(WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode))
                repeat(2) {
                    val count = packets.size
                    requestValue(key)

                    assertEquals(count + 1, packets.size)
                    assertArrayEquals(expectedPacket, packets.last())
                    assertEquals(receivedStore, ParameterStoreV3.values.value)
                    assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
                    assertTrue(savedValues.isEmpty())
                    assertFalse(UiState.v3WidgetsInteractionEnabled.value)
                    assertEquals(bleState, BLEState.state.value)
                    assertEquals(interfaceActivated, UiState.isInterfaceV3Activated)
                }
            }
        }
        assertEquals(initialValues.size * 4, packets.size)
    }

    @Test
    fun `slider reads without cache preserve absent and received values without writes`() {
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        val getSettings = GetSliderSettingsUseCaseV3(V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packets.add(it) },
            saveBleValue = { parameterInfo, value -> savedValues.add(parameterInfo to value) },
            readCachedValues = false,
        ))

        val emptyStore = ParameterStoreV3.values.value
        var expectedValues: Map<String, Int?> = initialValues.keys.associateWith { null }
        assertTrue(emptyStore.isEmpty())
        assertEquals(expectedValues, getSettings(initialValues.keys).values)
        assertEquals(emptyStore, ParameterStoreV3.values.value)
        assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })

        scalarSliderMaxValues.forEach { (key, maxValue) ->
            val info = ParameterInfoRegistry.require(key)
            assertEquals("{\"sliderValue\":${initialValues.getValue(key)}}", originalCache.getValue(key))
            assertNull(getSettings(setOf(key)).values.getValue(key))
            listOf(0, initialValues.getValue(key), maxValue).forEach { value ->
                val beforeResponse = ParameterStoreV3.values.value
                val received = ParameterTypedValueV3.Slider(SliderV3(value))
                ParameterStoreV3.put(info, received)
                expectedValues = expectedValues + (key to value)
                val receivedStore = beforeResponse + (ParameterStoreV3.toKey(info) to received)

                assertEquals(receivedStore, ParameterStoreV3.values.value)
                assertEquals(expectedValues, getSettings(initialValues.keys).values)
                assertEquals(receivedStore, ParameterStoreV3.values.value)
                assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
            }
        }
        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
    }

    @Test
    fun `default slider reads retain cached value without populating store or writing`() {
        val key = P_KEY_EMG_MAX_GAIN_VALUE
        val serialized = cached(key).data
        val getSettings = GetSliderSettingsUseCaseV3(repository)

        assertTrue(ParameterStoreV3.values.value.isEmpty())
        assertEquals(225, getSettings(setOf(key)).values.getValue(key))
        assertTrue(ParameterStoreV3.values.value.isEmpty())
        assertEquals(serialized, cached(key).data)
        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
    }

    @Test
    fun `queued slider writes preserve old packets and received state until a device response`() {
        val queuedRepository = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packets.add(it) },
            saveBleValue = { parameterInfo, value -> savedValues.add(parameterInfo to value) },
            readCachedValues = false,
            saveValueBeforeSending = false,
        )
        val getSettings = GetSliderSettingsUseCaseV3(queuedRepository)
        val setValue = SetSliderValueUseCaseV3(queuedRepository, requireInteractionEnabled = false)
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        var expectedValues: Map<String, Int?> = initialValues.keys.associateWith { null }
        UiState.v3WidgetsInteractionEnabled.value = false

        scalarSliderMaxValues.forEach { (key, maxValue) ->
            val info = ParameterInfoRegistry.require(key)
            val currentValue = initialValues.getValue(key)
            assertNull(getSettings(setOf(key)).values.getValue(key))
            listOf(null, maxValue).forEach { receivedValue ->
                if (receivedValue != null) {
                    val beforeResponse = ParameterStoreV3.values.value
                    val received = ParameterTypedValueV3.Slider(SliderV3(receivedValue))
                    ParameterStoreV3.put(info, received)
                    expectedValues = expectedValues + (key to receivedValue)
                    assertEquals(beforeResponse + (ParameterStoreV3.toKey(info) to received), ParameterStoreV3.values.value)
                }
                val receivedStore = ParameterStoreV3.values.value
                val countBeforeCommits = packets.size
                assertEquals(expectedValues, getSettings(initialValues.keys).values)
                listOf(0, currentValue, maxValue, currentValue, currentValue).forEachIndexed { index, value ->
                    val expectedPacket = requireNotNull(WidgetCommandBridgeV3.buildSetInt(
                        parameterID = info.parameterID,
                        dataCode = info.dataCode,
                        deviceAddress = info.deviceAddress,
                        dataOffset = info.dataOffsets,
                        value = value,
                    ))

                    setValue(key, value)

                    assertEquals(countBeforeCommits + index + 1, packets.size)
                    assertArrayEquals(expectedPacket, packets.last())
                    assertEquals(receivedStore, ParameterStoreV3.values.value)
                    assertEquals(expectedValues, getSettings(initialValues.keys).values)
                    assertEquals(receivedStore, ParameterStoreV3.values.value)
                    assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
                    assertTrue(savedValues.isEmpty())
                }
                assertArrayEquals(packets[countBeforeCommits + 3], packets[countBeforeCommits + 4])
            }
        }
        assertEquals(scalarSliderMaxValues.size * 10, packets.size)
    }

    @Test
    fun `queued EMG edits retain the paired draft until a complete received pair is read`() = runTest(dispatcher) {
        val open = P_KEY_EMG_GAIN_OPEN_VALUE
        val close = P_KEY_EMG_GAIN_CLOSE_VALUE
        val info = ParameterInfoRegistry.require(open)
        scalarSliderMaxValues.keys.forEach { key ->
            ParameterStoreV3.put(
                ParameterInfoRegistry.require(key), ParameterTypedValueV3.Slider(SliderV3(initialValues.getValue(key))),
            )
        }
        var expectedStore = ParameterStoreV3.values.value
        var scalarValues: Map<String, Int?> = scalarSliderMaxValues.keys.associateWith { initialValues.getValue(it) }
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        val bleState = BLEState.state.value
        val interfaceActivated = UiState.isInterfaceV3Activated
        UiState.v3WidgetsInteractionEnabled.value = false
        var duringEnqueue: (() -> Unit)? = null
        fun newRepository() = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packet ->
                packets.add(packet)
                val edit = duringEnqueue
                duringEnqueue = null
                edit?.invoke()
            },
            saveBleValue = { parameterInfo, value -> savedValues.add(parameterInfo to value) },
            readCachedValues = false,
            saveValueBeforeSending = false,
            retainEmgGainDrafts = true,
        )
        var emgRepository = newRepository()
        fun assertUnchanged() {
            assertEquals(expectedStore, ParameterStoreV3.values.value)
            assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
            assertTrue(savedValues.isEmpty())
            assertFalse(UiState.v3WidgetsInteractionEnabled.value)
            assertEquals(bleState, BLEState.state.value)
            assertEquals(interfaceActivated, UiState.isInterfaceV3Activated)
        }
        fun readPair(openValue: Int?, closeValue: Int?) {
            val count = packets.size
            assertEquals(
                scalarValues + (open to openValue) + (close to closeValue),
                GetSliderSettingsUseCaseV3(emgRepository)(initialValues.keys).values,
            )
            assertEquals(count, packets.size)
            assertUnchanged()
        }
        fun receive(pair: EMGGainsV3) {
            val count = packets.size
            val received = ParameterTypedValueV3.EmgGains(pair)
            ParameterStoreV3.put(info, received)
            expectedStore = expectedStore + (ParameterStoreV3.toKey(info) to received)
            assertEquals(count, packets.size)
            assertUnchanged()
        }
        fun commit(key: String, value: Int, vararg expectedPackets: ByteArray) {
            val count = packets.size
            SetSliderValueUseCaseV3(emgRepository, requireInteractionEnabled = false)(key, value)
            assertEquals(count + expectedPackets.size, packets.size)
            expectedPackets.forEachIndexed { index, packet -> assertArrayEquals(packet, packets[count + index]) }
            assertUnchanged()
        }
        fun pairPacket(openValue: Int, closeValue: Int) = WidgetCommandBridgeV3.buildSendEmgGains(openValue, closeValue)
        val readPacket = requireNotNull(WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode))

        // Unknown and incomplete RX pairs request their partner and drop each edit, despite the serialized cache.
        listOf(null, EMGGainsV3(0, 0), EMGGainsV3(0, 40), EMGGainsV3(20, 0)).forEach { pair ->
            if (pair != null) {
                receive(pair)
                assertEquals(
                    mapOf(open to pair.openGain, close to pair.closeGain),
                    GetSliderSettingsUseCaseV3(repository)(setOf(open, close)).values,
                )
            }
            readPair(null, null)
            commit(open, 22, readPacket)
            commit(close, 44, readPacket)
        }

        receive(EMGGainsV3(20, 40))
        // Without a preceding Get, the first edit falls back to RX and retains its draft before enqueue.
        duringEnqueue = { SetSliderValueUseCaseV3(emgRepository, requireInteractionEnabled = false)(close, 44) }
        commit(open, 22, pairPacket(22, 40), pairPacket(22, 44))
        commit(open, 0, pairPacket(0, 44))
        commit(close, 0, pairPacket(0, 0))
        commit(open, 100, pairPacket(100, 0))
        commit(close, 100, pairPacket(100, 100))
        commit(close, 100, pairPacket(100, 100))

        val countBeforeRequest = packets.size
        RequestSliderValueUseCaseV3(emgRepository)(open)
        assertEquals(countBeforeRequest + 1, packets.size)
        assertArrayEquals(readPacket, packets.last())
        assertUnchanged()
        commit(open, 99, pairPacket(99, 100))

        // Every consumed complete RX refreshes the draft, including an unchanged stale snapshot.
        readPair(20, 40)
        commit(close, 45, pairPacket(20, 45))
        readPair(20, 40)
        commit(open, 26, pairPacket(26, 40))
        receive(EMGGainsV3(61, 73))
        commit(close, 50, pairPacket(26, 50))
        readPair(61, 73)
        commit(open, 60, pairPacket(60, 73))

        // Response observers consume every complete pair, even when the received store value is unchanged.
        val replies = linkedMapOf(open to mutableListOf<Int?>(), close to mutableListOf<Int?>())
        val observeResponses = ObserveSliderResponsesUseCaseV3(emgRepository)
        val jobs = replies.keys.map { key ->
            observeResponses(key) {
                replies.getValue(key).add(GetSliderSettingsUseCaseV3(emgRepository)(setOf(key)).values.getValue(key))
                assertUnchanged()
            }
        }
        try {
            runCurrent()
            assertTrue(replies.values.all { it.isEmpty() })
            commit(close, 88, pairPacket(60, 88))
            repeat(2) { index ->
                receive(EMGGainsV3(61, 73))
                runCurrent()
                assertEquals(List(index + 1) { 61 }, replies.getValue(open))
                assertEquals(List(index + 1) { 73 }, replies.getValue(close))
                if (index == 0) {
                    commit(open, 0, pairPacket(0, 73))
                    commit(close, 99, pairPacket(0, 99))
                } else commit(open, 60, pairPacket(60, 73))
            }
            listOf(EMGGainsV3(0, 99), EMGGainsV3(99, 0), EMGGainsV3(0, 0)).forEach { pair ->
                receive(pair)
                runCurrent()
                readPair(null, null)
                commit(close, 100, pairPacket(60, 100))
            }
            assertEquals(listOf(61, 61, null, null, null), replies.getValue(open))
            assertEquals(listOf(73, 73, null, null, null), replies.getValue(close))
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
        ParameterStoreV3.clear()
        expectedStore = emptyMap()
        scalarValues = scalarSliderMaxValues.keys.associateWith { null }
        emgRepository = newRepository()
        readPair(null, null)
        commit(open, 0, pairPacket(0, 100))
        commit(close, 0, pairPacket(0, 0))
        emgRepository = newRepository()
        readPair(null, null)
        commit(open, 100, pairPacket(100, 0))
    }

    @Test
    fun `response observers preserve all seven slider replies without replay writes or interaction gating`() = runTest(dispatcher) {
        val responseRepository = responseRepository()
        val getSettings = GetSliderSettingsUseCaseV3(responseRepository)
        val observeResponses = ObserveSliderResponsesUseCaseV3(responseRepository)
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        val bleState = BLEState.state.value
        val interfaceActivated = UiState.isInterfaceV3Activated
        UiState.v3WidgetsInteractionEnabled.value = false
        scalarSliderMaxValues.keys.forEach { key ->
            ParameterStoreV3.put(
                ParameterInfoRegistry.require(key), ParameterTypedValueV3.Slider(SliderV3(initialValues.getValue(key))),
            )
        }
        val pairInfo = ParameterInfoRegistry.require(P_KEY_EMG_GAIN_OPEN_VALUE)
        ParameterStoreV3.put(pairInfo, ParameterTypedValueV3.EmgGains(EMGGainsV3(20, 40)))
        val replies = initialValues.keys.associateWith { mutableListOf<Int?>() }
        val jobs = replies.keys.map { key ->
            observeResponses(key) { replies.getValue(key).add(getSettings(setOf(key)).values.getValue(key)) }
        }
        fun receive(info: ParameterInfo<Int, Int, Int, Int>, value: ParameterTypedValueV3) {
            ParameterStoreV3.put(info, value)
            val receivedStore = ParameterStoreV3.values.value
            runCurrent()
            assertEquals(receivedStore, ParameterStoreV3.values.value)
        }
        try {
            runCurrent()
            assertTrue(replies.values.all { it.isEmpty() })
            scalarSliderMaxValues.keys.forEach { key ->
                val info = ParameterInfoRegistry.require(key)
                listOf(0, 0, 63, 63).forEach { value -> receive(info, ParameterTypedValueV3.Slider(SliderV3(value))) }
                assertEquals(listOf(0, 0, 63, 63), replies.getValue(key))
            }
            repeat(2) { receive(pairInfo, ParameterTypedValueV3.EmgGains(EMGGainsV3(61, 73))) }
            assertEquals(listOf(61, 61), replies.getValue(P_KEY_EMG_GAIN_OPEN_VALUE))
            assertEquals(listOf(73, 73), replies.getValue(P_KEY_EMG_GAIN_CLOSE_VALUE))

            val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
            val beforeOtherParameters = replies.mapValues { it.value.toList() }
            listOf(
                speedInfo.copy(deviceAddress = 999), speedInfo.copy(parameterID = 999), speedInfo.copy(dataCode = 999),
            ).forEach { info -> receive(info, ParameterTypedValueV3.Slider(SliderV3(99))) }
            assertEquals(beforeOtherParameters, replies)
            receive(speedInfo, ParameterTypedValueV3.Text("wrong typed value"))
            assertEquals(listOf(0, 0, 63, 63, null), replies.getValue(P_KEY_SPEED_SETTINGS))

            assertTrue(packets.isEmpty())
            assertTrue(savedValues.isEmpty())
            assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
            assertFalse(UiState.v3WidgetsInteractionEnabled.value)
            assertEquals(bleState, BLEState.state.value)
            assertEquals(interfaceActivated, UiState.isInterfaceV3Activated)
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
    }

    @Test
    fun `response delivery reads latest typed state skips removed values and stops after cancellation`() = runTest(dispatcher) {
        val key = P_KEY_SPEED_SETTINGS
        val info = ParameterInfoRegistry.require(key)
        val responseRepository = responseRepository()
        val getSettings = GetSliderSettingsUseCaseV3(responseRepository)
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        val replies = mutableListOf<Int?>()
        val job = ObserveSliderResponsesUseCaseV3(responseRepository)(key) {
            replies.add(getSettings(setOf(key)).values.getValue(key))
        }
        try {
            runCurrent()
            assertTrue(replies.isEmpty())
            assertNull(getSettings(setOf(key)).values.getValue(key))
            assertTrue(ParameterStoreV3.values.value.isEmpty())
            listOf(42, 43).forEach { ParameterStoreV3.put(info, ParameterTypedValueV3.Slider(SliderV3(it))) }
            val receivedStore = ParameterStoreV3.values.value
            runCurrent()
            assertEquals(listOf(43, 43), replies)
            assertEquals(receivedStore, ParameterStoreV3.values.value)

            ParameterStoreV3.put(info, ParameterTypedValueV3.Slider(SliderV3(51)))
            ParameterStoreV3.clear()
            runCurrent()
            assertEquals(listOf(43, 43), replies)
            assertNull(getSettings(setOf(key)).values.getValue(key))
            assertTrue(ParameterStoreV3.values.value.isEmpty())
            job.cancel()
            runCurrent()
            ParameterStoreV3.put(info, ParameterTypedValueV3.Slider(SliderV3(63)))
            runCurrent()
            assertEquals(listOf(43, 43), replies)
            assertTrue(packets.isEmpty())
            assertTrue(savedValues.isEmpty())
            assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })
        } finally {
            job.cancel()
            runCurrent()
        }
    }

    @Test
    fun `default slider use case preserves the interaction lock and optimistic writes`() {
        val key = P_KEY_EMG_MAX_GAIN_VALUE
        val info = ParameterInfoRegistry.require(key)
        val setValue = SetSliderValueUseCaseV3(repository)
        val originalValues = ParameterStoreV3.values.value
        val originalCache = initialValues.keys.associateWith { cached(it).data }
        UiState.v3WidgetsInteractionEnabled.value = false

        setValue(key, 240)

        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
        assertEquals(originalValues, ParameterStoreV3.values.value)
        assertEquals(originalCache, initialValues.keys.associateWith { cached(it).data })

        UiState.v3WidgetsInteractionEnabled.value = true
        setValue(key, 240)

        val expectedValue = ParameterTypedValueV3.Slider(SliderV3(240))
        assertEquals(expectedValue, ParameterStoreV3.get(info))
        assertEquals(listOf(info to expectedValue), savedValues)
        assertEquals("{\"sliderValue\":240}", cached(key).data)
        assertEquals(1, packets.size)
    }

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
        viewModel.onAction(V3SliderSettingsAction.ViewDetached)
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
            viewModel.onAction(V3SliderSettingsAction.SliderAction(V3SliderAction.SliderValueChanged(key, upper + 10)))
            assertEquals(upper, stateValues()[key])
            assertEquals(before, packets.size)
            viewModel.onAction(V3SliderSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(key, upper + 10)))
            runCurrent()
            assertEquals(upper, repository.getSliderValue(key))
            viewModel.onAction(V3SliderSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(key, -1)))
            runCurrent()
            assertEquals(0, repository.getSliderValue(key))
        }
        assertEquals(14, packets.size)
    }

    @Test
    fun `shared lock reaches sensors and cancels pending writes across repository recreation`() = runTest(dispatcher) {
        val sensors = V3SensorsCommandsRepositoryImpl(
            enqueuePacket = { packets.add(it) }, refreshWidgets = { error("No refresh expected") },
        )
        assertTrue(sensors.interactionEnabled.value)
        attach()
        runCurrent()
        initialValues.keys.forEach(::step)
        UiState.v3WidgetsInteractionEnabled.value = false
        val recreated = V3DeviceSettingsRepositoryImpl(enqueuePacket = { packets.add(it) })
        assertFalse(recreated.sliderInteractionEnabled.value)
        assertFalse(recreated.toggleSliderInteractionEnabled.value)
        assertFalse(recreated.spinnerInteractionEnabled.value)
        assertFalse(sensors.interactionEnabled.value)
        initialValues.keys.forEach { viewModel.onAction(V3SliderSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(it, 50))) }
        runCurrent()
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.isEnabled) }
        advanceTimeBy(301)
        runCurrent()
        UiState.v3WidgetsInteractionEnabled.value = true
        runCurrent()
        assertTrue(sensors.interactionEnabled.value)
        assertTrue(recreated.sliderInteractionEnabled.value)
        viewModel.uiState.value.sliders.values.forEach { assertTrue(it.isEnabled) }
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
        viewModel.onAction(V3SliderSettingsAction.ViewDetached)
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
