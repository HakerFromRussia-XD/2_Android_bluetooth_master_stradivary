package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository

enum class V3DeviceRoleChangeResult { PIN_REQUIRED, INVALID_PIN, APPLIED, UNCHANGED, BLOCKED }

class ChangeDeviceRoleUseCaseV3(
    private val repository: V3DeviceRoleRepository,
    private val requireInteractionEnabled: Boolean = true,
    private val skipUnchanged: Boolean = true,
) {
    fun requiresPin(role: V3DeviceRole): Boolean =
        role == V3DeviceRole.SERVICE_ENGINEER || role == V3DeviceRole.PROSTHETIST

    fun isPinValid(pin: String): Boolean = repository.isPinValid(pin)

    operator fun invoke(role: V3DeviceRole, pin: String? = null): V3DeviceRoleChangeResult {
        if (requireInteractionEnabled && !repository.interactionEnabled.value) return V3DeviceRoleChangeResult.BLOCKED
        if (skipUnchanged && role == repository.getSelectedRole()) return V3DeviceRoleChangeResult.UNCHANGED
        if (requiresPin(role)) {
            if (pin == null) return V3DeviceRoleChangeResult.PIN_REQUIRED
            if (!isPinValid(pin)) return V3DeviceRoleChangeResult.INVALID_PIN
        }
        repository.setSelectedRole(role)
        return V3DeviceRoleChangeResult.APPLIED
    }
}
