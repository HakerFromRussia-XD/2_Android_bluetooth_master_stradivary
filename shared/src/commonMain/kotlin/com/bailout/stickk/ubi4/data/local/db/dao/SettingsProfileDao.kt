package com.bailout.stickk.ubi4.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileEntity
import com.bailout.stickk.ubi4.data.local.db.entity.SettingsProfileValueEntity

@Dao
interface SettingsProfileDao {
    @Query("SELECT * FROM settings_profiles WHERE serial_number = :serial ORDER BY profile_id ASC")
    suspend fun getProfiles(serial: String): List<SettingsProfileEntity>

    @Query(
        """
        SELECT * FROM settings_profiles
        WHERE serial_number = :serial AND profile_id = :profileId
        LIMIT 1
        """
    )
    suspend fun getProfile(serial: String, profileId: Int): SettingsProfileEntity?

    @Query(
        """
        SELECT * FROM settings_profiles
        WHERE serial_number = :serial AND is_active = 1
        ORDER BY profile_id ASC
        LIMIT 1
        """
    )
    suspend fun getActiveProfile(serial: String): SettingsProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: SettingsProfileEntity)

    @Query("DELETE FROM settings_profiles WHERE serial_number = :serial")
    suspend fun deleteProfiles(serial: String)

    @Query("DELETE FROM settings_profiles WHERE serial_number = :serial AND profile_id = :profileId")
    suspend fun deleteProfile(serial: String, profileId: Int)

    @Transaction
    suspend fun deleteProfileWithValues(serial: String, profileId: Int) {
        deleteProfileValues(serial, profileId)
        deleteProfile(serial, profileId)
    }

    @Query("UPDATE settings_profiles SET is_active = 0, updated_ts_ms = :tsMs WHERE serial_number = :serial")
    suspend fun clearActive(serial: String, tsMs: Long)

    @Query(
        """
        SELECT * FROM settings_profile_values
        WHERE serial_number = :serial AND profile_id = :profileId
        ORDER BY target ASC, setting_key ASC
        """
    )
    suspend fun getValues(serial: String, profileId: Int): List<SettingsProfileValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertValue(entity: SettingsProfileValueEntity)

    @Transaction
    suspend fun upsertProfileValue(profile: SettingsProfileEntity, value: SettingsProfileValueEntity) {
        upsertValue(value)
        upsertProfile(profile)
    }

    @Transaction
    suspend fun replaceProfileWithValues(profile: SettingsProfileEntity, values: List<SettingsProfileValueEntity>) {
        deleteProfileValues(profile.serial_number, profile.profile_id)
        upsertProfile(profile)
        values.forEach { upsertValue(it) }
    }

    @Transaction
    suspend fun applyServerProfiles(
        serial: String,
        profiles: List<SettingsProfileEntity>,
        values: List<SettingsProfileValueEntity>,
        replacedIds: List<Int>,
        deletedIds: List<Int>,
    ) {
        deletedIds.forEach { deleteProfileWithValues(serial, it) }
        replacedIds.forEach { deleteProfileValues(serial, it) }
        profiles.forEach { upsertProfile(it) }
        values.forEach { upsertValue(it) }
    }

    @Query("DELETE FROM settings_profile_values WHERE serial_number = :serial")
    suspend fun deleteValues(serial: String)

    @Query("DELETE FROM settings_profile_values WHERE serial_number = :serial AND profile_id = :profileId")
    suspend fun deleteProfileValues(serial: String, profileId: Int)

    @Query(
        """
        INSERT OR REPLACE INTO settings_profile_values (
            serial_number, profile_id, setting_key, target,
            parameter_id, data_code, data_offset, device_address, codec_id,
            value_text, value_i1, value_i2, value_i3, updated_ts_ms
        )
        SELECT
            serial_number, :targetProfileId, setting_key, target,
            parameter_id, data_code, data_offset, device_address, codec_id,
            value_text, value_i1, value_i2, value_i3, :tsMs
        FROM settings_profile_values
        WHERE serial_number = :serial AND profile_id = :sourceProfileId
        """
    )
    suspend fun copyValues(
        serial: String,
        sourceProfileId: Int,
        targetProfileId: Int,
        tsMs: Long
    )
}
