package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

class ObserveProsthesisCalibrationAvailabilityUseCaseV3(private val repository: V3ProsthesisCalibrationRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
