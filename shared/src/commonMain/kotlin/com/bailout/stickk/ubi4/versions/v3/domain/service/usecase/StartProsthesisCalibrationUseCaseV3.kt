package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3ProsthesisCalibrationRepository

class StartProsthesisCalibrationUseCaseV3(
    private val repository: V3ProsthesisCalibrationRepository,
    private val requireInteractionEnabled: Boolean = true,
) {
    operator fun invoke(deviceAddress: String): Boolean =
        (!requireInteractionEnabled || repository.interactionEnabled.value) && repository.startCalibration(deviceAddress)
}
