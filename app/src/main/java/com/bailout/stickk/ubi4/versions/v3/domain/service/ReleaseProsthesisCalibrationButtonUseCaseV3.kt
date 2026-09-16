package com.bailout.stickk.ubi4.versions.v3.domain.service

class ReleaseProsthesisCalibrationButtonUseCaseV3(private val repository: V3ProsthesisCalibrationRepository) {
    operator fun invoke(deviceAddress: String) = repository.releaseCalibrationButton(deviceAddress)
}
