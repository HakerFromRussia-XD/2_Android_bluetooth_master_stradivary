package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class SelectSettingsProfileUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String, profileId: Int) {
        require(serial.isNotBlank()) { "Device serial is unavailable" }
        require(profileId in 1..V3SettingsProfiles.MAX_PROFILES) { "Invalid settings profile ID" }
        val profiles = repository.getProfiles(serial)
        require(profiles.profiles.any { it.profileId == profileId }) { "Settings profile no longer exists" }
        // Re-selecting the active profile still applies its stored values, as before.
        repository.selectProfile(serial, profileId)
    }
}
