package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import kotlinx.coroutines.flow.StateFlow
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3ProsthesisCalibrationRepository

class ObserveProsthesisCalibrationAvailabilityUseCaseV3(private val repository: V3ProsthesisCalibrationRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
