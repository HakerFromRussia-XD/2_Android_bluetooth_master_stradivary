package com.bailout.stickk.ubi4.versions.v3.presentation.advancedsettings

sealed interface V3AdvancedSettingsAction {
    data object ViewAttached : V3AdvancedSettingsAction
    data object ViewDetached : V3AdvancedSettingsAction
    data class SliderValueChanged(val parameterKey: String, val value: Int) : V3AdvancedSettingsAction
    data class SliderChangeCommitted(val parameterKey: String, val value: Int) : V3AdvancedSettingsAction
    data class SliderStepClicked(val parameterKey: String, val step: Int) : V3AdvancedSettingsAction
}
