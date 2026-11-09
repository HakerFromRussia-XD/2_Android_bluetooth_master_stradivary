package com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles

import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3BaseSettingsOffer

data class V3BaseSettingsUpdatesUiState(
    val offer: V3BaseSettingsOffer? = null,
    val isApplying: Boolean = false,
    val failed: Boolean = false,
)
