package com.bailout.stickk.ubi4.versions.v3.domain.settings

data class V3SpinnerSettingsSnapshot(
    val values: Map<String, Int?>,
    val isInteractionEnabled: Boolean,
)

sealed interface V3SpinnerSettingsChange {
    data class ValueChanged(val parameterKey: String, val value: Int?) : V3SpinnerSettingsChange
    data class InteractionChanged(val enabled: Boolean) : V3SpinnerSettingsChange
}
