package com.bailout.stickk.ubi4.data.local.repository

import com.bailout.stickk.ubi4.data.local.db.dao.SettingsProfileDao
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileEntity
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileValueEntity
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles.V3SettingsProfilesUpdates
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfiles
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.ParameterMetaV3
import com.bailout.stickk.ubi4.models.ble.WidgetKindV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.concurrent.Volatile

private const val MAX_SETTINGS_PROFILE_COUNT = 3
private const val BASE_PROFILE_ID = V3SettingsProfiles.BASE_PROFILE_ID
private const val BASE_SERVER_KEY = "BASE_SETTINGS"
const val SETTINGS_PROFILE_NAME_MAX_LENGTH = 30
private const val TARGET_BLE = "BLE"
private const val TARGET_MOBILE = "MOBILE"
private const val EMPTY_PROFILE_JSON = "{}"
private val settingsProfileServerJson = Json { encodeDefaults = true }

data class SettingsProfileState(
    val profileCount: Int,
    val activeProfileId: Int
)

data class SettingsProfileInfo(
    val profileId: Int,
    val customName: String?,
    val isActive: Boolean
)

data class SettingsProfileApplyValue(
    val target: String,
    val parameterInfo: ParameterInfo<Int, Int, Int, Int>?,
    val codecId: ParameterCodecIdV3?,
    val typedValue: ParameterTypedValueV3?,
    val mobileKey: String?,
    val mobileBoolean: Boolean?
)

