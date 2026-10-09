package com.bailout.stickk.ubi4.versions.v3.domain.firmware

// Sequencing, compatibility and retry rules remain in the shared firmware engine.
class ObserveUserFirmwareUpdatesUseCaseV3(private val repository: V3UserFirmwareRepository) {
    operator fun invoke() = repository.observe()
}

class RefreshUserFirmwareEnvironmentUseCaseV3(private val repository: V3UserFirmwareRepository) {
    operator fun invoke() = repository.refreshEnvironment()
}

class StartUserFirmwareUpdateUseCaseV3(private val repository: V3UserFirmwareActionsRepository) {
    operator fun invoke() = repository.startUpdate()
}

class PostponeUserFirmwareUpdateUseCaseV3(private val repository: V3UserFirmwareActionsRepository) {
    operator fun invoke() = repository.postponeUpdate()
}

class AcknowledgeUserFirmwareCompletionUseCaseV3(private val repository: V3UserFirmwareActionsRepository) {
    operator fun invoke() = repository.acknowledgeCompletion()
}

class CloseUserFirmwareUpdatesUseCaseV3(private val repository: V3UserFirmwareRepository) {
    operator fun invoke() = repository.close()
}
