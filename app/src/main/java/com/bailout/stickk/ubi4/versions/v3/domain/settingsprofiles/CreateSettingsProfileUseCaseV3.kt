package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class CreateSettingsProfileUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String) {
        require(serial.isNotBlank()) { "Device serial is unavailable" }
        if (!repository.getProfiles(serial).canCreate) return
        repository.createProfile(serial)
    }
}