class SettingsProfileRepository(
    private val dao: SettingsProfileDao
) {
    private val storageMutex = Mutex()

    private suspend fun <T> withStorageLock(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        storageMutex.withLock { block() }
    }

    suspend fun migrateSerial(
        oldSerial: String,
        newSerial: String
    ): SettingsProfileState = withStorageLock {
        val normalizedOld = normalizeSerial(oldSerial)
        val normalizedNew = normalizeSerial(newSerial) ?: return@withStorageLock SettingsProfileState(1, 1)
        if (normalizedOld == null || normalizedOld == normalizedNew) {
            return@withStorageLock ensureState(normalizedNew)
        }

        val oldProfiles = dao.getProfiles(normalizedOld)
        if (oldProfiles.isEmpty()) {
            return@withStorageLock ensureState(normalizedNew)
        }

        val ts = getTimeMillis()
        val oldActiveProfileId = oldProfiles.firstOrNull { it.is_active }?.profile_id
            ?: oldProfiles.first().profile_id
        oldProfiles.forEach { profile ->
            dao.upsertProfile(
                profile.copy(
                    serial_number = normalizedNew,
                    is_active = profile.profile_id == oldActiveProfileId,
                    updated_ts_ms = ts
                )
            )
            dao.getValues(normalizedOld, profile.profile_id).forEach { value ->
                dao.upsertValue(
                    value.copy(
                        serial_number = normalizedNew,
                        updated_ts_ms = ts
                    )
                )
            }
        }
        dao.deleteValues(normalizedOld)
        dao.deleteProfiles(normalizedOld)
        ensureState(normalizedNew)
    }

    suspend fun ensureState(serial: String): SettingsProfileState = withContext(Dispatchers.IO) {
        val normalizedSerial = normalizeSerial(serial) ?: return@withContext SettingsProfileState(1, 1)
        ensureDefaultProfile(normalizedSerial)
        val profiles = dao.getProfiles(normalizedSerial)
        val active = profiles.firstOrNull { it.is_active } ?: profiles.first()
        SettingsProfileState(
            profileCount = profiles.count { it.profile_id != BASE_PROFILE_ID }.coerceIn(0, MAX_SETTINGS_PROFILE_COUNT),
            activeProfileId = active.profile_id.coerceIn(BASE_PROFILE_ID, MAX_SETTINGS_PROFILE_COUNT)
        )
    }

    suspend fun getProfiles(serial: String): List<SettingsProfileInfo> = withContext(Dispatchers.IO) {
        val normalizedSerial = normalizeSerial(serial) ?: return@withContext emptyList()
        ensureDefaultProfile(normalizedSerial)
        dao.getProfiles(normalizedSerial).map { it.toProfileInfo() }
    }

    /** Adds the V3 baseline once; existing user profiles and their selection are preserved. */
    suspend fun ensureBaseProfile(serial: String) = withStorageLock {
        val normalizedSerial = normalizeSerial(serial) ?: return@withStorageLock
        if (dao.getProfile(normalizedSerial, BASE_PROFILE_ID) != null) return@withStorageLock
        val active = dao.getActiveProfile(normalizedSerial)
        val ts = getTimeMillis()
        dao.upsertProfile(SettingsProfileEntity(normalizedSerial, BASE_PROFILE_ID,
            "Базовые настройки", active == null, ts, ts))
        if (active != null) dao.copyValues(normalizedSerial, active.profile_id, BASE_PROFILE_ID, ts)
        snapshotCurrentBleValues(normalizedSerial, BASE_PROFILE_ID)
    }

    suspend fun deleteProfile(serial: String, profileId: Int): Pair<SettingsProfileState, List<SettingsProfileApplyValue>>? = withStorageLock {
        if (serial.isBlank() || profileId !in 1..MAX_SETTINGS_PROFILE_COUNT) return@withStorageLock null
        val profile = dao.getProfile(serial, profileId) ?: return@withStorageLock null
        if (profile.is_active) {
            checkNotNull(dao.getProfile(serial, BASE_PROFILE_ID)) { "Base settings are unavailable" }
            setActiveInternal(serial, BASE_PROFILE_ID, getTimeMillis())
        }
        dao.deleteProfileWithValues(serial, profileId)
        ensureState(serial) to if (profile.is_active) loadApplyValues(serial, BASE_PROFILE_ID) else emptyList()
    }

    suspend fun renameProfile(
        serial: String,
        profileId: Int,
        name: String
    ): SettingsProfileInfo? = withStorageLock {
        val normalizedSerial = normalizeSerial(serial) ?: return@withStorageLock null
        val normalizedName = name.trim()
        if (normalizedName.isEmpty() || normalizedName.length > SETTINGS_PROFILE_NAME_MAX_LENGTH) {
            return@withStorageLock null
        }
        if (profileId !in 1..MAX_SETTINGS_PROFILE_COUNT) return@withStorageLock null

        ensureDefaultProfile(normalizedSerial)
        val profile = dao.getProfile(normalizedSerial, profileId) ?: return@withStorageLock null
        if (profile.name == normalizedName) {
            return@withStorageLock profile.toProfileInfo()
        }

        profile.copy(
            name = normalizedName,
            updated_ts_ms = getTimeMillis()
        ).also { dao.upsertProfile(it) }.toProfileInfo()
    }

    suspend fun createProfileFromActive(serial: String): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        withStorageLock {
            val normalizedSerial = normalizeSerial(serial)
                ?: return@withStorageLock SettingsProfileState(1, 1) to emptyList()
            ensureDefaultProfile(normalizedSerial)
            val profiles = dao.getProfiles(normalizedSerial)
            val active = profiles.firstOrNull { it.is_active } ?: profiles.first()

            val userProfileCount = profiles.count { it.profile_id != BASE_PROFILE_ID }
            if (userProfileCount >= MAX_SETTINGS_PROFILE_COUNT) {
                val values = loadApplyValues(normalizedSerial, active.profile_id)
                return@withStorageLock SettingsProfileState(userProfileCount, active.profile_id) to values
            }

            if (active.profile_id != BASE_PROFILE_ID) snapshotCurrentBleValues(normalizedSerial, active.profile_id)
            val newProfileId = (1..MAX_SETTINGS_PROFILE_COUNT).first { id -> profiles.none { it.profile_id == id } }
            val ts = getTimeMillis()
            dao.upsertProfile(
                SettingsProfileEntity(
                    serial_number = normalizedSerial,
                    profile_id = newProfileId,
                    name = profileName(newProfileId),
                    is_active = false,
                    created_ts_ms = ts,
                    updated_ts_ms = ts
                )
            )
            dao.copyValues(
                serial = normalizedSerial,
                sourceProfileId = active.profile_id,
                targetProfileId = newProfileId,
                tsMs = ts
            )
            setActiveInternal(normalizedSerial, newProfileId, ts)
            SettingsProfileState(userProfileCount + 1, newProfileId) to loadApplyValues(normalizedSerial, newProfileId)
        }

    /** Copies only after explicit consent; changed capacity invalidates the original choice. */
    suspend fun copyBaseToPersonalProfile(
        serial: String,
        replacementProfileId: Int?,
    ): Pair<SettingsProfileState, List<SettingsProfileApplyValue>>? = withStorageLock {
        val normalizedSerial = normalizeSerial(serial) ?: return@withStorageLock null
        val profiles = dao.getProfiles(normalizedSerial)
        if (profiles.none { it.profile_id == BASE_PROFILE_ID && it.is_active }) return@withStorageLock null
        val freeId = (1..MAX_SETTINGS_PROFILE_COUNT).firstOrNull { id -> profiles.none { it.profile_id == id } }
        val targetId = if (replacementProfileId == null) freeId ?: return@withStorageLock null
        else {
            if (replacementProfileId != 1 || freeId != null) return@withStorageLock null
            replacementProfileId
        }
        val ts = getTimeMillis()
        val target = profiles.firstOrNull { it.profile_id == targetId }
            ?: SettingsProfileEntity(normalizedSerial, targetId, profileName(targetId), false, ts, ts)
        val values = dao.getValues(normalizedSerial, BASE_PROFILE_ID).map {
            it.copy(profile_id = targetId, updated_ts_ms = ts)
        }
        dao.replaceProfileWithValues(target.copy(updated_ts_ms = ts, engineer_revision = 0, engineer_upload_pending = false), values)
        setActiveInternal(normalizedSerial, targetId, ts)
        SettingsProfileState(profiles.count { it.profile_id != BASE_PROFILE_ID } + if (freeId != null) 1 else 0, targetId) to
            loadApplyValues(normalizedSerial, targetId)
    }

    suspend fun switchToProfile(serial: String, profileId: Int): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        withStorageLock {
            val normalizedSerial = normalizeSerial(serial)
                ?: return@withStorageLock SettingsProfileState(1, 1) to emptyList()
            ensureDefaultProfile(normalizedSerial)
            val safeProfileId = profileId.coerceIn(BASE_PROFILE_ID, MAX_SETTINGS_PROFILE_COUNT)
            val profile = dao.getProfile(normalizedSerial, safeProfileId)
                ?: return@withStorageLock ensureState(normalizedSerial) to emptyList()

            val active = dao.getActiveProfile(normalizedSerial)
            if (active?.profile_id != profile.profile_id) {
                setActiveInternal(normalizedSerial, profile.profile_id, getTimeMillis())
            }

            ensureState(normalizedSerial) to loadApplyValues(normalizedSerial, profile.profile_id)
        }

    suspend fun buildServerSettingsPayload(serial: String): String = withStorageLock {
        val normalizedSerial = normalizeSerial(serial) ?: return@withStorageLock settingsProfileServerJson.encodeToString(
            defaultServerProfiles()
        )
        ensureDefaultProfile(normalizedSerial)
        settingsProfileServerJson.encodeToString(
            defaultServerProfiles().toMutableMap().apply {
                if (dao.getProfile(normalizedSerial, BASE_PROFILE_ID) != null) {
                    this[BASE_SERVER_KEY] = buildServerProfileJson(normalizedSerial, BASE_PROFILE_ID)
                }
                for (profileId in 1..MAX_SETTINGS_PROFILE_COUNT) {
                    this["PROFILE$profileId"] = buildServerProfileJson(normalizedSerial, profileId)
                }
            }
        )
    }

    /** Seed an offline first connection before attempting any network request. */
    suspend fun captureDeviceSettings(serial: String) = withStorageLock {
        // A reconnect can already contain the engineer's new values. Wait for the
        // server before deciding which OLD local values need a personal backup.
        if (dao.getProfile(serial, BASE_PROFILE_ID) != null) return@withStorageLock
        val current = currentDeviceValues(serial) ?: return@withStorageLock
        if (comparableDeviceSettings(current).isEmpty()) return@withStorageLock
        val ts = getTimeMillis()
        val active = dao.getActiveProfile(serial)
        dao.replaceProfileWithValues(SettingsProfileEntity(serial, BASE_PROFILE_ID, "Базовые настройки", active == null, ts, ts),
            current.map { it.copy(profile_id = BASE_PROFILE_ID) })
        if (active != null && active.profile_id > 0 && dao.getValues(serial, active.profile_id).isEmpty() && active.name.isDefaultProfileName(active.profile_id))
            setActiveInternal(serial, BASE_PROFILE_ID, ts)
        V3SettingsProfilesUpdates.notifyChanged()
    }

    /** Network baseline wins; the connected prosthesis and personal profiles are never applied over. */
    suspend fun synchronizeBaseSettings(
        serial: String,
        payload: String,
        preserveCurrentSettings: Boolean = true,
    ): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> = withStorageLock {
        val remote = parseServerProfiles(payload, baseSettingsOnly = true).firstOrNull()
            ?: return@withStorageLock ensureState(serial) to emptyList()
        val localBase = dao.getProfile(serial, BASE_PROFILE_ID)
        if (localBase?.engineer_upload_pending == true) return@withStorageLock ensureState(serial) to emptyList()
        if ((localBase?.engineer_revision ?: 0) > remote.engineerRevision) {
            platformLog("SettingsProfileSync", "ignore older base serial=$serial local_revision=${localBase?.engineer_revision} server_revision=${remote.engineerRevision}")
            return@withStorageLock ensureState(serial) to emptyList()
        }
        val active = dao.getActiveProfile(serial)
        if (remote.engineerRevision > 0) {
            val previousBase = dao.getValues(serial, BASE_PROFILE_ID)
            if (preserveCurrentSettings && comparableDeviceSettings(previousBase).isNotEmpty() &&
                !sameDeviceSettings(previousBase, remote.values)) {
                val backupId = preservePersonalProfile(serial, previousBase, selectProfile = active?.profile_id == BASE_PROFILE_ID)
                platformLog("SettingsProfileSync", "backup previous base serial=$serial backup_profile=$backupId new_engineer_revision=${remote.engineerRevision}")
            }
        } else if (preserveCurrentSettings) {
            val current = currentDeviceValues(serial) ?: active?.let { dao.getValues(serial, it.profile_id) }.orEmpty()
            if (comparableDeviceSettings(current).isNotEmpty() && !sameDeviceSettings(current, remote.values)) {
                preservePersonalProfile(serial, current)
            }
        }
        val chosen = dao.getActiveProfile(serial)?.profile_id ?: BASE_PROFILE_ID
        dao.replaceProfileWithValues(SettingsProfileEntity(serial, BASE_PROFILE_ID, "Базовые настройки",
            chosen == BASE_PROFILE_ID, localBase?.created_ts_ms ?: getTimeMillis(), remote.updatedTsMs, remote.engineerRevision),
            remote.values.map { it.copy(serial_number = serial, profile_id = BASE_PROFILE_ID) })
        V3SettingsProfilesUpdates.notifyChanged()
        ensureState(serial) to emptyList()
    }

    private suspend fun currentDeviceValues(serial: String): List<SettingsProfileValueEntity>? {
        val serialInfo = PreferenceKeysUbi4.ParameterInfoRegistry.require(ConstantManagerUBI4.P_KEY_SET_SERIAL_NUMBER)
        val actual = (ParameterStoreV3.get(serialInfo) as? ParameterTypedValueV3.Text)?.value?.replace("\u0000", "")?.trim()
        if (actual != serial || serial.isBlank()) return null
        val active = dao.getActiveProfile(serial)
        val stored = active?.let { dao.getValues(serial, it.profile_id) }.orEmpty().associateBy { it.setting_key }.toMutableMap()
        var hasReadValues = false
        ParameterStoreV3.values.value.forEach { (key, typed) ->
            val info = key.toParameterInfo()
            val meta = metaFor(info) ?: return@forEach
            if (!isBleProfileSetting(meta)) return@forEach
            val entity = bleValueEntity(serial, active?.profile_id ?: BASE_PROFILE_ID, info, meta, typed) ?: return@forEach
            stored[entity.setting_key] = entity
            hasReadValues = true
        }
        return stored.values.toList().takeIf { hasReadValues }
    }

    private fun sameDeviceSettings(first: List<SettingsProfileValueEntity>, second: List<SettingsProfileValueEntity>): Boolean {
        val firstValues = comparableDeviceSettings(first)
        val secondValues = comparableDeviceSettings(second)
        val sharedKeys = firstValues.keys.intersect(secondValues.keys)
        // A startup read may contain more or fewer parameters than the saved snapshot.
        // Missing values are unknown, not edits; an entirely empty profile is not a match.
        if (sharedKeys.isEmpty()) return firstValues.isEmpty() && secondValues.isEmpty()
        return sharedKeys.all { firstValues[it] == secondValues[it] }
    }

    private fun comparableDeviceSettings(items: List<SettingsProfileValueEntity>): Map<String, JsonElement?> =
        items.mapNotNull { value ->
            if (value.target != TARGET_BLE) return@mapNotNull null
            val codec = runCatching { ParameterCodecIdV3.valueOf(value.codec_id) }.getOrNull()
            // The currently performed gesture changes during normal use, without editing settings.
            if (codec == ParameterCodecIdV3.CURRENT_GESTURE) return@mapNotNull null
            val typed = codec?.let { value.value_text?.let { text -> ParameterCodecRegistryV3.decodeFromSerialized(codec, text) } }
            val normalized = if (typed != null && codec != null) ParameterCodecRegistryV3.encodeToSerialized(codec, typed) else value.value_text
            // The cache holds the last gesture read, which need not be the same gesture as before.
            val key = if (typed is ParameterTypedValueV3.GestureSettings) "${value.setting_key}:gesture:${typed.value.gestureId}" else value.setting_key
            key to normalized?.toJsonElementOrString()
        }.toMap()

    private suspend fun preservePersonalProfile(
        serial: String,
        current: List<SettingsProfileValueEntity>,
        selectProfile: Boolean = true,
    ): Int {
        val profiles = dao.getProfiles(serial)
        val personal = profiles.filter { it.profile_id in 1..MAX_SETTINGS_PROFILE_COUNT }.sortedByDescending { it.is_active }
        val matching = personal.firstOrNull { sameDeviceSettings(current, dao.getValues(serial, it.profile_id)) }
        if (matching != null) {
            val existingKeys = dao.getValues(serial, matching.profile_id).map { it.setting_key }.toSet()
            val newlyRead = current.filter { it.setting_key !in existingKeys }
            newlyRead.forEach { dao.upsertValue(it.copy(serial_number = serial, profile_id = matching.profile_id)) }
            if (selectProfile && !matching.is_active) setActiveInternal(serial, matching.profile_id, getTimeMillis())
            platformLog("SettingsProfileSync", "reuse serial=$serial profile=${matching.profile_id} added_parameters=${newlyRead.size}")
            return matching.profile_id
        }
        val free = (1..MAX_SETTINGS_PROFILE_COUNT).firstOrNull { id ->
            val existing = personal.firstOrNull { it.profile_id == id }
            existing == null || existing.name.isDefaultProfileName(id) && dao.getValues(serial, id).isEmpty()
        } ?: 1
        val ts = getTimeMillis()
        val existing = personal.firstOrNull { it.profile_id == free }
        val profile = SettingsProfileEntity(serial, free, existing?.name ?: profileName(free), false, existing?.created_ts_ms ?: ts, ts)
        dao.replaceProfileWithValues(profile, current.map { it.copy(serial_number = serial, profile_id = free, updated_ts_ms = ts) })
        // Replacing the selected personal slot must not leave the serial without an active profile.
        if (selectProfile || existing?.is_active == true) setActiveInternal(serial, free, ts)
        platformLog("SettingsProfileSync", "preserve settings serial=$serial profile=$free replaced=${existing != null}")
        return free
    }

    suspend fun importServerSettingsPayload(
        serial: String,
        payload: String,
        preserveCurrentSettings: Boolean = false,
    ): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> = withStorageLock {
        val normalizedSerial = normalizeSerial(serial)
            ?: return@withStorageLock SettingsProfileState(1, 1) to emptyList()
        val profiles = parseServerProfiles(payload)
        if (profiles.isEmpty()) {
            return@withStorageLock ensureState(normalizedSerial) to emptyList()
        }

        // A previous upload may have been cancelled on restart. Merge by each setting's
        // timestamp so a stale server copy cannot erase edits saved on this phone.
        val engineerUpdate = profiles.firstOrNull { it.profileId == BASE_PROFILE_ID && it.engineerRevision > 0 }
        val preserve = preserveCurrentSettings && engineerUpdate != null
        if (preserve) ensureDefaultProfile(normalizedSerial)
        val localProfiles = dao.getProfiles(normalizedSerial).associateBy { it.profile_id }
        val localValues = localProfiles.keys.associateWith { dao.getValues(normalizedSerial, it) }
        val localActive = localProfiles.values.firstOrNull { it.is_active } ?: localProfiles.values.firstOrNull()
        val keepLocalSelection = preserve || localValues.values.any { it.isNotEmpty() } &&
            localActive != null && localProfiles.values.maxOf { it.updated_ts_ms } >= profiles.maxOf { it.updatedTsMs }
        val activeProfileId = if (keepLocalSelection) requireNotNull(localActive).profile_id else
            profiles.firstOrNull { it.isActive }?.profileId ?: profiles.first().profileId
        profiles.forEach { profile ->
            val local = localProfiles[profile.profileId]
            if (profile.profileId == BASE_PROFILE_ID && local != null && local.engineer_revision > 0 &&
                local.engineer_revision >= profile.engineerRevision) {
                dao.upsertProfile(local.copy(is_active = profile.profileId == activeProfileId))
                return@forEach
            }
            val replaceBase = profile.profileId == BASE_PROFILE_ID && profile.engineerRevision > (local?.engineer_revision ?: 0)
            // A newly generated empty default must not hide the server's custom profile name.
            val hasLocalEdits = local != null && (profile.profileId == BASE_PROFILE_ID ||
                localValues[profile.profileId].orEmpty().isNotEmpty() || !local.name.isDefaultProfileName(profile.profileId))
            val entity = if (!replaceBase && hasLocalEdits && local != null && local.updated_ts_ms >= profile.updatedTsMs) local.copy(
                    is_active = profile.profileId == activeProfileId,
                ) else SettingsProfileEntity(
                    serial_number = normalizedSerial,
                    profile_id = profile.profileId,
                    name = profile.name.ifBlank { profileName(profile.profileId) },
                    is_active = profile.profileId == activeProfileId,
                    created_ts_ms = profile.updatedTsMs,
                    updated_ts_ms = profile.updatedTsMs,
                    engineer_revision = profile.engineerRevision,
                )
            if (replaceBase) {
                dao.replaceProfileWithValues(entity, profile.values.map { it.copy(serial_number = normalizedSerial) })
                return@forEach
            }
            dao.upsertProfile(entity)
            val storedValues = localValues[profile.profileId].orEmpty().associateBy { it.setting_key }
            profile.values.forEach { value ->
                val stored = storedValues[value.setting_key]
                if (stored == null || value.updated_ts_ms > stored.updated_ts_ms) {
                    dao.upsertValue(value.copy(serial_number = normalizedSerial))
                }
            }
        }
        localProfiles.values.filter { local -> profiles.none { it.profileId == local.profile_id } }
            .forEach { local -> dao.upsertProfile(local.copy(is_active = local.profile_id == activeProfileId)) }

        val state = ensureState(normalizedSerial)
        // Import the baseline even when declined, but never apply it implicitly for a user.
        state to if (preserve) emptyList() else loadApplyValues(normalizedSerial, state.activeProfileId)
    }

    suspend fun saveBleValue(
        serial: String,
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        typedValue: ParameterTypedValueV3,
        canEditBaseSettings: Boolean = true,
    ) = withStorageLock {
        val normalizedSerial = normalizeSerial(serial) ?: return@withStorageLock
        val meta = metaFor(parameterInfo) ?: return@withStorageLock
        if (!isBleProfileSetting(meta)) return@withStorageLock
        ensureDefaultProfile(normalizedSerial)
        val activeProfileId = profileForWrite(normalizedSerial, canEditBaseSettings) ?: return@withStorageLock
        upsertBleValue(normalizedSerial, activeProfileId, parameterInfo, meta, typedValue, engineerEdit = activeProfileId == BASE_PROFILE_ID && canEditBaseSettings)
        if (activeProfileId == BASE_PROFILE_ID) V3SettingsProfilesUpdates.notifyChanged()
    }

    suspend fun saveMobileBoolean(
        serial: String,
        mobileKey: String,
        value: Boolean,
        canEditBaseSettings: Boolean = true,
    ) = withStorageLock {
        val normalizedSerial = normalizeSerial(serial) ?: return@withStorageLock
        if (mobileKey.isBlank()) return@withStorageLock
        ensureDefaultProfile(normalizedSerial)
        val activeProfileId = profileForWrite(normalizedSerial, canEditBaseSettings) ?: return@withStorageLock
        val ts = getTimeMillis()
        val entity = SettingsProfileValueEntity(
                serial_number = normalizedSerial,
                profile_id = activeProfileId,
                setting_key = "mobile:$mobileKey",
                target = TARGET_MOBILE,
                parameter_id = 0,
                data_code = 0,
                data_offset = 0,
                device_address = 0,
                codec_id = "",
                value_text = null,
                value_i1 = if (value) 1L else 0L,
                value_i2 = null,
                value_i3 = null,
                updated_ts_ms = ts
            )
        if (activeProfileId == BASE_PROFILE_ID && canEditBaseSettings) dao.upsertProfileValue(engineerEditProfile(normalizedSerial), entity)
        else dao.upsertValue(entity)
        if (activeProfileId == BASE_PROFILE_ID) V3SettingsProfilesUpdates.notifyChanged()
    }

    private suspend fun engineerEditProfile(serial: String): SettingsProfileEntity {
        val base = checkNotNull(dao.getProfile(serial, BASE_PROFILE_ID))
        val revision = maxOf(getTimeMillis(), base.engineer_revision + 1)
        return base.copy(engineer_revision = revision, updated_ts_ms = revision, engineer_upload_pending = true)
    }

    suspend fun getBaseSettingsRevision(serial: String): Long = withStorageLock {
        dao.getProfile(serial, BASE_PROFILE_ID)?.engineer_revision ?: 0
    }

    suspend fun getPendingEngineerRevision(serial: String): Long = withStorageLock {
        dao.getProfile(serial, BASE_PROFILE_ID)?.takeIf { it.engineer_upload_pending }?.engineer_revision ?: 0
    }

    suspend fun markEngineerBasePublished(serial: String, revision: Long) = withStorageLock {
        val base = dao.getProfile(serial, BASE_PROFILE_ID) ?: return@withStorageLock
        if (base.engineer_revision == revision && base.engineer_upload_pending) {
            dao.upsertProfile(base.copy(engineer_upload_pending = false))
            V3SettingsProfilesUpdates.notifyChanged()
        }
    }

    suspend fun acceptBaseSettings(serial: String, revision: Long): List<SettingsProfileApplyValue>? = withStorageLock {
        val base = dao.getProfile(serial, BASE_PROFILE_ID) ?: return@withStorageLock null
        if (revision <= 0 || base.engineer_revision != revision) return@withStorageLock null
        setActiveInternal(serial, BASE_PROFILE_ID, getTimeMillis())
        loadApplyValues(serial, BASE_PROFILE_ID)
    }

    private suspend fun profileForWrite(serial: String, canEditBaseSettings: Boolean): Int? {
        val activeId = dao.getActiveProfile(serial)?.profile_id ?: 1
        return activeId.takeIf { it != BASE_PROFILE_ID || canEditBaseSettings }
    }

    private suspend fun loadApplyValues(
        serial: String,
        profileId: Int
    ): List<SettingsProfileApplyValue> {
        return dao.getValues(serial, profileId).mapNotNull { entity ->
            when (entity.target) {
                TARGET_BLE -> {
                    val codecId = runCatching { ParameterCodecIdV3.valueOf(entity.codec_id) }.getOrNull()
                        ?: return@mapNotNull null
                    val typedValue = entity.value_text
                        ?.let { ParameterCodecRegistryV3.decodeFromSerialized(codecId, it) }
                        ?: return@mapNotNull null
                    SettingsProfileApplyValue(
                        target = TARGET_BLE,
                        parameterInfo = ParameterInfo(
                            entity.parameter_id,
                            entity.data_code,
                            entity.device_address,
                            entity.data_offset
                        ),
                        codecId = codecId,
                        typedValue = typedValue,
                        mobileKey = null,
                        mobileBoolean = null
                    )
                }
                TARGET_MOBILE -> SettingsProfileApplyValue(
                    target = TARGET_MOBILE,
                    parameterInfo = null,
                    codecId = null,
                    typedValue = null,
                    mobileKey = entity.setting_key.removePrefix("mobile:"),
                    mobileBoolean = entity.value_i1 == 1L
                )
                else -> null
            }
        }
    }

    private suspend fun snapshotCurrentBleValues(serial: String, profileId: Int) {
        ParameterStoreV3.values.value.forEach { (key, typedValue) ->
            val parameterInfo = key.toParameterInfo()
            val meta = metaFor(parameterInfo) ?: return@forEach
            if (!isBleProfileSetting(meta)) return@forEach
            upsertBleValue(serial, profileId, parameterInfo, meta, typedValue)
        }
    }

    private suspend fun upsertBleValue(
        serial: String,
        profileId: Int,
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        meta: ParameterMetaV3,
        typedValue: ParameterTypedValueV3,
        engineerEdit: Boolean = false,
    ) {
        val entity = bleValueEntity(serial, profileId, parameterInfo, meta, typedValue) ?: return
        if (engineerEdit) dao.upsertProfileValue(engineerEditProfile(serial), entity)
        else dao.upsertValue(entity)
    }

    private fun bleValueEntity(
        serial: String, profileId: Int, parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        meta: ParameterMetaV3, typedValue: ParameterTypedValueV3,
    ): SettingsProfileValueEntity? {
        val serialized = ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, typedValue)
            ?: return null
        val storageInfo = when (meta.codecId) {
            ParameterCodecIdV3.EMG_GAINS,
            ParameterCodecIdV3.THRESHOLDS -> parameterInfo.copy(dataOffsets = 0)
            else -> parameterInfo
        }
        val ts = getTimeMillis()
        return SettingsProfileValueEntity(
                serial_number = serial,
                profile_id = profileId,
                setting_key = storageInfo.toProfileKey(),
                target = TARGET_BLE,
                parameter_id = storageInfo.parameterID,
                data_code = storageInfo.dataCode,
                data_offset = storageInfo.dataOffsets,
                device_address = storageInfo.deviceAddress,
                codec_id = meta.codecId.name,
                value_text = serialized,
                value_i1 = null,
                value_i2 = null,
                value_i3 = null,
                updated_ts_ms = ts
            )
    }

    private suspend fun ensureDefaultProfile(serial: String) {
        val profiles = dao.getProfiles(serial)
        if (profiles.isNotEmpty()) return
        val ts = getTimeMillis()
        dao.upsertProfile(
            SettingsProfileEntity(
                serial_number = serial,
                profile_id = 1,
                name = profileName(1),
                is_active = true,
                created_ts_ms = ts,
                updated_ts_ms = ts
            )
        )
    }

    private suspend fun setActiveInternal(serial: String, profileId: Int, tsMs: Long) {
        dao.clearActive(serial, tsMs)
        val existing = dao.getProfile(serial, profileId) ?: return
        dao.upsertProfile(existing.copy(is_active = true, updated_ts_ms = tsMs))
    }

    private fun isBleProfileSetting(meta: ParameterMetaV3): Boolean {
        if (meta.parameterInfo == PreferenceKeysUbi4.ParameterInfoRegistry.require(ConstantManagerUBI4.P_KEY_SETTINGS_PROFILE)) {
            return false
        }
        return when (meta.widgetKind) {
            WidgetKindV3.PLOT,
            WidgetKindV3.SLIDER,
            WidgetKindV3.TOGGLE_SLIDER,
            WidgetKindV3.SPINNER,
            WidgetKindV3.GESTURES,
            WidgetKindV3.SWITCHER -> meta.codecId != ParameterCodecIdV3.NONE &&
                meta.widgetKind != WidgetKindV3.TEXT_INPUT &&
                meta.parameterInfo != PreferenceKeysUbi4.ParameterInfoRegistry.require(ConstantManagerUBI4.P_KEY_DEVICE_ROLE)
            else -> false
        }
    }

    private fun metaFor(parameterInfo: ParameterInfo<Int, Int, Int, Int>): ParameterMetaV3? {
        return PreferenceKeysUbi4.ParameterInfoRegistry.getMeta(parameterInfo)
            ?: PreferenceKeysUbi4.ParameterInfoRegistry.getMeta(parameterInfo.copy(dataOffsets = 0))
    }

    private fun normalizeSerial(serial: String?): String? =
        serial?.trim()?.takeIf { it.isNotBlank() }

    private fun profileName(profileId: Int): String = "Профиль №$profileId"

    private fun SettingsProfileEntity.toProfileInfo(): SettingsProfileInfo = SettingsProfileInfo(
        profileId = profile_id,
        customName = name.takeUnless { it.isDefaultProfileName(profile_id) },
        isActive = is_active
    )

    private fun String.isDefaultProfileName(profileId: Int): Boolean {
        val normalized = trim()
        return normalized == "Профиль №$profileId" || normalized == "Profile #$profileId"
    }

    private suspend fun buildServerProfileJson(serial: String, profileId: Int): String {
        val profile = dao.getProfile(serial, profileId) ?: return EMPTY_PROFILE_JSON
        val values = dao.getValues(serial, profileId)
        val profileJson = buildJsonObject {
            put("profile_id", profile.profile_id)
            put("name", profile.name)
            put("is_active", profile.is_active)
            put("updated_ts_ms", profile.updated_ts_ms)
            if (profileId == BASE_PROFILE_ID) {
                put("changed_by_service_engineer", profile.engineer_revision > 0)
                put("engineer_revision", profile.engineer_revision)
            }
            put(
                "settings",
                buildJsonArray {
                    values.forEach { value ->
                        add(value.toServerJson())
                    }
                }
            )
        }
        return settingsProfileServerJson.encodeToString(JsonObject.serializer(), profileJson)
    }

    private fun SettingsProfileValueEntity.toServerJson(): JsonObject = buildJsonObject {
        put("target", target)
        put("setting_key", setting_key)
        if (target == TARGET_BLE) {
            put("parameter_id", parameter_id)
            put("data_code", data_code)
            put("data_offset", data_offset)
            put("device_address", device_address)
            put("codec_id", codec_id)
        }
        put("value", toServerValueJson())
        put("updated_ts_ms", updated_ts_ms)
    }

    private fun SettingsProfileValueEntity.toServerValueJson(): JsonElement {
        return when (target) {
            TARGET_MOBILE -> JsonPrimitive(value_i1 == 1L)
            TARGET_BLE -> value_text?.toJsonElementOrString() ?: JsonNull
            else -> value_text?.toJsonElementOrString() ?: JsonNull
        }
    }

    private fun String.toJsonElementOrString(): JsonElement =
        runCatching { settingsProfileServerJson.parseToJsonElement(this) }
            .getOrElse { JsonPrimitive(this) }

    private fun defaultServerProfiles(): Map<String, String> =
        mapOf(
            "PROFILE1" to EMPTY_PROFILE_JSON,
            "PROFILE2" to EMPTY_PROFILE_JSON,
            "PROFILE3" to EMPTY_PROFILE_JSON
        )

    private fun parseServerProfiles(payload: String, baseSettingsOnly: Boolean = false): List<ServerSettingsProfile> {
        val root = runCatching { settingsProfileServerJson.parseToJsonElement(payload) }.getOrNull()
            ?: return emptyList()
        val profilesRoot = root.extractProfilesRoot() ?: return emptyList()

        val ids = if (baseSettingsOnly) BASE_PROFILE_ID..BASE_PROFILE_ID else BASE_PROFILE_ID..MAX_SETTINGS_PROFILE_COUNT
        return ids.mapNotNull { profileId ->
            val key = if (profileId == BASE_PROFILE_ID) BASE_SERVER_KEY else "PROFILE$profileId"
            val profilePayload = profilesRoot[key] ?: return@mapNotNull null
            val profileJson = profilePayload.toJsonObjectOrNull() ?: return@mapNotNull null
            if (profileJson.isEmpty()) return@mapNotNull null

            val safeProfileId = if (profileId == BASE_PROFILE_ID) BASE_PROFILE_ID else
                profileJson["profile_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?.coerceIn(1, MAX_SETTINGS_PROFILE_COUNT) ?: profileId
            val updatedTsMs = profileJson["updated_ts_ms"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: getTimeMillis()
            val values = profileJson["settings"]?.runCatchingJsonArray()
                ?.mapNotNull { it.toProfileValueEntityOrNull(safeProfileId) }
                .orEmpty()

            ServerSettingsProfile(
                profileId = safeProfileId,
                name = profileJson["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                isActive = profileJson["is_active"]?.jsonPrimitive?.booleanOrNull == true,
                updatedTsMs = updatedTsMs,
                engineerRevision = if (profileId == BASE_PROFILE_ID && profileJson["changed_by_service_engineer"]?.jsonPrimitive?.booleanOrNull == true)
                    profileJson["engineer_revision"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.coerceAtLeast(0) ?: 0 else 0,
                values = values
            )
        }
    }

    private fun JsonElement.extractProfilesRoot(): JsonObject? {
        val rootObject = runCatching { jsonObject }.getOrNull() ?: return null
        val settings = rootObject["settings"]
        return when {
            settings != null -> settings.toJsonObjectOrNull()
            rootObject.keys.any { it.startsWith("PROFILE") || it == BASE_SERVER_KEY } -> rootObject
            else -> null
        }
    }

    private fun JsonElement.toJsonObjectOrNull(): JsonObject? {
        return when (this) {
            is JsonObject -> this
            is JsonPrimitive -> contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { settingsProfileServerJson.parseToJsonElement(it).jsonObject }.getOrNull() }
            else -> null
        }
    }

    private fun JsonElement.runCatchingJsonArray() =
        runCatching { jsonArray }.getOrNull()

    private fun JsonElement.toProfileValueEntityOrNull(profileId: Int): SettingsProfileValueEntity? {
        val item = runCatching { jsonObject }.getOrNull() ?: return null
        val target = item["target"]?.jsonPrimitive?.contentOrNull ?: return null
        val settingKey = item["setting_key"]?.jsonPrimitive?.contentOrNull ?: return null
        val updatedTsMs = item["updated_ts_ms"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: getTimeMillis()

        return when (target) {
            TARGET_BLE -> SettingsProfileValueEntity(
                serial_number = "",
                profile_id = profileId,
                setting_key = settingKey,
                target = TARGET_BLE,
                parameter_id = item["parameter_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null,
                data_code = item["data_code"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null,
                data_offset = item["data_offset"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                device_address = item["device_address"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                codec_id = item["codec_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                value_text = item["value"]?.let { settingsProfileServerJson.encodeToString(JsonElement.serializer(), it) },
                value_i1 = null,
                value_i2 = null,
                value_i3 = null,
                updated_ts_ms = updatedTsMs
            )
            TARGET_MOBILE -> SettingsProfileValueEntity(
                serial_number = "",
                profile_id = profileId,
                setting_key = settingKey,
                target = TARGET_MOBILE,
                parameter_id = 0,
                data_code = 0,
                data_offset = 0,
                device_address = 0,
                codec_id = "",
                value_text = null,
                value_i1 = if (item["value"]?.jsonPrimitive?.booleanOrNull == true) 1L else 0L,
                value_i2 = null,
                value_i3 = null,
                updated_ts_ms = updatedTsMs
            )
            else -> null
        }
    }
}

private data class ServerSettingsProfile(
    val profileId: Int,
    val name: String,
    val isActive: Boolean,
    val updatedTsMs: Long,
    val engineerRevision: Long = 0,
    val values: List<SettingsProfileValueEntity>
)

object SettingsProfileRepositoryProvider {
    @Volatile
    private var repository: SettingsProfileRepository? = null

    fun init(dao: SettingsProfileDao): SettingsProfileRepository {
        return (repository ?: SettingsProfileRepository(dao)).also { repository = it }
    }

    fun get(): SettingsProfileRepository =
        checkNotNull(repository) { "SettingsProfileRepositoryProvider not init" }

    fun getOrNull(): SettingsProfileRepository? = repository
}

object SettingsProfileManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingWrites = MutableStateFlow<Map<Job, String>>(emptyMap())

    /** Waits for writes already submitted for this device; cancelling the waiter does not cancel saving. */
    suspend fun awaitPendingWrites(serial: String) {
        pendingWrites.value.filterValues { it == serial }.keys.toList().joinAll()
    }

    private fun launchProfileWrite(serial: String, write: suspend () -> Unit) {
        val job = scope.launch(start = CoroutineStart.LAZY) { write() }
        pendingWrites.update { it + (job to serial) }
        job.invokeOnCompletion { pendingWrites.update { it - job } }
        job.start()
    }

    @Volatile
    private var currentSerial: String = ""

    fun setCurrentSerial(serial: String?) {
        val normalized = serial?.trim().orEmpty()
        if (normalized.isBlank() || normalized == currentSerial) return
        if (normalized.startsWith(ConstantManagerUBI4.DEVICE_NAME_PREFIX)) {
            platformLog("SettingsProfileManager", "skip device-name value as serial=$normalized")
            return
        }
        val previousSerial = currentSerial
        currentSerial = normalized
        scope.launch {
            runCatching {
                SettingsProfileRepositoryProvider.get().migrateSerial(previousSerial, normalized)
            }
                .onFailure { platformLog("SettingsProfileManager", "migrateSerial failed: ${it.message}") }
        }
    }

    fun serial(): String = currentSerial

    suspend fun getState(): SettingsProfileState =
        SettingsProfileRepositoryProvider.getOrNull()?.ensureState(currentSerial)
            ?: SettingsProfileState(1, 1)

    suspend fun getProfiles(): List<SettingsProfileInfo> =
        SettingsProfileRepositoryProvider.getOrNull()?.getProfiles(currentSerial).orEmpty()

    suspend fun renameProfile(profileId: Int, name: String): SettingsProfileInfo? =
        SettingsProfileRepositoryProvider.getOrNull()?.renameProfile(currentSerial, profileId, name)

    suspend fun createProfileFromActive(): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        SettingsProfileRepositoryProvider.getOrNull()?.createProfileFromActive(currentSerial)
            ?: (SettingsProfileState(1, 1) to emptyList())

    suspend fun switchToProfile(profileId: Int): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        SettingsProfileRepositoryProvider.getOrNull()?.switchToProfile(currentSerial, profileId)
            ?: (SettingsProfileState(1, 1) to emptyList())

    suspend fun buildServerSettingsPayload(): String? {
        val serial = currentSerial
        if (serial.isBlank()) return null
        return SettingsProfileRepositoryProvider.getOrNull()?.buildServerSettingsPayload(serial)
    }

    fun saveBleValue(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        typedValue: ParameterTypedValueV3
    ) {
        val serial = serial()
        if (serial.isBlank()) return
        val canEditBase = !UiState.isInterfaceV3Activated || UiState.isServiceEngineerRole.value
        launchProfileWrite(serial) {
            runCatching {
                SettingsProfileRepositoryProvider.get().saveBleValue(serial, parameterInfo, typedValue, canEditBase)
            }.onFailure {
                platformLog("SettingsProfileManager", "saveBleValue failed: ${it.message}")
            }
        }
    }

    fun saveMobileBoolean(mobileKey: String, value: Boolean) {
        val serial = serial()
        if (serial.isBlank()) return
        val canEditBase = !UiState.isInterfaceV3Activated || UiState.isServiceEngineerRole.value
        launchProfileWrite(serial) {
            runCatching {
                SettingsProfileRepositoryProvider.get().saveMobileBoolean(serial, mobileKey, value, canEditBase)
            }.onFailure {
                platformLog("SettingsProfileManager", "saveMobileBoolean failed: ${it.message}")
            }
        }
    }
}

private fun ParameterInfo<Int, Int, Int, Int>.toProfileKey(): String =
    "${deviceAddress}:${parameterID}:${dataCode}:${dataOffsets}"

private fun ParameterStoreKeyV3.toParameterInfo(): ParameterInfo<Int, Int, Int, Int> =
    ParameterInfo(parameterID, dataCode, deviceAddress, 0)
