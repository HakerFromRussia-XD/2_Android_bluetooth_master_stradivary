package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import kotlinx.coroutines.flow.StateFlow
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository

class ObserveServiceEngineerAccessUseCaseV3(private val repository: V3DeviceRoleRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.serviceEngineerAccess
}
