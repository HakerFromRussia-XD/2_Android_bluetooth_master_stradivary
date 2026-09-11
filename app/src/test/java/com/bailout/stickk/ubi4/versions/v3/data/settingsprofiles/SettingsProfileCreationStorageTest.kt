package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.data.local.db.dao.SettingsProfileDao
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileEntity
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileValueEntity
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileState
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

// Execute the real KMM profile logic through app JVM tests while the shared test target is unavailable.
class SettingsProfileCreationStorageTest {
    private val repository = SettingsProfileRepository(ProfileDao())
    private val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private val profileInfo = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
    private val mobileKey = MobileSettingsKey.AUTO_LOGIN.key

    @BeforeEach
    fun setUp() = ParameterStoreV3.clear()

    @AfterEach
    fun tearDown() = ParameterStoreV3.clear()

    @Test
    fun `creation snapshots current BLE values copies mobile settings and keeps profiles isolated`() = runBlocking {
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(10)))
        repository.saveMobileBoolean("current", mobileKey, true)
        ParameterStoreV3.put(speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        ParameterStoreV3.put(profileInfo, ParameterTypedValueV3.Spinner(SpinnerV3(0)))

        val (state, values) = repository.createProfileFromActive("current")
        assertEquals(SettingsProfileState(2, 2), state)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), values.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(true, values.single { it.mobileKey == mobileKey }.mobileBoolean)
        assertTrue(values.none { it.parameterInfo == profileInfo })
        assertEquals(2, values.size)

        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        repository.saveMobileBoolean("current", mobileKey, false)
        val original = repository.switchToProfile("current", 1).second
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), original.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(true, original.single { it.mobileKey == mobileKey }.mobileBoolean)
        assertTrue(repository.switchToProfile("other", 1).second.isEmpty())
    }

    @Test
    fun `creation fills the gap after importing profiles one and three`() = runBlocking {
        repository.importServerSettingsPayload("current", """{
            "PROFILE1":{"profile_id":1,"name":"First","is_active":false,"settings":[]},
            "PROFILE3":{"profile_id":3,"name":"Sport","is_active":true,"settings":[]}
        }""")
        repository.saveMobileBoolean("current", mobileKey, true)
        val (state, values) = repository.createProfileFromActive("current")
        assertEquals(SettingsProfileState(3, 2), state)
        assertEquals(listOf(1, 2, 3), repository.getProfiles("current").map { it.profileId })
        assertEquals(true, values.single().mobileBoolean)
        assertEquals("Sport", repository.getProfiles("current").single { it.profileId == 3 }.customName)
        assertNotNull(repository.renameProfile("current", 2, "Copy"))
        assertEquals(2, repository.switchToProfile("current", 2).first.activeProfileId)
    }

    @Test
    fun `renaming an inactive profile preserves its values and the active profile`() = runBlocking {
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        repository.saveMobileBoolean("current", mobileKey, true)
        repository.createProfileFromActive("current")
        val valuesBefore = repository.switchToProfile("current", 1).second
        repository.switchToProfile("current", 2)
        val renamed = repository.renameProfile("current", 1, "  Everyday  ")
        assertEquals("Everyday", renamed?.customName)
        assertEquals(false, renamed?.isActive)
        assertEquals(2, repository.ensureState("current").activeProfileId)
        assertEquals(valuesBefore, repository.switchToProfile("current", 1).second)
    }

    @Test
    fun `creating at capacity keeps three profiles and the existing active selection`() = runBlocking {
        repository.createProfileFromActive("current")
        repository.createProfileFromActive("current")
        repository.switchToProfile("current", 1)
        val before = repository.getProfiles("current")
        assertEquals(SettingsProfileState(3, 1), repository.createProfileFromActive("current").first)
        assertEquals(before, repository.getProfiles("current"))
    }
}

private class ProfileDao : SettingsProfileDao {
    private val profiles = mutableListOf<SettingsProfileEntity>()
    private val values = mutableListOf<SettingsProfileValueEntity>()

    override suspend fun getProfiles(serial: String): List<SettingsProfileEntity> =
        profiles.filter { it.serial_number == serial }.sortedBy { it.profile_id }

    override suspend fun getProfile(serial: String, profileId: Int): SettingsProfileEntity? =
        profiles.firstOrNull { it.serial_number == serial && it.profile_id == profileId }

    override suspend fun getActiveProfile(serial: String): SettingsProfileEntity? =
        profiles
            .filter { it.serial_number == serial && it.is_active }
            .minByOrNull { it.profile_id }

    override suspend fun upsertProfile(entity: SettingsProfileEntity) {
        profiles.removeAll {
            it.serial_number == entity.serial_number &&
                it.profile_id == entity.profile_id
        }
        profiles += entity
    }

    override suspend fun deleteProfiles(serial: String) {
        profiles.removeAll { it.serial_number == serial }
    }

    override suspend fun clearActive(serial: String, tsMs: Long) {
        profiles.replaceAll { entity ->
            if (entity.serial_number == serial) {
                entity.copy(is_active = false, updated_ts_ms = tsMs)
            } else {
                entity
            }
        }
    }

    override suspend fun getValues(serial: String, profileId: Int): List<SettingsProfileValueEntity> =
        values
            .filter { it.serial_number == serial && it.profile_id == profileId }
            .sortedWith(compareBy<SettingsProfileValueEntity> { it.target }.thenBy { it.setting_key })

    override suspend fun upsertValue(entity: SettingsProfileValueEntity) {
        values.removeAll {
            it.serial_number == entity.serial_number &&
                it.profile_id == entity.profile_id &&
                it.setting_key == entity.setting_key
        }
        values += entity
    }

    override suspend fun deleteValues(serial: String) {
        values.removeAll { it.serial_number == serial }
    }

    override suspend fun copyValues(
        serial: String,
        sourceProfileId: Int,
        targetProfileId: Int,
        tsMs: Long
    ) {
        values
            .filter { it.serial_number == serial && it.profile_id == sourceProfileId }
            .forEach { source ->
                upsertValue(
                    source.copy(
                        profile_id = targetProfileId,
                        updated_ts_ms = tsMs
                    )
                )
            }
    }
}
