package com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders

sealed interface V3ToggleSliderAction {
    val parameterKey: String
    data class ToggleSliderValueChanged(override val parameterKey: String, val timeTenths: Int) : V3ToggleSliderAction
    data class ToggleSliderChangeCommitted(override val parameterKey: String, val timeTenths: Int) : V3ToggleSliderAction
    data class ToggleSliderStepClicked(override val parameterKey: String, val step: Int) : V3ToggleSliderAction
    data class ToggleSliderEnabledChanged(override val parameterKey: String, val enabled: Boolean) : V3ToggleSliderAction
}
