package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileInfo
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.GetSettingsProfilesUseCaseV3
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class V3SettingsProfilesRepositoryTest {
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val info = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{}")
    private val sharedRepository = mockk<SettingsProfileRepository>()
    private val repository = V3SettingsProfilesRepositoryImpl { error("Reading profiles must not apply values") }

    @BeforeEach
    fun setUp() {
        mockkObject(SettingsProfileManager, SettingsProfileRepositoryProvider)
        every { SettingsProfileManager.serial() } returns "current"
        every { SettingsProfileRepositoryProvider.getOrNull() } returns sharedRepository
        coEvery { sharedRepository.getProfiles(any()) } returns listOf(SettingsProfileInfo(1, null, false), SettingsProfileInfo(3, "Sport", true))
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache),
        ))
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        unmockkObject(SettingsProfileManager, SettingsProfileRepositoryProvider)
    }

    @Test
    fun `read uses the requested serial and cache stores the index rather than the profile ID`() = runTest {
        val snapshot = GetSettingsProfilesUseCaseV3(repository)("current")
        assertEquals(3, snapshot.activeProfileId)
        assertEquals("Sport", snapshot.profiles.last().customName)
        assertNull(ParameterStoreV3.get(info))
        repository.cacheSelection("current", snapshot)
        assertEquals(1, (ParameterStoreV3.get(info) as ParameterTypedValueV3.Spinner).value.spinnerValue)
        assertEquals("{\"spinnerValue\":1}", cache.data)
        coVerify(exactly = 1) { sharedRepository.getProfiles("current") }
        confirmVerified(sharedRepository)
    }

    @Test
    fun `old serial can be read but cannot overwrite the current selection cache`() = runTest {
        val snapshot = GetSettingsProfilesUseCaseV3(repository)("old")
        repository.cacheSelection("old", snapshot)
        assertNull(ParameterStoreV3.get(info))
        assertEquals("{}", cache.data)
        coVerify(exactly = 1) { sharedRepository.getProfiles("old") }
        confirmVerified(sharedRepository)
    }
}
