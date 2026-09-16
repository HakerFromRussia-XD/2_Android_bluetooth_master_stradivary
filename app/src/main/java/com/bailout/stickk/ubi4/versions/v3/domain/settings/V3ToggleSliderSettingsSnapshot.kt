package com.bailout.stickk.ubi4.versions.v3.domain.settings

data class V3ToggleSliderSettingsSnapshot(
    val values: Map<String, V3ToggleSliderValue?>,
    val isInteractionEnabled: Boolean,
)

sealed interface V3ToggleSliderSettingsChange {
    data class ValueChanged(val parameterKey: String, val value: V3ToggleSliderValue?) : V3ToggleSliderSettingsChange
    data class InteractionChanged(val enabled: Boolean) : V3ToggleSliderSettingsChange
}
