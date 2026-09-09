package com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue

data class ToggleSliderUiStateV3(
    val value: V3ToggleSliderValue,
    val allowedTimeRange: IntRange,
    val isInteractionEnabled: Boolean = false,
    val animateValueChange: Boolean = false,
) {
    val isSliderEnabled: Boolean get() = value.isEnabled && isInteractionEnabled
}
