package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository

/** Restores local access without writing preferences or sending a device command. */
class RestoreDeviceRoleUseCaseV3(private val repository: V3DeviceRoleRepository) {
    operator fun invoke(): V3DeviceRole = repository.getSelectedRole().also(repository::updateRoleAccess)
}
