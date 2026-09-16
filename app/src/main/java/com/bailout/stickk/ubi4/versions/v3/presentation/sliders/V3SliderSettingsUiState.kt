package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderSettingsSnapshot

data class SliderUiStateV3(
    val parameterKey: String,
    val value: Int?,
    val allowedRange: IntRange,
    val isEnabled: Boolean,
    val animateValueChange: Boolean = false,
)

data class V3SliderSettingsUiState(val sliders: Map<String, SliderUiStateV3>)

internal fun V3SliderSettingsSnapshot.toSliderUiState() = V3SliderSettingsUiState(
    sliders = values.mapValues { (key, value) ->
        SliderUiStateV3(key, value, V3SliderSettingsRules.allowedRange(key), isEnabled = false)
    },
)
