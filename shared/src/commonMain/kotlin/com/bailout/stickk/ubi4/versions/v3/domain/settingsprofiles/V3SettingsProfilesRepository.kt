package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.flow.Flow

interface V3SettingsProfilesRepository {
    val updates: Flow<Unit>
    fun currentSerial(): String
    suspend fun getProfiles(serial: String): V3SettingsProfiles
    suspend fun selectProfile(serial: String, profileId: Int)
    suspend fun createProfile(serial: String)
    suspend fun copyBaseToPersonalProfile(serial: String, replacementProfileId: Int?)
    suspend fun renameProfile(serial: String, profileId: Int, name: String)
    suspend fun deleteProfile(serial: String, profileId: Int)
    suspend fun sendEngineerUpdate(serial: String, profileId: Int) {
        error("Sending engineer updates is unavailable")
    }

    /** Keeps the existing local selection cache in sync; does not apply a profile or send BLE. */
    fun cacheSelection(serial: String, profiles: V3SettingsProfiles)
}

class SendEngineerSettingsProfileUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String, profileId: Int) {
        require(serial.isNotBlank() && profileId in 1..V3SettingsProfiles.MAX_PROFILES)
        repository.sendEngineerUpdate(serial, profileId)
    }
}
