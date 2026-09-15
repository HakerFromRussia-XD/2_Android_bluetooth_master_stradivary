package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction

import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction

import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction

sealed interface V3SensorsAction {
    data object ViewAttached : V3SensorsAction
    data object ViewDetached : V3SensorsAction
    data object RefreshRequested : V3SensorsAction
    data class ButtonsAction(val action: V3SensorsButtonsAction) : V3SensorsAction
    data class PlotAction(val action: V3PlotAction) : V3SensorsAction
    data class SliderAction(val action: V3SliderAction) : V3SensorsAction
}
