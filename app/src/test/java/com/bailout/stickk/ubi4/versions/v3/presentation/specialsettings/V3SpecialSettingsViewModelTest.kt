package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsAction
import com.bailout.stickk.ubi4.versions.v3.di.V3SpecialSettingsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetInfo
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SpecialSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val store = ViewModelStore()
    private val widgetsSource = FakeWidgetsSource()
    private val appSettings = FakeV3AppSettingsRepository()
    private lateinit var viewModel: V3SpecialSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3SpecialSettingsViewModelFactory(repository, widgetsSource, repository, repository, NoSettingsProfilesRepository, appSettings, sessionRepository = widgetsSource).create(V3SpecialSettingsViewModel::class.java)
        store.put("special-settings", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onAction(V3SpecialSettingsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SpecialSettingsAction.ViewDetached)
    private fun select(section: V3SpecialSettingsSection) =
        viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(section))
    private fun slider(action: V3SliderAction) = viewModel.onAction(V3SpecialSettingsAction.SliderAction(action))
    private fun step(key: String = P_KEY_SPEED_SETTINGS) = slider(V3SliderAction.SliderStepClicked(key, 1))
    private fun commit(value: Int, key: String = P_KEY_SPEED_SETTINGS) =
        slider(V3SliderAction.SliderChangeCommitted(key, value))
    private fun values() = viewModel.uiState.value.sliders.mapValues { it.value.value }
    private fun assertNoWrites() = assertTrue(repository.writes.isEmpty())

    @Test
    fun `opening restores only the three special settings sliders without writing`() = runTest(dispatcher) {
        commit(42)
        attach()
        runCurrent()
        assertEquals(V3SpecialSettingsSection.PROSTHESIS, viewModel.uiState.value.selectedSection)
        assertEquals(mapOf(P_KEY_SPEED_SETTINGS to 17, P_KEY_FORCE_SETTINGS to 70, P_KEY_EMG_MAX_GAIN_VALUE to 225), values())
        assertEquals(0..250, viewModel.uiState.value.sliders.getValue(P_KEY_EMG_MAX_GAIN_VALUE).allowedRange)
        viewModel.uiState.value.sliders.values.forEach { assertTrue(it.isEnabled) }
        commit(42, key = "another-screen-parameter")
        assertNoWrites()
    }

    @Test
    fun `slider action changes a draft and release applies it through the use case`() = runTest(dispatcher) {
        attach()
        runCurrent()
        slider(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 42))
        runCurrent()
        assertEquals(42, values()[P_KEY_SPEED_SETTINGS])
        assertEquals(17, repository.values.getValue(P_KEY_SPEED_SETTINGS).value)
        assertNoWrites()
        commit(42)
        runCurrent()
        assertEquals(listOf(P_KEY_SPEED_SETTINGS to 42), repository.writes)
    }

    @Test
    fun `application section cancels all deadlines and blocks input before collectors resume`() = runTest(dispatcher) {
        attach()
        runCurrent()
        repository.values.keys.forEach(::step)
        advanceTimeBy(299)
        select(V3SpecialSettingsSection.APPLICATION)
        commit(55)
        repository.values.keys.forEach(::step)
        advanceTimeBy(302)
        runCurrent()
        assertEquals(V3SpecialSettingsSection.APPLICATION, viewModel.uiState.value.selectedSection)
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.isEnabled) }
        assertNoWrites()

        repository.values.getValue(P_KEY_FORCE_SETTINGS).value = 83
        runCurrent()
        select(V3SpecialSettingsSection.PROSTHESIS)
        runCurrent()
        assertEquals(mapOf(P_KEY_SPEED_SETTINGS to 17, P_KEY_FORCE_SETTINGS to 83, P_KEY_EMG_MAX_GAIN_VALUE to 225), values())
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.animateValueChange) }
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
    }

    @Test
    fun `restoring the application section keeps sliders inactive across lifecycle and reconnect`() = runTest(dispatcher) {
        appSettings.settingsSection = V3SpecialSettingsSection.APPLICATION
        attach()
        runCurrent()
        step()
        detach()
        repository.sliderInteractionEnabled.value = false
        runCurrent()
        repository.sliderInteractionEnabled.value = true
        attach()
        runCurrent()
        commit(42)
        advanceTimeBy(301)
        runCurrent()
        viewModel.uiState.value.sliders.values.forEach { assertFalse(it.isEnabled) }
        assertNoWrites()
    }

    @Test
    fun `section callbacks while stopped are ignored and previous drafts are discarded`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        detach()
        select(V3SpecialSettingsSection.APPLICATION)
        select(V3SpecialSettingsSection.PROSTHESIS)
        commit(55)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        attach()
        runCurrent()
        assertEquals(17, values()[P_KEY_SPEED_SETTINGS])
        commit(42)
        assertEquals(listOf(P_KEY_SPEED_SETTINGS to 42), repository.writes)
    }

    @Test
    fun `repeated attach or current section selection does not reset a pending edit`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(200)
        attach()
        select(V3SpecialSettingsSection.PROSTHESIS)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(P_KEY_SPEED_SETTINGS to 18), repository.writes)
    }

    @Test
    fun `connection lock rejects a release before its collector runs and cancels deadlines`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        repository.sliderInteractionEnabled.value = false
        commit(42)
        advanceTimeBy(301)
        runCurrent()
        repository.sliderInteractionEnabled.value = true
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
    }

    @Test
    fun `clearing the screen viewmodel cancels its pending write`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        store.clear()
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
    }

    @Test
    fun `hiding special settings does not cancel another screen slider deadline`() = runTest(dispatcher) {
        val otherScreen = V3SliderSettingsViewModel(
            GetSliderSettingsUseCaseV3(repository), ObserveSliderSettingsUseCaseV3(repository), SetSliderValueUseCaseV3(repository), setOf(P_KEY_FORCE_SETTINGS),
        )
        store.put("other-screen", otherScreen)
        otherScreen.onAction(V3SliderSettingsAction.ViewAttached)
        attach()
        runCurrent()
        step()
        otherScreen.onAction(V3SliderSettingsAction.SliderAction(V3SliderAction.SliderStepClicked(P_KEY_FORCE_SETTINGS, 1)))
        select(V3SpecialSettingsSection.APPLICATION)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(listOf(P_KEY_FORCE_SETTINGS to 71), repository.writes)
        assertTrue(otherScreen.uiState.value.sliders.getValue(P_KEY_FORCE_SETTINGS).isEnabled)
    }

    @Test
    fun `initial snapshot needs no event and section switches publish their own composition`() = runTest(dispatcher) {
        assertEquals(widgetsSource.prosthesisWidgets, viewModel.uiState.value.widgets)
        assertEquals(V3DeviceProfile.STANDARD_V3, viewModel.uiState.value.deviceProfile)
        attach()
        select(V3SpecialSettingsSection.APPLICATION)
        assertEquals(widgetsSource.mobileWidgets, viewModel.uiState.value.widgets)
        assertEquals(V3SpecialSettingsSection.APPLICATION, viewModel.uiState.value.selectedSection)
        attach()
        runCurrent()
        widgetsSource.updates.emit(Unit)
        runCurrent()
        assertEquals(widgetsSource.mobileWidgets, viewModel.uiState.value.widgets)
        assertNoWrites()
    }

    @Test
    fun `replayed and repeated composition keeps a slider draft and its deadline`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(200)
        repeat(3) { widgetsSource.updates.emit(Unit); runCurrent() }
        assertEquals(18, values()[P_KEY_SPEED_SETTINGS])
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(P_KEY_SPEED_SETTINGS to 18), repository.writes)
    }

    @Test
    fun `removing sliders cancels pending writes and late composition restores latest values`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        val widgets = widgetsSource.prosthesisWidgets
        widgetsSource.prosthesisWidgets = emptyList()
        widgetsSource.updates.emit(Unit)
        runCurrent()
        commit(42)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        assertTrue(viewModel.uiState.value.widgets.isEmpty())
        repository.values.getValue(P_KEY_SPEED_SETTINGS).value = null
        runCurrent()
        assertEquals(null, values()[P_KEY_SPEED_SETTINGS])
        repository.values.getValue(P_KEY_SPEED_SETTINGS).value = 63
        widgetsSource.prosthesisWidgets = widgets
        widgetsSource.updates.emit(Unit)
        runCurrent()
        assertEquals(63, values()[P_KEY_SPEED_SETTINGS])
        assertEquals(widgets, viewModel.uiState.value.widgets)
        assertNoWrites()
    }

    @Test
    fun `device address and device profile changes discard pending edits even with identical widgets`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(200)
        widgetsSource.address = "second-device"
        repository.values.getValue(P_KEY_SPEED_SETTINGS).value = 63
        widgetsSource.updates.emit(Unit)
        runCurrent()
        assertEquals(63, values()[P_KEY_SPEED_SETTINGS])
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
        step()
        widgetsSource.profile = V3DeviceProfile.INDY3
        widgetsSource.updates.emit(Unit)
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(V3DeviceProfile.INDY3, viewModel.uiState.value.deviceProfile)
        assertNoWrites()
    }

    @Test
    fun `attach reads changes without an event and non V3 profile blocks slider actions`() = runTest(dispatcher) {
        attach()
        runCurrent()
        detach()
        widgetsSource.prosthesisWidgets = widgetsSource.prosthesisWidgets.take(1)
        attach()
        assertEquals(widgetsSource.prosthesisWidgets, viewModel.uiState.value.widgets)
        commit(42, P_KEY_FORCE_SETTINGS)
        assertNoWrites()
        widgetsSource.profile = V3DeviceProfile.NOT_V3
        widgetsSource.updates.emit(Unit)
        runCurrent()
        commit(42)
        advanceTimeBy(301)
        runCurrent()
        assertNoWrites()
    }

    @ParameterizedTest
    @EnumSource(V3SpecialSettingsSection::class)
    fun `new screen restores either saved section before rendering without writing`(section: V3SpecialSettingsSection) = runTest(dispatcher) {
        appSettings.settingsSection = section
        store.clear()
        viewModel = V3SpecialSettingsViewModelFactory(repository, widgetsSource, repository, repository, NoSettingsProfilesRepository, appSettings, sessionRepository = widgetsSource)
            .create(V3SpecialSettingsViewModel::class.java)
        store.put("restored", viewModel)
        assertEquals(section, viewModel.uiState.value.selectedSection)
        assertEquals(widgetsSource.snapshot(section).widgets, viewModel.uiState.value.widgets)
        attach(); runCurrent(); detach(); attach(); runCurrent()
        assertEquals(section, viewModel.uiState.value.selectedSection)
        assertTrue(appSettings.sectionWrites.isEmpty())
        assertNoWrites()
    }

    @Test
    fun `section changes persist once independently of auto login and widget updates`() = runTest(dispatcher) {
        attach(); runCurrent()
        select(V3SpecialSettingsSection.APPLICATION)
        select(V3SpecialSettingsSection.APPLICATION)
        widgetsSource.updates.emit(Unit); runCurrent()
        detach(); attach(); runCurrent()
        assertEquals(V3SpecialSettingsSection.APPLICATION, viewModel.uiState.value.selectedSection)
        select(V3SpecialSettingsSection.PROSTHESIS)
        assertEquals(listOf(V3SpecialSettingsSection.APPLICATION, V3SpecialSettingsSection.PROSTHESIS), appSettings.sectionWrites)
        assertTrue(appSettings.writes.isEmpty())
        assertNoWrites()
    }

    @Test
    fun `stopped or non V3 callbacks cannot change the stored section`() = runTest(dispatcher) {
        select(V3SpecialSettingsSection.APPLICATION)
        attach(); runCurrent(); detach()
        select(V3SpecialSettingsSection.APPLICATION)
        assertTrue(appSettings.sectionWrites.isEmpty())
        widgetsSource.profile = V3DeviceProfile.NOT_V3
        attach(); runCurrent()
        select(V3SpecialSettingsSection.APPLICATION)
        assertTrue(appSettings.sectionWrites.isEmpty())
        assertEquals(V3SpecialSettingsSection.PROSTHESIS, appSettings.settingsSection)
    }

    @Test
    fun `return reads an externally changed preference without replaying a user action`() = runTest(dispatcher) {
        attach(); runCurrent(); detach()
        appSettings.settingsSection = V3SpecialSettingsSection.APPLICATION
        attach(); runCurrent()
        assertEquals(V3SpecialSettingsSection.APPLICATION, viewModel.uiState.value.selectedSection)
        assertEquals(widgetsSource.mobileWidgets, viewModel.uiState.value.widgets)
        assertTrue(appSettings.sectionWrites.isEmpty())
        assertNoWrites()
    }

    @Test
    fun `section remains selectable while device parameters are locked`() = runTest(dispatcher) {
        repository.sliderInteractionEnabled.value = false
        attach(); runCurrent()
        select(V3SpecialSettingsSection.APPLICATION)
        assertEquals(V3SpecialSettingsSection.APPLICATION, viewModel.uiState.value.selectedSection)
        assertEquals(listOf(V3SpecialSettingsSection.APPLICATION), appSettings.sectionWrites)
        assertNoWrites()
    }

    @Test
    fun `section save failure retains the current screen and only explicit selection retries`() = runTest(dispatcher) {
        attach(); runCurrent()
        appSettings.sectionSaveError = IllegalStateException("Preferences unavailable")
        select(V3SpecialSettingsSection.APPLICATION)
        assertTrue(viewModel.uiState.value.settingsSectionSaveFailed)
        assertEquals(V3SpecialSettingsSection.PROSTHESIS, viewModel.uiState.value.selectedSection)
        detach(); attach(); runCurrent()
        assertTrue(appSettings.sectionWrites.isEmpty())
        appSettings.sectionSaveError = null
        select(V3SpecialSettingsSection.APPLICATION)
        assertFalse(viewModel.uiState.value.settingsSectionSaveFailed)
        assertEquals(listOf(V3SpecialSettingsSection.APPLICATION), appSettings.sectionWrites)
        assertNoWrites()
    }

    @Test
    fun `section read failure uses the default without overwriting preferences and recovers on return`() = runTest(dispatcher) {
        store.clear()
        appSettings.sectionReadError = IllegalStateException("Invalid stored type")
        viewModel = V3SpecialSettingsViewModelFactory(repository, widgetsSource, repository, repository, NoSettingsProfilesRepository, appSettings, sessionRepository = widgetsSource)
            .create(V3SpecialSettingsViewModel::class.java)
        store.put("failed-read", viewModel)
        assertEquals(V3SpecialSettingsSection.PROSTHESIS, viewModel.uiState.value.selectedSection)
        assertTrue(viewModel.uiState.value.settingsSectionReadFailed)
        attach(); runCurrent()
        appSettings.sectionReadError = null
        appSettings.settingsSection = V3SpecialSettingsSection.APPLICATION
        detach(); attach(); runCurrent()
        assertEquals(V3SpecialSettingsSection.APPLICATION, viewModel.uiState.value.selectedSection)
        assertFalse(viewModel.uiState.value.settingsSectionReadFailed)
        assertTrue(appSettings.sectionWrites.isEmpty())
        assertNoWrites()
    }

    @Test
    fun `animation policy follows the composition snapshot without discarding drafts`() = runTest(dispatcher) {
        attach(); runCurrent()
        slider(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 42))
        runCurrent()
        widgetsSource.animationsEnabled = false
        widgetsSource.updates.emit(Unit); runCurrent()
        assertFalse(viewModel.uiState.value.animationsEnabled)
        assertEquals(42, values()[P_KEY_SPEED_SETTINGS])
        detach()
        widgetsSource.animationsEnabled = true
        attach(); runCurrent()
        assertTrue(viewModel.uiState.value.animationsEnabled)
        assertEquals(17, values()[P_KEY_SPEED_SETTINGS])
        assertNoWrites()
    }

    private class FakeWidgetsSource : V3SpecialSettingsWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot(V3SpecialSettingsSection.PROSTHESIS).let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile, section: V3SpecialSettingsSection) = snapshot(section).widgets
        override val updates = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        var profile = V3DeviceProfile.STANDARD_V3
        var address = "first-device"
        var animationsEnabled = true
        var prosthesisWidgets: List<V3SpecialSettingsWidget> =
            listOf(P_KEY_SPEED_SETTINGS, P_KEY_FORCE_SETTINGS, P_KEY_EMG_MAX_GAIN_VALUE).mapIndexed { index, key ->
                V3SpecialSettingsWidget.Slider(V3SpecialSettingsWidgetInfo(key, key, index), 0, 100, 1f)
            }
        val mobileWidgets = listOf(V3SpecialSettingsWidget.Switch(V3SpecialSettingsWidgetInfo("auto-login", "Auto login", 0), false))
        fun snapshot(section: V3SpecialSettingsSection) = V3SpecialSettingsWidgetsSnapshot(
            profile, address, if (section == V3SpecialSettingsSection.PROSTHESIS) prosthesisWidgets else mobileWidgets,
            animationsEnabled = animationsEnabled,
        )
    }

    private class FakeRepository : V3DeviceSettingsRepository, V3ToggleSliderSettingsRepository, V3SpinnerSettingsRepository {
        override val spinnerInteractionEnabled get() = sliderInteractionEnabled
        override fun getSpinnerValue(parameterKey: String): Int? = null
        override fun observeSpinnerValue(parameterKey: String) = MutableStateFlow<Int?>(null)
        override fun setSpinnerValue(parameterKey: String, value: Int) = error("No Spinner in this fixture")
        override val toggleSliderInteractionEnabled get() = sliderInteractionEnabled
        override fun getToggleSliderValue(parameterKey: String): V3ToggleSliderValue? = null
        override fun observeToggleSliderValue(parameterKey: String) = MutableStateFlow<V3ToggleSliderValue?>(null)
        override fun saveToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) = error("No ToggleSlider in this fixture")
        override fun sendToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) = error("No ToggleSlider in this fixture")
        val values = mapOf(
            P_KEY_SPEED_SETTINGS to MutableStateFlow<Int?>(17),
            P_KEY_FORCE_SETTINGS to MutableStateFlow<Int?>(70),
            P_KEY_EMG_MAX_GAIN_VALUE to MutableStateFlow<Int?>(225),
        )
        override val sliderInteractionEnabled = MutableStateFlow(true)
        val writes = mutableListOf<Pair<String, Int>>()
        override fun getSliderValue(parameterKey: String) = values.getValue(parameterKey).value
        override fun observeSliderValue(parameterKey: String) = values.getValue(parameterKey)
        override fun setSliderValue(parameterKey: String, value: Int) {
            writes.add(parameterKey to value)
            values.getValue(parameterKey).value = value
        }
    }
}
