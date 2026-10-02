package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository

class GetDeviceRoleUseCaseV3(private val repository: V3DeviceRoleRepository) {
    operator fun invoke(): V3DeviceRole = repository.getSelectedRole()
}
