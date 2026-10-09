package com.bailout.stickk.ubi4.versions.v3.data.service

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.data.settings.V3DeviceSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.di.createDeviceRoleRepository
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
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.V3DeviceRoleChangeResult
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ChangeDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.GetDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ObserveServiceEngineerAccessUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.RestoreDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceRoleUiState

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
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalAddress = WidgetRepoProvider.mac()
    private lateinit var repository: V3DeviceRoleRepositoryImpl

    @BeforeEach fun setUp() {
        ParameterStoreV3.clear()
        UiState.isServiceEngineerRole.value = false
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = 1, parametersList = arrayListOf(cache)))
        every { preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, 2) } answers { stored }
        every { preferences.edit() } returns editor
        every { editor.putInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, any()) } answers {
            stored = secondArg(); events += "preferences"; editor
        }
        every { editor.apply() } answers { events += "apply:${UiState.isServiceEngineerRole.value}" }
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
        repository = createDeviceRoleRepository(preferences, settings)
    }
    @AfterEach fun tearDown() {
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.isServiceEngineerRole.value = originalAccess
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.isInterfaceV3Activated = originalMode
        UiState.activeV3DeviceProfile = originalProfile
        WidgetRepoProvider.setCurrentMac(originalAddress)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `separate screen repositories share access and creating another does not reset it`() = runTest {
        fun anotherRepository() = createDeviceRoleRepository(preferences,
            V3DeviceSettingsRepositoryImpl(enqueuePacket = { error("Access changes must not send commands") }))
        val accountRepository = anotherRepository()
        val observed = mutableListOf<Boolean>()
        backgroundScope.launch {
            ObserveServiceEngineerAccessUseCaseV3(accountRepository)().collect { observed += it }
        }
        runCurrent()

        repository.updateRoleAccess(V3DeviceRole.SERVICE_ENGINEER); runCurrent()
        assertTrue(accountRepository.serviceEngineerAccess.value)
        assertTrue(UiState.isServiceEngineerRole.value)
        val recreatedRepository = anotherRepository()
        assertTrue(recreatedRepository.serviceEngineerAccess.value)

        recreatedRepository.updateRoleAccess(V3DeviceRole.USER); runCurrent()
        assertFalse(repository.serviceEngineerAccess.value)
        assertFalse(UiState.isServiceEngineerRole.value)
        UiState.isServiceEngineerRole.value = true; runCurrent()
        assertTrue(repository.serviceEngineerAccess.value)
        assertTrue(recreatedRepository.serviceEngineerAccess.value)
        assertEquals(listOf(false, true, false, true), observed)
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
        assertEquals(listOf("preferences", "apply:false", "profile", "queue"), events)
        assertEquals(V3DeviceRoleChangeResult.UNCHANGED, change(V3DeviceRole.SERVICE_ENGINEER))
        assertEquals(1, packets.size)
        assertEquals(V3DeviceRoleChangeResult.APPLIED, change(V3DeviceRole.USER))
        assertArrayEquals(byteArrayOf(0, 1, 15, 2, 15), packets.last())
        assertEquals(2, packets.size)
        assertFalse(UiState.isServiceEngineerRole.value)
        assertEquals(listOf("preferences", "apply:false", "profile", "queue",
            "preferences", "apply:true", "profile", "queue"), events)
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

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 1, 2, 99])
    fun `native get and restore retain the three local roles without touching shared access or transport`(value: Int) {
        stored = value
        UiState.v3WidgetsInteractionEnabled.value = false
        val settings = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { error("Restoration must not enqueue a packet") },
            saveBleValue = { _, _ -> error("Restoration must not persist a device setting") },
            readCachedValues = false,
            saveValueBeforeSending = false,
        )
        val native = V3DeviceRoleRepositoryImpl(
            readSavedRole = { stored },
            saveRole = { error("Restoration must not overwrite the raw saved role") },
            settings = settings,
            allowProsthetist = true,
            updateSharedAccess = false,
        )
        val expected = when (value) {
            0 -> V3DeviceRole.PROSTHETIST
            1 -> V3DeviceRole.SERVICE_ENGINEER
            else -> V3DeviceRole.USER
        }
        assertSame(settings.spinnerInteractionEnabled, native.interactionEnabled)
        assertFalse(native.interactionEnabled.value)
        for (access in listOf(false, true)) {
            UiState.isServiceEngineerRole.value = access
            assertEquals(expected, GetDeviceRoleUseCaseV3(native)())
            assertEquals(expected, RestoreDeviceRoleUseCaseV3(native)())
            assertEquals(access, native.serviceEngineerAccess.value)
            assertEquals(access, UiState.isServiceEngineerRole.value)
        }
        assertEquals(value, stored)
        assertNull(ParameterStoreV3.get(info))
        assertEquals("{}", cache.data)
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())
    }

    @Test fun `native role confirmation validates before sending and preserves repeated offline queue order without device cache writes`() {
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.isInterfaceV3Activated = false
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
        WidgetRepoProvider.setCurrentMac("")
        val received = ParameterTypedValueV3.Spinner(SpinnerV3(99))
        ParameterStoreV3.put(info, received)
        cache.data = "{\"spinnerValue\":99}"
        val settings = V3DeviceSettingsRepositoryImpl(
            enqueuePacket = { packet ->
                assertEquals(stored, packet[3].toInt())
                assertFalse(UiState.isServiceEngineerRole.value)
                assertSame(received, ParameterStoreV3.get(info))
                assertEquals("{\"spinnerValue\":99}", cache.data)
                packets += packet
                events += "queue:$stored"
            },
            saveBleValue = { _, _ -> error("Native role selection must not write a profile value") },
            readCachedValues = false,
            saveValueBeforeSending = false,
        )
        val native = V3DeviceRoleRepositoryImpl(
            readSavedRole = { stored },
            saveRole = { value -> stored = value; events += "preferences:$value" },
            settings = settings,
            allowProsthetist = true,
            updateSharedAccess = false,
        )
        val change = ChangeDeviceRoleUseCaseV3(native, requireInteractionEnabled = false, skipUnchanged = false)
        assertTrue(change.requiresPin(V3DeviceRole.PROSTHETIST))
        assertTrue(change.requiresPin(V3DeviceRole.SERVICE_ENGINEER))
        assertFalse(change.requiresPin(V3DeviceRole.USER))
        assertTrue(change.isPinValid("1234"))
        assertFalse(change.isPinValid("0000"))
        assertFalse(change.isPinValid("12345"))
        for (role in listOf(V3DeviceRole.PROSTHETIST, V3DeviceRole.SERVICE_ENGINEER)) {
            assertEquals(V3DeviceRoleChangeResult.PIN_REQUIRED, change(role))
            assertEquals(V3DeviceRoleChangeResult.INVALID_PIN, change(role, "0000"))
        }
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())
        assertEquals(2, stored)

        val roles = listOf(V3DeviceRole.PROSTHETIST, V3DeviceRole.SERVICE_ENGINEER, V3DeviceRole.USER)
        val expectedPackets = listOf(
            byteArrayOf(0, 1, 15, 0, 0xB3.toByte()),
            byteArrayOf(0, 1, 15, 1, 0xED.toByte()),
            byteArrayOf(0, 1, 15, 2, 15),
        )
        roles.forEachIndexed { index, role ->
            repeat(2) {
                assertEquals(V3DeviceRoleChangeResult.APPLIED, change(role, if (change.requiresPin(role)) "1234" else null))
                assertArrayEquals(expectedPackets[index], packets.last())
                assertEquals(role.wireValue, stored)
                assertEquals(role, GetDeviceRoleUseCaseV3(native)())
            }
        }
        assertEquals(6, packets.size) // SET only; no read after SET or hidden restoration requests.
        assertEquals(listOf("preferences:0", "queue:0", "preferences:0", "queue:0",
            "preferences:1", "queue:1", "preferences:1", "queue:1",
            "preferences:2", "queue:2", "preferences:2", "queue:2"), events)
        assertFalse(native.interactionEnabled.value)
        assertFalse(native.serviceEngineerAccess.value)
        assertSame(received, ParameterStoreV3.get(info))
        assertEquals("{\"spinnerValue\":99}", cache.data)
    }

    @Test fun `native bypasses are independent while the default role API and Android role list remain unchanged`() {
        assertEquals(listOf(V3DeviceRole.SERVICE_ENGINEER, V3DeviceRole.USER),
            V3ServiceRoleUiState(V3DeviceRole.USER).roles)
        assertEquals(0, V3ServiceRoleUiState(V3DeviceRole.SERVICE_ENGINEER).displayedIndex)
        assertEquals(1, V3ServiceRoleUiState(V3DeviceRole.USER).displayedIndex)
        assertEquals(listOf(1, 2, 0), V3DeviceRole.entries.map { it.wireValue })
        assertThrows(IllegalArgumentException::class.java) { repository.setSelectedRole(V3DeviceRole.PROSTHETIST) }
        assertEquals(2, stored)
        assertFalse(UiState.isServiceEngineerRole.value)
        assertNull(ParameterStoreV3.get(info))
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())

        stored = 0
        val native = V3DeviceRoleRepositoryImpl(
            readSavedRole = { stored },
            saveRole = { value -> stored = value; events += "preferences:$value" },
            settings = V3DeviceSettingsRepositoryImpl(
                enqueuePacket = { packets += it },
                saveBleValue = { _, _ -> error("Native role selection must not write a profile value") },
                readCachedValues = false,
                saveValueBeforeSending = false,
            ),
            allowProsthetist = true,
            updateSharedAccess = false,
        )
        UiState.v3WidgetsInteractionEnabled.value = false
        assertEquals(V3DeviceRoleChangeResult.BLOCKED,
            ChangeDeviceRoleUseCaseV3(native)(V3DeviceRole.PROSTHETIST, "1234"))
        assertEquals(V3DeviceRoleChangeResult.BLOCKED,
            ChangeDeviceRoleUseCaseV3(native, skipUnchanged = false)(V3DeviceRole.PROSTHETIST, "1234"))
        assertEquals(V3DeviceRoleChangeResult.UNCHANGED,
            ChangeDeviceRoleUseCaseV3(native, requireInteractionEnabled = false)(V3DeviceRole.PROSTHETIST))
        val change = ChangeDeviceRoleUseCaseV3(native, requireInteractionEnabled = false, skipUnchanged = false)
        assertEquals(V3DeviceRoleChangeResult.PIN_REQUIRED, change(V3DeviceRole.PROSTHETIST))
        assertTrue(events.isEmpty())
        assertTrue(packets.isEmpty())

        UiState.v3WidgetsInteractionEnabled.value = true
        assertTrue(native.interactionEnabled.value)
        assertEquals(V3DeviceRoleChangeResult.UNCHANGED, ChangeDeviceRoleUseCaseV3(native)(V3DeviceRole.PROSTHETIST))
        assertEquals(V3DeviceRoleChangeResult.APPLIED, change(V3DeviceRole.PROSTHETIST, "1234"))
        assertArrayEquals(byteArrayOf(0, 1, 15, 0, 0xB3.toByte()), packets.single())
        assertEquals(listOf("preferences:0"), events)
        assertFalse(UiState.isServiceEngineerRole.value)
    }
}
