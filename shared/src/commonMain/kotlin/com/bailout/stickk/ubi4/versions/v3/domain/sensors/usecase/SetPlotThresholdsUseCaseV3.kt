package com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository

class SetPlotThresholdsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(thresholds: V3PlotThresholds) {
        require(thresholds.open in 0..255 && thresholds.close in 0..255)
        if (repository.interactionEnabled.value) repository.setThresholds(thresholds)
    }
}
