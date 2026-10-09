package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import com.bailout.stickk.ubi4.versions.v3.di.V3SensorsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.ble.ThresholdsV3
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.PlotItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_PLOT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThreshold
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.data.sensors.V3SensorsPlotRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.GetPlotSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.ObservePlotSamplesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.ObservePlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.RequestPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    private val source = object : V3SensorsWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot().let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile) = snapshot().widgets
        override val updates = MutableSharedFlow<Unit>()
        var current = V3SensorsWidgetsSnapshot(V3DeviceProfile.STANDARD_V3, "first", V3SensorsWidgetMapper().fromItems(listOf(
            PlotItemV3("Plot", PlotParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_PLOT), ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD)),
            )))),
        )))
        fun snapshot() = current
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
        viewModel = V3SensorsViewModelFactory(sliders, source, repository, FakeV3SensorsCommandsRepository(), sessionRepository = source).create(V3SensorsViewModel::class.java)
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

@OptIn(ExperimentalCoroutinesApi::class)
class V3SensorsPlotReadPolicyTest {
    private val dispatcher = StandardTestDispatcher()
    private val info = ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD)
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalValues = ParameterStoreV3.values.value
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalAddress = WidgetRepoProvider.mac()
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode,
        data = "{\"openThreshold\":158,\"closeThreshold\":109}")
    private val packets = mutableListOf<ByteArray>()
    private var profileWrites = 0
    private var nativeReads = 0

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
        UiState.isInterfaceV3Activated = false
        WidgetRepoProvider.setCurrentMac("")
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache)))
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        originalValues.forEach { (key, value) ->
            ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
        }
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.activeV3DeviceProfile = originalProfile
        UiState.isInterfaceV3Activated = originalMode
        WidgetRepoProvider.setCurrentMac(originalAddress)
        Dispatchers.resetMain()
    }

    private fun nativeRepository(
        target: ParameterRef? = ParameterRef(info.deviceAddress, info.parameterID, info.dataCode),
        fallback: List<ParameterRef>? = null,
    ) = V3SensorsPlotRepositoryImpl(
        enqueuePacket = { packets += it },
        saveBleValue = { _, _ -> profileWrites++ },
        readThresholds = { nativeReads++; V3PlotThresholds(901, 902) },
        saveValueAfterSending = false,
        sampleTargets = fallback,
        observeThresholdSnapshots = true,
        thresholdTarget = target,
    )

    private fun put(open: Int, close: Int) = ParameterStoreV3.put(
        info, ParameterTypedValueV3.Thresholds(ThresholdsV3(open, close)),
    )

    private fun assertNoObservationSideEffects() {
        assertTrue(packets.isEmpty())
        assertEquals(0, profileWrites)
        assertEquals(0, nativeReads)
        assertEquals("{\"openThreshold\":158,\"closeThreshold\":109}", cache.data)
    }

    @Test
    fun `requests preserve exact encoder bytes offline repeats and nil packets without state writes`() {
        put(31, 83)
        val before = ParameterStoreV3.values.value
        val repository = nativeRepository()
        assertFalse(repository.interactionEnabled.value)
        val request = RequestPlotThresholdsUseCaseV3(repository)
        request(15, 47)
        request(15, 47)
        request(15, 48)
        request(15, 26)
        request(7, 47)
        request(1, 47)
        val profile = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
        request(profile.parameterID, profile.dataCode)
        val expected = listOf(
            byteArrayOf(0, 15, 48, 0, 0x72),
            byteArrayOf(0, 15, 48, 0, 0x72),
            byteArrayOf(0, 15, 48, 0, 0x72),
            byteArrayOf(0, 15, 26, 0, 0x54),
            byteArrayOf(0, 7, 47, 0, 0xA3.toByte()),
            byteArrayOf(0, 1, 47, 0, 0x72),
        )
        assertEquals(expected.size, packets.size)
        expected.zip(packets).forEach { (bytes, queued) -> assertArrayEquals(bytes, queued) }
        assertEquals(before, ParameterStoreV3.values.value)
        assertEquals(0, profileWrites)
        assertEquals(0, nativeReads)
        assertEquals("{\"openThreshold\":158,\"closeThreshold\":109}", cache.data)
        assertEquals(V3DeviceProfile.NOT_V3, UiState.activeV3DeviceProfile)
        assertFalse(repository.interactionEnabled.value)
    }

    @Test
    fun `native snapshot events have no initial read and preserve alias targets repeats and nullable payloads`() = runTest(dispatcher) {
        put(31, 83)
        val nativeEvents = List(3) { mutableListOf<V3PlotThresholds?>() }
        val androidEvents = mutableListOf<V3PlotThresholds?>()
        val jobs = listOf(47, 48, 26).mapIndexed { index, code ->
            ObservePlotThresholdsUseCaseV3(nativeRepository(ParameterRef(info.deviceAddress, info.parameterID, code)))
                .observe { nativeEvents[index] += it }
        } + ObservePlotThresholdsUseCaseV3(V3SensorsPlotRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, _ -> profileWrites++ },
        )).observe { androidEvents += it }
        try {
            runCurrent()
            assertTrue(nativeEvents.all { it.isEmpty() })
            assertEquals(listOf(V3PlotThresholds(31, 83)), androidEvents)
            put(41, 72)
            runCurrent()
            put(41, 72)
            runCurrent()
            put(0, 72)
            runCurrent()
            put(41, 0)
            runCurrent()
            put(0, 0)
            runCurrent()
            ParameterStoreV3.put(info, ParameterTypedValueV3.Slider(SliderV3(sliderValue = 4)))
            runCurrent()
            val expected = listOf(V3PlotThresholds(41, 72), V3PlotThresholds(41, 72), null, null, null, null)
            nativeEvents.forEach { assertEquals(expected, it) }
            assertEquals(listOf(
                V3PlotThresholds(31, 83), V3PlotThresholds(41, 72), V3PlotThresholds(41, 72),
                V3PlotThresholds(0, 72), V3PlotThresholds(41, 0), V3PlotThresholds(0, 0), null,
            ), androidEvents)
            // Raw aliases/foreign keys have no THRESHOLDS metadata in the actual public bridge.
            listOf(
                ParameterInfo(info.parameterID, 48, info.deviceAddress, 0),
                ParameterInfo(info.parameterID, 26, info.deviceAddress, 0),
                ParameterInfo(info.parameterID, info.dataCode, info.deviceAddress + 1, 0),
                ParameterInfoRegistry.require(P_KEY_PLOT),
            ).forEach { other ->
                ParameterStoreV3.put(other, ParameterTypedValueV3.Thresholds(ThresholdsV3(51, 91)))
                runCurrent()
            }
            nativeEvents.forEach { assertEquals(expected, it) }
            assertEquals(7, androidEvents.size)
            assertNoObservationSideEffects()
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
    }

    @Test
    fun `unresolved targets use every binding identity and callback cancellation never synthesizes a replay`() = runTest(dispatcher) {
        val accepted = mutableListOf<V3PlotThresholds?>()
        val rejected = List(3) { mutableListOf<V3PlotThresholds?>() }
        val binding = ParameterRef(info.deviceAddress, info.parameterID, 233)
        val useCase = ObservePlotThresholdsUseCaseV3(nativeRepository(null, listOf(ParameterRef(7, 9, 1), binding)))
        val jobs = mutableListOf<Job>()
        try {
            val first = useCase.observe { accepted += it }.also { jobs += it }
            listOf(
                nativeRepository(null, emptyList()),
                nativeRepository(null, listOf(ParameterRef(info.deviceAddress, info.parameterID + 1, 47))),
                nativeRepository(ParameterRef(info.deviceAddress + 1, info.parameterID, 47), listOf(binding)),
            ).forEachIndexed { index, repository ->
                jobs += ObservePlotThresholdsUseCaseV3(repository).observe { rejected[index] += it }
            }
            runCurrent()
            assertTrue(accepted.isEmpty())
            put(41, 83)
            runCurrent()
            assertEquals(listOf(V3PlotThresholds(41, 83)), accepted)
            assertTrue(rejected.all { it.isEmpty() })
            first.cancel()
            runCurrent()
            assertTrue(first.isCancelled)
            put(42, 84)
            runCurrent()
            assertEquals(1, accepted.size)
            jobs += useCase.observe { accepted += it }
            runCurrent()
            assertEquals(1, accepted.size) // SharedFlow has no replay, even though the current store is populated.
            put(51, 91)
            runCurrent()
            assertEquals(listOf(V3PlotThresholds(41, 83), V3PlotThresholds(51, 91)), accepted)
            assertTrue(rejected.all { it.isEmpty() })
            assertNoObservationSideEffects()
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class V3SensorsPlotSamplesPolicyTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var originalSource: MutableStateFlow<PlotParameterRef>
    private lateinit var source: MutableStateFlow<PlotParameterRef>
    private val packets = mutableListOf<ByteArray>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        originalSource = WidgetState.plotArrayFlow
        source = MutableStateFlow(PlotParameterRef(0, 0, arrayListOf()))
        WidgetState.plotArrayFlow = source
    }

    @AfterEach
    fun tearDown() {
        WidgetState.plotArrayFlow = originalSource
        Dispatchers.resetMain()
    }

    private fun repository(targets: List<ParameterRef>? = null) = V3SensorsPlotRepositoryImpl(
        enqueuePacket = { packets += it }, sampleTargets = targets,
    )

    @Test
    fun `default observation retains six channels and its original initial frame`() = runTest(dispatcher) {
        val plot = ParameterInfoRegistry.require(P_KEY_PLOT)
        val emissions = mutableListOf<List<Int>>()
        val job = launch { ObservePlotSamplesUseCaseV3(repository())().collect { emissions += it } }
        try {
            runCurrent()
            assertEquals(listOf(List(6) { 0 }), emissions)
            source.value = PlotParameterRef(plot.deviceAddress, plot.parameterID, arrayListOf(11, 22, 33, 44, 55, 66, 77))
            runCurrent()
            assertEquals(listOf(11, 22, 33, 44, 55, 66), emissions.last())
            source.value = PlotParameterRef(plot.deviceAddress, plot.parameterID, arrayListOf(88))
            runCurrent()
            assertEquals(listOf(88, 22, 33, 44, 55, 66), emissions.last())
            source.value = PlotParameterRef(plot.deviceAddress, plot.parameterID, arrayListOf())
            runCurrent()
            assertEquals(listOf(88, 22, 33, 44, 55, 66), emissions.last())
            assertEquals(4, emissions.size)
            source.value = PlotParameterRef(plot.deviceAddress + 1, plot.parameterID, arrayListOf(3, 4))
            runCurrent()
            source.value = PlotParameterRef(plot.deviceAddress, plot.parameterID + 1, arrayListOf(3, 4))
            runCurrent()
            assertEquals(4, emissions.size)
            assertTrue(packets.isEmpty())
        } finally {
            job.cancel()
            runCurrent()
        }
    }

    @Test
    fun `native targets retain raw source replay partial empty and repeated deliveries`() = runTest(dispatcher) {
        source.value = PlotParameterRef(7, 9, arrayListOf(10, 255, 3))
        val emissions = mutableListOf<List<Int>>()
        val emptyTargetEmissions = mutableListOf<List<Int>>()
        val jobs = listOf(
            launch { repository(listOf(ParameterRef(7, 9, 233), ParameterRef(8, 10, 47))).observeSamples().collect { emissions += it } },
            launch { repository(emptyList()).observeSamples().collect { emptyTargetEmissions += it } },
        )
        try {
            runCurrent()
            // Current matching source is replayed once; no generated zero frame precedes it.
            assertEquals(listOf(listOf(10, 255)), emissions)
            source.value = PlotParameterRef(7, 9, arrayListOf(10, 255, 9))
            runCurrent()
            assertEquals(listOf(listOf(10, 255), listOf(10, 255)), emissions)
            // Equality conflation belongs to the raw StateFlow, before the two-channel projection.
            source.value = PlotParameterRef(7, 9, arrayListOf(10, 255, 9))
            runCurrent()
            assertEquals(2, emissions.size)
            source.value = PlotParameterRef(7, 9, arrayListOf(250))
            runCurrent()
            assertEquals(listOf(250), emissions.last())
            source.value = PlotParameterRef(7, 9, arrayListOf())
            runCurrent()
            assertEquals(emptyList<Int>(), emissions.last())
            source.value = PlotParameterRef(8, 10, arrayListOf(-6, 300, 8, 9))
            runCurrent()
            assertEquals(listOf(-6, 300), emissions.last())
            assertEquals(5, emissions.size)
            source.value = PlotParameterRef(7, 10, arrayListOf(1, 2))
            runCurrent()
            source.value = PlotParameterRef(8, 9, arrayListOf(1, 2))
            runCurrent()
            assertEquals(5, emissions.size)
            assertTrue(emptyTargetEmissions.isEmpty())
            assertTrue(packets.isEmpty())
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
    }

    @Test
    fun `callback subscription cancels and replays the actual source on resubscription without commands`() = runTest(dispatcher) {
        source.value = PlotParameterRef(7, 9, arrayListOf(31))
        val useCase = ObservePlotSamplesUseCaseV3(repository(listOf(ParameterRef(7, 9, 1))))
        val emissions = mutableListOf<List<Int>>()
        val jobs = mutableListOf<Job>()
        try {
            val first = useCase.observe { emissions += it }.also { jobs += it }
            assertTrue(emissions.isEmpty())
            runCurrent()
            assertEquals(1, source.subscriptionCount.value)
            assertEquals(listOf(listOf(31)), emissions)
            source.value = PlotParameterRef(7, 9, arrayListOf(32, 99))
            runCurrent()
            assertEquals(listOf(32, 99), emissions.last())
            first.cancel()
            runCurrent()
            assertEquals(0, source.subscriptionCount.value)
            source.value = PlotParameterRef(7, 9, arrayListOf(41, 83, 200))
            runCurrent()
            assertEquals(2, emissions.size)
            useCase.observe { emissions += it }.also { jobs += it }
            runCurrent()
            assertEquals(1, source.subscriptionCount.value)
            assertEquals(listOf(listOf(31), listOf(32, 99), listOf(41, 83)), emissions)
            assertTrue(packets.isEmpty())
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
            assertEquals(0, source.subscriptionCount.value)
        }
    }
}

class V3SensorsPlotRepositoryPolicyTest {
    private val info = ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD)
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalValues = ParameterStoreV3.values.value
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalAddress = WidgetRepoProvider.mac()
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode,
        data = "{\"openThreshold\":158,\"closeThreshold\":109}")
    private val packets = mutableListOf<ByteArray>()

    @BeforeEach
    fun setUp() {
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache)))
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        originalValues.forEach { (key, value) ->
            ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
        }
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.activeV3DeviceProfile = originalProfile
        UiState.isInterfaceV3Activated = originalMode
        WidgetRepoProvider.setCurrentMac(originalAddress)
    }

    @Test
    fun `native threshold getter is authoritative including null while default typed and serialized reads remain unchanged`() {
        var nativeValue: V3PlotThresholds? = null
        var reads = 0
        val native = V3SensorsPlotRepositoryImpl(
            enqueuePacket = { error("Reading thresholds must not enqueue commands") },
            saveBleValue = { _, _ -> error("Reading thresholds must not write a profile") },
            readThresholds = { reads++; nativeValue },
            saveValueAfterSending = false,
        )
        val android = V3SensorsPlotRepositoryImpl(
            enqueuePacket = { error("Reading thresholds must not enqueue commands") },
            saveBleValue = { _, _ -> error("Reading thresholds must not write a profile") },
        )
        val typed = ParameterTypedValueV3.Thresholds(ThresholdsV3(77, 88))
        ParameterStoreV3.put(info, typed)
        val get = GetPlotSettingsUseCaseV3(native)
        assertNull(get().thresholds) // A native omitted/zero snapshot must not fall through a populated typed store.
        assertEquals(V3PlotThresholds(77, 88), android.getThresholds())
        nativeValue = V3PlotThresholds(41, 52)
        assertEquals(nativeValue, get().thresholds)
        nativeValue = null
        ParameterStoreV3.clear()
        assertNull(get().thresholds) // Nor may null fall through the serialized profile/cache fallback.
        assertEquals(V3PlotThresholds(158, 109), android.getThresholds())
        assertEquals(3, reads)
        assertEquals(2, get().channelCount)
        assertEquals(4, reads)
        assertNull(ParameterStoreV3.get(info))
        assertEquals("{\"openThreshold\":158,\"closeThreshold\":109}", cache.data)
        assertTrue(packets.isEmpty())
    }

    @Test
    fun `native queue policy keeps repeated offline packets and leaves device stores and profile untouched`() {
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
        UiState.isInterfaceV3Activated = false
        WidgetRepoProvider.setCurrentMac("")
        val received = ParameterTypedValueV3.Thresholds(ThresholdsV3(101, 203))
        ParameterStoreV3.put(info, received)
        val serialized = cache.data
        val native = V3SensorsPlotRepositoryImpl(
            enqueuePacket = { packet ->
                assertSame(received, ParameterStoreV3.get(info))
                assertEquals(serialized, cache.data)
                packets += packet
            },
            saveBleValue = { _, _ -> error("Native SET must not save a profile value") },
            readThresholds = { error("SET must not perform an extra GET") },
            saveValueAfterSending = false,
        )
        assertSame(UiState.v3WidgetsInteractionEnabled, native.interactionEnabled)
        assertFalse(native.interactionEnabled.value)
        val prepared = V3PlotThresholds(91, 37) // Existing native UI pair (37,91), already swapped into device order.
        SetPlotThresholdsUseCaseV3(native)(prepared)
        assertTrue(packets.isEmpty()) // Disabling persistence does not implicitly bypass the default interaction gate.
        val set = SetPlotThresholdsUseCaseV3(native, requireInteractionEnabled = false)
        assertThrows(IllegalArgumentException::class.java) { set(V3PlotThresholds(-1, 37)) }
        assertThrows(IllegalArgumentException::class.java) { set(V3PlotThresholds(91, 256)) }
        assertTrue(packets.isEmpty())
        repeat(2) { set(prepared) }
        val edit = EditPlotThresholdUseCaseV3()
        val ui = edit(edit(V3PlotThresholds(37, 91), V3PlotThreshold.OPEN, -1), V3PlotThreshold.CLOSE, 256)
        assertEquals(V3PlotThresholds(0, 255), ui)
        set(V3PlotThresholds(ui.close, ui.open))
        val expected = byteArrayOf(0x80.toByte(), 15, 3, 0, 0xD3.toByte(), 47, 91, 37, 0x83.toByte())
        assertArrayEquals(expected, packets[0])
        assertArrayEquals(expected, packets[1])
        assertArrayEquals(byteArrayOf(0x80.toByte(), 15, 3, 0, 0xD3.toByte(), 47, 0xFF.toByte(), 0, 0x4A), packets[2])
        assertEquals(3, packets.size) // SET only: no extra GET/read-after-SET or persistence-driven retry.
        assertFalse(native.interactionEnabled.value)
        assertSame(received, ParameterStoreV3.get(info))
        assertEquals(serialized, cache.data)
    }

    @Test
    fun `default Android setter preserves validation gate and queue then typed profile cache ordering`() {
        val events = mutableListOf<String>()
        val value = V3PlotThresholds(255, 109)
        val serialized = cache.data
        val android = V3SensorsPlotRepositoryImpl(
            enqueuePacket = { packet ->
                assertNull(ParameterStoreV3.get(info))
                assertEquals(serialized, cache.data)
                packets += packet
                events += "queue"
            },
            saveBleValue = { key, typed ->
                assertEquals(info, key)
                assertEquals(ParameterTypedValueV3.Thresholds(ThresholdsV3(value.open, value.close)), typed)
                assertSame(typed, ParameterStoreV3.get(info))
                assertEquals(serialized, cache.data)
                events += "profile"
            },
        )
        val set = SetPlotThresholdsUseCaseV3(android)
        UiState.v3WidgetsInteractionEnabled.value = false
        set(value)
        assertThrows(IllegalArgumentException::class.java) { set(V3PlotThresholds(256, 109)) }
        assertTrue(packets.isEmpty())
        assertTrue(events.isEmpty())
        assertNull(ParameterStoreV3.get(info))
        assertEquals(serialized, cache.data)
        UiState.v3WidgetsInteractionEnabled.value = true
        set(value)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 15, 3, 0, 0xD3.toByte(), 47, 0xFF.toByte(), 109, 0xD2.toByte()), packets.single())
        assertEquals(listOf("queue", "profile"), events)
        assertEquals(value, android.getThresholds())
        assertEquals("{\"openThreshold\":255,\"closeThreshold\":109}", ParameterProvider.getParameterV3(info).data)
    }
}
