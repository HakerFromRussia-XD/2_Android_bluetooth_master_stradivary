package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.di.V3ServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.*
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = Repository()
    private val source = Source()
    private lateinit var viewModel: V3ServiceViewModel
    private val thumb = P_KEY_GLOBAL_THUMB_CLOSED_POSITION
    private val fingers = P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3ServiceViewModelFactory(repository, source, repository, FakeV3DeviceRoleRepository(), FakeV3DeviceInfoRepository(), FakeV3ProsthesisCalibrationRepository(), sessionRepository = source).create(V3ServiceViewModel::class.java)
        store.put("service", viewModel)
    }

    @AfterEach
    fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun attach() = viewModel.onAction(V3ServiceAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3ServiceAction.ViewDetached)
    private fun action(value: V3SliderAction) = viewModel.onAction(V3ServiceAction.SliderAction(value))
    private fun commit(value: Int, key: String = thumb) = action(V3SliderAction.SliderChangeCommitted(key, value))
    private fun step(key: String = thumb) = action(V3SliderAction.SliderStepClicked(key, 1))
    private fun values() = viewModel.uiState.value.sliders.mapValues { it.value.value }
    private fun noWrites() = assertTrue(repository.writes.isEmpty())

    @Test
    fun `standard V3 restores both sliders with domain ranges and no writes`() = runTest(dispatcher) {
        assertEquals(source.current.widgets, viewModel.uiState.value.widgets)
        assertEquals(mapOf(thumb to 65, fingers to 62), values())
        assertTrue(viewModel.uiState.value.sliders.values.all { !it.isEnabled })
        commit(20)
        attach(); runCurrent()
        viewModel.uiState.value.sliders.values.forEach { assertTrue(it.isEnabled); assertEquals(0..100, it.allowedRange) }
        attach(); source.updates.emit(Unit); runCurrent()
        commit(20, P_KEY_EMG_GAIN_OPEN_VALUE)
        noWrites()
    }

    @Test
    fun `INDY3 keeps its composition without exposing or accepting the absent sliders`() = runTest(dispatcher) {
        source.current = source.current.copy(deviceProfile = V3DeviceProfile.INDY3, widgets = listOf(V3ServiceWidget.BleLog))
        attach(); runCurrent()
        assertEquals(V3DeviceProfile.INDY3, viewModel.uiState.value.deviceProfile)
        assertEquals(source.current.widgets, viewModel.uiState.value.widgets)
        assertTrue(viewModel.uiState.value.sliders.isEmpty())
        commit(30); step(fingers)
        advanceTimeBy(301); runCurrent()
        noWrites()
    }

    @Test
    fun `draft is local and committing each slider uses domain limits`() = runTest(dispatcher) {
        attach(); runCurrent()
        action(V3SliderAction.SliderValueChanged(thumb, 80))
        assertEquals(80, values()[thumb])
        assertEquals(65, repository.values.getValue(thumb).value)
        noWrites()
        commit(150); commit(-3, fingers)
        assertEquals(listOf(thumb to 100, fingers to 0), repository.writes)
    }

    @Test
    fun `repeated composition and attach retain pending writes without duplication`() = runTest(dispatcher) {
        attach(); runCurrent()
        step(); step(); step(fingers)
        advanceTimeBy(200)
        source.updates.emit(Unit); attach(); runCurrent()
        assertEquals(mapOf(thumb to 67, fingers to 63), values())
        advanceTimeBy(101); runCurrent()
        assertEquals(listOf(thumb to 67, fingers to 63), repository.writes)
        source.updates.emit(Unit); runCurrent()
        advanceTimeBy(301); runCurrent()
        assertEquals(2, repository.writes.size)
    }

    @Test
    fun `incoming values and animation changes render without sending or changing composition`() = runTest(dispatcher) {
        attach(); runCurrent()
        val widgets = viewModel.uiState.value.widgets
        repository.values.getValue(thumb).value = 71
        runCurrent()
        assertEquals(71, values()[thumb])
        assertEquals(widgets, viewModel.uiState.value.widgets)
        source.current = source.current.copy(animationsEnabled = false)
        source.updates.emit(Unit); runCurrent()
        assertFalse(viewModel.uiState.value.animationsEnabled)
        noWrites()
    }

    @Test
    fun `leaving cancels scheduled writes and returning restores latest data`() = runTest(dispatcher) {
        attach(); runCurrent(); step()
        advanceTimeBy(299)
        detach(); commit(90)
        advanceTimeBy(302); runCurrent()
        noWrites()
        repository.values.getValue(thumb).value = 68
        runCurrent()
        attach(); runCurrent()
        assertEquals(68, values()[thumb])
        assertFalse(viewModel.uiState.value.sliders.getValue(thumb).animateValueChange)
        noWrites()
    }

    @Test
    fun `fresh lock rejects a commit before collection and cancels the pending step`() = runTest(dispatcher) {
        attach(); runCurrent(); step()
        repository.sliderInteractionEnabled.value = false
        commit(90)
        advanceTimeBy(301); runCurrent()
        assertTrue(viewModel.uiState.value.sliders.values.all { !it.isEnabled })
        repository.sliderInteractionEnabled.value = true
        runCurrent(); advanceTimeBy(301); runCurrent()
        noWrites()
    }

    @Test
    fun `device change cancels writes even with identical widget composition`() = runTest(dispatcher) {
        attach(); runCurrent(); step()
        source.current = source.current.copy(deviceAddress = "second-device")
        repository.values.getValue(thumb).value = 40
        source.updates.emit(Unit); runCurrent()
        advanceTimeBy(301); runCurrent()
        assertEquals(40, values()[thumb])
        noWrites()
    }

    @Test
    fun `removing a slider rejects its stale callback and preserves the other control`() = runTest(dispatcher) {
        attach(); runCurrent(); step()
        source.current = source.current.copy(widgets = source.current.widgets.drop(1))
        source.updates.emit(Unit); runCurrent()
        commit(20)
        advanceTimeBy(301); runCurrent()
        assertEquals(setOf(fingers), viewModel.uiState.value.sliders.keys)
        noWrites()
        commit(30, fingers)
        assertEquals(listOf(fingers to 30), repository.writes)
    }

    @Test
    fun `switching to INDY3 or leaving V3 cancels writes without recreating missing sliders`() = runTest(dispatcher) {
        attach(); runCurrent(); step()
        val original = source.current
        source.current = original.copy(deviceProfile = V3DeviceProfile.INDY3, widgets = listOf(V3ServiceWidget.BleLog))
        source.updates.emit(Unit); runCurrent()
        commit(90); advanceTimeBy(301); runCurrent()
        assertTrue(viewModel.uiState.value.sliders.isEmpty())
        source.current = original
        source.updates.emit(Unit); runCurrent(); step()
        source.current = original.copy(deviceProfile = V3DeviceProfile.NOT_V3, widgets = emptyList())
        source.updates.emit(Unit); runCurrent()
        commit(90); advanceTimeBy(301); runCurrent()
        assertTrue(viewModel.uiState.value.widgets.isEmpty())
        assertTrue(viewModel.uiState.value.sliders.isEmpty())
        noWrites()
    }

    @Test
    fun `cleared screen ignores late actions and cannot send a slider value`() = runTest(dispatcher) {
        attach(); runCurrent(); step()
        store.clear()
        val state = viewModel.uiState.value
        attach(); commit(90); step(fingers); detach()
        advanceTimeBy(301); runCurrent()
        assertEquals(state, viewModel.uiState.value)
        noWrites()
    }

    private class Repository : V3DeviceSettingsRepository, V3SpinnerSettingsRepository {
        override val spinnerInteractionEnabled = MutableStateFlow(true)
        override fun getSpinnerValue(parameterKey: String): Int? = null
        override fun observeSpinnerValue(parameterKey: String) = MutableStateFlow<Int?>(null)
        override fun setSpinnerValue(parameterKey: String, value: Int) = error("No Spinner in this fixture")
        override val sliderInteractionEnabled = MutableStateFlow(true)
        val values = mapOf(P_KEY_GLOBAL_THUMB_CLOSED_POSITION to MutableStateFlow<Int?>(65),
            P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION to MutableStateFlow<Int?>(62))
        val writes = mutableListOf<Pair<String, Int>>()
        override fun getSliderValue(parameterKey: String) = values.getValue(parameterKey).value
        override fun observeSliderValue(parameterKey: String) = values.getValue(parameterKey)
        override fun setSliderValue(parameterKey: String, value: Int) {
            writes += parameterKey to value
            values.getValue(parameterKey).value = value
        }
    }

    private class Source : V3ServiceWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot().let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile) = snapshot().widgets
        override val updates = MutableSharedFlow<Unit>()
        var current = V3ServiceWidgetsSnapshot(V3DeviceProfile.STANDARD_V3, "first-device",
            V3ServiceWidgetMapper().fromItems(listOf(P_KEY_GLOBAL_THUMB_CLOSED_POSITION, P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION)
                .mapIndexed { index, key -> SliderItemV3(key, SliderParameterWidgetSStruct(
                    BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4, widgetPosition = index,
                        parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key)))), 0, 100, 1f)) }))
        fun snapshot() = current
    }
}
