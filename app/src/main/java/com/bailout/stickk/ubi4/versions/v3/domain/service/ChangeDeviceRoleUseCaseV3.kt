package com.bailout.stickk.ubi4.versions.v3.domain.service

enum class V3DeviceRoleChangeResult { PIN_REQUIRED, INVALID_PIN, APPLIED, UNCHANGED, BLOCKED }

class ChangeDeviceRoleUseCaseV3(private val repository: V3DeviceRoleRepository) {
    operator fun invoke(role: V3DeviceRole, pin: String? = null): V3DeviceRoleChangeResult {
        if (!repository.interactionEnabled.value) return V3DeviceRoleChangeResult.BLOCKED
        if (role == repository.getSelectedRole()) return V3DeviceRoleChangeResult.UNCHANGED
        if (role == V3DeviceRole.SERVICE_ENGINEER) {
            if (pin == null) return V3DeviceRoleChangeResult.PIN_REQUIRED
            if (!repository.isPinValid(pin)) return V3DeviceRoleChangeResult.INVALID_PIN
        }
        repository.setSelectedRole(role)
        return V3DeviceRoleChangeResult.APPLIED
    }
}
