package com.bailout.stickk.ubi4.data.local.repository

import com.bailout.stickk.ubi4.data.local.db.dao.SettingsProfileDao
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileEntity
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileValueEntity
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.ParameterMetaV3
import com.bailout.stickk.ubi4.models.ble.WidgetKindV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

private const val MAX_SETTINGS_PROFILE_COUNT = 3
private const val TARGET_BLE = "BLE"
private const val TARGET_MOBILE = "MOBILE"

data class SettingsProfileState(
    val profileCount: Int,
    val activeProfileId: Int
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
    suspend fun ensureState(serial: String): SettingsProfileState = withContext(Dispatchers.IO) {
        val normalizedSerial = normalizeSerial(serial) ?: return@withContext SettingsProfileState(1, 1)
        ensureDefaultProfile(normalizedSerial)
        val profiles = dao.getProfiles(normalizedSerial)
        val active = profiles.firstOrNull { it.is_active } ?: profiles.first()
        SettingsProfileState(
            profileCount = profiles.size.coerceIn(1, MAX_SETTINGS_PROFILE_COUNT),
            activeProfileId = active.profile_id.coerceIn(1, MAX_SETTINGS_PROFILE_COUNT)
        )
    }

    suspend fun createProfileFromActive(serial: String): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        withContext(Dispatchers.IO) {
            val normalizedSerial = normalizeSerial(serial)
                ?: return@withContext SettingsProfileState(1, 1) to emptyList()
            ensureDefaultProfile(normalizedSerial)
            val profiles = dao.getProfiles(normalizedSerial)
            val active = profiles.firstOrNull { it.is_active } ?: profiles.first()

            if (profiles.size >= MAX_SETTINGS_PROFILE_COUNT) {
                val values = loadApplyValues(normalizedSerial, active.profile_id)
                return@withContext SettingsProfileState(profiles.size, active.profile_id) to values
            }

            snapshotCurrentBleValues(normalizedSerial, active.profile_id)
            val newProfileId = (profiles.maxOfOrNull { it.profile_id } ?: 0) + 1
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
            SettingsProfileState(profiles.size + 1, newProfileId) to loadApplyValues(normalizedSerial, newProfileId)
        }

    suspend fun switchToProfile(serial: String, profileId: Int): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        withContext(Dispatchers.IO) {
            val normalizedSerial = normalizeSerial(serial)
                ?: return@withContext SettingsProfileState(1, 1) to emptyList()
            ensureDefaultProfile(normalizedSerial)
            val safeProfileId = profileId.coerceIn(1, MAX_SETTINGS_PROFILE_COUNT)
            val profile = dao.getProfile(normalizedSerial, safeProfileId)
                ?: return@withContext ensureState(normalizedSerial) to emptyList()

            val active = dao.getActiveProfile(normalizedSerial)
            if (active?.profile_id != profile.profile_id) {
                setActiveInternal(normalizedSerial, profile.profile_id, getTimeMillis())
            }

            ensureState(normalizedSerial) to loadApplyValues(normalizedSerial, profile.profile_id)
        }

    suspend fun saveBleValue(
        serial: String,
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        typedValue: ParameterTypedValueV3
    ) = withContext(Dispatchers.IO) {
        val normalizedSerial = normalizeSerial(serial) ?: return@withContext
        val meta = metaFor(parameterInfo) ?: return@withContext
        if (!isBleProfileSetting(meta)) return@withContext
        ensureDefaultProfile(normalizedSerial)
        val activeProfileId = dao.getActiveProfile(normalizedSerial)?.profile_id ?: 1
        upsertBleValue(normalizedSerial, activeProfileId, parameterInfo, meta, typedValue)
    }

    suspend fun saveMobileBoolean(
        serial: String,
        mobileKey: String,
        value: Boolean
    ) = withContext(Dispatchers.IO) {
        val normalizedSerial = normalizeSerial(serial) ?: return@withContext
        if (mobileKey.isBlank()) return@withContext
        ensureDefaultProfile(normalizedSerial)
        val activeProfileId = dao.getActiveProfile(normalizedSerial)?.profile_id ?: 1
        val ts = getTimeMillis()
        dao.upsertValue(
            SettingsProfileValueEntity(
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
        )
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
            val meta = PreferenceKeysUbi4.ParameterInfoRegistry.getMeta(parameterInfo) ?: return@forEach
            if (!isBleProfileSetting(meta)) return@forEach
            upsertBleValue(serial, profileId, parameterInfo, meta, typedValue)
        }
    }

    private suspend fun upsertBleValue(
        serial: String,
        profileId: Int,
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        meta: ParameterMetaV3,
        typedValue: ParameterTypedValueV3
    ) {
        val serialized = ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, typedValue)
            ?: return
        val storageInfo = when (meta.codecId) {
            ParameterCodecIdV3.EMG_GAINS,
            ParameterCodecIdV3.THRESHOLDS -> parameterInfo.copy(dataOffsets = 0)
            else -> parameterInfo
        }
        val ts = getTimeMillis()
        dao.upsertValue(
            SettingsProfileValueEntity(
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
}

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

    @Volatile
    private var currentSerial: String = ""

    fun setCurrentSerial(serial: String?) {
        val normalized = serial?.trim().orEmpty()
        if (normalized.isBlank() || normalized == currentSerial) return
        currentSerial = normalized
        scope.launch {
            runCatching { SettingsProfileRepositoryProvider.get().ensureState(normalized) }
                .onFailure { platformLog("SettingsProfileManager", "ensureState failed: ${it.message}") }
        }
    }

    fun serial(): String = currentSerial

    suspend fun getState(): SettingsProfileState =
        SettingsProfileRepositoryProvider.getOrNull()?.ensureState(currentSerial)
            ?: SettingsProfileState(1, 1)

    suspend fun createProfileFromActive(): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        SettingsProfileRepositoryProvider.getOrNull()?.createProfileFromActive(currentSerial)
            ?: (SettingsProfileState(1, 1) to emptyList())

    suspend fun switchToProfile(profileId: Int): Pair<SettingsProfileState, List<SettingsProfileApplyValue>> =
        SettingsProfileRepositoryProvider.getOrNull()?.switchToProfile(currentSerial, profileId)
            ?: (SettingsProfileState(1, 1) to emptyList())

    fun saveBleValue(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        typedValue: ParameterTypedValueV3
    ) {
        val serial = currentSerial
        if (serial.isBlank()) return
        scope.launch {
            runCatching {
                SettingsProfileRepositoryProvider.get().saveBleValue(serial, parameterInfo, typedValue)
            }.onFailure {
                platformLog("SettingsProfileManager", "saveBleValue failed: ${it.message}")
            }
        }
    }

    fun saveMobileBoolean(mobileKey: String, value: Boolean) {
        val serial = currentSerial
        if (serial.isBlank()) return
        scope.launch {
            runCatching {
                SettingsProfileRepositoryProvider.get().saveMobileBoolean(serial, mobileKey, value)
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
