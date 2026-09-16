package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

class ObserveDeviceRoleAvailabilityUseCaseV3(private val repository: V3DeviceRoleRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.interactionEnabled
}
