package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.di.V3ServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField.*
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
class V3ServiceTextInputStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeV3DeviceInfoRepository()
    private val source = Source()
    private lateinit var vm: V3ServiceViewModel
    private fun attach() = vm.onAction(V3ServiceAction.ViewAttached)
    private fun edit(text: String, field: V3DeviceInfoField = DEVICE_NAME) = vm.onAction(V3ServiceAction.TextInputChanged(field, text))
    private fun prefill(field: V3DeviceInfoField = DEVICE_NAME) = vm.onAction(V3ServiceAction.TextInputPrefillRequested(field))
    private fun send(field: V3DeviceInfoField = DEVICE_NAME) = vm.onAction(V3ServiceAction.TextInputSendClicked(field))
    private fun input(field: V3DeviceInfoField = DEVICE_NAME) = vm.uiState.value.textInputs.getValue(field)

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
        vm = V3ServiceViewModelFactory(sliders, source, spinners, FakeV3DeviceRoleRepository(), repository, FakeV3ProsthesisCalibrationRepository(), sessionRepository = source)
            .create(V3ServiceViewModel::class.java)
        store.put("service", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles start blank and prefill only on request`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        repository.current[DEVICE_NAME] = "Current"
        repository.current[SERIAL_NUMBER] = "INDY3-000123"
        attach(); runCurrent()
        assertEquals(setOf(DEVICE_NAME, SERIAL_NUMBER), vm.uiState.value.textInputs.keys)
        assertTrue(vm.uiState.value.textInputs.values.all { it.text.isEmpty() && it.canSend })
        prefill(); prefill(SERIAL_NUMBER)
        assertEquals("Current", input().text)
        assertEquals("INDY3-000123", input(SERIAL_NUMBER).text)
        edit("Draft")
        source.updates.emit(Unit); runCurrent()
        assertEquals("Draft", input().text)
        repository.current[DEVICE_NAME] = "Latest"
        prefill()
        assertEquals("Latest", input().text)
        val cursor = input().cursorRevision
        prefill()
        assertEquals(cursor + 1, input().cursorRevision)
        repository.current[DEVICE_NAME] = " "
        prefill()
        assertEquals("Latest", input().text)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `name limit retains complete unicode characters while serial has no name restriction`() = runTest(dispatcher) {
        attach(); runCurrent()
        edit("ПротезAB")
        assertEquals("ПротезA", input().text)
        assertEquals(V3TextInputMessage.LIMIT_REACHED, vm.uiState.value.textInputFeedback?.message)
        edit("INDY3-000123456789😀", SERIAL_NUMBER)
        assertEquals("INDY3-000123456789😀", input(SERIAL_NUMBER).text)
        assertEquals("ПротезA", input().text)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `only send writes trimmed text and consuming feedback never repeats it`() = runTest(dispatcher) {
        attach(); runCurrent()
        edit("  "); send()
        assertEquals(V3TextInputMessage.ENTER_TEXT, vm.uiState.value.textInputFeedback?.message)
        assertTrue(repository.writes.isEmpty())
        edit(" Name "); send()
        val feedback = requireNotNull(vm.uiState.value.textInputFeedback)
        assertEquals(V3TextInputMessage.SENT, feedback.message)
        assertEquals(listOf(DEVICE_NAME to "Name"), repository.writes)
        vm.onAction(V3ServiceAction.TextInputFeedbackShown(feedback.id + 1))
        assertEquals(feedback, vm.uiState.value.textInputFeedback)
        vm.onAction(V3ServiceAction.TextInputFeedbackShown(feedback.id))
        repeat(3) { source.updates.emit(Unit); attach(); runCurrent() }
        assertNull(vm.uiState.value.textInputFeedback)
        assertEquals(" Name ", input().text)
        assertEquals(1, repository.writes.size)
        edit(" 000123 ", SERIAL_NUMBER); send(SERIAL_NUMBER)
        assertEquals(SERIAL_NUMBER to "000123", repository.writes.last())
    }

    @Test fun `lock blocks even before state collection but editing and prefill remain available`() = runTest(dispatcher) {
        attach(); runCurrent(); edit("Draft")
        repository.interactionEnabled.value = false
        send(); runCurrent()
        assertFalse(input().canSend)
        edit("Other")
        assertEquals("Other", input().text)
        repository.current[DEVICE_NAME] = "Current"; prefill()
        assertEquals("Current", input().text)
        assertTrue(repository.writes.isEmpty())
        repository.interactionEnabled.value = true; runCurrent(); send()
        assertEquals(listOf(DEVICE_NAME to "Current"), repository.writes)
    }

    @Test fun `temporary stop retains draft but destroyed view starts blank`() = runTest(dispatcher) {
        attach(); runCurrent(); edit("Draft")
        vm.onAction(V3ServiceAction.ViewDetached)
        edit("Late"); send()
        assertFalse(input().canSend)
        attach(); runCurrent()
        assertEquals("Draft", input().text)
        vm.onAction(V3ServiceAction.ViewDestroyed)
        attach(); runCurrent()
        assertEquals("", input().text)
        assertNull(vm.uiState.value.textInputFeedback)
        assertTrue(repository.writes.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["device", "profile", "removed", "not-v3", "cleared"])
    fun `stale input cannot write after device or screen changes before collection`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent(); edit("Draft")
        when (reason) {
            "device" -> source.address = "second"
            "profile" -> source.profile = V3DeviceProfile.INDY3
            "removed" -> source.visible = false
            "not-v3" -> source.profile = V3DeviceProfile.NOT_V3
            "cleared" -> store.clear()
        }
        send(); edit("Late"); runCurrent()
        assertTrue(repository.writes.isEmpty())
        assertNull(vm.uiState.value.textInputFeedback)
        if (reason == "removed" || reason == "not-v3") assertTrue(vm.uiState.value.textInputs.isEmpty())
    }

    @Test fun `preparation failure keeps draft and produces existing failure feedback`() = runTest(dispatcher) {
        attach(); runCurrent(); edit("Name")
        repository.canPrepare = false; send()
        assertEquals(V3TextInputMessage.PREPARATION_FAILED, vm.uiState.value.textInputFeedback?.message)
        assertEquals("Name", input().text)
        assertTrue(repository.writes.isEmpty())
        vm.onAction(V3ServiceAction.ViewDetached)
        assertNull(vm.uiState.value.textInputFeedback)
    }

    private class Source : V3ServiceWidgetsSource, V3DeviceSessionRepository {
        override fun getSession() = snapshot().let {
            V3DeviceSession(it.deviceProfile, it.deviceAddress, !it.animationsEnabled)
        }
        override fun widgets(profile: V3DeviceProfile) = snapshot().widgets
        override val updates = MutableSharedFlow<Unit>()
        var profile = V3DeviceProfile.STANDARD_V3
        var address = "first"
        var visible = true
        fun snapshot() = V3ServiceWidgetsSnapshot(profile, address, if (!visible) emptyList() else
            V3ServiceWidgetMapper().fromItems(listOf(P_KEY_SET_DEVICE_NAME, P_KEY_SET_SERIAL_NUMBER).map { key ->
                TextInputItemV3(key, "Send", CommandParameterWidgetSStruct(
                    BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4,
                        parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key))))))
            }))
    }
}
