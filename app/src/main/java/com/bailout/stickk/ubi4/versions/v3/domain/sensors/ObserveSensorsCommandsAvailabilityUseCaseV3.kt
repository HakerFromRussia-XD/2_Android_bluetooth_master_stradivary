package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.StateFlow

class ObserveSensorsCommandsAvailabilityUseCaseV3(private val repository: V3SensorsCommandsRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
