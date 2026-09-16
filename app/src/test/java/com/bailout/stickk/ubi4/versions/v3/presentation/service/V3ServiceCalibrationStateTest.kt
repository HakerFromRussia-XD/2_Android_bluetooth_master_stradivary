package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.di.V3ServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_START_CALIBRATE_COMMAND
import com.bailout.stickk.ubi4.versions.v3.domain.settings.*
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceCalibrationStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val source = Source()
    private val repository = FakeV3ProsthesisCalibrationRepository()
    private lateinit var vm: V3ServiceViewModel
    private fun attach() = vm.onAction(V3ServiceAction.ViewAttached)
    private fun press(id: Long = 1) = vm.onAction(V3ServiceAction.CalibrationButtonPressed(id))
    private fun release(id: Long = 1) = vm.onAction(V3ServiceAction.CalibrationButtonReleased(id))
    private fun state() = requireNotNull(vm.uiState.value.calibration)

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        val sliders = mockk<V3DeviceSettingsRepository> {
            every { sliderInteractionEnabled } returns MutableStateFlow(true)
            every { getSliderValue(any()) } returns null
            every { observeSliderValue(any()) } returns flowOf(null)
        }
        val spinners = mockk<V3SpinnerSettingsRepository> {
            every { spinnerInteractionEnabled } returns MutableStateFlow(true)
            every { observeSpinnerValue(any()) } returns flowOf(null)
        }
        vm = V3ServiceViewModelFactory(sliders, source, spinners, FakeV3DeviceRoleRepository(),
            FakeV3DeviceInfoRepository(), repository, sessionRepository = source).create(V3ServiceViewModel::class.java)
        store.put("service", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles render silently and preserve one start then release per accepted press`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        assertFalse(state().isEnabled)
        press(); release()
        attach(); runCurrent()
        repeat(3) { attach(); source.updates.emit(Unit); runCurrent() }
        assertTrue(state().isEnabled)
        assertFalse(state().isPressed)
        assertTrue(repository.commands.isEmpty())
        press(); press(); press(2)
        assertTrue(state().isPressed)
        source.updates.emit(Unit); runCurrent()
        release(2)
        assertTrue(state().isPressed)
        release(); release()
        assertFalse(state().isPressed)
        assertEquals(listOf("start:first-device", "release:first-device"), repository.commands)
        press(3); release(1)
        assertTrue(state().isPressed)
        release(3)
        assertEquals(4, repository.commands.size)
    }

    @Test fun `fresh lock rejects a start before collection and releases an accepted press only once`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.interactionEnabled.value = false
        press(); runCurrent()
        assertFalse(state().isEnabled)
        assertTrue(repository.commands.isEmpty())
        repository.interactionEnabled.value = true; runCurrent(); press(2)
        repository.interactionEnabled.value = false
        release(2); runCurrent(); release(2)
        assertEquals(listOf("start:first-device", "release:first-device"), repository.commands)
        repository.interactionEnabled.value = true; runCurrent(); press(3)
        repository.interactionEnabled.value = false; runCurrent()
        assertFalse(state().isPressed)
        release(3); press(4)
        assertEquals(4, repository.commands.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["detach", "destroy", "cleared", "removed", "profile", "not-v3"])
    fun `losing screen context releases its press and stale release is ignored`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent(); press()
        when (reason) {
            "detach" -> vm.onAction(V3ServiceAction.ViewDetached)
            "destroy" -> vm.onAction(V3ServiceAction.ViewDestroyed)
            "cleared" -> store.clear()
            "removed" -> source.visible = false
            "profile" -> source.profile = V3DeviceProfile.INDY3
            "not-v3" -> source.profile = V3DeviceProfile.NOT_V3
        }
        source.updates.emit(Unit); runCurrent(); release()
        assertEquals(listOf("start:first-device", "release:first-device"), repository.commands)
        if (reason == "cleared" || reason == "destroy" || reason == "detach") {
            press(2)
            assertEquals(2, repository.commands.size)
        }
        if (reason == "removed" || reason == "not-v3") assertNull(vm.uiState.value.calibration)
    }

    @ParameterizedTest
    @ValueSource(strings = ["device", "profile", "removed", "not-v3"])
    fun `stale down is rejected before collecting composition changes`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent()
        when (reason) {
            "device" -> { source.address = "second-device"; repository.currentAddress = "second-device" }
            "profile" -> source.profile = V3DeviceProfile.INDY3
            "removed" -> source.visible = false
            "not-v3" -> source.profile = V3DeviceProfile.NOT_V3
        }
        press(); release(); runCurrent()
        assertTrue(repository.commands.isEmpty())
    }

    @Test fun `device switch never releases onto the new device and new press has its own token`() = runTest(dispatcher) {
        attach(); runCurrent(); press()
        source.address = "second-device"; repository.currentAddress = "second-device"
        source.updates.emit(Unit); runCurrent(); release()
        assertEquals(listOf("start:first-device"), repository.commands)
        assertFalse(state().isPressed)
        press(2); release(1); release(2)
        assertEquals(listOf("start:first-device", "start:second-device", "release:second-device"), repository.commands)
    }

    @Test fun `rejected repository start never produces a release packet`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.currentAddress = "disconnected"
        press(); release(); store.clear()
        assertTrue(repository.commands.isEmpty())
    }

    private class Source : V3ServiceWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot().let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile) = snapshot().widgets
        override val updates = MutableSharedFlow<Unit>()
        var profile = V3DeviceProfile.STANDARD_V3
        var address = "first-device"
        var visible = true
        fun snapshot() = V3ServiceWidgetsSnapshot(profile, address, if (!visible) emptyList() else
            V3ServiceWidgetMapper().fromItems(listOf(ButtonsItemV3("Калибровка протеза", "", "", "",
                CommandParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4,
                    parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_START_CALIBRATE_COMMAND)))))))))
    }
}
