package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

class GetSettingsProfilesDeviceSerialUseCaseV3(private val repository: V3SettingsProfilesRepository) {
    operator fun invoke(): String = repository.currentSerial()
}
