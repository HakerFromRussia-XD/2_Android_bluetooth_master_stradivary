package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommandsRepository

class GetInitializationCommandsUseCaseV3(private val repository: V3InitializationCommandsRepository) {
    operator fun invoke() = repository.getCommands()
}

class ObserveInitializationCommandsAvailabilityUseCaseV3(private val repository: V3InitializationCommandsRepository) {
    operator fun invoke() = repository.availability
}

class SendInitializationCommandUseCaseV3(private val repository: V3InitializationCommandsRepository) {
    operator fun invoke(deviceAddress: String, command: V3InitializationCommand) = repository.enqueue(deviceAddress, command)
}
