package com.bailout.stickk.ubi4.versions.v3.domain.settings

data class V3SliderSettingsSnapshot(
    val values: Map<String, Int?>,
    val isInteractionEnabled: Boolean,
)

sealed interface V3SliderSettingsChange {
    data class ValueChanged(val parameterKey: String, val value: Int?) : V3SliderSettingsChange
    data class InteractionChanged(val enabled: Boolean) : V3SliderSettingsChange
}
