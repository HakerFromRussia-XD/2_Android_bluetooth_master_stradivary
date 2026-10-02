package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

object V3SettingsProfileNameRules {
    const val MAX_LENGTH = 30

    fun isValid(name: String): Boolean = name.trim().length in 1..MAX_LENGTH
}
