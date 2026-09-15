package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.PlotItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_PLOT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThreshold
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SensorsPlotStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeV3SensorsPlotRepository()
    private val sliders = mockk<V3DeviceSettingsRepository>(relaxed = true)
    private val source = object : V3SensorsWidgetsSource {
        override val updates = MutableSharedFlow<Unit>()
        var current = V3SensorsWidgetsSnapshot(V3DeviceProfile.STANDARD_V3, "first", V3SensorsWidgetMapper().fromItems(listOf(
            PlotItemV3("Plot", PlotParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_PLOT), ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD)),
            )))),
        )))
        override fun snapshot() = current
    }
    private lateinit var viewModel: V3SensorsViewModel
    private fun plot() = requireNotNull(viewModel.uiState.value.plot)
    private fun attach() = viewModel.onAction(V3SensorsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SensorsAction.ViewDetached)
    private fun change(threshold: V3PlotThreshold, value: Int) = viewModel.onAction(
        V3SensorsAction.PlotAction(V3PlotAction.ThresholdValueChanged(threshold, value)),
    )
    private fun commit() = viewModel.onAction(V3SensorsAction.PlotAction(V3PlotAction.ThresholdChangeCommitted))
    private fun assertNoWrites() {
        assertTrue(repository.writes.isEmpty())
        verify(exactly = 0) { sliders.setSliderValue(any(), any()) }
    }

    private fun sensorsTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { store.clear() }
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { sliders.sliderInteractionEnabled } returns MutableStateFlow(true)
        every { sliders.getSliderValue(any()) } returns 18
        every { sliders.observeSliderValue(any()) } returns MutableStateFlow(18)
        viewModel = V3SensorsViewModelFactory(sliders, source, repository, FakeV3SensorsCommandsRepository()).create(V3SensorsViewModel::class.java)
        store.put("sensors", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @ParameterizedTest
    @EnumSource(V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles restore thresholds and observe updates without a command`(profile: V3DeviceProfile) = sensorsTest {
        source.current = source.current.copy(deviceProfile = profile)
        attach()
        runCurrent()
        assertEquals(V3PlotThresholds(158, 109), plot().thresholds)
        assertTrue(plot().isEnabled)
        repository.thresholds.value = V3PlotThresholds(175, 120)
        runCurrent()
        assertEquals(V3PlotThresholds(175, 120), plot().thresholds)
        assertNoWrites()
    }

    @Test
    fun `drag clamps selected threshold and release sends the complete pair once`() = sensorsTest {
        attach(); runCurrent()
        change(V3PlotThreshold.OPEN, 300)
        assertEquals(V3PlotThresholds(255, 109), plot().thresholds)
        assertFalse(plot().animateThresholdChanges)
        assertNoWrites()
        commit(); commit(); runCurrent()
        assertEquals(listOf(V3PlotThresholds(255, 109)), repository.writes)
        change(V3PlotThreshold.CLOSE, -10)
        commit(); runCurrent()
        assertEquals(V3PlotThresholds(255, 0), repository.writes.last())
    }

    @Test
    fun `constant samples continue producing frames and frames never reset a draft`() = sensorsTest {
        attach(); runCurrent()
        repository.samples.emit(listOf(90, 150, 0, 0, 0, 0))
        change(V3PlotThreshold.OPEN, 170)
        advanceTimeBy(75); runCurrent()
        assertEquals(listOf(90, 150, 0, 0, 0, 0), plot().frame?.values)
        val first = requireNotNull(plot().frame).sequence
        advanceTimeBy(25); runCurrent()
        assertTrue(requireNotNull(plot().frame).sequence > first)
        assertEquals(V3PlotThresholds(170, 109), plot().thresholds)
        assertNoWrites()
    }

    @Test
    fun `transition pauses graph insertion and resumes without restarting interpolation`() = sensorsTest {
        attach(); runCurrent()
        repository.samples.emit(listOf(90, 0, 0, 0, 0, 0))
        advanceTimeBy(25); runCurrent()
        val frame = plot().frame
        assertEquals(30, frame?.values?.first())
        repository.paused = true
        advanceTimeBy(100); runCurrent()
        assertEquals(frame, plot().frame)
        assertTrue(plot().isPaused)
        repository.paused = false
        advanceTimeBy(25); runCurrent()
        assertEquals(60, plot().frame?.values?.first())
        assertNoWrites()
    }

    @Test
    fun `fresh lock rejects release then displays zero and restores cache after unlock`() = sensorsTest {
        attach(); runCurrent()
        change(V3PlotThreshold.OPEN, 180)
        repository.interactionEnabled.value = false
        commit()
        runCurrent()
        assertEquals(V3PlotThresholds(), plot().thresholds)
        assertFalse(plot().isEnabled)
        assertFalse(plot().animateThresholdChanges)
        repository.thresholds.value = V3PlotThresholds(190, 130)
        repository.interactionEnabled.value = true
        runCurrent()
        assertEquals(V3PlotThresholds(190, 130), plot().thresholds)
        commit()
        assertNoWrites()
    }

    @Test
    fun `stopping unsubscribes stops frames and discards draft before return`() = sensorsTest {
        attach(); runCurrent()
        change(V3PlotThreshold.CLOSE, 120)
        detach(); runCurrent()
        assertEquals(0, repository.samples.subscriptionCount.value)
        assertEquals(0, repository.thresholds.subscriptionCount.value)
        val stopped = plot()
        advanceTimeBy(200); runCurrent()
        assertEquals(stopped, plot())
        commit()
        attach(); runCurrent()
        assertEquals(V3PlotThresholds(158, 109), plot().thresholds)
        commit()
        assertNoWrites()
    }

    @Test
    fun `same composition keeps draft and one observer while refreshing channel metadata`() = sensorsTest {
        attach(); runCurrent()
        change(V3PlotThreshold.OPEN, 180)
        repository.channels = 6
        source.updates.emit(Unit)
        attach(); runCurrent()
        assertEquals(6, plot().channelCount)
        assertEquals(180, plot().thresholds.open)
        assertEquals(1, repository.samples.subscriptionCount.value)
        assertNoWrites()
    }

    @Test
    fun `device switch discards previous draft and removing Plot stops observations`() = sensorsTest {
        attach(); runCurrent()
        change(V3PlotThreshold.OPEN, 180)
        source.current = source.current.copy(deviceAddress = "second")
        source.updates.emit(Unit); runCurrent()
        commit()
        assertEquals(V3PlotThresholds(158, 109), plot().thresholds)
        source.current = source.current.copy(widgets = emptyList())
        source.updates.emit(Unit); runCurrent()
        assertNull(viewModel.uiState.value.plot)
        assertEquals(0, repository.samples.subscriptionCount.value)
        change(V3PlotThreshold.CLOSE, 120); commit()
        assertNoWrites()
    }

    @Test
    fun `leaving V3 or clearing ViewModel releases subscriptions`() = sensorsTest {
        attach(); runCurrent()
        source.current = source.current.copy(deviceProfile = V3DeviceProfile.NOT_V3)
        source.updates.emit(Unit); runCurrent()
        assertNull(viewModel.uiState.value.plot)
        assertEquals(0, repository.samples.subscriptionCount.value)
        commit(); assertNoWrites()
        source.current = source.current.copy(deviceProfile = V3DeviceProfile.STANDARD_V3)
        source.updates.emit(Unit); runCurrent()
        assertEquals(1, repository.samples.subscriptionCount.value)
        store.clear(); runCurrent()
        val finalState = viewModel.uiState.value
        attach()
        change(V3PlotThreshold.OPEN, 200)
        commit(); runCurrent()
        assertEquals(0, repository.samples.subscriptionCount.value)
        assertEquals(0, repository.thresholds.subscriptionCount.value)
        assertEquals(finalState, viewModel.uiState.value)
        assertNoWrites()
    }
}
