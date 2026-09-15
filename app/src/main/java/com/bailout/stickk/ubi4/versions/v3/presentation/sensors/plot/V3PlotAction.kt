package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThreshold

sealed interface V3PlotAction {
    data class ThresholdValueChanged(val threshold: V3PlotThreshold, val value: Int) : V3PlotAction
    data object ThresholdChangeCommitted : V3PlotAction
}
