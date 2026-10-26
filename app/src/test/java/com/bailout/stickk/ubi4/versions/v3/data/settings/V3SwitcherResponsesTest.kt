package com.bailout.stickk.ubi4.versions.v3.data.settings

import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SwitcherV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_TEST_SWITCHER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSwitcherValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.RequestSwitcherValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSwitcherValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSwitcherResponsesUseCaseV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3SwitcherResponsesTest {
    private val dispatcher = StandardTestDispatcher()
    private val info = ParameterInfoRegistry.require(P_KEY_TEST_SWITCHER)
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{\"checked\":true}")
    private val packets = mutableListOf<ByteArray>()
    private val savedValues = mutableListOf<ParameterTypedValueV3>()
    private lateinit var observeResponses: ObserveSwitcherResponsesUseCaseV3
    private lateinit var settingsRepository: V3SwitcherSettingsRepositoryImpl

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = false
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(
            BaseSubDeviceInfoStruct(deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache)),
        )
        observeResponses = ObserveSwitcherResponsesUseCaseV3(V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packets.add(it) }, saveBleValue = { _, value -> savedValues.add(value) },
            readCachedValues = false, saveValueBeforeSending = false,
        ))
        settingsRepository = V3SwitcherSettingsRepositoryImpl { packets.add(it) }
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        Dispatchers.resetMain()
    }

    private fun incoming(checked: Boolean) =
        ParameterStoreV3.put(info, ParameterTypedValueV3.Switcher(SwitcherV3(checked)))

    private fun assertNoWrites() {
        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        assertEquals("{\"checked\":true}", ParameterProvider.getParameterV3(info).data)
    }

    @Test
    fun `responses preserve repetitions false omission and route filtering without writes`() = runTest(dispatcher) {
        incoming(true)
        val replies = mutableListOf<Boolean?>()
        val job = observeResponses(info.deviceAddress, info.parameterID, info.dataCode) { replies.add(it) }
        try {
            runCurrent()
            assertTrue(replies.isEmpty())
            listOf(false, false, true, true).forEach { checked ->
                incoming(checked)
                val received = ParameterStoreV3.values.value
                runCurrent()
                assertEquals(received, ParameterStoreV3.values.value)
                assertNoWrites()
            }
            assertEquals(listOf(null, null, true, true), replies)
            listOf(info.copy(deviceAddress = 999), info.copy(parameterID = 999), info.copy(dataCode = 999)).forEach {
                ParameterStoreV3.put(it, ParameterTypedValueV3.Switcher(SwitcherV3(true)))
            }
            runCurrent()
            assertEquals(listOf(null, null, true, true), replies)
            ParameterStoreV3.put(info.copy(dataOffsets = 999), ParameterTypedValueV3.Switcher(SwitcherV3(true)))
            runCurrent()
            ParameterStoreV3.put(info, ParameterTypedValueV3.Text("wrong codec value"))
            runCurrent()
            assertEquals(listOf(null, null, true, true, true, null), replies)
            assertNoWrites()
        } finally {
            job.cancel()
            runCurrent()
        }
    }

    @Test
    fun `queued responses use latest snapshot skip cleared values and stop after cancel`() = runTest(dispatcher) {
        val replies = mutableListOf<Boolean?>()
        val job = observeResponses(info.deviceAddress, info.parameterID, info.dataCode) { replies.add(it) }
        try {
            runCurrent()
            incoming(false)
            incoming(true)
            runCurrent()
            assertEquals(listOf(true, true), replies)
            incoming(true)
            ParameterStoreV3.clear()
            runCurrent()
            assertEquals(listOf(true, true), replies)
            job.cancel()
            runCurrent()
            incoming(true)
            runCurrent()
            assertEquals(listOf(true, true), replies)
            assertNoWrites()
        } finally {
            job.cancel()
            runCurrent()
        }
    }

    @Test
    fun `response value stays captured when consumer changes live state during delivery`() = runTest(dispatcher) {
        val replies = mutableListOf<Pair<Boolean?, Boolean>>()
        val job = observeResponses(info.deviceAddress, info.parameterID, info.dataCode) { received ->
            if (received == true) incoming(false)
            val live = (ParameterStoreV3.get(info) as ParameterTypedValueV3.Switcher).value.checked
            replies.add(received to live)
        }
        try {
            runCurrent()
            incoming(true)
            runCurrent()
            assertEquals(listOf(true to false, null to false), replies)
            assertNoWrites()
        } finally {
            job.cancel()
            runCurrent()
        }
    }

    @Test
    fun `current value preserves uncached nullable reads and default false omission`() {
        val getValue = GetSwitcherValueUseCaseV3(settingsRepository)
        fun current() = getValue(info.deviceAddress, info.parameterID, info.dataCode)
        assertNull(current()) // Serialized cache is not the received typed store.
        incoming(true)
        assertTrue(current() == true)
        incoming(false)
        val received = ParameterStoreV3.values.value
        assertNull(current()) // The existing codec omits checked=false.
        assertEquals(received, ParameterStoreV3.values.value)
        assertNull(getValue(999, info.parameterID, info.dataCode))
        assertNull(getValue(info.deviceAddress, 999, info.dataCode))
        assertNull(getValue(info.deviceAddress, info.parameterID, 999))
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("wrong codec value"))
        assertNull(current())
        assertNoWrites()
    }

    @Test
    fun `read requests keep existing mapping repeated packets and profile no request rule`() {
        val requestValue = RequestSwitcherValueUseCaseV3(settingsRepository)
        val handSide = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        val expected = listOf(info, handSide, info).map {
            requireNotNull(WidgetCommandBridgeV3.buildReadRequest(it.parameterID, it.dataCode))
        }
        val received = ParameterStoreV3.values.value
        listOf(info, handSide, info).forEach { requestValue(it.parameterID, it.dataCode) }
        val profile = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
        requestValue(profile.parameterID, profile.dataCode)
        assertEquals(expected.size, packets.size)
        expected.zip(packets).forEach { (before, after) -> assertArrayEquals(before, after) }
        assertEquals(received, ParameterStoreV3.values.value)
        assertTrue(savedValues.isEmpty())
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        assertEquals("{\"checked\":true}", cache.data)
    }

    @Test
    fun `writes preserve repeated commands while locked without saving a received value or profile`() {
        val setValue = SetSwitcherValueUseCaseV3(settingsRepository)
        incoming(true)
        val received = ParameterStoreV3.values.value
        val values = listOf(false, false, true, true)
        val expected = values.map {
            requireNotNull(WidgetCommandBridgeV3.buildSetBoolean(info.parameterID, info.dataCode, info.deviceAddress, it))
        }
        values.forEach { setValue(info.deviceAddress, info.parameterID, info.dataCode, it) }
        setValue(999, 999, 999, false) // Unsupported metadata still suppresses the packet.
        assertEquals(values.size, packets.size)
        expected.zip(packets).forEach { (before, after) -> assertArrayEquals(before, after) }
        assertEquals(received, ParameterStoreV3.values.value)
        assertTrue(savedValues.isEmpty())
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
        assertEquals("{\"checked\":true}", cache.data)
    }
}
