package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.Flow

class ObservePlotThresholdsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(): Flow<V3PlotThresholds?> = repository.observeThresholds()
}
