package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.flow.Flow

interface V3SettingsProfilesRepository {
    val updates: Flow<Unit>
    fun currentSerial(): String
    suspend fun getProfiles(serial: String): V3SettingsProfiles
    suspend fun selectProfile(serial: String, profileId: Int)
    suspend fun createProfile(serial: String)
    suspend fun renameProfile(serial: String, profileId: Int, name: String)

    /** Keeps the existing local selection cache in sync; does not apply a profile or send BLE. */
    fun cacheSelection(serial: String, profiles: V3SettingsProfiles)
}
