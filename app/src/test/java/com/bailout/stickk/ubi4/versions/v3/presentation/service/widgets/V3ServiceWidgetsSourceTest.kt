package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.models.blelog.BleLogButtonItem
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_START_CALIBRATE_COMMAND
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.localizedString
import dev.icerock.moko.resources.StringResource
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceWidgetsSourceTest {
    private val originalWidgets = UiState.listWidgets.toSet()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3.toSet()
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalV3Mode = UiState.isInterfaceV3Activated
    private val originalSnapshotApplied = WidgetState.dbSnapshotAppliedWithCrc
    private val originalAddress = WidgetRepoProvider.mac()
    private val originalLocale = Locale.getDefault()
    private val originalUpdates = UiState.updateFlow.replayCache
    private val mapper = V3ServiceWidgetMapper()
    private val factory = DataFactory()
    private val executor = mockk<BleCommandExecutor>(relaxed = true)
    private val bleManager = mockk<BleManagerKmm>(relaxed = true)

    @BeforeEach
    fun setUp() {
        UiState.listWidgets.clear()
        UiState.updateFlow.resetReplayCache()
        UiState.isInterfaceV3Activated = true
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3
        WidgetRepoProvider.setCurrentMac("test-v3-device")
    }

    @AfterEach
    fun tearDown() {
        UiState.listWidgets.clear()
        UiState.listWidgets.addAll(originalWidgets)
        GlobalParameters.baseSubDevicesInfoStructSetV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3.addAll(originalDevices)
        UiState.activeV3DeviceProfile = originalProfile
        UiState.isInterfaceV3Activated = originalV3Mode
        WidgetState.dbSnapshotAppliedWithCrc = originalSnapshotApplied
        WidgetRepoProvider.setCurrentMac(originalAddress)
        Locale.setDefault(originalLocale)
        UiState.updateFlow.resetReplayCache()
        originalUpdates.forEach { UiState.updateFlow.tryEmit(it) }
        unmockkStatic(::localizedString)
    }

    @ParameterizedTest
    @CsvSource("STANDARD_V3,ru", "STANDARD_V3,en", "INDY3,ru", "INDY3,en")
    fun `both generators preserve all Service items and delegate metadata`(profile: V3DeviceProfile, language: String) = runTest {
        generate(profile, language)
        val source = DataFactoryV3ServiceWidgetsSource()
        val snapshot = source.snapshot()
        val expected = factory.prepareData(display = 4)
        assertEquals(profile, snapshot.deviceProfile)
        assertEquals("test-v3-device", snapshot.deviceAddress)
        val standard = profile == V3DeviceProfile.STANDARD_V3
        assertEquals(if (standard) 9 else 6, snapshot.widgets.size)
        val sliderKeys = if (standard) listOf(P_KEY_GLOBAL_THUMB_CLOSED_POSITION, P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION) else emptyList()
        assertEquals(sliderKeys, snapshot.widgets.filterIsInstance<V3ServiceWidget.Slider>().map { it.parameterKey })
        val spinnerKeys = if (standard) listOf(P_KEY_EMG_CONTROL_MODE, P_KEY_LEFT_RIGHT_HAND, P_KEY_DEVICE_ROLE)
            else listOf(P_KEY_EMG_CONTROL_MODE, P_KEY_DEVICE_ROLE)
        assertEquals(spinnerKeys, snapshot.widgets.filterIsInstance<V3ServiceWidget.Spinner>().map { it.parameterKey })
        assertEquals(spinnerKeys.map { ParameterInfoRegistry.require(it) },
            snapshot.widgets.filterIsInstance<V3ServiceWidget.Spinner>().map { it.info.parameters.single() })
        assertEquals(listOf(4) + (if (standard) listOf(2) else emptyList()) + listOf(3),
            snapshot.widgets.filterIsInstance<V3ServiceWidget.Spinner>().map { it.options.size })
        assertEquals(listOf(P_KEY_SET_DEVICE_NAME, P_KEY_SET_SERIAL_NUMBER).map { ParameterInfoRegistry.require(it) },
            snapshot.widgets.filterIsInstance<V3ServiceWidget.TextInput>().map { it.info.parameters.single() })
        assertEquals(com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField.entries.toList(),
            snapshot.widgets.filterIsInstance<V3ServiceWidget.TextInput>().map { it.field })
        val calibration = snapshot.widgets.filterIsInstance<V3ServiceWidget.Buttons>().single()
        assertEquals(listOf(ParameterInfoRegistry.require(P_KEY_START_CALIBRATE_COMMAND)), calibration.info.parameters)
        assertEquals(4, calibration.info.display)
        assertEquals(V3ServiceWidget.BleLog, snapshot.widgets.last())
        assertEquals((0 until snapshot.widgets.size - 1).toList(), expected.mapNotNull { base(it)?.widgetPosition })
        assertEquals(expected, mapper.toItems(snapshot.widgets))
        val role = snapshot.widgets.filterIsInstance<V3ServiceWidget.Spinner>().single { it.parameterKey == P_KEY_DEVICE_ROLE }
        val roleOptions = role.options.drop(1) // Only service engineer and user are currently enabled.
        val forService = mapper.toItems(snapshot.widgets, roleOptions)
        val roleItem = forService.filterIsInstance<SpinnerItemV3>().last().widget as SpinnerParameterWidgetSStruct
        assertEquals(roleOptions, roleItem.dataSpinnerParameterWidgetStruct.spinnerItems)
        assertEquals(role.info.parameters, roleItem.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.toList())
        assertEquals(expected.filterNot { it is SpinnerItemV3 }, forService.filterNot { it is SpinnerItemV3 })
        // Snapshot collections must survive mutations by either the shared factory or delegates.
        val fromFactory = mapper.fromItems(expected)
        val forAdapters = mapper.toItems(fromFactory)
        expected.forEach { base(it)?.parameterInfoSet?.clear() }
        (expected.filterIsInstance<SpinnerItemV3>().first().widget as SpinnerParameterWidgetSStruct)
            .dataSpinnerParameterWidgetStruct.spinnerItems.let { (it as MutableList<String>)[0] = "changed factory option" }
        forAdapters.forEach { base(it)?.parameterInfoSet?.clear() }
        (forAdapters.filterIsInstance<SpinnerItemV3>().first().widget as SpinnerParameterWidgetSStruct)
            .dataSpinnerParameterWidgetStruct.spinnerItems.let { (it as MutableList<String>)[0] = "changed adapter option" }
        assertEquals(snapshot.widgets, fromFactory)
        assertTrue(mapper.toItems(fromFactory).all { base(it)?.parameterInfoSet?.isNotEmpty() != false })
        verify { executor wasNot Called; bleManager wasNot Called }
    }

    @Test
    fun `source update is only a composition signal and preserves animation policy`() = runTest {
        generate(V3DeviceProfile.STANDARD_V3, "ru")
        val source = DataFactoryV3ServiceWidgetsSource()
        WidgetState.dbSnapshotAppliedWithCrc = true
        assertFalse(source.snapshot().animationsEnabled)
        WidgetState.dbSnapshotAppliedWithCrc = false
        assertTrue(source.snapshot().animationsEnabled)
        UiState.updateFlow.resetReplayCache()
        var received = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.updates.take(1).collect { received++ }
        }
        UiState.updateFlow.emit(0)
        assertEquals(1, received)
        verify { executor wasNot Called; bleManager wasNot Called }
    }

    @Test
    fun `source exposes no V3 widgets outside V3 even with a previous device composition`() = runTest {
        generate(V3DeviceProfile.STANDARD_V3, "en")
        UiState.isInterfaceV3Activated = false
        val snapshot = DataFactoryV3ServiceWidgetsSource().snapshot()
        assertEquals(V3DeviceProfile.NOT_V3, snapshot.deviceProfile)
        assertTrue(snapshot.widgets.isEmpty())
        verify { executor wasNot Called; bleManager wasNot Called }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.generate(profile: V3DeviceProfile, language: String) {
        useResourceStrings(language)
        UiState.activeV3DeviceProfile = profile
        val parser = BLEParserV3(backgroundScope, executor, bleManager)
        if (profile == V3DeviceProfile.INDY3) parser.generatedHardcodeWidgetsINDY3() else parser.generatedHardcodeWidgets()
    }

    private fun base(item: Any): BaseParameterWidgetStruct? = when (item) {
        is SpinnerItemV3 -> (item.widget as SpinnerParameterWidgetSStruct).baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SliderItemV3 -> (item.widget as SliderParameterWidgetSStruct).baseParameterWidgetSStruct.baseParameterWidgetStruct
        is ButtonsItemV3 -> (item.widget as CommandParameterWidgetSStruct).baseParameterWidgetSStruct.baseParameterWidgetStruct
        is TextInputItemV3 -> (item.widget as CommandParameterWidgetSStruct).baseParameterWidgetSStruct.baseParameterWidgetStruct
        BleLogButtonItem -> null
        else -> error("Unexpected Service item")
    }

    private fun useResourceStrings(language: String) {
        Locale.setDefault(Locale(language))
        val resourceDirectory = File("../shared/src/commonMain/moko-resources/strings")
        val strings = readStrings(File(resourceDirectory, "base/strings.xml")) +
            if (language == "ru") readStrings(File(resourceDirectory, "ru/strings.xml")) else emptyMap()
        val names = SharedRes.strings::class.java.methods
            .filter { it.parameterCount == 0 && it.returnType == StringResource::class.java }
            .associate { (it.invoke(SharedRes.strings) as StringResource) to it.name.removePrefix("get").replaceFirstChar(Char::lowercase) }
        mockkStatic(::localizedString)
        every { localizedString(any()) } answers { strings.getValue(names.getValue(firstArg())) }
    }

    private fun readStrings(file: File): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
}
