package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Loads the current device's profiles and synchronizes its local selection before presentation. */
class LoadSettingsProfilesUseCaseV3(
    private val getProfiles: GetSettingsProfilesUseCaseV3,
    private val repository: V3SettingsProfilesRepository,
) {
    suspend operator fun invoke(serial: String): V3SettingsProfiles {
        val profiles = getProfiles(serial)
        currentCoroutineContext().ensureActive()
        if (serial == repository.currentSerial()) repository.cacheSelection(serial, profiles)
        return profiles
    }
}
