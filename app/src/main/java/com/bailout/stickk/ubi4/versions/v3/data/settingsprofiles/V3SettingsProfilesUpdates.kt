package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Android invalidation bridge until the existing profile mutations move behind the repository. */
object V3SettingsProfilesUpdates {
    private val revision = MutableStateFlow(0L)
    val updates = revision.map { Unit }

    fun notifyChanged() { revision.update { it + 1 } }
}
