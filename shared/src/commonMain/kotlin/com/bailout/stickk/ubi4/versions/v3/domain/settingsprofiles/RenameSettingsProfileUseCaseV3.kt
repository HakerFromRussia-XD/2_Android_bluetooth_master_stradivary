package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class RenameSettingsProfileUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String, profileId: Int, name: String) {
        require(serial.isNotBlank()) { "Device serial is unavailable" }
        require(profileId in 1..V3SettingsProfiles.MAX_PROFILES) { "Invalid settings profile ID" }
        require(V3SettingsProfileNameRules.isValid(name)) { "Invalid settings profile name" }
        val profile = repository.getProfiles(serial).profiles.firstOrNull { it.profileId == profileId }
        requireNotNull(profile) { "Settings profile no longer exists" }
        val normalizedName = name.trim()
        if (profile.customName == normalizedName) return
        repository.renameProfile(serial, profileId, normalizedName)
    }
}
