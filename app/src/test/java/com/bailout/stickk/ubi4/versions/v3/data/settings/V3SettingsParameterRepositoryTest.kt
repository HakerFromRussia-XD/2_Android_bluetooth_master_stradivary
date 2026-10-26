package com.bailout.stickk.ubi4.versions.v3.data.settings

import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSettingsParameterUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.RequestSettingsParameterUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SendSettingsParameterUseCaseV3
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class V3SettingsParameterRepositoryTest {
    private val toggle = ParameterInfoRegistry.require(P_KEY_EMG_CHANGE_GESTURE)
    private val spinner = ParameterInfoRegistry.require(P_KEY_HAND_CONTROL_MODE)
    private val oldDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val oldInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val packets = mutableListOf<ByteArray>()
    private var persistenceCalls = 0
    private var spinnerHooks = 0
    private val cache = BaseParameterInfoStruct(ID = toggle.parameterID, dataCode = toggle.dataCode, data = "cached value")
    private val repository = V3DeviceSettingsRepositoryImpl(
        enqueuePacket = { packets.add(it) },
        saveBleValue = { _, _ -> persistenceCalls++ },
        beforeSpinnerValueSent = { _, _ -> spinnerHooks++ },
    )
    private val get = GetSettingsParameterUseCaseV3(repository)
    private val request = RequestSettingsParameterUseCaseV3(repository)
    private val send = SendSettingsParameterUseCaseV3(repository)

    @BeforeEach fun setup() {
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = false
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(
            BaseSubDeviceInfoStruct(deviceAddress = toggle.deviceAddress, parametersList = arrayListOf(cache)),
        )
    }

    @AfterEach fun cleanup() {
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = oldInteraction
        GlobalParameters.baseSubDevicesInfoStructSetV3 = oldDevices
    }

    private fun assertNoPersistence() {
        assertEquals(0, persistenceCalls)
        assertEquals(0, spinnerHooks)
        assertEquals("cached value", ParameterProvider.getParameterV3(toggle).data)
        assertFalse(UiState.v3WidgetsInteractionEnabled.value)
    }

    @Test fun `toggle current value preserves missing default zero and signed Int snapshots without cache fallback`() {
        fun current() = get.toggleSliderValue(toggle.deviceAddress, toggle.parameterID, toggle.dataCode)
        assertNull(current())
        for (value in listOf(0, 1, 127, 128, 255, -1, Int.MIN_VALUE, Int.MAX_VALUE)) {
            ParameterStoreV3.put(toggle, ParameterTypedValueV3.Toggle(ToggleV3(value)))
            val previous = ParameterStoreV3.values.value
            assertEquals(value.takeIf { it != 0 }, current())
            assertEquals(previous, ParameterStoreV3.values.value)
            assertNull(get.toggleSliderValue(999, toggle.parameterID, toggle.dataCode))
        }
        ParameterStoreV3.put(toggle, ParameterTypedValueV3.Spinner(SpinnerV3(7)))
        assertNull(current())
        assertTrue(packets.isEmpty())
        assertNoPersistence()
    }

    @Test fun `spinner current value retains zero rejects negatives and reads the typed store even without registry metadata`() {
        for (info in listOf(spinner, ParameterInfo(999, 888, 777, 0))) {
            fun current() = get.spinnerValue(info.deviceAddress, info.parameterID, info.dataCode)
            assertNull(current())
            for (value in listOf(-1, Int.MIN_VALUE, 0, 1, Int.MAX_VALUE)) {
                ParameterStoreV3.put(info.copy(dataOffsets = 5), ParameterTypedValueV3.Spinner(SpinnerV3(value)))
                val previous = ParameterStoreV3.values.value
                assertEquals(value.takeIf { it >= 0 }, current())
                assertEquals(previous, ParameterStoreV3.values.value)
            }
            ParameterStoreV3.put(info, ParameterTypedValueV3.Toggle(ToggleV3(129)))
            assertNull(current())
        }
        assertTrue(packets.isEmpty())
        assertNoPersistence()
    }

    @Test fun `requests retain old GET mapping null profile requests repetitions and offline queueing`() {
        val previous = ParameterStoreV3.values.value
        for (key in listOf(P_KEY_EMG_CHANGE_GESTURE, P_KEY_SCREEN_TIMEOUT, P_KEY_HAND_CONTROL_MODE, P_KEY_SETTINGS_PROFILE)) {
            val info = ParameterInfoRegistry.require(key)
            val expected = WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode)
            repeat(2) {
                val count = packets.size
                request(info.parameterID, info.dataCode)
                assertEquals(count + if (expected == null) 0 else 1, packets.size)
                if (expected != null) assertArrayEquals(expected, packets.last())
                assertEquals(previous, ParameterStoreV3.values.value)
            }
        }
        assertNoPersistence()
    }

    @Test fun `sending preserves encoder address offset repetitions and no optimistic save even with repository save enabled`() {
        ParameterStoreV3.put(toggle, ParameterTypedValueV3.Toggle(ToggleV3(200)))
        val previous = ParameterStoreV3.values.value
        val bindings = listOf(toggle, spinner, toggle.copy(deviceAddress = 999), spinner.copy(dataOffsets = 9), ParameterInfo(999, 888, 777, 0))
        for (info in bindings) for (value in listOf(0, 0, 129, -1, Int.MAX_VALUE)) {
            val expected = WidgetCommandBridgeV3.buildSetInt(info.parameterID, info.dataCode, info.deviceAddress, info.dataOffsets, value)
            val count = packets.size
            send(info.deviceAddress, info.parameterID, info.dataCode, info.dataOffsets, value)
            assertEquals(count + if (expected == null) 0 else 1, packets.size)
            if (expected != null) assertArrayEquals(expected, packets.last())
            assertEquals(previous, ParameterStoreV3.values.value)
            assertNoPersistence()
        }
        assertTrue(packets.isNotEmpty())
    }
}
