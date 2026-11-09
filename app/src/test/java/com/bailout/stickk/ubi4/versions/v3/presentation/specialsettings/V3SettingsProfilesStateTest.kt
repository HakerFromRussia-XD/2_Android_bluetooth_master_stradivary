package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.presentation.service.FakeV3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.di.V3SpecialSettingsViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfileOperation
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.V3ToggleSliderAction
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.GetSettingsProfilesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfile
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfiles
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfilesRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetInfo
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSnapshot
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetsSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SettingsProfilesStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val interaction = MutableStateFlow(true)
    private val repository = ProfilesRepository()
    private val source = WidgetsSource()
    private val appSettings = FakeV3AppSettingsRepository()
    private val roles = FakeV3DeviceRoleRepository()
    private val sliders = mockk<V3DeviceSettingsRepository>(relaxed = true) {
        every { sliderInteractionEnabled } returns interaction
        every { getSliderValue(any()) } returns null
        every { observeSliderValue(any()) } returns emptyFlow()
    }
    private val toggles = mockk<V3ToggleSliderSettingsRepository>(relaxed = true) {
        every { toggleSliderInteractionEnabled } returns interaction
        every { getToggleSliderValue(any()) } returns null
        every { observeToggleSliderValue(any()) } returns emptyFlow()
    }
    private val spinners = mockk<V3SpinnerSettingsRepository>(relaxed = true) {
        every { spinnerInteractionEnabled } returns interaction
        every { getSpinnerValue(any()) } returns null
        every { observeSpinnerValue(any()) } returns emptyFlow()
    }
    private lateinit var viewModel: V3SpecialSettingsViewModel
    private var expectedToggleSaves = 0
    private var expectedSliderWrites = 0
    private var expectedToggleSends = 0
    private var expectedSpinnerWrites = 0

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3SpecialSettingsViewModelFactory(sliders, source, toggles, spinners, repository, appSettings, sessionRepository = source, roleRepository = roles)
            .create(V3SpecialSettingsViewModel::class.java)
        store.put("screen", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        verify(exactly = expectedToggleSends) { toggles.sendToggleSliderValue(any(), any()) }
        verify(exactly = expectedSpinnerWrites) { spinners.setSpinnerValue(any(), any()) }
        verify(exactly = expectedSliderWrites) { sliders.setSliderValue(any(), any()) }
        verify(exactly = expectedToggleSaves) { toggles.saveToggleSliderValue(any(), any()) }
    }

    private fun attach() = viewModel.onAction(V3SpecialSettingsAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3SpecialSettingsAction.ViewDetached)
    private fun section(section: V3SpecialSettingsSection) = viewModel.onAction(V3SpecialSettingsAction.SettingsSectionSelected(section))
    private fun state() = requireNotNull(viewModel.uiState.value.settingsProfiles)
    private fun create() = viewModel.onAction(V3SpecialSettingsAction.SettingsProfileCreateRequested)
    private fun editName(id: Int) = viewModel.onAction(V3SpecialSettingsAction.SettingsProfileRenameRequested(id))
    private fun submitName(request: Long, name: String) = viewModel.onAction(V3SpecialSettingsAction.SettingsProfileNameSubmitted(request, name))
    private fun dismissName(request: Long) = viewModel.onAction(V3SpecialSettingsAction.SettingsProfileNameDismissed(request))
    private fun select(id: Int) = viewModel.onAction(V3SpecialSettingsAction.SettingsProfileSelected(id))

    private fun prepareBaseEdit(full: Boolean = false) {
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(0, null), V3SettingsProfile(1, "Personal")) +
            if (full) listOf(V3SettingsProfile(2, null), V3SettingsProfile(3, null)) else emptyList(), 0)
        every { sliders.getSliderValue(P_KEY_SPEED_SETTINGS) } returns 30
        every { toggles.getToggleSliderValue(P_KEY_EMG_MOVEMENT_LOCK) } returns V3ToggleSliderValue(10, true)
        source.extraWidgets = listOf(
            V3SpecialSettingsWidget.Slider(V3SpecialSettingsWidgetInfo(P_KEY_SPEED_SETTINGS, "Speed", 1), 0, 100, 1f),
            V3SpecialSettingsWidget.ToggleSlider(V3SpecialSettingsWidgetInfo(P_KEY_EMG_MOVEMENT_LOCK, "Lock", 2), 10, 100, 0.1f, "s"),
            V3SpecialSettingsWidget.Spinner(V3SpecialSettingsWidgetInfo(P_KEY_HAND_CONTROL_MODE, "Mode", 3), listOf("First", "Second"), 0),
        )
    }

    private fun editAction(control: String): V3SpecialSettingsAction = when (control) {
        "slider-drag" -> V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 70))
        "slider-commit" -> V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 70))
        "slider-step" -> V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderStepClicked(P_KEY_SPEED_SETTINGS, 1))
        "toggle-drag" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderValueChanged(P_KEY_EMG_MOVEMENT_LOCK, 20))
        "toggle-commit" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderChangeCommitted(P_KEY_EMG_MOVEMENT_LOCK, 20))
        "toggle-step" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderStepClicked(P_KEY_EMG_MOVEMENT_LOCK, 1))
        "toggle-enabled" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_EMG_MOVEMENT_LOCK, false))
        else -> V3SpecialSettingsAction.SpinnerAction(V3SpinnerAction.SpinnerValueSelected(P_KEY_HAND_CONTROL_MODE, 1))
    }

    @ParameterizedTest
    @CsvSource("false,slider-drag", "false,slider-commit", "false,slider-step", "false,toggle-drag", "false,toggle-commit", "false,toggle-step", "false,toggle-enabled", "false,spinner",
        "true,slider-drag", "true,slider-commit", "true,slider-step", "true,toggle-drag", "true,toggle-commit", "true,toggle-step", "true,toggle-enabled", "true,spinner")
    fun `confirmation copies baseline selects personal profile and applies the requested edit only once`(full: Boolean, control: String) = runTest(dispatcher) {
        prepareBaseEdit(full)
        attach(); runCurrent()
        viewModel.onAction(editAction(control))
        val prompt = requireNotNull(viewModel.uiState.value.baseSettingsEditPrompt)
        assertEquals(if (full) V3SettingsProfile(1, "Personal") else null, prompt.replacementProfile)
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsCopyConfirmed(prompt.requestId))
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsCopyConfirmed(prompt.requestId))
        runCurrent()
        advanceTimeBy(301); runCurrent()
        assertEquals(listOf("first" to if (full) 1 else null), repository.copies)
        assertEquals(if (full) 1 else 2, state().activeProfileId)
        assertNull(viewModel.uiState.value.baseSettingsEditPrompt)
        when {
            control.startsWith("slider") -> {
                expectedSliderWrites = 1
                verify(exactly = 1) { sliders.setSliderValue(P_KEY_SPEED_SETTINGS, if (control == "slider-step") 31 else 70) }
            }
            control.startsWith("toggle") -> {
                expectedToggleSaves = 1
                expectedToggleSends = 1
                val value = when (control) {
                    "toggle-enabled" -> V3ToggleSliderValue(10, false)
                    "toggle-step" -> V3ToggleSliderValue(11, true)
                    else -> V3ToggleSliderValue(20, true)
                }
                verify(exactly = 1) { toggles.saveToggleSliderValue(P_KEY_EMG_MOVEMENT_LOCK, value) }
            }
            else -> {
                expectedSpinnerWrites = 1
                verify(exactly = 1) { spinners.setSpinnerValue(P_KEY_HAND_CONTROL_MODE, 1) }
            }
        }
    }

    @Test fun `old dialog callbacks cannot confirm or dismiss a newer base edit`() = runTest(dispatcher) {
        prepareBaseEdit()
        attach(); runCurrent()
        viewModel.onAction(editAction("slider-commit"))
        val old = requireNotNull(viewModel.uiState.value.baseSettingsEditPrompt)
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsReadOnlyDismissed(old.requestId))
        viewModel.onAction(editAction("slider-step"))
        val current = requireNotNull(viewModel.uiState.value.baseSettingsEditPrompt)
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsCopyConfirmed(old.requestId))
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsReadOnlyDismissed(old.requestId))
        runCurrent()
        assertEquals(current, viewModel.uiState.value.baseSettingsEditPrompt)
        assertTrue(repository.copies.isEmpty())
        assertEquals(0, state().activeProfileId)
    }

    @Test fun `filling profile slots while dialog is open never overwrites without replacement consent`() = runTest(dispatcher) {
        prepareBaseEdit()
        attach(); runCurrent()
        viewModel.onAction(editAction("slider-commit"))
        val prompt = requireNotNull(viewModel.uiState.value.baseSettingsEditPrompt)
        repository.snapshot = repository.snapshot.copy(profiles = (0..3).map { V3SettingsProfile(it, null) })
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsCopyConfirmed(prompt.requestId))
        runCurrent()
        assertEquals(0, state().activeProfileId)
        assertEquals(V3SettingsProfileOperation.COPY_BASE, state().failedOperation)
        assertEquals(listOf("first" to null), repository.copies)
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "device", "locked", "application", "cleared"])
    fun `late copy completion cannot apply a pending edit after the screen is invalidated`(reason: String) = runTest(dispatcher) {
        prepareBaseEdit()
        val finish = CompletableDeferred<Unit>()
        val originalCopy = repository.copier
        repository.copier = { serial, replacement ->
            withContext(NonCancellable) { finish.await(); originalCopy(serial, replacement) }
        }
        attach(); runCurrent()
        viewModel.onAction(editAction("slider-commit"))
        val prompt = requireNotNull(viewModel.uiState.value.baseSettingsEditPrompt)
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsCopyConfirmed(prompt.requestId))
        runCurrent()
        when (reason) {
            "detached" -> detach()
            "device" -> { repository.serial = "second"; source.address = "second-device"; source.updates.emit(Unit) }
            "locked" -> interaction.value = false
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "cleared" -> store.clear()
        }
        runCurrent(); finish.complete(Unit); runCurrent()
        assertFalse(viewModel.uiState.value.isBaseSettingsReadOnlyDialogVisible)
        assertEquals(1, repository.copies.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["slider-drag", "slider-commit", "slider-step", "toggle-drag", "toggle-commit", "toggle-step", "toggle-enabled", "spinner"])
    fun `user attempting a base edit sees a dialog without changing drafts selection or sending values`(control: String) = runTest(dispatcher) {
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(0, null), V3SettingsProfile(1, "Personal")), 0)
        every { sliders.getSliderValue(P_KEY_SPEED_SETTINGS) } returns 30
        every { toggles.getToggleSliderValue(P_KEY_EMG_MOVEMENT_LOCK) } returns V3ToggleSliderValue(10, true)
        source.extraWidgets = listOf(
            V3SpecialSettingsWidget.Slider(V3SpecialSettingsWidgetInfo(P_KEY_SPEED_SETTINGS, "Speed", 1), 0, 100, 1f),
            V3SpecialSettingsWidget.ToggleSlider(V3SpecialSettingsWidgetInfo(P_KEY_EMG_MOVEMENT_LOCK, "Lock", 2), 10, 100, 0.1f, "s"),
            V3SpecialSettingsWidget.Spinner(V3SpecialSettingsWidgetInfo(P_KEY_HAND_CONTROL_MODE, "Mode", 3), listOf("First", "Second"), 0),
        )
        attach(); runCurrent()
        val beforeSlider = viewModel.uiState.value.sliders.getValue(P_KEY_SPEED_SETTINGS).value
        val beforeToggle = viewModel.uiState.value.toggleSliders.getValue(P_KEY_EMG_MOVEMENT_LOCK).value
        val beforeSpinner = viewModel.uiState.value.spinners.getValue(P_KEY_HAND_CONTROL_MODE).selectedIndex
        val action = when (control) {
            "slider-drag" -> V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 70))
            "slider-commit" -> V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 70))
            "slider-step" -> V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderStepClicked(P_KEY_SPEED_SETTINGS, 1))
            "toggle-drag" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderValueChanged(P_KEY_EMG_MOVEMENT_LOCK, 20))
            "toggle-commit" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderChangeCommitted(P_KEY_EMG_MOVEMENT_LOCK, 20))
            "toggle-step" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderStepClicked(P_KEY_EMG_MOVEMENT_LOCK, 1))
            "toggle-enabled" -> V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_EMG_MOVEMENT_LOCK, false))
            else -> V3SpecialSettingsAction.SpinnerAction(V3SpinnerAction.SpinnerValueSelected(P_KEY_HAND_CONTROL_MODE, 1))
        }
        viewModel.onAction(action)
        assertTrue(viewModel.uiState.value.isBaseSettingsReadOnlyDialogVisible)
        advanceTimeBy(301); runCurrent()
        assertEquals(0, state().activeProfileId)
        assertEquals(listOf(0, 1), state().profiles.map { it.profileId })
        assertTrue(repository.creations.isEmpty())
        assertTrue(repository.selections.isEmpty())
        assertEquals(beforeSlider, viewModel.uiState.value.sliders.getValue(P_KEY_SPEED_SETTINGS).value)
        assertEquals(beforeToggle, viewModel.uiState.value.toggleSliders.getValue(P_KEY_EMG_MOVEMENT_LOCK).value)
        assertEquals(beforeSpinner, viewModel.uiState.value.spinners.getValue(P_KEY_HAND_CONTROL_MODE).selectedIndex)
        viewModel.onAction(V3SpecialSettingsAction.BaseSettingsReadOnlyDismissed(requireNotNull(viewModel.uiState.value.baseSettingsEditPrompt).requestId))
        assertFalse(viewModel.uiState.value.isBaseSettingsReadOnlyDialogVisible)
        assertTrue(viewModel.uiState.value.sliders.getValue(P_KEY_SPEED_SETTINGS).isEnabled)
        viewModel.onAction(action)
        assertTrue(viewModel.uiState.value.isBaseSettingsReadOnlyDialogVisible)
        detach()
        assertFalse(viewModel.uiState.value.isBaseSettingsReadOnlyDialogVisible)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `engineer baseline and user personal profile remain editable`(engineer: Boolean) = runTest(dispatcher) {
        roles.selected = if (engineer) V3DeviceRole.SERVICE_ENGINEER else V3DeviceRole.USER
        val activeId = if (engineer) 0 else 1
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(0, null), V3SettingsProfile(1, "Personal")), activeId)
        source.extraWidgets = listOf(V3SpecialSettingsWidget.Slider(
            V3SpecialSettingsWidgetInfo(P_KEY_SPEED_SETTINGS, "Speed", 1), 0, 100, 1f,
        ))
        attach(); runCurrent()
        viewModel.onAction(V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 70)))
        expectedSliderWrites = 1
        verify(exactly = 1) { sliders.setSliderValue(P_KEY_SPEED_SETTINGS, 70) }
        assertFalse(viewModel.uiState.value.isBaseSettingsReadOnlyDialogVisible)
        assertEquals(activeId, state().activeProfileId)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3])
    fun `selects by ID once including a repeated choice of the active profile`(id: Int) = runTest(dispatcher) {
        attach(); runCurrent()
        select(id)
        assertEquals(V3SettingsProfileOperation.SELECT, state().operation)
        assertFalse(state().isEnabled)
        runCurrent()
        assertEquals(listOf("first" to id), repository.selections)
        assertEquals(id, state().activeProfileId)
        assertNull(state().operation)
        assertTrue(state().isEnabled)
        repository.updates.emit(Unit); attach(); runCurrent()
        assertEquals(1, repository.selections.size)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `profile operation cancels screen timers and rejects edits or another operation while busy`(creating: Boolean) = runTest(dispatcher) {
        source.extraWidgets = listOf(
            V3SpecialSettingsWidget.Slider(V3SpecialSettingsWidgetInfo(P_KEY_SPEED_SETTINGS, "Speed", 1), 0, 100, 1f),
            V3SpecialSettingsWidget.ToggleSlider(V3SpecialSettingsWidgetInfo(P_KEY_EMG_MOVEMENT_LOCK, "Lock", 2), 10, 100, 0.1f, "s"),
        )
        val finished = CompletableDeferred<Unit>()
        repository.selector = { _, id -> finished.await(); repository.snapshot = repository.snapshot.copy(activeProfileId = id) }
        repository.creator = { finished.await(); repository.snapshot = V3SettingsProfiles(repository.snapshot.profiles + V3SettingsProfile(2, null), 2) }
        attach(); runCurrent()
        viewModel.onAction(V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderStepClicked(P_KEY_SPEED_SETTINGS, 1)))
        viewModel.onAction(V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_EMG_MOVEMENT_LOCK, true)))
        expectedToggleSaves = 1
        if (creating) create() else select(1)
        runCurrent()
        assertFalse(viewModel.uiState.value.sliders.getValue(P_KEY_SPEED_SETTINGS).isEnabled)
        assertFalse(viewModel.uiState.value.toggleSliders.getValue(P_KEY_EMG_MOVEMENT_LOCK).isInteractionEnabled)
        viewModel.onAction(V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 80)))
        viewModel.onAction(V3SpecialSettingsAction.ToggleSliderAction(V3ToggleSliderAction.ToggleSliderEnabledChanged(P_KEY_EMG_MOVEMENT_LOCK, false)))
        select(3); create()
        repository.updates.emit(Unit)
        advanceTimeBy(301); runCurrent()
        assertEquals(if (creating) V3SettingsProfileOperation.CREATE else V3SettingsProfileOperation.SELECT, state().operation)
        assertEquals(if (creating) emptyList() else listOf("first" to 1), repository.selections)
        assertEquals(if (creating) listOf("first") else emptyList(), repository.creations)
        finished.complete(Unit); runCurrent()
        assertEquals(if (creating) 2 else 1, state().activeProfileId)
        assertTrue(viewModel.uiState.value.sliders.getValue(P_KEY_SPEED_SETTINGS).isEnabled)
        assertTrue(viewModel.uiState.value.toggleSliders.getValue(P_KEY_EMG_MOVEMENT_LOCK).isInteractionEnabled)
        advanceTimeBy(301); runCurrent()
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "locked", "application", "removed", "invalid"])
    fun `unavailable or unknown profile selection is ignored`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent()
        when (reason) {
            "detached" -> detach()
            "locked" -> interaction.value = false
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "removed" -> { source.visible = false; source.updates.emit(Unit); runCurrent() }
        }
        select(if (reason == "invalid") 2 else 1); runCurrent()
        assertTrue(repository.selections.isEmpty())
    }

    @Test
    fun `stopping while selection waits cancels it and does not retry on return`() = runTest(dispatcher) {
        val finished = CompletableDeferred<Unit>()
        var applied = false
        repository.selector = { _, _ -> finished.await(); applied = true }
        attach(); runCurrent()
        select(1); runCurrent()
        detach()
        finished.complete(Unit); runCurrent()
        assertFalse(applied)
        assertNull(state().operation)
        attach(); runCurrent()
        assertEquals(1, repository.selections.size)
        assertEquals(3, state().activeProfileId)
        assertTrue(state().isEnabled)
    }

    @Test
    fun `failed selection reloads actual profile state and allows explicit retry`() = runTest(dispatcher) {
        repository.selector = { _, id -> repository.snapshot = repository.snapshot.copy(activeProfileId = id); error("Apply failed") }
        attach(); runCurrent()
        select(1); runCurrent()
        assertEquals(1, state().activeProfileId)
        assertEquals(V3SettingsProfileOperation.SELECT, state().failedOperation)
        assertNull(state().operation)
        repository.selector = { _, id -> repository.snapshot = repository.snapshot.copy(activeProfileId = id) }
        select(3); runCurrent()
        assertNull(state().failedOperation)
        assertEquals(3, state().activeProfileId)
    }

    @Test
    fun `initial read sorts IDs and keeps a noncontiguous active ID distinct from its index`() = runTest(dispatcher) {
        runCurrent()
        assertTrue(repository.reads.isEmpty())
        attach()
        assertTrue(state().isLoading)
        assertFalse(state().isEnabled)
        runCurrent()
        assertEquals(listOf(1, 3), state().profiles.map { it.profileId })
        assertEquals(listOf(null, "Sport"), state().profiles.map { it.customName })
        assertEquals(3, state().activeProfileId)
        assertTrue(state().canCreate)
        assertTrue(state().isEnabled)
        assertEquals(1, repository.reads.size)
        assertEquals(3, repository.cached.single().second.activeProfileId)
        repeat(3) { source.updates.emit(Unit); runCurrent() }
        assertEquals(1, repository.reads.size)
    }

    @Test
    fun `operation or import invalidation refreshes names and active ID without applying settings`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(1, "Work"), V3SettingsProfile(3, "Home")), 1)
        repository.updates.emit(Unit); runCurrent()
        assertEquals("Work", state().profiles.first().customName)
        assertEquals(1, state().activeProfileId)
        assertEquals(2, repository.reads.size)
        interaction.value = false; runCurrent()
        assertFalse(state().isEnabled)
        interaction.value = true; runCurrent()
        assertTrue(state().isEnabled)
        assertEquals(2, repository.reads.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["standard-v3", "not-v3", "application", "removed"])
    fun `absent selector never reads profiles`(reason: String) = runTest(dispatcher) {
        when (reason) {
            "standard-v3" -> source.profile = V3DeviceProfile.STANDARD_V3
            "not-v3" -> source.profile = V3DeviceProfile.NOT_V3
            "application" -> appSettings.settingsSection = V3SpecialSettingsSection.APPLICATION
            "removed" -> source.visible = false
        }
        attach(); runCurrent()
        repository.updates.emit(Unit); runCurrent()
        assertNull(viewModel.uiState.value.settingsProfiles)
        assertTrue(repository.reads.isEmpty())
        assertTrue(repository.cached.isEmpty())
    }

    @Test
    fun `returning to the screen reads the latest profiles without relying on an event`() = runTest(dispatcher) {
        attach(); runCurrent()
        detach()
        assertFalse(state().isEnabled)
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(2, "New")), 2)
        attach(); runCurrent()
        assertEquals(2, state().activeProfileId)
        section(V3SpecialSettingsSection.APPLICATION)
        assertNull(viewModel.uiState.value.settingsProfiles)
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(1, "Latest")), 1)
        section(V3SpecialSettingsSection.PROSTHESIS); runCurrent()
        assertEquals("Latest", state().profiles.single().customName)
    }

    @Test
    fun `late result from the previous device cannot replace or cache the current profiles`() = runTest(dispatcher) {
        val oldResult = CompletableDeferred<V3SettingsProfiles>()
        repository.reader = { serial ->
            if (serial == "first") withContext(NonCancellable) { oldResult.await() }
            else V3SettingsProfiles(listOf(V3SettingsProfile(2, "Second device")), 2)
        }
        attach(); runCurrent()
        repository.serial = "second"
        source.address = "second-device"
        source.updates.emit(Unit); runCurrent()
        assertEquals("Second device", state().profiles.single().customName)
        oldResult.complete(repository.snapshot); runCurrent()
        assertEquals(2, state().activeProfileId)
        assertEquals(listOf("second"), repository.cached.map { it.first })
    }

    @Test
    fun `superseded read on the same device cannot overwrite a newer import`() = runTest(dispatcher) {
        val oldResult = CompletableDeferred<V3SettingsProfiles>()
        repository.reader = { withContext(NonCancellable) { oldResult.await() } }
        attach(); runCurrent()
        repository.reader = { V3SettingsProfiles(listOf(V3SettingsProfile(1, "Imported")), 1) }
        repository.updates.emit(Unit); runCurrent()
        oldResult.complete(repository.snapshot); runCurrent()
        assertEquals("Imported", state().profiles.single().customName)
        assertEquals(1, repository.cached.size)
    }

    @Test
    fun `detaching discards an unfinished read and leaves no false loading state`() = runTest(dispatcher) {
        val result = CompletableDeferred<V3SettingsProfiles>()
        repository.reader = { withContext(NonCancellable) { result.await() } }
        attach(); runCurrent()
        detach()
        result.complete(repository.snapshot); runCurrent()
        assertFalse(state().isLoading)
        assertFalse(state().isEnabled)
        assertTrue(repository.cached.isEmpty())
    }

    @Test
    fun `a read failure is contained and a subsequent attachment retries`() = runTest(dispatcher) {
        repository.reader = { error("Database unavailable") }
        attach(); runCurrent()
        assertTrue(state().loadFailed)
        assertFalse(state().isLoading)
        assertFalse(state().isEnabled)
        repository.reader = { repository.snapshot }
        detach(); attach(); runCurrent()
        assertFalse(state().loadFailed)
        assertTrue(state().isEnabled)
    }

    @Test
    fun `clearing the ViewModel prevents a noncancellable read from updating the local cache`() = runTest(dispatcher) {
        val result = CompletableDeferred<V3SettingsProfiles>()
        repository.reader = { withContext(NonCancellable) { result.await() } }
        attach(); runCurrent()
        store.clear()
        result.complete(repository.snapshot); runCurrent()
        assertTrue(repository.cached.isEmpty())
    }

    @Test
    fun `domain preserves default profile fallback and the three profile limit`() = runTest(dispatcher) {
        val getProfiles = GetSettingsProfilesUseCaseV3(repository)
        repository.snapshot = V3SettingsProfiles(emptyList(), 99)
        assertEquals(V3SettingsProfiles(listOf(V3SettingsProfile(1, null)), 1), getProfiles("first"))
        repository.snapshot = V3SettingsProfiles((4 downTo 1).map { V3SettingsProfile(it, null) }, 4)
        val result = getProfiles("first")
        assertEquals(listOf(1, 2, 3), result.profiles.map { it.profileId })
        assertEquals(1, result.activeProfileId)
        assertFalse(result.canCreate)
        assertTrue(repository.cached.isEmpty())
    }

    @Test
    fun `plus creates and selects the third profile once then disappears from state`() = runTest(dispatcher) {
        attach(); runCurrent()
        create(); create()
        assertEquals(V3SettingsProfileOperation.CREATE, state().operation)
        assertFalse(state().isEnabled)
        runCurrent()
        assertEquals(listOf("first"), repository.creations)
        assertEquals(listOf(1, 2, 3), state().profiles.map { it.profileId })
        assertEquals(2, state().activeProfileId)
        assertFalse(state().canCreate)
        assertNull(state().operation)
        assertTrue(state().isEnabled)
        create(); repository.updates.emit(Unit); attach(); runCurrent()
        assertEquals(1, repository.creations.size)
        assertTrue(repository.selections.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "locked", "application", "removed", "loading", "limit", "standard-v3"])
    fun `plus is ignored when creation is unavailable`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent()
        when (reason) {
            "detached" -> detach()
            "locked" -> interaction.value = false
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "removed" -> { source.visible = false; source.updates.emit(Unit); runCurrent() }
            "loading" -> attach()
            "limit" -> {
                repository.snapshot = V3SettingsProfiles((1..3).map { V3SettingsProfile(it, null) }, 1)
                repository.updates.emit(Unit); runCurrent()
            }
            "standard-v3" -> { source.profile = V3DeviceProfile.STANDARD_V3; source.updates.emit(Unit); runCurrent() }
        }
        create(); runCurrent()
        assertTrue(repository.creations.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "locked", "application", "removed", "device", "cleared"])
    fun `invalidating the screen cancels creation without automatic retry`(reason: String) = runTest(dispatcher) {
        val finish = CompletableDeferred<Unit>()
        var applied = false
        repository.creator = { finish.await(); applied = true }
        attach(); runCurrent(); create(); runCurrent()
        when (reason) {
            "detached" -> detach()
            "locked" -> interaction.value = false
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "removed" -> { source.visible = false; source.updates.emit(Unit) }
            "device" -> { repository.serial = "second"; source.address = "second-device"; source.updates.emit(Unit) }
            "cleared" -> store.clear()
        }
        runCurrent(); finish.complete(Unit); runCurrent()
        assertFalse(applied)
        if (reason != "cleared") { detach(); attach(); runCurrent() }
        assertEquals(listOf("first"), repository.creations)
    }

    @Test
    fun `creation failure keeps actual database state and never creates again on refresh`() = runTest(dispatcher) {
        repository.creator = {
            repository.snapshot = V3SettingsProfiles(repository.snapshot.profiles + V3SettingsProfile(2, null), 2)
            error("Applying values failed")
        }
        attach(); runCurrent(); create(); runCurrent()
        assertEquals(V3SettingsProfileOperation.CREATE, state().failedOperation)
        assertEquals(2, state().activeProfileId)
        assertFalse(state().canCreate)
        assertNull(state().operation)
        assertTrue(state().isEnabled)
        repository.updates.emit(Unit); detach(); attach(); runCurrent()
        assertEquals(1, repository.creations.size)
        select(1); runCurrent()
        assertNull(state().failedOperation)
    }

    @Test
    fun `rename opens from screen state and saves by ID once without selecting a profile`() = runTest(dispatcher) {
        attach(); runCurrent(); editName(1)
        val editor = requireNotNull(state().nameEditor)
        assertEquals(V3SettingsProfile(1, null), editor.profile)
        editName(1)
        assertEquals(editor, state().nameEditor)
        submitName(editor.requestId, "  Everyday  ")
        submitName(editor.requestId, "Duplicate")
        assertNull(state().nameEditor)
        assertEquals(V3SettingsProfileOperation.RENAME, state().operation)
        assertFalse(state().isEnabled)
        runCurrent()
        assertEquals(listOf(Triple("first", 1, "Everyday")), repository.renames)
        assertEquals("Everyday", state().profiles.first().customName)
        assertEquals(3, state().activeProfileId)
        assertNull(state().operation)
        assertTrue(state().isEnabled)
        repository.updates.emit(Unit); detach(); attach(); runCurrent()
        assertEquals(1, repository.renames.size)
        assertTrue(repository.selections.isEmpty())
        assertTrue(repository.creations.isEmpty())
    }

    @Test fun `baseline can be selected but never opens the name editor`() = runTest(dispatcher) {
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(0, null), V3SettingsProfile(1, null)), 0)
        attach(); runCurrent()
        editName(0)
        assertNull(state().nameEditor)
        assertTrue(repository.renames.isEmpty())
        assertTrue(state().canCreate)
        select(1); runCurrent()
        select(0); runCurrent()
        assertEquals(0, state().activeProfileId)
    }

    @Test fun `profile deletion requires confirmation and baseline cannot be deleted`() = runTest(dispatcher) {
        repository.snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(0, null), V3SettingsProfile(1, "Personal")), 1)
        attach(); runCurrent()
        viewModel.onAction(V3SpecialSettingsAction.SettingsProfileDeleteRequested(0))
        assertNull(state().deleteConfirmation)
        viewModel.onAction(V3SpecialSettingsAction.SettingsProfileDeleteRequested(1))
        assertEquals(1, state().deleteConfirmation?.profileId)
        assertTrue(repository.deletions.isEmpty())
        viewModel.onAction(V3SpecialSettingsAction.SettingsProfileDeleteDismissed(1))
        assertNull(state().deleteConfirmation)
        assertTrue(repository.deletions.isEmpty())
        viewModel.onAction(V3SpecialSettingsAction.SettingsProfileDeleteRequested(1))
        viewModel.onAction(V3SpecialSettingsAction.SettingsProfileDeleteConfirmed(1))
        viewModel.onAction(V3SpecialSettingsAction.SettingsProfileDeleteConfirmed(1))
        runCurrent()
        assertEquals(listOf(1), repository.deletions)
        assertEquals(0, state().activeProfileId)
        assertEquals(listOf(0), state().profiles.map { it.profileId })
        assertNull(state().deleteConfirmation)
        assertTrue(state().canCreate)
    }

    @Test
    fun `callbacks from a closed editor cannot save or close a newly opened editor for the same ID`() = runTest(dispatcher) {
        attach(); runCurrent(); editName(3)
        val old = requireNotNull(state().nameEditor)
        dismissName(old.requestId)
        assertNull(state().nameEditor)
        editName(3)
        val current = requireNotNull(state().nameEditor)
        assertNotEquals(old.requestId, current.requestId)
        submitName(old.requestId, "Wrong"); dismissName(old.requestId)
        assertEquals(current, state().nameEditor)
        submitName(current.requestId, "Correct"); runCurrent()
        assertEquals(listOf(Triple("first", 3, "Correct")), repository.renames)
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "locked", "application", "removed", "device", "serial", "reload"])
    fun `editor closes when its screen or profile data is invalidated`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent(); editName(3)
        val editor = requireNotNull(state().nameEditor)
        when (reason) {
            "detached" -> detach()
            "locked" -> interaction.value = false
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "removed" -> { source.visible = false; source.updates.emit(Unit) }
            "device" -> { source.address = "second-device"; source.updates.emit(Unit) }
            "serial" -> { repository.serial = "second"; repository.updates.emit(Unit) }
            "reload" -> repository.updates.emit(Unit)
        }
        runCurrent()
        assertNull(viewModel.uiState.value.settingsProfiles?.nameEditor)
        submitName(editor.requestId, "Stale"); runCurrent()
        assertTrue(repository.renames.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["detached", "locked", "application", "invalid", "busy"])
    fun `rename cannot open when unavailable or profile does not exist`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent()
        when (reason) {
            "detached" -> detach()
            "locked" -> interaction.value = false
            "application" -> section(V3SpecialSettingsSection.APPLICATION)
            "busy" -> select(1)
        }
        editName(if (reason == "invalid") 2 else 3)
        assertNull(viewModel.uiState.value.settingsProfiles?.nameEditor)
        runCurrent()
        assertTrue(repository.renames.isEmpty())
    }

    @Test
    fun `pending rename rejects profile operations but does not cancel a scheduled parameter edit`() = runTest(dispatcher) {
        source.extraWidgets = listOf(V3SpecialSettingsWidget.Slider(
            V3SpecialSettingsWidgetInfo(P_KEY_SPEED_SETTINGS, "Speed", 1), 0, 100, 1f,
        ))
        val finish = CompletableDeferred<Unit>()
        repository.renamer = { _, _, _ -> finish.await() }
        attach(); runCurrent()
        viewModel.onAction(V3SpecialSettingsAction.SliderAction(V3SliderAction.SliderStepClicked(P_KEY_SPEED_SETTINGS, 1)))
        editName(3); submitName(requireNotNull(state().nameEditor).requestId, "New name")
        runCurrent()
        select(1); create(); editName(1)
        assertTrue(viewModel.uiState.value.sliders.getValue(P_KEY_SPEED_SETTINGS).isEnabled)
        advanceTimeBy(301); runCurrent()
        expectedSliderWrites = 1
        verify(exactly = 1) { sliders.setSliderValue(P_KEY_SPEED_SETTINGS, 1) }
        assertTrue(repository.selections.isEmpty())
        assertTrue(repository.creations.isEmpty())
        assertNull(state().nameEditor)
        finish.complete(Unit); runCurrent()
    }

    @Test
    fun `stopping cancels a pending rename and return never retries it`() = runTest(dispatcher) {
        val finish = CompletableDeferred<Unit>()
        var saved = false
        repository.renamer = { _, _, _ -> finish.await(); saved = true }
        attach(); runCurrent(); editName(3)
        submitName(requireNotNull(state().nameEditor).requestId, "New name"); runCurrent()
        detach(); finish.complete(Unit); runCurrent(); attach(); runCurrent()
        assertFalse(saved)
        assertNull(state().nameEditor)
        assertNull(state().operation)
        assertEquals(1, repository.renames.size)
    }

    @Test
    fun `rename failure reloads the stored name and allows an explicit retry`() = runTest(dispatcher) {
        repository.renamer = { _, _, _ -> error("Database unavailable") }
        attach(); runCurrent(); editName(3)
        submitName(requireNotNull(state().nameEditor).requestId, "New"); runCurrent()
        assertEquals(V3SettingsProfileOperation.RENAME, state().failedOperation)
        assertEquals("Sport", state().profiles.last().customName)
        assertNull(state().operation)
        assertTrue(state().isEnabled)
        repository.renamer = { _, id, name -> repository.snapshot = repository.snapshot.copy(
            profiles = repository.snapshot.profiles.map { if (it.profileId == id) it.copy(customName = name) else it },
        ) }
        editName(3); submitName(requireNotNull(state().nameEditor).requestId, "New"); runCurrent()
        assertNull(state().failedOperation)
        assertEquals("New", state().profiles.last().customName)
        assertEquals(2, repository.renames.size)
    }

    private class ProfilesRepository : V3SettingsProfilesRepository {
        val copies = mutableListOf<Pair<String, Int?>>()
        var copier: suspend (String, Int?) -> Unit = { _, replacement ->
            check(snapshot.activeProfileId == 0)
            val freeId = (1..3).firstOrNull { id -> snapshot.profiles.none { it.profileId == id } }
            check((replacement == null) == (freeId != null))
            val id = replacement ?: requireNotNull(freeId)
            snapshot = snapshot.copy(
                profiles = if (replacement == null) snapshot.profiles + V3SettingsProfile(id, null) else snapshot.profiles,
                activeProfileId = id,
            )
        }
        override suspend fun copyBaseToPersonalProfile(serial: String, replacementProfileId: Int?) {
            copies += serial to replacementProfileId
            copier(serial, replacementProfileId)
        }
        val deletions = mutableListOf<Int>()
        override suspend fun deleteProfile(serial: String, profileId: Int) {
            deletions += profileId
            snapshot = snapshot.copy(profiles = snapshot.profiles.filter { it.profileId != profileId },
                activeProfileId = if (snapshot.activeProfileId == profileId) 0 else snapshot.activeProfileId)
        }
        override val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var serial = "first"
        var snapshot = V3SettingsProfiles(listOf(V3SettingsProfile(3, "Sport"), V3SettingsProfile(1, null)), 3)
        var reader: suspend (String) -> V3SettingsProfiles = { snapshot }
        val reads = mutableListOf<String>()
        val cached = mutableListOf<Pair<String, V3SettingsProfiles>>()
        var selector: suspend (String, Int) -> Unit = { _, id -> snapshot = snapshot.copy(activeProfileId = id) }
        val selections = mutableListOf<Pair<String, Int>>()
        val creations = mutableListOf<String>()
        val renames = mutableListOf<Triple<String, Int, String>>()
        var renamer: suspend (String, Int, String) -> Unit = { _, id, name ->
            snapshot = snapshot.copy(profiles = snapshot.profiles.map { if (it.profileId == id) it.copy(customName = name) else it })
        }
        override suspend fun renameProfile(serial: String, profileId: Int, name: String) {
            renames.add(Triple(serial, profileId, name))
            renamer(serial, profileId, name)
        }
        var creator: suspend (String) -> Unit = {
            val id = (1..3).first { id -> snapshot.profiles.none { it.profileId == id } }
            snapshot = V3SettingsProfiles(snapshot.profiles + V3SettingsProfile(id, null), id)
        }
        override suspend fun createProfile(serial: String) {
            creations.add(serial)
            creator(serial)
        }
        override fun currentSerial() = serial
        override suspend fun getProfiles(serial: String): V3SettingsProfiles {
            reads.add(serial)
            return reader(serial)
        }
        override fun cacheSelection(serial: String, profiles: V3SettingsProfiles) { cached.add(serial to profiles) }
        override suspend fun selectProfile(serial: String, profileId: Int) {
            selections.add(serial to profileId)
            selector(serial, profileId)
        }
    }

    private class WidgetsSource : V3SpecialSettingsWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot(V3SpecialSettingsSection.PROSTHESIS).let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile, section: V3SpecialSettingsSection) = snapshot(section).widgets
        override val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var profile = V3DeviceProfile.INDY3
        var address = "first-device"
        var visible = true
        var extraWidgets = emptyList<V3SpecialSettingsWidget>()
        fun snapshot(section: V3SpecialSettingsSection) = V3SpecialSettingsWidgetsSnapshot(
            profile, address,
            if (visible && profile != V3DeviceProfile.STANDARD_V3 && section == V3SpecialSettingsSection.PROSTHESIS) listOf(
                V3SpecialSettingsWidget.SettingsProfile(V3SpecialSettingsWidgetInfo(P_KEY_SETTINGS_PROFILE, "Профили настроек", 5), emptyList(), 0),
            ) + extraWidgets else emptyList(),
        )
    }
}
