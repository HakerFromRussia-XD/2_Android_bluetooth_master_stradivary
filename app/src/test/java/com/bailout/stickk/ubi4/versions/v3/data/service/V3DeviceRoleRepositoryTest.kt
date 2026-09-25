package com.bailout.stickk.ubi4.versions.v3.data.service

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class V3DeviceRoleRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private var stored = 2
    private val info = ParameterInfoRegistry.require(P_KEY_DEVICE_ROLE)
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{}")
    private val events = mutableListOf<String>()
    private val packets = mutableListOf<ByteArray>()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalAccess = UiState.isServiceEngineerRole.value
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private lateinit var repository: V3DeviceRoleRepositoryImpl

    @BeforeEach fun setUp() {
        ParameterStoreV3.clear()
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = 1, parametersList = arrayListOf(cache)))
        every { preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, 2) } answers { stored }
        every { preferences.edit() } returns editor
        every { editor.putInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, any()) } answers {
            stored = secondArg(); events += "preferences"; editor
        }
        val settings = V3DeviceSettingsRepositoryImpl(
            saveBleValue = { parameter, value ->
                assertEquals(info, parameter)
                assertEquals(value, ParameterStoreV3.get(info))
                val wireValue = (value as ParameterTypedValueV3.Spinner).value.spinnerValue
                assertEquals(wireValue, stored)
                assertEquals(wireValue == 1, UiState.isServiceEngineerRole.value)
                events += "profile"
            },
            enqueuePacket = {
                assertEquals("{\"spinnerValue\":$stored}", cache.data)
                packets += it; events += "queue"
            },
        )
        repository = V3DeviceRoleRepositoryImpl(preferences, settings)
    }
    @AfterEach fun tearDown() {
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.isServiceEngineerRole.value = originalAccess
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 1, 2, 99])
    fun `restoration normalizes disabled and invalid roles only for display and access`(value: Int) {
        stored = value
        val expected = if (value == 1) V3DeviceRole.SERVICE_ENGINEER else V3DeviceRole.USER
        assertEquals(expected, RestoreDeviceRoleUseCaseV3(repository)())
        assertEquals(value == 1, UiState.isServiceEngineerRole.value)
        assertEquals(value == 1, repository.serviceEngineerAccess.value)
        assertEquals(value, stored)
        assertEquals("{}", cache.data)
        assertNull(ParameterStoreV3.get(info))
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `access observation follows existing shared flag without changing preferences or sending commands`() = runTest {
        UiState.isServiceEngineerRole.value = false
        val access = ObserveServiceEngineerAccessUseCaseV3(repository)()
        assertFalse(access is MutableStateFlow<*>)
        val observed = mutableListOf<Boolean>()
        backgroundScope.launch { access.collect { observed += it } }
        runCurrent()
        UiState.isServiceEngineerRole.value = true; runCurrent()
        repository.updateRoleAccess(V3DeviceRole.USER); runCurrent()
        assertEquals(listOf(false, true, false), observed)
        assertEquals(2, stored)
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @Test fun `engineer confirmation and return to user preserve wire values and persistence order`() {
        val change = ChangeDeviceRoleUseCaseV3(repository)
        assertEquals(V3DeviceRoleChangeResult.PIN_REQUIRED, change(V3DeviceRole.SERVICE_ENGINEER))
        assertEquals(V3DeviceRoleChangeResult.INVALID_PIN, change(V3DeviceRole.SERVICE_ENGINEER, "0000"))
        assertTrue(events.isEmpty())
        assertEquals(V3DeviceRoleChangeResult.APPLIED, change(V3DeviceRole.SERVICE_ENGINEER, "1234"))
        assertArrayEquals(byteArrayOf(0, 1, 15, 1, 0xED.toByte()), packets.single())
        assertEquals(listOf("preferences", "profile", "queue"), events)
        assertEquals(V3DeviceRoleChangeResult.UNCHANGED, change(V3DeviceRole.SERVICE_ENGINEER))
        assertEquals(1, packets.size)
        assertEquals(V3DeviceRoleChangeResult.APPLIED, change(V3DeviceRole.USER))
        assertArrayEquals(byteArrayOf(0, 1, 15, 2, 15), packets.last())
        assertEquals(2, packets.size)
        assertFalse(UiState.isServiceEngineerRole.value)
        verify(exactly = 2) { editor.apply() }
    }

    @Test fun `device notifications do not replace the locally selected role`() {
        stored = 2
        ParameterStoreV3.put(info, ParameterTypedValueV3.Spinner(SpinnerV3(1)))
        assertEquals(V3DeviceRole.USER, RestoreDeviceRoleUseCaseV3(repository)())
        assertFalse(UiState.isServiceEngineerRole.value)
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @Test fun `interaction lock rejects a correct PIN without preference or device writes`() {
        UiState.v3WidgetsInteractionEnabled.value = false
        assertEquals(V3DeviceRoleChangeResult.BLOCKED,
            ChangeDeviceRoleUseCaseV3(repository)(V3DeviceRole.SERVICE_ENGINEER, "1234"))
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())
    }
}
