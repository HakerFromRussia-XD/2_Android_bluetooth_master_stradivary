package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

data class V3SettingsProfile(val profileId: Int, val customName: String?)

data class V3SettingsProfiles(val profiles: List<V3SettingsProfile>, val activeProfileId: Int) {
    val canCreate: Boolean get() = profiles.size < MAX_PROFILES

    fun normalized(): V3SettingsProfiles {
        val sorted = profiles.sortedBy { it.profileId }.take(MAX_PROFILES)
            .ifEmpty { listOf(V3SettingsProfile(1, null)) }
        val activeId = activeProfileId.takeIf { id -> sorted.any { it.profileId == id } } ?: sorted.first().profileId
        return V3SettingsProfiles(sorted, activeId)
    }

    companion object {
        const val MAX_PROFILES = 3
    }
}
