package com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.GesturesItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.localizedString
import dev.icerock.moko.resources.StringResource
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

@OptIn(ExperimentalCoroutinesApi::class)
class V3GesturesWidgetsSourceTest {
    private val originalWidgets = UiState.listWidgets.toSet()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3.toSet()
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalV3Mode = UiState.isInterfaceV3Activated
    private val originalLocale = Locale.getDefault()
    private val originalUpdates = UiState.updateFlow.replayCache
    private val mapper = V3GesturesWidgetMapper()
    private val executor = mockk<BleCommandExecutor>(relaxed = true)
    private val bleManager = mockk<BleManagerKmm>(relaxed = true)

    @BeforeEach fun setUp() {
        UiState.listWidgets.clear()
        UiState.updateFlow.resetReplayCache()
        UiState.isInterfaceV3Activated = true
    }

    @AfterEach fun tearDown() {
        UiState.listWidgets.clear(); UiState.listWidgets.addAll(originalWidgets)
        GlobalParameters.baseSubDevicesInfoStructSetV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3.addAll(originalDevices)
        UiState.activeV3DeviceProfile = originalProfile
        UiState.isInterfaceV3Activated = originalV3Mode
        Locale.setDefault(originalLocale)
        UiState.updateFlow.resetReplayCache()
        originalUpdates.forEach { UiState.updateFlow.tryEmit(it) }
        unmockkStatic(::localizedString)
    }

    @ParameterizedTest @CsvSource("STANDARD_V3,ru", "STANDARD_V3,en", "INDY3,ru", "INDY3,en")
    fun `composition matches unchanged generators and factory for both device profiles`(profile: V3DeviceProfile, language: String) = runTest {
        useResourceStrings(language)
        UiState.activeV3DeviceProfile = profile
        val parser = BLEParserV3(backgroundScope, executor, bleManager)
        if (profile == V3DeviceProfile.INDY3) parser.generatedHardcodeWidgetsINDY3() else parser.generatedHardcodeWidgets()
        val before = UiState.listWidgets.toList()
        val expected = DataFactory().prepareData(display = 0)
        val widgets = DataFactoryV3GesturesWidgetsSource().widgets(profile)
        assertEquals(if (profile == V3DeviceProfile.STANDARD_V3) 1 else 0, widgets.size)
        assertEquals(expected, mapper.toItems(widgets))
        if (widgets.isNotEmpty()) {
            assertEquals(listOf(P_KEY_CURRENT_GESTURE, P_KEY_GESTURE_SETTING, P_KEY_GESTURE_GROUPE)
                .map { ParameterInfoRegistry.require(it) }, widgets.single().parameters)
            assertEquals(0, widgets.single().display)
        }
        assertEquals(before, UiState.listWidgets.toList())
        verify { executor wasNot Called; bleManager wasNot Called }
    }

    @Test fun `UBI4 profile never calls shared factory or consumes its widgets`() {
        val factory = mockk<DataFactory>()
        val source = DataFactoryV3GesturesWidgetsSource(factory)
        assertTrue(source.widgets(V3DeviceProfile.NOT_V3).isEmpty())
        verify { factory wasNot Called }
    }

    @Test fun `mapper preserves both label encodings order and all metadata without sharing mutable structures`() {
        val parameters = mutableSetOf(ParameterInfo(15, 36, 2, 0), ParameterInfo(15, 35, 3, 1))
        val base = BaseParameterWidgetStruct(1, 1, 8, 0, 7, 4, 9, 2, 12, 3, parameters, "setting")
        val other = base.copy(widgetPosition = 1, widgetId = 10, parameterInfoSet = parameters.toMutableSet())
        val original = listOf(GesturesItemV3("First", BaseParameterWidgetEStruct(base, 17)),
            GesturesItemV3("Second", BaseParameterWidgetSStruct(other, "Second%label")))
        val widgets = mapper.fromItems(original)
        assertEquals(listOf("First", "Second"), widgets.map { it.title })
        assertEquals(original, mapper.toItems(widgets))
        val delegateItems = mapper.toItems(widgets)
        base.widgetCode = 99; base.parameterInfoSet.clear()
        other.widgetPosition = 99; other.parameterInfoSet.clear()
        (delegateItems.first().widget as BaseParameterWidgetEStruct).baseParameterWidgetStruct.parameterInfoSet.clear()
        (delegateItems.last().widget as BaseParameterWidgetSStruct).baseParameterWidgetStruct.widgetPosition = 99
        assertEquals(listOf(7, 1), widgets.map { it.widgetPosition })
        assertEquals(listOf(8, 8), widgets.map { it.widgetCode })
        assertEquals(listOf(2, 2), widgets.map { it.parameters.size })
        assertEquals(2, (mapper.toItems(widgets).first().widget as BaseParameterWidgetEStruct)
            .baseParameterWidgetStruct.parameterInfoSet.size)
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
