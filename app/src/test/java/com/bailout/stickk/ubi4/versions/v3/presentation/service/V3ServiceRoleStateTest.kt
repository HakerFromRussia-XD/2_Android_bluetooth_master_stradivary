package com.bailout.stickk.ubi4.versions.v3.presentation.service

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.domain.settings.*
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.*
import com.bailout.stickk.ubi4.data.widget.endStructures.*
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
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
class V3ServiceRoleStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeV3DeviceRoleRepository()
    private val source = Source()
    private val sliders = mockk<V3DeviceSettingsRepository> {
        every { sliderInteractionEnabled } returns MutableStateFlow(true)
        every { getSliderValue(any()) } returns null
        every { observeSliderValue(any()) } returns flowOf(null)
    }
    private val spinners = mockk<V3SpinnerSettingsRepository> {
        every { spinnerInteractionEnabled } returns MutableStateFlow(true)
        every { observeSpinnerValue(any()) } returns flowOf(null)
    }
    private lateinit var vm: V3ServiceViewModel
    private fun state() = requireNotNull(vm.uiState.value.role)
    private fun attach() = vm.onAction(V3ServiceAction.ViewAttached)
    private fun select(role: V3DeviceRole = V3DeviceRole.SERVICE_ENGINEER) = vm.onAction(V3ServiceAction.RoleSelected(role))
    private fun request(): Long { select(); return requireNotNull(state().pinRequest).id }
    private fun submit(id: Long, pin: String = "1234") = vm.onAction(V3ServiceAction.RolePinSubmitted(id, pin))

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3ServiceViewModelFactory(sliders, source, spinners, repository, FakeV3DeviceInfoRepository()).create(V3ServiceViewModel::class.java)
        store.put("service", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles restore exactly the two existing roles without writes`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        repository.selected = V3DeviceRole.SERVICE_ENGINEER
        attach(); runCurrent()
        assertEquals(listOf(V3DeviceRole.SERVICE_ENGINEER, V3DeviceRole.USER), state().roles)
        assertEquals(0, state().displayedIndex)
        assertEquals(V3DeviceRole.SERVICE_ENGINEER, repository.access)
        repeat(3) { source.updates.emit(Unit); attach(); runCurrent() }
        select(V3DeviceRole.SERVICE_ENGINEER)
        assertNull(state().pinRequest)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `engineer remains only a displayed draft until correct PIN saves once`() = runTest(dispatcher) {
        attach(); runCurrent()
        val id = request()
        assertEquals(V3DeviceRole.USER, state().selectedRole)
        assertEquals(0, state().displayedIndex)
        assertEquals(V3DeviceRole.USER, repository.access)
        assertTrue(repository.writes.isEmpty())
        source.updates.emit(Unit); attach(); runCurrent()
        assertEquals(id, state().pinRequest?.id)
        submit(id)
        assertEquals(listOf(V3DeviceRole.SERVICE_ENGINEER), repository.writes)
        assertEquals(V3DeviceRole.SERVICE_ENGINEER, state().selectedRole)
        assertNull(state().pinRequest)
        assertEquals(V3RolePinFeedback(id, true), state().pinFeedback)
        vm.onAction(V3ServiceAction.RolePinFeedbackShown(id))
        submit(id); runCurrent()
        assertNull(state().pinFeedback)
        assertEquals(1, repository.writes.size)
    }

    @Test fun `wrong PIN reverts selection and feedback is consumed once`() = runTest(dispatcher) {
        attach(); runCurrent()
        val id = request(); submit(id, "0000")
        assertEquals(1, state().displayedIndex)
        assertEquals(V3DeviceRole.USER, repository.access)
        assertEquals(V3RolePinFeedback(id, false), state().pinFeedback)
        vm.onAction(V3ServiceAction.RolePinFeedbackShown(id + 1))
        assertNotNull(state().pinFeedback)
        vm.onAction(V3ServiceAction.RolePinFeedbackShown(id))
        source.updates.emit(Unit); runCurrent()
        assertNull(state().pinFeedback)
        submit(id)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `cancel returns to stored role and stale dialog cannot submit or cancel a new request`() = runTest(dispatcher) {
        attach(); runCurrent()
        val first = request()
        vm.onAction(V3ServiceAction.RolePinCancelled(first))
        assertEquals(1, state().displayedIndex)
        assertNull(state().pinFeedback)
        val second = request()
        assertNotEquals(first, second)
        submit(first)
        vm.onAction(V3ServiceAction.RolePinCancelled(first))
        assertEquals(second, state().pinRequest?.id)
        assertTrue(repository.writes.isEmpty())
        submit(second)
        assertEquals(1, repository.writes.size)
    }

    @Test fun `user role needs no PIN and selecting it again sends nothing`() = runTest(dispatcher) {
        repository.selected = V3DeviceRole.SERVICE_ENGINEER
        attach(); runCurrent()
        select(V3DeviceRole.USER)
        assertNull(state().pinRequest)
        assertNull(state().pinFeedback)
        assertEquals(V3DeviceRole.USER, repository.access)
        assertEquals(listOf(V3DeviceRole.USER), repository.writes)
        select(V3DeviceRole.USER)
        assertEquals(1, repository.writes.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["detach", "lock", "device", "profile", "removed", "not-v3", "preferences", "cleared"])
    fun `late correct PIN cannot apply after losing its screen or device context`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent()
        val id = request()
        when (reason) {
            "detach" -> vm.onAction(V3ServiceAction.ViewDetached)
            "lock" -> repository.interactionEnabled.value = false
            "device" -> source.address = "second-device"
            "profile" -> source.profile = V3DeviceProfile.INDY3
            "removed" -> source.visible = false
            "not-v3" -> source.profile = V3DeviceProfile.NOT_V3
            "preferences" -> repository.selected = V3DeviceRole.SERVICE_ENGINEER
            "cleared" -> { store.clear(); attach() }
        }
        submit(id); runCurrent()
        assertTrue(repository.writes.isEmpty())
        if (reason != "cleared") assertNull(vm.uiState.value.role?.pinRequest)
        assertNull(vm.uiState.value.role?.pinFeedback)
    }

    @Test fun `block and unlock discard pending PIN and returning requires a fresh request`() = runTest(dispatcher) {
        attach(); runCurrent()
        val id = request()
        repository.interactionEnabled.value = false; runCurrent()
        assertFalse(state().isEnabled)
        assertNull(state().pinRequest)
        repository.interactionEnabled.value = true; runCurrent()
        submit(id)
        assertTrue(repository.writes.isEmpty())
        val next = request()
        vm.onAction(V3ServiceAction.ViewDetached)
        attach(); runCurrent(); submit(next)
        assertEquals(1, state().displayedIndex)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `domain requires PIN independently from screen and never writes on restore`() {
        val change = ChangeDeviceRoleUseCaseV3(repository)
        assertEquals(V3DeviceRoleChangeResult.PIN_REQUIRED, change(V3DeviceRole.SERVICE_ENGINEER))
        assertEquals(V3DeviceRoleChangeResult.INVALID_PIN, change(V3DeviceRole.SERVICE_ENGINEER, "0000"))
        repository.interactionEnabled.value = false
        assertEquals(V3DeviceRoleChangeResult.BLOCKED, change(V3DeviceRole.SERVICE_ENGINEER, "1234"))
        assertEquals(V3DeviceRole.USER, RestoreDeviceRoleUseCaseV3(repository)())
        assertTrue(repository.writes.isEmpty())
    }

    private class Source : V3ServiceWidgetsSource {
        override val updates = MutableSharedFlow<Unit>()
        var profile = V3DeviceProfile.STANDARD_V3
        var address = "first-device"
        var visible = true
        override fun snapshot() = V3ServiceWidgetsSnapshot(profile, address, if (!visible) emptyList() else
            V3ServiceWidgetMapper().fromItems(listOf(SpinnerItemV3("Role", SpinnerParameterWidgetSStruct(
                BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4,
                    parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_DEVICE_ROLE)))),
                DataSpinnerParameterWidgetStruct(listOf("Prosthetist", "Service engineer", "User"), 0))))))
    }
}
