package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class CopyBaseSettingsToPersonalProfileUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    suspend operator fun invoke(serial: String, replacementProfileId: Int?) {
        require(serial.isNotBlank()) { "Device serial is unavailable" }
        require(replacementProfileId == null || replacementProfileId == 1) { "Only profile one can be replaced" }
        repository.copyBaseToPersonalProfile(serial, replacementProfileId)
    }
}
