package com.bailout.stickk.ubi4.versions.v3.domain.device

class UpdateDeviceInteractionUseCaseV3(private val repository: V3DeviceInteractionRepository) {
    operator fun invoke(enabled: Boolean): Boolean = repository.setInteractionEnabled(enabled)
}
