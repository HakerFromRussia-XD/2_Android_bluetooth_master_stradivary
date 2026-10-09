package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.di.V3SpecialSettingsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSpinnerSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.RequestSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetInfo
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SpinnerSettingsIntegrationTest {
    private val key = P_KEY_HAND_CONTROL_MODE
    private val info = ParameterInfoRegistry.require(key)
    private val gestureKey = P_KEY_GESTURE_CHANGE_MODE
    private val gestureInfo = ParameterInfoRegistry.require(gestureKey)
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val cachedValue = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{\"spinnerValue\":2}")
    private val cachedGestureValue = BaseParameterInfoStruct(ID = gestureInfo.parameterID, dataCode = gestureInfo.dataCode, data = "{\"spinnerValue\":1}")
    private val spinnerRanges = linkedMapOf(
        key to 0..4, gestureKey to 0..1, P_KEY_EMG_CONTROL_MODE to 0..3, P_KEY_LEFT_RIGHT_HAND to 0..1,
    )
    private val cachedValues = linkedMapOf(
        key to cachedValue, gestureKey to cachedGestureValue,
        P_KEY_EMG_CONTROL_MODE to cachedSpinner(P_KEY_EMG_CONTROL_MODE, 2),
        P_KEY_LEFT_RIGHT_HAND to cachedSpinner(P_KEY_LEFT_RIGHT_HAND, 1),
    )
    private val savedKeys = mutableListOf<String>()
    private val packets = mutableListOf<ByteArray>()
    private val savedValues = mutableListOf<ParameterTypedValueV3>()
    private val source = WidgetsSource()
    private lateinit var repository: V3DeviceSettingsRepositoryImpl
    private lateinit var viewModel: V3SpecialSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = ArrayList(cachedValues.values),
        ))
        repository = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = {
                // Preserve store -> profile -> serialized cache -> command queue ordering.
                assertEquals(packets.size + 1, savedValues.size)
                val value = (savedValues.last() as ParameterTypedValueV3.Spinner).value.spinnerValue
                val cache = if (savedKeys.last() == key) cachedValue else cachedGestureValue
                // The existing codec omits the default zero value.
                assertEquals(if (value == 0) "{}" else "{\"spinnerValue\":$value}", cache.data)
                packets.add(it)
            },
            saveBleValue = { parameter, value ->
                if (parameter == info) savedKeys.add(key) else {
                    assertEquals(gestureInfo, parameter)
                    savedKeys.add(gestureKey)
                }
                assertEquals(value, ParameterStoreV3.get(parameter))
                savedValues.add(value)
            },
        )
        viewModel = V3SpecialSettingsViewModelFactory(repository, source, repository, repository, NoSettingsProfilesRepository, FakeV3AppSettingsRepository(), sessionRepository = source)
            .create(V3SpecialSettingsViewModel::class.java)
        store.put("special-settings", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onAction(V3SpecialSettingsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SpecialSettingsAction.ViewDetached)
    private fun section(section: V3SpecialSettingsSection) = viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(section))
    private fun select(value: Int, parameterKey: String = key) =
        viewModel.onAction(V3SpecialSettingsAction.SpinnerAction(V3SpinnerAction.SpinnerValueSelected(parameterKey, value)))
    private fun state(parameterKey: String = key) = viewModel.uiState.value.spinners.getValue(parameterKey)
    private fun incoming(value: Int, parameterKey: String = key) =
        ParameterStoreV3.put(ParameterInfoRegistry.require(parameterKey), ParameterTypedValueV3.Spinner(SpinnerV3(value)))
    private fun assertNoWrites() {
        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
    }
    private fun cachedSpinner(parameterKey: String, value: Int): BaseParameterInfoStruct {
        val parameter = ParameterInfoRegistry.require(parameterKey)
        return BaseParameterInfoStruct(ID = parameter.parameterID, dataCode = parameter.dataCode, data = "{\"spinnerValue\":$value}")
    }
    private fun queuedRepository() = V3DeviceSettingsRepositoryImpl(
        enqueuePacket = packets::add,
        saveBleValue = { _, value -> savedValues.add(value) },
        readCachedValues = false,
        saveValueBeforeSending = false,
    )
    private fun cachedData() = cachedValues.mapValues { it.value.data }
    private fun seedUnrelatedSlider() = ParameterStoreV3.put(
        ParameterInfoRegistry.require(P_KEY_FORCE_SETTINGS), ParameterTypedValueV3.Slider(SliderV3(70)),
    )
    private fun assertUnchanged(receivedStore: Map<ParameterStoreKeyV3, ParameterTypedValueV3>, cache: Map<String, String>) {
        assertEquals(receivedStore, ParameterStoreV3.values.value)
        assertEquals(cache, cachedData())
        assertTrue(savedValues.isEmpty())
        assertTrue(savedKeys.isEmpty())
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
    }

    @Test
    fun `native profile rows preserve raw indices and nil GET without local profile or received state writes`() {
        val profileInfo = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
        val profileCache = cachedSpinner(P_KEY_SETTINGS_PROFILE, 99)
        GlobalParameters.baseSubDevicesInfoStructSetV3.first().parametersList.add(profileCache)
        val native = queuedRepository()
        val get = GetSpinnerSettingsUseCaseV3(native)
        val request = RequestSpinnerValueUseCaseV3(native)
        val set = SetSpinnerValueUseCaseV3(native, requireInteractionEnabled = false, allowedValues = { key ->
            if (key == P_KEY_SETTINGS_PROFILE) Int.MIN_VALUE..Int.MAX_VALUE else V3SpinnerSettingsRules.allowedValues(key)
        })
        UiState.v3WidgetsInteractionEnabled.value = false
        assertNull(get(setOf(P_KEY_SETTINGS_PROFILE)).values[P_KEY_SETTINGS_PROFILE])
        assertThrows(IllegalArgumentException::class.java) { SetSpinnerValueUseCaseV3(native)(P_KEY_SETTINGS_PROFILE, 0) }
        request(P_KEY_SETTINGS_PROFILE)
        assertTrue(packets.isEmpty())
        incoming(2, P_KEY_SETTINGS_PROFILE)
        val receivedStore = ParameterStoreV3.values.value
        val cache = cachedData()
        for (index in listOf(0, 1, 1, 2, 99, -1, 256, Int.MIN_VALUE, Int.MAX_VALUE)) {
            set(P_KEY_SETTINGS_PROFILE, index)
            val header = byteArrayOf(0, 15, 2, index.toByte())
            var crc = 0
            header.forEach { byte ->
                crc = crc xor (byte.toInt() and 255)
                repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8C else crc ushr 1 }
            }
            assertArrayEquals(header + crc.toByte(), packets.last())
            assertEquals(2, get(setOf(P_KEY_SETTINGS_PROFILE)).values[P_KEY_SETTINGS_PROFILE])
            assertUnchanged(receivedStore, cache)
            assertEquals("{\"spinnerValue\":99}", profileCache.data)
        }
        assertEquals(9, packets.size)
        assertEquals(ParameterTypedValueV3.Spinner(SpinnerV3(2)), ParameterStoreV3.get(profileInfo))
        assertThrows(IllegalArgumentException::class.java) { set(P_KEY_HAND_CONTROL_MODE, 5) }
        assertThrows(IllegalArgumentException::class.java) { set("unknown", 0) }
        assertEquals(9, packets.size)
    }

    @Test
    fun `uncached Spinner reads preserve missing zero negative and out of range received values without writes`() {
        val getSettings = GetSpinnerSettingsUseCaseV3(queuedRepository())
        val cache = cachedData()
        var expectedValues: Map<String, Int?> = spinnerRanges.keys.associateWith { null }
        UiState.v3WidgetsInteractionEnabled.value = false
        seedUnrelatedSlider()
        val initialStore = ParameterStoreV3.values.value
        assertEquals(expectedValues, getSettings(spinnerRanges.keys).values)
        assertUnchanged(initialStore, cache)

        spinnerRanges.keys.forEach { parameterKey ->
            listOf(0, -3, 255).forEach { received ->
                incoming(received, parameterKey)
                expectedValues = expectedValues + (parameterKey to received)
                val receivedStore = ParameterStoreV3.values.value
                val snapshot = getSettings(spinnerRanges.keys)
                assertEquals(expectedValues, snapshot.values)
                assertFalse(snapshot.isInteractionEnabled)
                assertUnchanged(receivedStore, cache)
                assertTrue(packets.isEmpty())
            }
        }
    }

    @Test
    fun `queued Spinner choices send old packets immediately offline without changing received state or saving`() {
        val spinnerRepository = queuedRepository()
        val getSettings = GetSpinnerSettingsUseCaseV3(spinnerRepository)
        val defaultSet = SetSpinnerValueUseCaseV3(spinnerRepository)
        val setValue = SetSpinnerValueUseCaseV3(spinnerRepository, requireInteractionEnabled = false)
        val cache = cachedData()
        val bleState = BLEState.state.value
        val interfaceActivated = UiState.isInterfaceV3Activated
        var expectedValues: Map<String, Int?> = spinnerRanges.keys.associateWith { null }
        UiState.v3WidgetsInteractionEnabled.value = false
        seedUnrelatedSlider()
        spinnerRanges.forEach { (parameterKey, choices) ->
            val parameter = ParameterInfoRegistry.require(parameterKey)
            listOf(null, choices.last, 255).forEach { received ->
                if (received != null) {
                    incoming(received, parameterKey)
                    expectedValues = expectedValues + (parameterKey to received)
                }
                val receivedStore = ParameterStoreV3.values.value
                (choices.toList() + listOf(0, 0)).forEach { choice ->
                    val count = packets.size
                    defaultSet(parameterKey, choice)
                    assertEquals(count, packets.size)
                    assertUnchanged(receivedStore, cache)
                    setValue(parameterKey, choice)
                    assertEquals(count + 1, packets.size)
                    assertArrayEquals(requireNotNull(WidgetCommandBridgeV3.buildSetInt(
                        parameter.parameterID, parameter.dataCode, parameter.deviceAddress, parameter.dataOffsets, choice,
                    )), packets.last())
                    assertEquals(expectedValues, getSettings(spinnerRanges.keys).values)
                    assertUnchanged(receivedStore, cache)
                    assertEquals(bleState, BLEState.state.value)
                    assertEquals(interfaceActivated, UiState.isInterfaceV3Activated)
                }
            }
        }
        val receivedStore = ParameterStoreV3.values.value
        val count = packets.size
        listOf(defaultSet, setValue).forEach { useCase ->
            spinnerRanges.forEach { (parameterKey, choices) ->
                listOf(-1, choices.last + 1, 255).forEach { invalid ->
                    assertThrows(IllegalArgumentException::class.java) { useCase(parameterKey, invalid) }
                }
            }
            listOf(P_KEY_SETTINGS_PROFILE, P_KEY_DEVICE_ROLE, "unknown-spinner").forEach { unsupported ->
                assertThrows(IllegalArgumentException::class.java) { useCase(unsupported, 0) }
            }
        }
        assertEquals(spinnerRanges.values.sumOf { it.count() + 2 } * 3, packets.size)
        assertEquals(count, packets.size)
        assertUnchanged(receivedStore, cache)
    }

    @Test
    fun `Spinner requests repeat old read packets offline for cached and received values without writes`() {
        val request = RequestSpinnerValueUseCaseV3(queuedRepository())
        val cache = cachedData()
        val bleState = BLEState.state.value
        val interfaceActivated = UiState.isInterfaceV3Activated
        UiState.v3WidgetsInteractionEnabled.value = false
        seedUnrelatedSlider()
        listOf(false, true).forEach { hasReceivedValues ->
            if (hasReceivedValues) spinnerRanges.keys.forEach { incoming(0, it) }
            val receivedStore = ParameterStoreV3.values.value
            spinnerRanges.keys.forEach { parameterKey ->
                val parameter = ParameterInfoRegistry.require(parameterKey)
                val expectedPacket = requireNotNull(WidgetCommandBridgeV3.buildReadRequest(parameter.parameterID, parameter.dataCode))
                repeat(2) {
                    val count = packets.size
                    request(parameterKey)
                    assertEquals(count + 1, packets.size)
                    assertArrayEquals(expectedPacket, packets.last())
                    assertUnchanged(receivedStore, cache)
                    assertEquals(bleState, BLEState.state.value)
                    assertEquals(interfaceActivated, UiState.isInterfaceV3Activated)
                }
            }
        }
        assertEquals(spinnerRanges.size * 4, packets.size)
    }

    @Test
    fun `Spinner callback runs before queue only for allowed sends and its failure stops transmission`() = runTest(dispatcher) {
        val parameterKey = P_KEY_LEFT_RIGHT_HAND
        val parameter = ParameterInfoRegistry.require(parameterKey)
        val cache = cachedData()
        UiState.v3WidgetsInteractionEnabled.value = false
        seedUnrelatedSlider()
        incoming(0, parameterKey)
        var expectedStore = ParameterStoreV3.values.value
        val updates = mutableListOf<ParameterStoreKeyV3>()
        backgroundScope.launch { ParameterStoreV3.updates.collect { updates.add(it) } }
        runCurrent()
        val events = mutableListOf<String>()
        val failure = IllegalStateException("Hand side callback failed")
        var failCallback = false
        val spinnerRepository = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packet ->
                assertEquals("callback", events.last())
                assertUnchanged(expectedStore, cache)
                events.add("queue")
                packets.add(packet)
            },
            saveBleValue = { _, value -> savedValues.add(value) },
            readCachedValues = false,
            saveValueBeforeSending = false,
            beforeSpinnerValueSent = { parameterInfo, value ->
                assertEquals(parameter, parameterInfo)
                assertUnchanged(expectedStore, cache)
                events.add("callback")
                if (failCallback) throw failure
                val typed = ParameterTypedValueV3.Spinner(SpinnerV3(value))
                ParameterStoreV3.put(parameterInfo, typed)
                expectedStore = expectedStore + (ParameterStoreV3.toKey(parameterInfo) to typed)
            },
        )
        SetSpinnerValueUseCaseV3(spinnerRepository)(parameterKey, 1)
        assertTrue(events.isEmpty())
        assertNoWrites()
        assertUnchanged(expectedStore, cache)

        val setValue = SetSpinnerValueUseCaseV3(spinnerRepository, requireInteractionEnabled = false)
        setValue(parameterKey, 1)
        runCurrent()
        assertEquals(listOf("callback", "queue"), events)
        assertEquals(listOf(ParameterStoreV3.toKey(parameter)), updates)
        assertEquals(ParameterTypedValueV3.Spinner(SpinnerV3(1)), ParameterStoreV3.get(parameter))
        assertArrayEquals(requireNotNull(WidgetCommandBridgeV3.buildSetInt(
            parameter.parameterID, parameter.dataCode, parameter.deviceAddress, parameter.dataOffsets, 1,
        )), packets.single())
        assertUnchanged(expectedStore, cache)

        failCallback = true
        assertEquals(failure, assertThrows(IllegalStateException::class.java) { setValue(parameterKey, 0) })
        runCurrent()
        assertEquals(listOf("callback", "queue", "callback"), events)
        assertEquals(listOf(ParameterStoreV3.toKey(parameter)), updates)
        assertEquals(1, packets.size)
        assertUnchanged(expectedStore, cache)
    }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles restore incoming and cached state without sending on attachment`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        assertEquals(2, state().selectedIndex)
        select(3)
        assertNoWrites()
        attach()
        runCurrent()
        assertTrue(state().isEnabled)
        incoming(4)
        runCurrent()
        assertEquals(4, state().selectedIndex)
        ParameterStoreV3.clear()
        runCurrent()
        assertEquals(2, state().selectedIndex)
        repeat(3) { attach(); source.updates.emit(Unit); runCurrent() }
        assertNoWrites()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4])
    fun `each of the five user choices saves once and sends its unchanged command immediately`(value: Int) = runTest(dispatcher) {
        attach()
        runCurrent()
        select(value)
        assertEquals(value, state().selectedIndex)
        assertEquals(value, repository.getSpinnerValue(key))
        assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x3B, value), packets.single())
        runCurrent()
        attach()
        source.updates.emit(Unit)
        runCurrent()
        assertEquals(1, packets.size)
        assertEquals(1, savedValues.size)
    }

    @Test
    fun `missing values use the widget default and invalid incoming values clamp only for display`() = runTest(dispatcher) {
        cachedValue.data = ""
        source.initialIndex = 1
        attach()
        runCurrent()
        assertNull(repository.getSpinnerValue(key))
        assertEquals(1, state().selectedIndex)
        incoming(255)
        runCurrent()
        assertEquals(4, state().selectedIndex)
        assertEquals(255, repository.getSpinnerValue(key))
        incoming(-3)
        runCurrent()
        assertEquals(0, state().selectedIndex)
        assertEquals(-3, repository.getSpinnerValue(key))
        assertNoWrites()
    }

    @ParameterizedTest
    @ValueSource(strings = ["application", "detach", "lock", "removed", "not-v3"])
    fun `hidden stopped or blocked spinner rejects selection`(reason: String) = runTest(dispatcher) {
        attach()
        runCurrent()
        when (reason) {
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "detach" -> detach()
            "lock" -> UiState.v3WidgetsInteractionEnabled.value = false
            "removed" -> { source.visible = false; source.updates.emit(Unit); runCurrent() }
            "not-v3" -> { source.profile = V3DeviceProfile.NOT_V3; source.updates.emit(Unit); runCurrent() }
        }
        select(3)
        runCurrent()
        assertFalse(state().isEnabled)
        assertNoWrites()
    }

    @Test
    fun `restoring section and changing device reads the latest value without a stale selection`() = runTest(dispatcher) {
        attach()
        runCurrent()
        section(V3SpecialSettingsSection.APPLICATION)
        incoming(3)
        runCurrent()
        section(V3SpecialSettingsSection.PROSTHESIS)
        assertEquals(3, state().selectedIndex)
        detach()
        cachedValue.data = "{\"spinnerValue\":1}"
        ParameterStoreV3.clear()
        source.address = "second-device"
        source.profile = V3DeviceProfile.INDY3
        attach()
        runCurrent()
        assertEquals(1, state().selectedIndex)
        assertTrue(state().isEnabled)
        assertNoWrites()
    }

    @Test
    fun `empty or shorter option lists block unavailable choices and retain the raw value`() = runTest(dispatcher) {
        attach()
        runCurrent()
        source.options = emptyList()
        source.updates.emit(Unit)
        runCurrent()
        assertNull(state().selectedIndex)
        assertFalse(state().isEnabled)
        select(0)
        source.options = listOf("Normal", "Sport")
        source.updates.emit(Unit)
        runCurrent()
        assertEquals(1, state().selectedIndex)
        assertEquals(2, repository.getSpinnerValue(key))
        select(2)
        assertNoWrites()
    }

    @Test
    fun `domain rejects invalid values and special selectors independently of presentation`() = runTest(dispatcher) {
        attach()
        runCurrent()
        val setValue = SetSpinnerValueUseCaseV3(repository)
        listOf(-1, 5, 255).forEach { value ->
            select(value)
            assertThrows(IllegalArgumentException::class.java) { setValue(key, value) }
        }
        listOf(P_KEY_SETTINGS_PROFILE, P_KEY_DEVICE_ROLE).forEach { otherKey ->
            select(1, otherKey)
            assertThrows(IllegalArgumentException::class.java) { setValue(otherKey, 1) }
        }
        UiState.v3WidgetsInteractionEnabled.value = false
        setValue(key, 3)
        assertNoWrites()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `gesture choice saves only its own parameter and sends the existing command`(value: Int) = runTest(dispatcher) {
        attach()
        runCurrent()
        assertEquals(1, state(gestureKey).selectedIndex)
        select(value, gestureKey)
        assertEquals(value, state(gestureKey).selectedIndex)
        assertEquals(value, repository.getSpinnerValue(gestureKey))
        assertEquals(listOf(gestureKey), savedKeys)
        assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x2D, value), packets.single())
        assertEquals(2, state().selectedIndex)
        assertEquals("{\"spinnerValue\":2}", cachedValue.data)
        assertNull(ParameterStoreV3.get(info))
        runCurrent()
        attach()
        source.updates.emit(Unit)
        runCurrent()
        assertEquals(1, packets.size)
        assertEquals(1, savedValues.size)
    }

    @Test
    fun `two spinners keep independent selections caches and commands`() = runTest(dispatcher) {
        attach()
        runCurrent()
        select(0, gestureKey)
        select(4)
        runCurrent()
        assertEquals(0, state(gestureKey).selectedIndex)
        assertEquals(4, state().selectedIndex)
        assertEquals(listOf(gestureKey, key), savedKeys)
        assertEquals(2, packets.size)
        assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x2D, 0), packets[0])
        assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x3B, 4), packets[1])
        assertEquals("{}", cachedGestureValue.data)
        assertEquals("{\"spinnerValue\":4}", cachedValue.data)
        incoming(1, gestureKey)
        runCurrent()
        assertEquals(1, state(gestureKey).selectedIndex)
        assertEquals(4, state().selectedIndex)
        assertEquals(2, packets.size)
    }

    @Test
    fun `gesture fallback incoming and cache restoration never write`() = runTest(dispatcher) {
        cachedGestureValue.data = ""
        attach()
        runCurrent()
        assertEquals(0, state(gestureKey).selectedIndex)
        assertNull(repository.getSpinnerValue(gestureKey))
        incoming(255, gestureKey)
        runCurrent()
        assertEquals(1, state(gestureKey).selectedIndex)
        assertEquals(255, repository.getSpinnerValue(gestureKey))
        incoming(-1, gestureKey)
        runCurrent()
        assertEquals(0, state(gestureKey).selectedIndex)
        assertEquals(-1, repository.getSpinnerValue(gestureKey))
        section(V3SpecialSettingsSection.APPLICATION)
        incoming(1, gestureKey)
        runCurrent()
        section(V3SpecialSettingsSection.PROSTHESIS)
        assertEquals(1, state(gestureKey).selectedIndex)
        detach()
        cachedGestureValue.data = "{}"
        ParameterStoreV3.clear()
        attach()
        runCurrent()
        assertEquals(0, state(gestureKey).selectedIndex)
        assertEquals(2, state().selectedIndex)
        assertNoWrites()
    }

    @ParameterizedTest
    @ValueSource(strings = ["application", "detach", "lock", "removed", "indy3"])
    fun `unavailable gesture spinner rejects selection without blocking the other key`(reason: String) = runTest(dispatcher) {
        attach()
        runCurrent()
        when (reason) {
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "detach" -> detach()
            "lock" -> UiState.v3WidgetsInteractionEnabled.value = false
            "removed" -> { source.gestureVisible = false; source.updates.emit(Unit); runCurrent() }
            "indy3" -> { source.profile = V3DeviceProfile.INDY3; source.updates.emit(Unit); runCurrent() }
        }
        select(0, gestureKey)
        runCurrent()
        assertFalse(state(gestureKey).isEnabled)
        assertEquals(1, repository.getSpinnerValue(gestureKey))
        assertNoWrites()
        if (reason == "removed" || reason == "indy3") {
            assertNull(state(gestureKey).selectedIndex)
            assertTrue(state().isEnabled)
            select(3)
            assertEquals(listOf(key), savedKeys)
        }
    }

    @Test
    fun `gesture domain range remains zero to one even with an extra UI option`() = runTest(dispatcher) {
        source.gestureOptions = listOf("No action", "Move to open position", "Unexpected option")
        attach()
        runCurrent()
        val setValue = SetSpinnerValueUseCaseV3(repository)
        listOf(-1, 2, 4, 255).forEach { value ->
            select(value, gestureKey)
            assertThrows(IllegalArgumentException::class.java) { setValue(gestureKey, value) }
        }
        UiState.v3WidgetsInteractionEnabled.value = false
        setValue(gestureKey, 0)
        assertNoWrites()
    }

    private class WidgetsSource : V3SpecialSettingsWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot(V3SpecialSettingsSection.PROSTHESIS).let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile, section: V3SpecialSettingsSection) = snapshot(section).widgets
        override val updates = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        var profile = V3DeviceProfile.STANDARD_V3
        var address = "first-device"
        var visible = true
        var gestureVisible = true
        var gestureOptions = listOf("No action", "Move to open position")
        var initialIndex = 0
        var options = listOf("Normal", "Sport", "Smooth force", "Smooth speed", "Smooth force and speed")
        fun snapshot(section: V3SpecialSettingsSection) = V3SpecialSettingsWidgetsSnapshot(
            profile, address,
            if (section == V3SpecialSettingsSection.PROSTHESIS && visible) buildList {
                add(V3SpecialSettingsWidget.Spinner(
                    V3SpecialSettingsWidgetInfo(P_KEY_HAND_CONTROL_MODE, "Режим работы протеза", 6), options, initialIndex,
                ))
                if (profile == V3DeviceProfile.STANDARD_V3 && gestureVisible) add(V3SpecialSettingsWidget.Spinner(
                    V3SpecialSettingsWidgetInfo(P_KEY_GESTURE_CHANGE_MODE, "Действие при смене жеста", 7), gestureOptions, 0,
                ))
            } else emptyList(),
        )
    }
}
