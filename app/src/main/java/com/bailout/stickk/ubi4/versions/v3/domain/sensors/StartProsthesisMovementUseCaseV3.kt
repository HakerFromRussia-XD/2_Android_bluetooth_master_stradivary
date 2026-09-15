package com.bailout.stickk.ubi4.versions.v3.domain.sensors

class StartProsthesisMovementUseCaseV3(private val repository: V3SensorsCommandsRepository) {
    operator fun invoke(deviceAddress: String, movement: V3ProsthesisMovement): Boolean =
        repository.interactionEnabled.value && repository.startMovement(deviceAddress, movement)
}
