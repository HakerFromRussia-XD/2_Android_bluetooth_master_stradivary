package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3AutoLoginStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val interaction = MutableStateFlow(false)
    private val appSettings = FakeV3AppSettingsRepository()
    private val source = Source()
    private val sliders = mockk<V3DeviceSettingsRepository>(relaxed = true) {
        every { sliderInteractionEnabled } returns interaction
        every { observeSliderValue(any()) } returns emptyFlow()
        every { getSliderValue(any()) } returns null
    }
    private val toggles = mockk<V3ToggleSliderSettingsRepository>(relaxed = true) {
        every { toggleSliderInteractionEnabled } returns interaction
        every { observeToggleSliderValue(any()) } returns emptyFlow()
        every { getToggleSliderValue(any()) } returns null
    }
    private val spinners = mockk<V3SpinnerSettingsRepository>(relaxed = true) {
        every { spinnerInteractionEnabled } returns interaction
        every { observeSpinnerValue(any()) } returns emptyFlow()
        every { getSpinnerValue(any()) } returns null
    }
    private lateinit var viewModel: V3SpecialSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3SpecialSettingsViewModelFactory(sliders, source, toggles, spinners, NoSettingsProfilesRepository, appSettings)
            .create(V3SpecialSettingsViewModel::class.java)
        store.put("screen", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        verify(exactly = 0) {
            sliders.setSliderValue(any(), any())
            toggles.saveToggleSliderValue(any(), any())
            toggles.sendToggleSliderValue(any(), any())
            spinners.setSpinnerValue(any(), any())
        }
    }

    private fun attach() = viewModel.onAction(V3SpecialSettingsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SpecialSettingsAction.ViewDetached)
    private fun application() = viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(V3SpecialSettingsSection.APPLICATION))
    private fun change(enabled: Boolean) = viewModel.onAction(V3SpecialSettingsAction.AutoLoginChanged(enabled))
    private fun state() = viewModel.uiState.value.autoLogin

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both V3 variants edit app preferences while BLE controls are locked`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        attach(); application(); runCurrent()
        assertFalse(state().isChecked)
        assertTrue(state().isEnabled)
        change(true); change(true); change(false); runCurrent()
        assertEquals(listOf(true, false), appSettings.writes)
        assertFalse(state().isChecked)
        interaction.value = true; runCurrent()
        interaction.value = false; runCurrent()
        assertTrue(state().isEnabled)
    }

    @Test
    fun `entry external profile changes and return only read the stored value`() = runTest(dispatcher) {
        appSettings.autoLogin.value = true
        attach(); application(); runCurrent()
        assertTrue(state().isChecked)
        appSettings.autoLogin.value = false; runCurrent()
        assertFalse(state().isChecked)
        detach(); runCurrent()
        assertEquals(0, appSettings.autoLogin.subscriptionCount.value)
        appSettings.autoLogin.value = true; runCurrent()
        assertFalse(state().isChecked)
        assertFalse(state().isEnabled)
        attach(); runCurrent()
        assertTrue(state().isChecked)
        assertEquals(1, appSettings.autoLogin.subscriptionCount.value)
        assertTrue(appSettings.writes.isEmpty())
        store.clear(); runCurrent()
        assertEquals(0, appSettings.autoLogin.subscriptionCount.value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "prosthesis", "removed", "not-v3"])
    fun `callbacks outside the visible V3 app settings cannot write`(reason: String) = runTest(dispatcher) {
        attach(); application(); runCurrent()
        when (reason) {
            "detached" -> detach()
            "prosthesis" -> viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(V3SpecialSettingsSection.PROSTHESIS))
            "removed" -> { source.visible = false; source.updates.emit(Unit); runCurrent() }
            "not-v3" -> { source.profile = V3DeviceProfile.NOT_V3; source.updates.emit(Unit); runCurrent() }
        }
        change(true); runCurrent()
        assertFalse(state().isEnabled)
        assertTrue(appSettings.writes.isEmpty())
    }

    @Test
    fun `saving failure restores the stored value and only an explicit action retries`() = runTest(dispatcher) {
        attach(); application(); runCurrent()
        appSettings.saveError = IllegalStateException("Preferences unavailable")
        change(true); runCurrent()
        assertFalse(state().isChecked)
        assertTrue(state().saveFailed)
        assertTrue(state().isEnabled)
        source.updates.emit(Unit); detach(); attach(); runCurrent()
        assertTrue(appSettings.writes.isEmpty())
        appSettings.saveError = null
        change(true); runCurrent()
        assertTrue(state().isChecked)
        assertFalse(state().saveFailed)
        assertEquals(listOf(true), appSettings.writes)
    }

    @Test
    fun `failed reads disable input and returning retries without saving`() = runTest(dispatcher) {
        appSettings.readError = IllegalStateException("Invalid stored type")
        attach(); application(); runCurrent()
        assertTrue(state().readFailed)
        assertFalse(state().isLoading)
        assertFalse(state().isEnabled)
        change(true)
        appSettings.readError = null
        appSettings.autoLogin.value = true
        detach(); attach(); runCurrent()
        assertTrue(state().isChecked)
        assertTrue(state().isEnabled)
        assertFalse(state().readFailed)
        assertTrue(appSettings.writes.isEmpty())
    }

    private class Source : V3SpecialSettingsWidgetsSource {
        override val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var profile = V3DeviceProfile.STANDARD_V3
        var visible = true
        override fun snapshot(section: V3SpecialSettingsSection) = V3SpecialSettingsWidgetsSnapshot(
            profile, "device",
            if (visible && profile != V3DeviceProfile.NOT_V3 && section == V3SpecialSettingsSection.APPLICATION) listOf(
                V3SpecialSettingsWidget.Switch(V3SpecialSettingsWidgetInfo(MobileSettingsKey.AUTO_LOGIN.key, "Auto login", 0), true),
            ) else emptyList(),
        )
    }
}
