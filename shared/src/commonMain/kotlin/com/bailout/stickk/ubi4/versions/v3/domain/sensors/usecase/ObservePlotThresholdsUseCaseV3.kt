package com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase

import kotlinx.coroutines.flow.Flow
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository

class ObservePlotThresholdsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(): Flow<V3PlotThresholds?> = repository.observeThresholds()
}
