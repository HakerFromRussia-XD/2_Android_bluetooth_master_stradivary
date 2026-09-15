package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetMapper
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class V3SensorsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private val source = FakeWidgetsSource()
    private lateinit var viewModel: V3SensorsViewModel
    private val open = P_KEY_EMG_GAIN_OPEN_VALUE
    private val close = P_KEY_EMG_GAIN_CLOSE_VALUE

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3SensorsViewModelFactory(repository, source, FakeV3SensorsPlotRepository(), FakeV3SensorsCommandsRepository()).create(V3SensorsViewModel::class.java)
        store.put("sensors", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onAction(V3SensorsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SensorsAction.ViewDetached)
    private fun slider(action: V3SliderAction) = viewModel.onAction(V3SensorsAction.SliderAction(action))
    private fun step(key: String = open) = slider(V3SliderAction.SliderStepClicked(key, 1))
    private fun commit(value: Int, key: String = open) = slider(V3SliderAction.SliderChangeCommitted(key, value))
    private fun values() = viewModel.uiState.value.sliders.mapValues { it.value.value }
    private fun assertNoWrites() = assertTrue(repository.writes.isEmpty())

    @ParameterizedTest
    @EnumSource(V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `initial snapshot and attaching restore both sensors without writes`(profile: V3DeviceProfile) = runTest(dispatcher) {
        assertEquals(source.current.widgets, viewModel.uiState.value.widgets)
        assertEquals(mapOf(open to 18, close to 62), values())
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.isEnabled) }
        commit(40)
        source.current = source.current.copy(deviceProfile = profile)
        attach()
        runCurrent()
        assertEquals(profile, viewModel.uiState.value.deviceProfile)
        assertEquals(setOf(open, close), viewModel.uiState.value.sliders.keys)
        viewModel.uiState.value.sliders.values.forEach {
            assertTrue(it.isEnabled)
            assertEquals(0..100, it.allowedRange)
        }
        commit(40, P_KEY_SPEED_SETTINGS)
        assertNoWrites()
    }

    @Test
    fun `draft does not write and commit goes through the use case`() = runTest(dispatcher) {
        attach()
        runCurrent()
        slider(V3SliderAction.SliderValueChanged(open, 40))
        assertEquals(40, values()[open])
        assertEquals(18, repository.values.getValue(open).value)
        assertNoWrites()
        commit(140)
        commit(-5, close)
        assertEquals(listOf(open to 100, close to 0), repository.writes)
    }

    @Test
    fun `same composition and repeated attach keep both pending edits`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step(open)
        step(close)
        advanceTimeBy(200)
        source.updates.emit(Unit)
        attach()
        runCurrent()
        assertEquals(mapOf(open to 19, close to 63), values())
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(open to 19, close to 63), repository.writes)
    }

    @Test
    fun `stop cancels deadlines and returning restores latest values without resending`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(299)
        detach()
        commit(44)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        repository.values.getValue(open).value = 25
        runCurrent()
        attach()
        runCurrent()
        assertEquals(25, values()[open])
        assertFalse(viewModel.uiState.value.sliders.getValue(open).animateValueChange)
        assertNoWrites()
    }

    @Test
    fun `fresh interaction lock rejects release before collector and cancels queued writes`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        repository.sliderInteractionEnabled.value = false
        commit(42)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        assertFalse(viewModel.uiState.value.sliders.getValue(open).isEnabled)
        repository.sliderInteractionEnabled.value = true
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
    }

    @Test
    fun `changing device cancels pending edit even when widget composition is identical`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        source.current = source.current.copy(deviceAddress = "second-device")
        source.updates.emit(Unit)
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(18, values()[open])
        assertNoWrites()
    }

    @Test
    fun `removing a slider cancels its write and rejects stale callbacks`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step(open)
        source.current = source.current.copy(widgets = source.current.widgets.drop(1))
        source.updates.emit(Unit)
        runCurrent()
        commit(40, open)
        assertFalse(viewModel.uiState.value.sliders.getValue(open).isEnabled)
        assertTrue(viewModel.uiState.value.sliders.getValue(close).isEnabled)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        commit(30, close)
        assertEquals(listOf(close to 30), repository.writes)
    }

    @Test
    fun `leaving V3 blocks both sliders and cancels writes`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        source.current = source.current.copy(deviceProfile = V3DeviceProfile.NOT_V3, widgets = emptyList())
        source.updates.emit(Unit)
        runCurrent()
        commit(50)
        step(close)
        advanceTimeBy(301)
        runCurrent()
        assertTrue(viewModel.uiState.value.widgets.isEmpty())
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.isEnabled) }
        assertNoWrites()
    }

    @Test
    fun `clearing viewmodel cancels scheduled writes`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        store.clear()
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
    }

    @Test
    fun `callbacks after clearing the screen cannot change drafts or send values`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        store.clear()
        val finalState = viewModel.uiState.value
        commit(40)
        attach()
        slider(V3SliderAction.SliderValueChanged(close, 50))
        commit(50, close)
        step(close)
        viewModel.onAction(V3SensorsAction.RefreshRequested)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        assertEquals(finalState, viewModel.uiState.value)
    }

    @Test
    fun `local refresh indicator ends on composition event without unlocking device`() = runTest(dispatcher) {
        attach()
        runCurrent()
        repository.sliderInteractionEnabled.value = false
        viewModel.onAction(V3SensorsAction.RefreshRequested)
        runCurrent()
        assertTrue(viewModel.uiState.value.isRefreshIndicatorVisible)
        repository.values.getValue(open).value = 24
        runCurrent()
        assertTrue(viewModel.uiState.value.isRefreshIndicatorVisible)
        source.updates.emit(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.isRefreshIndicatorVisible)
        assertFalse(viewModel.uiState.value.sliders.getValue(open).isEnabled)
        assertFalse(repository.sliderInteractionEnabled.value)
        assertNoWrites()
    }

    @Test
    fun `leaving screen clears local refresh indicator and ignores stale refresh action`() = runTest(dispatcher) {
        attach()
        runCurrent()
        viewModel.onAction(V3SensorsAction.RefreshRequested)
        detach()
        viewModel.onAction(V3SensorsAction.RefreshRequested)
        attach()
        runCurrent()
        assertFalse(viewModel.uiState.value.isRefreshIndicatorVisible)
        assertNoWrites()
    }

    @Test
    fun `animation policy follows composition source without changing values`() = runTest(dispatcher) {
        attach()
        runCurrent()
        source.current = source.current.copy(animationsEnabled = false)
        source.updates.emit(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.animationsEnabled)
        assertEquals(mapOf(open to 18, close to 62), values())
        assertNoWrites()
    }

    private class FakeRepository : V3DeviceSettingsRepository {
        override val sliderInteractionEnabled = MutableStateFlow(true)
        val values = mapOf(
            P_KEY_EMG_GAIN_OPEN_VALUE to MutableStateFlow<Int?>(18),
            P_KEY_EMG_GAIN_CLOSE_VALUE to MutableStateFlow<Int?>(62),
        )
        val writes = mutableListOf<Pair<String, Int>>()
        override fun observeSliderValue(parameterKey: String) = values.getValue(parameterKey)
        override fun getSliderValue(parameterKey: String) = values.getValue(parameterKey).value
        override fun setSliderValue(parameterKey: String, value: Int) {
            writes += parameterKey to value
            values.getValue(parameterKey).value = value
        }
    }

    private class FakeWidgetsSource : V3SensorsWidgetsSource {
        override val updates = MutableSharedFlow<Unit>()
        var current = V3SensorsWidgetsSnapshot(
            V3DeviceProfile.STANDARD_V3, "first-device",
            V3SensorsWidgetMapper().fromItems(listOf(P_KEY_EMG_GAIN_OPEN_VALUE, P_KEY_EMG_GAIN_CLOSE_VALUE).map { key ->
                SliderItemV3(key, SliderParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                    parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key)),
                )), 0, 100, 1f))
            }),
        )
        override fun snapshot() = current
    }
}
