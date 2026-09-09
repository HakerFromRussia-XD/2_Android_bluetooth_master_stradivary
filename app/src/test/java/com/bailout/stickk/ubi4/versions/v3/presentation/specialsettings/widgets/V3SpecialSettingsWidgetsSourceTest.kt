package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.models.widgets.SwitchItem
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.localizedString
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsSection
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
class V3SpecialSettingsWidgetsSourceTest {
    private val originalWidgets = UiState.listWidgets.toSet()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3.toSet()
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalV3Mode = UiState.isInterfaceV3Activated
    private val originalAddress = WidgetRepoProvider.mac()
    private val originalLocale = Locale.getDefault()
    private val originalUpdates = UiState.updateFlow.replayCache
    private val mapper = V3SpecialSettingsWidgetMapper()
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
        WidgetRepoProvider.setCurrentMac(originalAddress)
        Locale.setDefault(originalLocale)
        UiState.updateFlow.resetReplayCache()
        originalUpdates.forEach { UiState.updateFlow.tryEmit(it) }
        unmockkStatic(::localizedString)
    }

    @ParameterizedTest
    @CsvSource("STANDARD_V3,ru", "STANDARD_V3,en", "INDY3,ru", "INDY3,en")
    fun `unchanged generator preserves both sections and adapter metadata`(profile: V3DeviceProfile, language: String) = runTest {
        useResourceStrings(language)
        UiState.activeV3DeviceProfile = profile
        val parser = BLEParserV3(backgroundScope, executor, bleManager)
        if (profile == V3DeviceProfile.INDY3) parser.generatedHardcodeWidgetsINDY3() else parser.generatedHardcodeWidgets()
        val source = DataFactoryV3SpecialSettingsWidgetsSource()
        val snapshot = source.snapshot(V3SpecialSettingsSection.PROSTHESIS)
        val expectedKeys = if (profile == V3DeviceProfile.INDY3) {
            listOf(P_KEY_EMG_MOVEMENT_LOCK, P_KEY_EMG_MAX_GAIN_VALUE, P_KEY_FORCE_SETTINGS, P_KEY_SPEED_SETTINGS, P_KEY_HAND_CONTROL_MODE, P_KEY_SETTINGS_PROFILE)
        } else {
            listOf(P_KEY_EMG_CHANGE_GESTURE, P_KEY_EMG_MOVEMENT_LOCK, P_KEY_SCREEN_TIMEOUT, P_KEY_EMG_MAX_GAIN_VALUE, P_KEY_FORCE_SETTINGS, P_KEY_SPEED_SETTINGS, P_KEY_HAND_CONTROL_MODE, P_KEY_GESTURE_CHANGE_MODE)
        }
        assertEquals(expectedKeys, snapshot.widgets.map { it.info.key })
        assertEquals(expectedKeys.indices.toList(), snapshot.widgets.map { it.info.position })
        assertEquals(profile, snapshot.deviceProfile)
        assertEquals("test-v3-device", snapshot.deviceAddress)
        assertTrue(UiState.listWidgets.size > snapshot.widgets.size)
        assertEquals(profile == V3DeviceProfile.INDY3, snapshot.widgets.any { it is V3SpecialSettingsWidget.SettingsProfile })
        snapshot.widgets.filterIsInstance<V3SpecialSettingsWidget.ToggleSlider>().forEach {
            assertEquals(10, it.minProgress)
            assertEquals(100, it.maxProgress)
            assertEquals(0.1f, it.increment)
        }
        assertCompatibleItems(factory.prepareData(2), mapper.toItems(snapshot.widgets))

        val mobile = source.snapshot(V3SpecialSettingsSection.APPLICATION).widgets
        val switch = mobile.single() as V3SpecialSettingsWidget.Switch
        assertEquals(MobileSettingsKey.AUTO_LOGIN.key, switch.info.key)
        assertEquals(if (language == "ru") "Автоматический вход" else "Auto login", switch.info.title)
        assertFalse(switch.initialChecked)
        assertCompatibleItems(factory.mobileWidgets(), mapper.toItems(mobile))

        // Subscribing to replay and invalidations only reads the existing composition.
        val observed = mutableListOf<V3SpecialSettingsWidgetsSnapshot>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.updates.take(2).collect { observed += source.snapshot(V3SpecialSettingsSection.PROSTHESIS) }
        }
        UiState.updateFlow.emit(0)
        job.join()
        assertEquals(listOf(snapshot, snapshot), observed)
        verify { executor wasNot Called; bleManager wasNot Called }
    }

    @Test
    fun `V3 source does not read or modify UBI4 widgets`() {
        val factory = mockk<DataFactory>()
        val ubi4Widget = Any()
        UiState.listWidgets.add(ubi4Widget)
        UiState.isInterfaceV3Activated = false
        val source = DataFactoryV3SpecialSettingsWidgetsSource(factory)
        V3SpecialSettingsSection.entries.forEach { section ->
            val snapshot = source.snapshot(section)
            assertEquals(V3DeviceProfile.NOT_V3, snapshot.deviceProfile)
            assertTrue(snapshot.widgets.isEmpty())
        }
        assertEquals(setOf(ubi4Widget), UiState.listWidgets)
        verify { factory wasNot Called }
    }

    @Test
    fun `empty initial composition is available without an update event`() {
        val source = DataFactoryV3SpecialSettingsWidgetsSource()
        assertTrue(UiState.updateFlow.replayCache.isEmpty())
        assertTrue(source.snapshot(V3SpecialSettingsSection.PROSTHESIS).widgets.isEmpty())
        assertTrue(UiState.updateFlow.replayCache.isEmpty())
    }

    private fun assertCompatibleItems(original: List<Any>, restored: List<Any>) {
        assertEquals(original.size, restored.size)
        original.zip(restored).forEach { (before, after) ->
            assertEquals(before::class, after::class)
            when (before) {
                is SliderItemV3 -> {
                    after as SliderItemV3
                    assertEquals(before.title, after.title)
                    val raw = before.widget as SliderParameterWidgetSStruct
                    val result = after.widget as SliderParameterWidgetSStruct
                    assertEquals(raw.copy(baseParameterWidgetSStruct = result.baseParameterWidgetSStruct), result)
                    assertIdentity(raw.baseParameterWidgetSStruct.baseParameterWidgetStruct, result.baseParameterWidgetSStruct.baseParameterWidgetStruct)
                }
                is ToggleSliderItemV3 -> {
                    after as ToggleSliderItemV3
                    assertEquals(before.title, after.title)
                    val raw = before.widget as ToggleSliderParameterWidgetSStruct
                    val result = after.widget as ToggleSliderParameterWidgetSStruct
                    assertEquals(raw.copy(baseParameterWidgetSStruct = result.baseParameterWidgetSStruct), result)
                    assertIdentity(raw.baseParameterWidgetSStruct.baseParameterWidgetStruct, result.baseParameterWidgetSStruct.baseParameterWidgetStruct)
                }
                is SpinnerItemV3 -> {
                    after as SpinnerItemV3
                    assertEquals(before.title, after.title)
                    val raw = before.widget as SpinnerParameterWidgetSStruct
                    val result = after.widget as SpinnerParameterWidgetSStruct
                    assertEquals(raw.dataSpinnerParameterWidgetStruct, result.dataSpinnerParameterWidgetStruct)
                    assertIdentity(raw.baseParameterWidgetSStruct.baseParameterWidgetStruct, result.baseParameterWidgetSStruct.baseParameterWidgetStruct)
                    val copiedState = mapper.fromItems(listOf(before))
                    (result.dataSpinnerParameterWidgetStruct.spinnerItems as MutableList<String>).clear()
                    assertEquals(copiedState, mapper.fromItems(listOf(before)))
                }
                is SwitchItem -> {
                    after as SwitchItem
                    assertEquals(before, after)
                }
            }
        }
    }

    private fun assertIdentity(
        original: com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct,
        restored: com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct,
    ) {
        // These fields determine the existing V3 delegate IDs and parameter bindings.
        assertEquals(original.parameterInfoSet, restored.parameterInfoSet)
        assertEquals(original.widgetPosition, restored.widgetPosition)
        assertEquals(original.display, restored.display)
        assertEquals(original.widgetCode, restored.widgetCode)
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
