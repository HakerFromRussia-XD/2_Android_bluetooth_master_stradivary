package com.bailout.stickk.ubi4.versions.v3.domain.device

class ObserveDeviceSessionChangesUseCaseV3(private val repository: V3DeviceSessionRepository) {
    operator fun invoke() = repository.updates
}
