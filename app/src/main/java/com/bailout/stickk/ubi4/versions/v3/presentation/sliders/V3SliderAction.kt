package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

sealed interface V3SliderAction {
    val parameterKey: String

    data class SliderValueChanged(override val parameterKey: String, val value: Int) : V3SliderAction
    data class SliderChangeCommitted(override val parameterKey: String, val value: Int) : V3SliderAction
    data class SliderStepClicked(override val parameterKey: String, val step: Int) : V3SliderAction
}
