package com.bailout.stickk.ubi4.versions.v3.domain.service

class StartProsthesisCalibrationUseCaseV3(private val repository: V3ProsthesisCalibrationRepository) {
    operator fun invoke(deviceAddress: String): Boolean =
        repository.interactionEnabled.value && repository.startCalibration(deviceAddress)
}
