package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.EditToggleSliderUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SendToggleSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetInfo
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.V3ToggleSliderAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3ToggleSliderSettingsIntegrationTest {
    companion object {
        @JvmStatic
        fun additionalParameters() = listOf(
            Arguments.of(P_KEY_EMG_CHANGE_GESTURE, 0x0F, 0x15, 25),
            Arguments.of(P_KEY_SCREEN_TIMEOUT, 0x10, 0x08, 60),
        )
    }

    private val key = P_KEY_EMG_MOVEMENT_LOCK
    private val info = ParameterInfoRegistry.require(key)
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val cachedValue = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{\"toggleValue\":165}")
    private val cachedValues = mapOf(
        key to cachedValue,
        P_KEY_EMG_CHANGE_GESTURE to cachedToggle(P_KEY_EMG_CHANGE_GESTURE, 0x99),
        P_KEY_SCREEN_TIMEOUT to cachedToggle(P_KEY_SCREEN_TIMEOUT, 0xBC),
    )
    private val packets = mutableListOf<ByteArray>()
    private val savedValues = mutableListOf<ParameterTypedValueV3>()
    private val savedParameters = mutableListOf<ParameterInfo<Int, Int, Int, Int>>()
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
            enqueuePacket = packets::add,
            saveBleValue = { parameter, value ->
                assertTrue(parameter in cachedValues.keys.map(ParameterInfoRegistry::require))
                assertEquals(value, ParameterStoreV3.get(parameter))
                savedValues.add(value)
                savedParameters.add(parameter)
            },
        )
        viewModel = V3SpecialSettingsViewModelFactory(repository, source, repository)
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
    private fun select(section: V3SpecialSettingsSection) = viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(section))
    private fun action(action: V3ToggleSliderAction) = viewModel.onAction(V3SpecialSettingsAction.ToggleSliderAction(action))
    private fun commit(value: Int) = action(V3ToggleSliderAction.ToggleSliderChangeCommitted(key, value))
    private fun step(value: Int = 1) = action(V3ToggleSliderAction.ToggleSliderStepClicked(key, value))
    private fun enable(enabled: Boolean) = action(V3ToggleSliderAction.ToggleSliderEnabledChanged(key, enabled))
    private fun state() = viewModel.uiState.value.toggleSliders.getValue(key)
    private fun incoming(packed: Int) = ParameterStoreV3.put(info, ParameterTypedValueV3.Toggle(ToggleV3(packed)))
    private fun assertPacket(packed: Int) = assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x39, packed), packets.single())
    private fun cachedToggle(parameterKey: String, packed: Int): BaseParameterInfoStruct {
        val parameter = ParameterInfoRegistry.require(parameterKey)
        return BaseParameterInfoStruct(ID = parameter.parameterID, dataCode = parameter.dataCode, data = "{\"toggleValue\":$packed}")
    }

    private fun showAllToggleSliders() {
        source.parameterKeys = setOf(P_KEY_EMG_CHANGE_GESTURE, key, P_KEY_SCREEN_TIMEOUT)
        attach()
    }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both device profiles restore cache incoming values and clear without sending`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        attach()
        runCurrent()
        assertEquals(V3ToggleSliderValue(37, true), state().value)
        assertEquals(10..100, state().allowedTimeRange)
        assertTrue(state().isSliderEnabled)
        incoming(0x12)
        runCurrent()
        assertEquals(V3ToggleSliderValue(18, false), state().value)
        assertTrue(state().isInteractionEnabled)
        assertFalse(state().isSliderEnabled)
        ParameterStoreV3.clear()
        runCurrent()
        assertEquals(V3ToggleSliderValue(37, true), state().value)
        assertTrue(packets.isEmpty())
        assertTrue(savedValues.isEmpty())
    }

    @Test
    fun `drag only changes state while release saves immediately and sends after 300 ms`() = runTest(dispatcher) {
        attach()
        runCurrent()
        action(V3ToggleSliderAction.ToggleSliderValueChanged(key, 42))
        assertEquals(42, state().value.timeTenths)
        assertEquals(37, repository.getToggleSliderValue(key)?.timeTenths)
        assertTrue(savedValues.isEmpty())
        advanceTimeBy(301)
        assertTrue(packets.isEmpty())
        commit(42)
        assertEquals(V3ToggleSliderValue(42, true), repository.getToggleSliderValue(key))
        assertEquals("{\"toggleValue\":170}", cachedValue.data)
        assertEquals(1, savedValues.size)
        advanceTimeBy(299)
        runCurrent()
        assertTrue(packets.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertPacket(0xAA)
    }

    @Test
    fun `plus minus and repeated attachment preserve one deadline with the latest edit`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(200)
        step()
        advanceTimeBy(200)
        step(-1)
        runCurrent()
        attach()
        select(V3SpecialSettingsSection.PROSTHESIS)
        source.updates.emit(Unit)
        runCurrent()
        advanceTimeBy(299)
        runCurrent()
        assertTrue(packets.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(3, savedValues.size)
        assertPacket(0xA6)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 9, 100, 127])
    fun `flag changes preserve even out of display range incoming time`(time: Int) = runTest(dispatcher) {
        attach()
        incoming(time)
        runCurrent()
        enable(true)
        assertEquals(V3ToggleSliderValue(time, true), repository.getToggleSliderValue(key))
        advanceTimeBy(300)
        runCurrent()
        assertPacket(0x80 or time)
        packets.clear()
        enable(false)
        commit(40)
        step()
        advanceTimeBy(300)
        runCurrent()
        assertPacket(time)
        assertEquals(2, savedValues.size)
        assertFalse(state().isSliderEnabled)
        assertTrue(state().isInteractionEnabled)
    }

    @ParameterizedTest
    @ValueSource(ints = [10, 100])
    fun `user time stays in domain bounds and preserves enabled flag`(boundary: Int) = runTest(dispatcher) {
        attach()
        runCurrent()
        commit(boundary)
        step(if (boundary == 10) -1 else 1)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(boundary, state().value.timeTenths)
        assertPacket(0x80 or boundary)
    }

    @ParameterizedTest
    @ValueSource(strings = ["application", "detach", "lock", "removed", "address", "profile", "clear"])
    fun `leaving or invalidating the screen cancels only the pending transmission`(reason: String) = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(299)
        when (reason) {
            "application" -> select(V3SpecialSettingsSection.APPLICATION)
            "detach" -> detach()
            "lock" -> UiState.v3WidgetsInteractionEnabled.value = false
            "removed" -> { source.visible = false; source.updates.emit(Unit) }
            "address" -> { source.address = "second-device"; source.updates.emit(Unit) }
            "profile" -> { source.profile = V3DeviceProfile.INDY3; source.updates.emit(Unit) }
            "clear" -> store.clear()
        }
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertTrue(packets.isEmpty())
        // Preserve the existing optimistic cache/profile update; cancellation is not a rollback.
        assertEquals(V3ToggleSliderValue(38, true), repository.getToggleSliderValue(key))
        assertEquals(1, savedValues.size)
    }

    @Test
    fun `live connection lock rejects input before state collector and use cases reject it too`() = runTest(dispatcher) {
        attach()
        runCurrent()
        UiState.v3WidgetsInteractionEnabled.value = false
        commit(42)
        enable(false)
        assertNull(EditToggleSliderUseCaseV3(repository).setTime(key, 42))
        assertNull(EditToggleSliderUseCaseV3(repository).setEnabled(key, false))
        SendToggleSliderValueUseCaseV3(repository)(key)
        advanceTimeBy(301)
        runCurrent()
        assertTrue(savedValues.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @Test
    fun `domain rejects invalid time and unrelated parameters even outside the screen`() {
        val edit = EditToggleSliderUseCaseV3(repository)
        listOf(0, 9, 101, Int.MAX_VALUE).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { edit.setTime(key, invalid) }
        }
        assertThrows(IllegalArgumentException::class.java) { edit.setEnabled(P_KEY_FORCE_SETTINGS, true) }
        assertThrows(IllegalArgumentException::class.java) { SendToggleSliderValueUseCaseV3(repository)(P_KEY_FORCE_SETTINGS) }
        assertTrue(savedValues.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @Test
    fun `returning from application restores latest value and does not accept other ToggleSliders`() = runTest(dispatcher) {
        attach()
        runCurrent()
        action(V3ToggleSliderAction.ToggleSliderValueChanged(key, 42))
        select(V3SpecialSettingsSection.APPLICATION)
        enable(false)
        incoming(0xBF)
        runCurrent()
        select(V3SpecialSettingsSection.PROSTHESIS)
        assertEquals(V3ToggleSliderValue(63, true), state().value)
        assertFalse(state().animateValueChange)
        action(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_SCREEN_TIMEOUT, true))
        step(2)
        advanceTimeBy(301)
        runCurrent()
        assertTrue(savedValues.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("additionalParameters")
    fun `additional parameters use their own cache profile key and device command`(
        parameterKey: String, command: Int, subcommand: Int, initialTime: Int,
    ) = runTest(dispatcher) {
        showAllToggleSliders()
        runCurrent()
        val parameter = ParameterInfoRegistry.require(parameterKey)
        fun currentState() = viewModel.uiState.value.toggleSliders.getValue(parameterKey)
        fun assertOwnPacket(packed: Int) {
            assertArrayEquals(BLECommandsV3.sendCommand(command, subcommand, packed), packets.single())
            assertEquals(parameter, savedParameters.last())
            assertEquals("{\"toggleValue\":$packed}", cachedValues.getValue(parameterKey).data)
        }

        assertEquals(V3ToggleSliderValue(initialTime, true), currentState().value)
        assertEquals(10..100, currentState().allowedTimeRange)
        action(V3ToggleSliderAction.ToggleSliderValueChanged(parameterKey, 42))
        assertEquals(42, currentState().value.timeTenths)
        assertEquals(initialTime, repository.getToggleSliderValue(parameterKey)?.timeTenths)
        assertTrue(savedValues.isEmpty())
        action(V3ToggleSliderAction.ToggleSliderChangeCommitted(parameterKey, 42))
        assertEquals(V3ToggleSliderValue(42, true), repository.getToggleSliderValue(parameterKey))
        assertEquals(1, savedValues.size)
        assertEquals("{\"toggleValue\":170}", cachedValues.getValue(parameterKey).data)
        advanceTimeBy(299)
        runCurrent()
        assertTrue(packets.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertOwnPacket(0xAA)

        packets.clear()
        action(V3ToggleSliderAction.ToggleSliderEnabledChanged(parameterKey, false))
        action(V3ToggleSliderAction.ToggleSliderChangeCommitted(parameterKey, 55))
        action(V3ToggleSliderAction.ToggleSliderStepClicked(parameterKey, 1))
        advanceTimeBy(300)
        runCurrent()
        assertOwnPacket(42)
        assertFalse(currentState().isSliderEnabled)
        assertTrue(currentState().isInteractionEnabled)

        packets.clear()
        ParameterStoreV3.put(parameter, ParameterTypedValueV3.Toggle(ToggleV3(0)))
        runCurrent()
        action(V3ToggleSliderAction.ToggleSliderEnabledChanged(parameterKey, true))
        advanceTimeBy(300)
        runCurrent()
        assertOwnPacket(0x80)
        assertEquals(V3ToggleSliderValue(0, true), currentState().value)

        for (boundary in listOf(10, 100)) {
            packets.clear()
            action(V3ToggleSliderAction.ToggleSliderChangeCommitted(parameterKey, boundary))
            action(V3ToggleSliderAction.ToggleSliderStepClicked(parameterKey, if (boundary == 10) -1 else 1))
            advanceTimeBy(300)
            runCurrent()
            assertOwnPacket(0x80 or boundary)
            assertEquals(boundary, currentState().value.timeTenths)
        }
        assertEquals(V3ToggleSliderValue(37, true), repository.getToggleSliderValue(key))
        assertTrue(savedParameters.all { it == parameter })
        val edit = EditToggleSliderUseCaseV3(repository)
        listOf(9, 101).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { edit.setTime(parameterKey, invalid) }
        }
    }

    @Test
    fun `three ToggleSliders keep independent deadlines and profile values`() = runTest(dispatcher) {
        showAllToggleSliders()
        runCurrent()
        step()
        advanceTimeBy(100)
        action(V3ToggleSliderAction.ToggleSliderStepClicked(P_KEY_EMG_CHANGE_GESTURE, 1))
        advanceTimeBy(100)
        action(V3ToggleSliderAction.ToggleSliderStepClicked(P_KEY_SCREEN_TIMEOUT, 1))
        advanceTimeBy(50)
        action(V3ToggleSliderAction.ToggleSliderStepClicked(P_KEY_EMG_CHANGE_GESTURE, 1))
        advanceTimeBy(49)
        runCurrent()
        assertTrue(packets.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertPacket(0xA6)
        advanceTimeBy(199)
        runCurrent()
        assertEquals(1, packets.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, packets.size)
        assertArrayEquals(BLECommandsV3.sendCommand(0x10, 0x08, 0xBD), packets[1])
        advanceTimeBy(50)
        runCurrent()
        assertEquals(3, packets.size)
        assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x15, 0x9B), packets[2])
        assertEquals(
            listOf(key, P_KEY_EMG_CHANGE_GESTURE, P_KEY_SCREEN_TIMEOUT, P_KEY_EMG_CHANGE_GESTURE).map(ParameterInfoRegistry::require),
            savedParameters,
        )
    }

    @Test
    fun `removing one widget cancels its deadline while the other two still send`() = runTest(dispatcher) {
        showAllToggleSliders()
        runCurrent()
        source.parameterKeys.forEach { action(V3ToggleSliderAction.ToggleSliderStepClicked(it, 1)) }
        advanceTimeBy(299)
        source.parameterKeys = setOf(key, P_KEY_SCREEN_TIMEOUT)
        source.updates.emit(Unit)
        runCurrent()
        action(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_EMG_CHANGE_GESTURE, false))
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, packets.size)
        assertArrayEquals(BLECommandsV3.sendCommand(0x0F, 0x39, 0xA6), packets[0])
        assertArrayEquals(BLECommandsV3.sendCommand(0x10, 0x08, 0xBD), packets[1])
        assertEquals(3, savedValues.size)
        assertFalse(viewModel.uiState.value.toggleSliders.getValue(P_KEY_EMG_CHANGE_GESTURE).isInteractionEnabled)
    }

    @ParameterizedTest
    @ValueSource(strings = ["application", "lock"])
    fun `leaving or locking cancels all three pending commands`(reason: String) = runTest(dispatcher) {
        showAllToggleSliders()
        runCurrent()
        source.parameterKeys.forEach { action(V3ToggleSliderAction.ToggleSliderStepClicked(it, 1)) }
        advanceTimeBy(299)
        if (reason == "application") select(V3SpecialSettingsSection.APPLICATION)
        else UiState.v3WidgetsInteractionEnabled.value = false
        // Must reject input immediately, before the state collector runs.
        source.parameterKeys.forEach { action(V3ToggleSliderAction.ToggleSliderEnabledChanged(it, false)) }
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertTrue(packets.isEmpty())
        assertEquals(3, savedValues.size)
        assertTrue(viewModel.uiState.value.toggleSliders.values.none { it.isInteractionEnabled })
    }

    @Test
    fun `switching to INDY3 cancels pending commands and only movement lock accepts actions`() = runTest(dispatcher) {
        showAllToggleSliders()
        runCurrent()
        source.parameterKeys.forEach { action(V3ToggleSliderAction.ToggleSliderStepClicked(it, 1)) }
        advanceTimeBy(299)
        source.profile = V3DeviceProfile.INDY3
        source.updates.emit(Unit)
        runCurrent()
        val activeKeys = viewModel.uiState.value.toggleSliders.filterValues { it.isInteractionEnabled }.keys
        assertEquals(setOf(key), activeKeys)
        assertEquals(listOf(key), viewModel.uiState.value.widgets.map { it.info.key })
        action(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_SCREEN_TIMEOUT, false))
        action(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_EMG_CHANGE_GESTURE, false))
        advanceTimeBy(301)
        runCurrent()
        assertTrue(packets.isEmpty())
        assertEquals(3, savedValues.size)
        step()
        advanceTimeBy(300)
        runCurrent()
        assertPacket(0xA7)
    }

    private class WidgetsSource : V3SpecialSettingsWidgetsSource {
        override val updates = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        var profile = V3DeviceProfile.STANDARD_V3
        var address = "first-device"
        var visible = true
        var parameterKeys = setOf(P_KEY_EMG_MOVEMENT_LOCK)
        override fun snapshot(section: V3SpecialSettingsSection) = V3SpecialSettingsWidgetsSnapshot(
            profile, address,
            if (section == V3SpecialSettingsSection.PROSTHESIS && visible) parameterKeys
                .filter { profile != V3DeviceProfile.INDY3 || it == P_KEY_EMG_MOVEMENT_LOCK }
                .mapIndexed { index, key ->
                V3SpecialSettingsWidget.ToggleSlider(
                    V3SpecialSettingsWidgetInfo(key, key, index),
                    10, 100, 0.1f, "сек",
                )
            } else emptyList(),
        )
    }
}
