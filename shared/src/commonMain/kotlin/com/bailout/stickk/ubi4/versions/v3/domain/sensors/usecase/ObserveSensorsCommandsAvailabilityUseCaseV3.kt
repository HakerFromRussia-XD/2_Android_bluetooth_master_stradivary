package com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase

import kotlinx.coroutines.flow.StateFlow
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsCommandsRepository

class ObserveSensorsCommandsAvailabilityUseCaseV3(private val repository: V3SensorsCommandsRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
