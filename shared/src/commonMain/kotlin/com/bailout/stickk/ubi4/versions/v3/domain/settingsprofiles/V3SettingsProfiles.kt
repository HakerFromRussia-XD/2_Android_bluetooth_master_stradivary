package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

data class V3SettingsProfile(val profileId: Int, val customName: String?, val hasEngineerDraft: Boolean = false) {
    val isBase: Boolean get() = profileId == V3SettingsProfiles.BASE_PROFILE_ID
    val canRename: Boolean get() = !isBase
}

data class V3SettingsProfiles(val profiles: List<V3SettingsProfile>, val activeProfileId: Int) {
    val canCreate: Boolean get() = profiles.count { !it.isBase } < MAX_PROFILES

    fun normalized(): V3SettingsProfiles {
        val sorted = profiles.filter { it.profileId in BASE_PROFILE_ID..MAX_PROFILES }
            .distinctBy { it.profileId }.sortedBy { it.profileId }
            .ifEmpty { listOf(V3SettingsProfile(1, null)) }
        val activeId = activeProfileId.takeIf { id -> sorted.any { it.profileId == id } } ?: sorted.first().profileId
        return V3SettingsProfiles(sorted, activeId)
    }

    companion object {
        const val BASE_PROFILE_ID = 0
        const val MAX_PROFILES = 3
    }
}
