package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class GetSettingsProfilesUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String): V3SettingsProfiles {
        return repository.getProfiles(serial).normalized()
    }
}
