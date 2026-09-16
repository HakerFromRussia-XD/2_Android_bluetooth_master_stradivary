package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.di.V3ServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.*
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceSpinnerIntegrationTest {
    private val emg = P_KEY_EMG_CONTROL_MODE
    private val side = P_KEY_LEFT_RIGHT_HAND
    private val keys = listOf(emg, side)
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val cache = keys.associateWith { key ->
        val info = ParameterInfoRegistry.require(key)
        BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode,
            data = if (key == emg) "{\"spinnerValue\":2}" else "{}")
    }
    private val packets = mutableListOf<ByteArray>()
    private val saved = mutableListOf<Pair<String, ParameterTypedValueV3>>()
    private val source = Source()
    private lateinit var repository: V3DeviceSettingsRepositoryImpl
    private lateinit var viewModel: V3ServiceViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = 1, parametersList = ArrayList(cache.values),
        ))
        repository = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = {
                assertEquals(packets.size + 1, saved.size)
                val (key, typed) = saved.last()
                val value = (typed as ParameterTypedValueV3.Spinner).value.spinnerValue
                assertEquals(serialized(value), cache.getValue(key).data)
                packets += it
            },
            saveBleValue = { parameter, value ->
                val key = keys.single { ParameterInfoRegistry.require(it) == parameter }
                assertEquals(value, ParameterStoreV3.get(parameter))
                saved += key to value
            },
        )
        viewModel = V3ServiceViewModelFactory(repository, source, repository, FakeV3DeviceRoleRepository(), FakeV3DeviceInfoRepository(), FakeV3ProsthesisCalibrationRepository(), sessionRepository = source).create(V3ServiceViewModel::class.java)
        store.put("service", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onAction(V3ServiceAction.ViewAttached)
    private fun detach() = viewModel.onAction(V3ServiceAction.ViewDetached)
    private fun select(key: String, value: Int) = viewModel.onAction(
        V3ServiceAction.SpinnerAction(V3SpinnerAction.SpinnerValueSelected(key, value)))
    private fun state(key: String) = viewModel.uiState.value.spinners.getValue(key)
    private fun incoming(key: String, value: Int) = ParameterStoreV3.put(
        ParameterInfoRegistry.require(key), ParameterTypedValueV3.Spinner(SpinnerV3(value)))
    private fun serialized(value: Int) = if (value == 0) "{}" else "{\"spinnerValue\":$value}"
    private fun noWrites() { assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty()) }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles restore cache and incoming values without writing`(profile: V3DeviceProfile) = runTest(dispatcher) {
        source.profile = profile
        assertEquals(2, state(emg).selectedIndex)
        assertFalse(state(emg).isEnabled)
        select(emg, 1)
        attach(); runCurrent()
        assertEquals(if (profile == V3DeviceProfile.INDY3) setOf(emg) else keys.toSet(), viewModel.uiState.value.spinners.keys)
        assertTrue(state(emg).isEnabled)
        val composition = viewModel.uiState.value.widgets
        incoming(emg, 3); runCurrent()
        assertEquals(3, state(emg).selectedIndex)
        assertEquals(composition, viewModel.uiState.value.widgets)
        ParameterStoreV3.clear(); runCurrent()
        assertEquals(2, state(emg).selectedIndex)
        repeat(3) { attach(); source.updates.emit(Unit); runCurrent() }
        noWrites()
    }

    @ParameterizedTest
    @CsvSource("emg,0,00120500FA", "emg,1,00120501A4", "emg,2,0012050246", "emg,3,0012050318",
        "side,0,00100E0096", "side,1,00100E01C8")
    fun `each choice preserves its packet CRC and store profile cache queue ordering`(name: String, value: Int, hex: String) = runTest(dispatcher) {
        val key = if (name == "emg") emg else side
        val other = keys.single { it != key }
        val otherCache = cache.getValue(other).data
        attach(); runCurrent()
        select(key, value)
        assertEquals(value, state(key).selectedIndex)
        assertEquals(value, repository.getSpinnerValue(key))
        assertEquals(listOf(key), saved.map { it.first })
        assertArrayEquals(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), packets.single())
        assertEquals(otherCache, cache.getValue(other).data)
        assertNull(ParameterStoreV3.get(ParameterInfoRegistry.require(other)))
        runCurrent(); attach(); source.updates.emit(Unit); runCurrent()
        assertEquals(1, packets.size)
    }

    @Test
    fun `missing values fallback and invalid incoming values clamp only for display`() = runTest(dispatcher) {
        keys.forEach { cache.getValue(it).data = "" }
        source.initialIndex = 1
        attach(); runCurrent()
        keys.forEach { key ->
            assertEquals(1, state(key).selectedIndex)
            assertNull(repository.getSpinnerValue(key))
            incoming(key, 255); runCurrent()
            assertEquals(if (key == emg) 3 else 1, state(key).selectedIndex)
            assertEquals(255, repository.getSpinnerValue(key))
            incoming(key, -3); runCurrent()
            assertEquals(0, state(key).selectedIndex)
            assertEquals(-3, repository.getSpinnerValue(key))
        }
        noWrites()
    }

    @ParameterizedTest
    @ValueSource(strings = ["detach", "lock", "removed", "not-v3", "cleared"])
    fun `stopped blocked or removed controls cannot send even before pending collection`(reason: String) = runTest(dispatcher) {
        attach(); runCurrent()
        when (reason) {
            "detach" -> detach()
            "lock" -> UiState.v3WidgetsInteractionEnabled.value = false
            "removed" -> source.visible = false
            "not-v3" -> source.profile = V3DeviceProfile.NOT_V3
            "cleared" -> { store.clear(); attach() }
        }
        keys.forEach { select(it, 1) }
        runCurrent()
        noWrites()
    }

    @Test
    fun `INDY3 rejects hand side without blocking EMG mode`() = runTest(dispatcher) {
        attach(); runCurrent()
        source.profile = V3DeviceProfile.INDY3
        select(side, 1)
        runCurrent()
        assertEquals(setOf(emg), viewModel.uiState.value.spinners.keys)
        noWrites()
        select(emg, 3)
        assertEquals(listOf(emg), saved.map { it.first })
        assertEquals("{}", cache.getValue(side).data)
    }

    @Test
    fun `two selections remain independent and returning restores the latest cache`() = runTest(dispatcher) {
        attach(); runCurrent()
        select(emg, 0); select(side, 1); runCurrent()
        assertEquals(listOf(emg, side), saved.map { it.first })
        assertEquals(0, state(emg).selectedIndex)
        assertEquals(1, state(side).selectedIndex)
        detach()
        cache.getValue(emg).data = serialized(3)
        cache.getValue(side).data = serialized(0)
        ParameterStoreV3.clear()
        attach(); runCurrent()
        assertEquals(3, state(emg).selectedIndex)
        assertEquals(0, state(side).selectedIndex)
        assertEquals(2, packets.size)
    }

    @Test
    fun `device change rejects previous selection and reads the new device before collection`() = runTest(dispatcher) {
        attach(); runCurrent()
        source.address = "second-device"
        cache.getValue(emg).data = serialized(1)
        select(emg, 3)
        assertEquals(1, state(emg).selectedIndex)
        noWrites()
        select(emg, 0)
        assertEquals(1, packets.size)
    }

    @Test
    fun `domain rejects invalid values and special selectors independently of UI options`() = runTest(dispatcher) {
        source.extraOptions = true
        attach(); runCurrent()
        val setValue = SetSpinnerValueUseCaseV3(repository)
        keys.forEach { key ->
            listOf(-1, if (key == emg) 4 else 2, 255).forEach { value ->
                select(key, value)
                assertThrows(IllegalArgumentException::class.java) { setValue(key, value) }
            }
        }
        listOf(P_KEY_DEVICE_ROLE, P_KEY_SETTINGS_PROFILE).forEach { key ->
            select(key, 1)
            assertThrows(IllegalArgumentException::class.java) { setValue(key, 1) }
        }
        select(P_KEY_HAND_CONTROL_MODE, 1)
        UiState.v3WidgetsInteractionEnabled.value = false
        keys.forEach { setValue(it, 1) }
        noWrites()
    }

    @Test
    fun `empty and shortened options disable unavailable choices without changing device values`() = runTest(dispatcher) {
        attach(); runCurrent()
        source.optionCount = 0
        source.updates.emit(Unit); runCurrent()
        keys.forEach { assertNull(state(it).selectedIndex); assertFalse(state(it).isEnabled); select(it, 0) }
        source.optionCount = 1
        source.updates.emit(Unit); runCurrent()
        keys.forEach { assertEquals(0, state(it).selectedIndex); select(it, 1) }
        assertEquals(2, repository.getSpinnerValue(emg))
        noWrites()
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
        var initialIndex = 0
        var extraOptions = false
        var optionCount: Int? = null
        fun snapshot() = V3ServiceWidgetsSnapshot(profile, address,
            if (!visible || profile == V3DeviceProfile.NOT_V3) emptyList() else {
                val keys = listOf(P_KEY_EMG_CONTROL_MODE) +
                    (if (profile == V3DeviceProfile.STANDARD_V3) listOf(P_KEY_LEFT_RIGHT_HAND) else emptyList()) + P_KEY_DEVICE_ROLE
                V3ServiceWidgetMapper().fromItems(keys.mapIndexed { position, key ->
                    val count = optionCount ?: ((if (key == P_KEY_EMG_CONTROL_MODE) 4 else 2) + if (extraOptions) 1 else 0)
                    SpinnerItemV3(key, SpinnerParameterWidgetSStruct(
                        BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4, widgetPosition = position,
                            parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key)))),
                        DataSpinnerParameterWidgetStruct(List(count) { "Option $it" }, initialIndex)))
                })
            })
    }
}
