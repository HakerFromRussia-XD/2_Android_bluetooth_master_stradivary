package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class DeleteSettingsProfileUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String, profileId: Int) {
        require(serial.isNotBlank()) { "Device serial is unavailable" }
        require(profileId in 1..V3SettingsProfiles.MAX_PROFILES) { "Base settings cannot be deleted" }
        repository.deleteProfile(serial, profileId)
    }
}
