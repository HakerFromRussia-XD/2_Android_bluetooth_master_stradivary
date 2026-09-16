package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

sealed interface V3SliderSettingsAction {
    data object ViewAttached : V3SliderSettingsAction
    data object ViewDetached : V3SliderSettingsAction
    data class SliderAction(val action: V3SliderAction) : V3SliderSettingsAction
}
