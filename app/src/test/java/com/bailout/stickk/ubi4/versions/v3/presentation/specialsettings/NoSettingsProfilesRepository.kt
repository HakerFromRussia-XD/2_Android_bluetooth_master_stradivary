package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfiles
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfilesRepository
import kotlinx.coroutines.flow.emptyFlow

/** Other widget fixtures have no profile selector and must never request its data. */
internal object NoSettingsProfilesRepository : V3SettingsProfilesRepository {
    override val updates = emptyFlow<Unit>()
    override fun currentSerial() = ""
    override suspend fun getProfiles(serial: String): V3SettingsProfiles = error("No profile selector in this fixture")
    override suspend fun renameProfile(serial: String, profileId: Int, name: String) = error("Profile rename is not expected")
    override suspend fun createProfile(serial: String) = error("Profile creation is not expected")
    override suspend fun selectProfile(serial: String, profileId: Int) = error("No profile selector in this fixture")
    override fun cacheSelection(serial: String, profiles: V3SettingsProfiles) = error("No profile selector in this fixture")
}
