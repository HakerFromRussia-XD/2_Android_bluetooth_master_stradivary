package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.StateFlow

class ObservePlotAvailabilityUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
