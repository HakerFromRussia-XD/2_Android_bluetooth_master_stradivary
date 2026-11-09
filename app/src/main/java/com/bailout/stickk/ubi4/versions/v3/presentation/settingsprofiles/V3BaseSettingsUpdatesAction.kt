package com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles

import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3BaseSettingsOffer

sealed interface V3BaseSettingsUpdatesAction {
    data object ViewCreated : V3BaseSettingsUpdatesAction
    data object ViewResumed : V3BaseSettingsUpdatesAction
    data object ViewDestroyed : V3BaseSettingsUpdatesAction
    data class Respond(val offer: V3BaseSettingsOffer, val accept: Boolean) : V3BaseSettingsUpdatesAction
}
