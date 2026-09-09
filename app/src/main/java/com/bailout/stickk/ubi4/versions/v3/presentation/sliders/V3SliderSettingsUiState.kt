package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

data class SliderUiStateV3(
    val parameterKey: String,
    val value: Int?,
    val allowedRange: IntRange,
    val isEnabled: Boolean,
    val animateValueChange: Boolean = false,
)

data class V3SliderSettingsUiState(val sliders: Map<String, SliderUiStateV3>)
