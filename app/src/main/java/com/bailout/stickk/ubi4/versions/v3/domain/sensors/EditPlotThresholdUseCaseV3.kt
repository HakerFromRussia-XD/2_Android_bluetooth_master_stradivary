package com.bailout.stickk.ubi4.versions.v3.domain.sensors

class EditPlotThresholdUseCaseV3 {
    operator fun invoke(current: V3PlotThresholds, threshold: V3PlotThreshold, value: Int): V3PlotThresholds =
        when (threshold) {
            V3PlotThreshold.OPEN -> current.copy(open = value.coerceIn(0, 255))
            V3PlotThreshold.CLOSE -> current.copy(close = value.coerceIn(0, 255))
        }
}
