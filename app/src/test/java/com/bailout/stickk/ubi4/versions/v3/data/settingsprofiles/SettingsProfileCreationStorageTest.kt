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
import kotlinx.serialization.json.*
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

    @Test fun `deleting active personal profile selects baseline and returns its values`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(20)))
        repository.createProfileFromActive("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)), false)
        val result = requireNotNull(repository.deleteProfile("current", 1))
        assertEquals(0, result.first.activeProfileId)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(20)), result.second.single().typedValue)
        assertEquals(listOf(0), repository.getProfiles("current").map { it.profileId })
        assertNull(repository.deleteProfile("current", 0))
        assertNull(repository.deleteProfile("current", 1))
    }

    @Test fun `deleting inactive profile keeps selection and reuses its slot without stale values`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(20)))
        repeat(3) { repository.createProfileFromActive("current") }
        val result = requireNotNull(repository.deleteProfile("current", 2))
        assertEquals(3, result.first.activeProfileId)
        assertTrue(result.second.isEmpty())
        assertEquals(listOf(0, 1, 3), repository.getProfiles("current").map { it.profileId })
        assertEquals(2, repository.createProfileFromActive("current").first.activeProfileId)
    }

    @Test fun `base profile is added once without renumbering or renaming existing profiles`() = runBlocking {
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        repository.renameProfile("current", 1, "Everyday")
        repository.ensureBaseProfile("current")
        repository.ensureBaseProfile("current")
        assertEquals(listOf(0, 1), repository.getProfiles("current").map { it.profileId })
        assertEquals("Everyday", repository.getProfiles("current").single { it.profileId == 1 }.customName)
        assertEquals(1, repository.ensureState("current").activeProfileId)
        assertNull(repository.renameProfile("current", 0, "Renamed"))
    }

    @Test fun `user cannot edit baseline or overwrite personal profile by attempting an edit`() = runBlocking {
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        repository.saveMobileBoolean("current", mobileKey, true)
        repository.ensureBaseProfile("current")
        repository.renameProfile("current", 1, "My name")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        repository.saveMobileBoolean("current", "stale_only_in_profile_one", true)
        repository.switchToProfile("current", 0)
        val profiles = repository.getProfiles("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(55)), canEditBaseSettings = false)
        repository.saveMobileBoolean("current", mobileKey, false, canEditBaseSettings = false)
        assertEquals(0, repository.ensureState("current").activeProfileId)
        assertEquals(profiles, repository.getProfiles("current"))
        val first = repository.switchToProfile("current", 1).second
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(80)), first.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(true, first.single { it.mobileKey == mobileKey }.mobileBoolean)
        assertTrue(first.any { it.mobileKey == "stale_only_in_profile_one" })
        assertEquals("My name", repository.getProfiles("current").single { it.profileId == 1 }.customName)
        val base = repository.switchToProfile("current", 0).second
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), base.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(true, base.single { it.mobileKey == mobileKey }.mobileBoolean)
    }

    @Test fun `confirmed baseline copy fills the first free slot and preserves all existing values`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        repository.saveMobileBoolean("current", mobileKey, true)
        repository.createProfileFromActive("current")
        repository.renameProfile("current", 1, "Personal")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        repository.switchToProfile("current", 0)
        val revision = repository.getBaseSettingsRevision("current")
        val (state, copied) = requireNotNull(repository.copyBaseToPersonalProfile("current", null))
        assertEquals(SettingsProfileState(2, 2), state)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), copied.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(true, copied.single { it.mobileKey == mobileKey }.mobileBoolean)
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(55)), canEditBaseSettings = false)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(55)), repository.switchToProfile("current", 2).second.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(80)), repository.switchToProfile("current", 1).second.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), repository.switchToProfile("current", 0).second.single { it.parameterInfo == speedInfo }.typedValue)
        assertEquals(revision, repository.getBaseSettingsRevision("current"))
    }

    @Test fun `full capacity copy replaces only profile one parameters while keeping its name`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        repeat(3) { index ->
            repository.createProfileFromActive("current")
            repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(70 + index)))
        }
        repository.renameProfile("current", 1, "Work")
        repository.switchToProfile("current", 1)
        repository.saveMobileBoolean("current", "stale", true)
        repository.switchToProfile("current", 0)
        assertNull(repository.copyBaseToPersonalProfile("current", null))
        val (state, copied) = requireNotNull(repository.copyBaseToPersonalProfile("current", 1))
        assertEquals(SettingsProfileState(3, 1), state)
        assertEquals("Work", repository.getProfiles("current").single { it.profileId == 1 }.customName)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), copied.single().typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(71)), repository.switchToProfile("current", 2).second.single().typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(72)), repository.switchToProfile("current", 3).second.single().typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), repository.switchToProfile("current", 0).second.single().typedValue)
    }

    @Test fun `copy rejects stale replacement consent and never replaces a personal active profile`() = runBlocking {
        repository.ensureBaseProfile("current")
        assertNull(repository.copyBaseToPersonalProfile("current", 1))
        assertNull(repository.copyBaseToPersonalProfile("current", 0))
        assertNull(repository.copyBaseToPersonalProfile("current", 2))
        assertNull(repository.copyBaseToPersonalProfile("missing", null))
        assertEquals(listOf(0), repository.getProfiles("current").map { it.profileId })
        repeat(3) { repository.createProfileFromActive("current") }
        assertNull(repository.copyBaseToPersonalProfile("current", 1))
        assertEquals(3, repository.ensureState("current").activeProfileId)
        repository.switchToProfile("current", 0)
        repository.deleteProfile("current", 2)
        assertNull(repository.copyBaseToPersonalProfile("current", 1))
        assertEquals(2, requireNotNull(repository.copyBaseToPersonalProfile("current", null)).first.activeProfileId)
    }

    @Test fun `user mobile edit on baseline creates no profile and changes no value`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveMobileBoolean("current", mobileKey, true, canEditBaseSettings = false)
        assertEquals(0, repository.ensureState("current").activeProfileId)
        assertTrue(repository.switchToProfile("current", 0).second.isEmpty())
        assertEquals(listOf(0), repository.getProfiles("current").map { it.profileId })
    }

    @Test fun `engineer edits baseline without switching and cannot rename it`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(70)), canEditBaseSettings = true)
        repository.saveMobileBoolean("current", mobileKey, false, canEditBaseSettings = true)
        assertEquals(0, repository.ensureState("current").activeProfileId)
        assertEquals(listOf(0), repository.getProfiles("current").map { it.profileId })
        assertNull(repository.renameProfile("current", 0, "Engineer baseline"))
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(70)), repository.switchToProfile("current", 0).second.single { it.parameterInfo == speedInfo }.typedValue)
    }

    @Test fun `baseline plus three user profiles survive server roundtrip without reducing capacity`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(12)))
        repeat(3) { repository.createProfileFromActive("current") }
        assertEquals(listOf(0, 1, 2, 3), repository.getProfiles("current").map { it.profileId })
        assertEquals(3, repository.ensureState("current").profileCount)
        assertEquals(3, repository.createProfileFromActive("current").first.profileCount)
        repository.switchToProfile("current", 0)
        val restored = SettingsProfileRepository(ProfileDao())
        restored.importServerSettingsPayload("current", repository.buildServerSettingsPayload("current"))
        assertEquals(listOf(0, 1, 2, 3), restored.getProfiles("current").map { it.profileId })
        assertEquals(0, restored.ensureState("current").activeProfileId)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(12)), restored.switchToProfile("current", 0).second.single { it.parameterInfo == speedInfo }.typedValue)
    }

    @Test fun `creating from baseline does not snapshot optimistic UI edits into baseline`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(12)))
        ParameterStoreV3.put(speedInfo, ParameterTypedValueV3.Slider(SliderV3(99)))
        val copy = repository.createProfileFromActive("current").second
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(12)), copy.single().typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(12)), repository.switchToProfile("current", 0).second.single().typedValue)
    }

    @Test fun `engineer update imports baseline without selecting it or replacing personal settings`() = runBlocking {
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        repository.renameProfile("current", 1, "Personal")
        val engineer = SettingsProfileRepository(ProfileDao())
        engineer.ensureBaseProfile("current")
        engineer.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        val revision = engineer.getBaseSettingsRevision("current")
        assertTrue(revision > 0)
        val payload = engineer.buildServerSettingsPayload("current")
        val root = Json.parseToJsonElement(payload).jsonObject
        val base = Json.parseToJsonElement(root.getValue("BASE_SETTINGS").jsonPrimitive.content).jsonObject
        assertTrue(base.getValue("changed_by_service_engineer").jsonPrimitive.boolean)
        assertEquals(revision, base.getValue("engineer_revision").jsonPrimitive.long)
        val result = repository.importServerSettingsPayload("current", payload, preserveCurrentSettings = true)
        assertTrue(result.second.isEmpty())
        assertEquals(1, result.first.activeProfileId)
        assertEquals("Personal", repository.getProfiles("current").single { it.profileId == 1 }.customName)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(80)), repository.switchToProfile("current", 1).second.single().typedValue)
        assertEquals(revision, repository.getBaseSettingsRevision("current"))
        val accepted = repository.acceptBaseSettings("current", revision)
        assertEquals(0, repository.ensureState("current").activeProfileId)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), accepted?.single()?.typedValue)
    }

    @Test fun `skip leaves currently selected baseline unapplied on every subsequent download`() = runBlocking {
        repository.ensureBaseProfile("current")
        val engineer = SettingsProfileRepository(ProfileDao())
        engineer.ensureBaseProfile("current")
        engineer.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        val payload = engineer.buildServerSettingsPayload("current")
        repeat(2) {
            val result = repository.importServerSettingsPayload("current", payload, preserveCurrentSettings = true)
            assertEquals(0, result.first.activeProfileId)
            assertTrue(result.second.isEmpty())
        }
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), repository.switchToProfile("current", 0).second.single().typedValue)
    }

    @Test fun `old response cannot accept a newer baseline and user copy does not change engineer revision`() = runBlocking {
        repository.ensureBaseProfile("current")
        assertEquals(0, repository.getBaseSettingsRevision("current"))
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(20)))
        val first = repository.getBaseSettingsRevision("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(30)))
        val second = repository.getBaseSettingsRevision("current")
        assertTrue(second > first)
        assertNull(repository.acceptBaseSettings("current", first))
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(40)), canEditBaseSettings = false)
        assertEquals(second, repository.getBaseSettingsRevision("current"))
    }

    @Test fun `engineer sends baseline and all local personal profiles`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(20)))
        repeat(3) { repository.createProfileFromActive("current") }
        val result = Json.parseToJsonElement(repository.buildServerSettingsPayload("current")).jsonObject
        assertEquals(setOf("BASE_SETTINGS", "PROFILE1", "PROFILE2", "PROFILE3"), result.keys)
        assertTrue(result.containsKey("BASE_SETTINGS"))
    }

    @Test fun `pending engineer upload persists until the exact revision is published`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(20)))
        val previous = repository.getPendingEngineerRevision("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(30)))
        val current = repository.getPendingEngineerRevision("current")
        assertTrue(current > previous)
        repository.markEngineerBasePublished("current", previous)
        assertEquals(current, repository.getPendingEngineerRevision("current"))
        repository.markEngineerBasePublished("current", current)
        assertEquals(0, repository.getPendingEngineerRevision("current"))
        val downloaded = SettingsProfileRepository(ProfileDao())
        downloaded.importServerSettingsPayload("current", repository.buildServerSettingsPayload("current"), true)
        assertEquals(0, downloaded.getPendingEngineerRevision("current"))
        assertEquals(current, downloaded.getBaseSettingsRevision("current"))
    }

    @Test fun `fresh phone imports personal profiles while an engineer baseline awaits acceptance`() = runBlocking {
        repository.ensureBaseProfile("current")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(42)))
        repository.createProfileFromActive("current")
        repository.renameProfile("current", 1, "Personal")
        repository.saveBleValue("current", speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)), canEditBaseSettings = false)
        val fresh = SettingsProfileRepository(ProfileDao())
        val result = fresh.importServerSettingsPayload("current", repository.buildServerSettingsPayload("current"), true)
        assertTrue(result.second.isEmpty())
        assertEquals("Personal", fresh.getProfiles("current").single { it.profileId == 1 }.customName)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(80)), fresh.switchToProfile("current", 1).second.single().typedValue)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), fresh.switchToProfile("current", 0).second.single().typedValue)
    }
}

internal class ProfileDao : SettingsProfileDao {
    private val profiles = mutableListOf<SettingsProfileEntity>()
    private val values = mutableListOf<SettingsProfileValueEntity>()
    override suspend fun deleteProfile(serial: String, profileId: Int) {
        profiles.removeAll { it.serial_number == serial && it.profile_id == profileId }
    }

    override suspend fun deleteProfileValues(serial: String, profileId: Int) {
        values.removeAll { it.serial_number == serial && it.profile_id == profileId }
    }

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
