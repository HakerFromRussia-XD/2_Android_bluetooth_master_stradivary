package com.bailout.stickk.ubi4.versions.v3.domain.service

class GetDeviceRoleUseCaseV3(private val repository: V3DeviceRoleRepository) {
    operator fun invoke(): V3DeviceRole = repository.getSelectedRole()
}
