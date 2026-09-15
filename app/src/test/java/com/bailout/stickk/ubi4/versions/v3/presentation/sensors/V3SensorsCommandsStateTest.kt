package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetMapper
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SensorsCommandsStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val commands = FakeV3SensorsCommandsRepository()
    private val source = Source()
    private lateinit var viewModel: V3SensorsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val sliders = mockk<V3DeviceSettingsRepository>(relaxed = true)
        every { sliders.sliderInteractionEnabled } returns MutableStateFlow(true)
        every { sliders.getSliderValue(any()) } returns 20
        every { sliders.observeSliderValue(any()) } returns MutableStateFlow(20)
        viewModel = V3SensorsViewModelFactory(sliders, source, FakeV3SensorsPlotRepository(), commands)
            .create(V3SensorsViewModel::class.java)
        store.put("sensors", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onAction(V3SensorsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SensorsAction.ViewDetached)
    private fun refresh() = viewModel.onAction(V3SensorsAction.RefreshRequested)
    private fun press(movement: V3ProsthesisMovement = V3ProsthesisMovement.OPEN, id: Long = 1) =
        viewModel.onAction(V3SensorsAction.ButtonsAction(V3SensorsButtonsAction.ButtonPressed(movement, id)))
    private fun release(id: Long = 1) =
        viewModel.onAction(V3SensorsAction.ButtonsAction(V3SensorsButtonsAction.ButtonReleased(id)))

    @ParameterizedTest
    @EnumSource(V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both versions expose open and close without sending on attach or render`(profile: V3DeviceProfile) = runTest(dispatcher) {
        press()
        source.current = source.current.copy(deviceProfile = profile)
        attach()
        runCurrent()
        attach()
        source.updates.emit(Unit)
        runCurrent()
        assertEquals(setOf(V3ProsthesisMovement.OPEN, V3ProsthesisMovement.CLOSE), viewModel.uiState.value.buttons.availableMovements)
        assertTrue(viewModel.uiState.value.buttons.isEnabled)
        assertTrue(commands.events.isEmpty())
        press()
        assertEquals(setOf(V3ProsthesisMovement.OPEN), viewModel.uiState.value.buttons.pressedMovements)
        release()
        press(V3ProsthesisMovement.CLOSE, 2)
        release(2)
        assertEquals(listOf("first-device:OPEN", "first-device:STOP", "first-device:CLOSE", "first-device:STOP"), commands.events)
        assertTrue(viewModel.uiState.value.buttons.pressedMovements.isEmpty())
    }

    @Test
    fun `duplicate events and an old release cannot affect a later accepted press`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        press()
        release()
        press(V3ProsthesisMovement.CLOSE, 2)
        release()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP", "first-device:CLOSE"), commands.events)
        assertEquals(setOf(V3ProsthesisMovement.CLOSE), viewModel.uiState.value.buttons.pressedMovements)
        release(2)
        release(2)
        assertEquals(4, commands.events.size)
    }

    @Test
    fun `accepted press stops on lock while a fresh locked press is rejected before collection`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        commands.interactionEnabled.value = false
        press(V3ProsthesisMovement.CLOSE, 2)
        runCurrent()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP"), commands.events)
        assertFalse(viewModel.uiState.value.buttons.isEnabled)
        release()
        release(2)
        commands.interactionEnabled.value = true
        runCurrent()
        assertEquals(2, commands.events.size)
        assertTrue(viewModel.uiState.value.buttons.pressedMovements.isEmpty())
    }

    @Test
    fun `release before lock collector still sends exactly one stop`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        commands.interactionEnabled.value = false
        release()
        runCurrent()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP"), commands.events)
    }

    @Test
    fun `detach stops accepted presses and returning never replays them`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        detach()
        press(V3ProsthesisMovement.CLOSE, 2)
        release()
        attach()
        runCurrent()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP"), commands.events)
        assertTrue(viewModel.uiState.value.buttons.pressedMovements.isEmpty())
    }

    @Test
    fun `unchanged composition retains the active press until release`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        attach()
        source.updates.emit(Unit)
        runCurrent()
        assertEquals(listOf("first-device:OPEN"), commands.events)
        release()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP"), commands.events)
    }

    @Test
    fun `removing buttons stops the press and blocks callbacks from the removed widget`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        source.current = source.current.copy(widgets = emptyList())
        source.updates.emit(Unit)
        runCurrent()
        press(V3ProsthesisMovement.CLOSE, 2)
        release()
        assertFalse(viewModel.uiState.value.buttons.isEnabled)
        assertEquals(listOf("first-device:OPEN", "first-device:STOP"), commands.events)
    }

    @Test
    fun `switching device rejects stale starts and does not send old stop to new device`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        commands.currentAddress = "second-device"
        press(V3ProsthesisMovement.CLOSE, 2)
        source.current = source.current.copy(deviceAddress = "second-device")
        source.updates.emit(Unit)
        runCurrent()
        release()
        assertEquals(listOf("first-device:OPEN"), commands.events)
        press(V3ProsthesisMovement.CLOSE, 3)
        release(3)
        assertEquals(listOf("first-device:OPEN", "second-device:CLOSE", "second-device:STOP"), commands.events)
    }

    @Test
    fun `leaving V3 and clearing ViewModel stop accepted presses`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        source.current = source.current.copy(deviceProfile = V3DeviceProfile.NOT_V3)
        source.updates.emit(Unit)
        runCurrent()
        press(V3ProsthesisMovement.CLOSE, 2)
        refresh()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP"), commands.events)
        source.current = source.current.copy(deviceProfile = V3DeviceProfile.INDY3)
        attach()
        press(V3ProsthesisMovement.CLOSE, 3)
        store.clear()
        release(3)
        press(id = 4)
        refresh()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP", "first-device:CLOSE", "first-device:STOP"), commands.events)
    }

    @Test
    fun `refresh stops the press first and composition update cannot complete BLE synchronization`() = runTest(dispatcher) {
        attach()
        runCurrent()
        press()
        refresh()
        refresh()
        assertEquals(listOf("first-device:OPEN", "first-device:STOP", "first-device:REFRESH"), commands.events)
        assertTrue(viewModel.uiState.value.isRefreshIndicatorVisible)
        source.updates.emit(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.isRefreshIndicatorVisible)
        assertTrue(commands.refreshInProgress.value)
        refresh()
        assertFalse(viewModel.uiState.value.isRefreshIndicatorVisible)
        assertEquals(3, commands.events.size)
        commands.refreshInProgress.value = false
        refresh()
        assertEquals("first-device:REFRESH", commands.events.last())
        assertEquals(4, commands.events.size)
    }

    @Test
    fun `locked controls can refresh but detached or stale device requests cannot`() = runTest(dispatcher) {
        refresh()
        attach()
        runCurrent()
        commands.interactionEnabled.value = false
        refresh()
        detach()
        refresh()
        commands.refreshInProgress.value = false
        attach()
        runCurrent()
        assertEquals(listOf("first-device:REFRESH"), commands.events)
        assertFalse(viewModel.uiState.value.isRefreshIndicatorVisible)
        commands.currentAddress = "second-device"
        refresh()
        assertEquals(1, commands.events.size)
        assertFalse(viewModel.uiState.value.isRefreshIndicatorVisible)
    }

    private class Source : V3SensorsWidgetsSource {
        override val updates = MutableSharedFlow<Unit>()
        var current = V3SensorsWidgetsSnapshot(V3DeviceProfile.STANDARD_V3, "first-device",
            V3SensorsWidgetMapper().fromItems(listOf(ButtonsItemV3(
                title = "Открыть", title2 = "Закрыть", title3 = "", description = "", widget = CommandParameterWidgetSStruct(
                    BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 1,
                        parameterInfoSet = mutableSetOf(ParameterInfo(15, 1, 5, 0), ParameterInfo(15, 2, 6, 1)),
                    )),
                ),
            ))),
        )
        override fun snapshot() = current
    }
}
