package com.bailout.stickk.ubi4.versions.v3.domain.sensors

class StopProsthesisMovementUseCaseV3(private val repository: V3SensorsCommandsRepository) {
    operator fun invoke(deviceAddress: String) = repository.stopMovement(deviceAddress)
}
