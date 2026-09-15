package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole

data class V3ServiceRoleUiState(
    val selectedRole: V3DeviceRole,
    val isEnabled: Boolean = false,
    val pinRequest: V3RolePinRequest? = null,
    val pinFeedback: V3RolePinFeedback? = null,
) {
    val roles: List<V3DeviceRole> get() = V3DeviceRole.entries
    val displayedIndex: Int get() = roles.indexOf(pinRequest?.role ?: selectedRole)
}

data class V3RolePinRequest(val id: Long, val role: V3DeviceRole)
data class V3RolePinFeedback(val requestId: Long, val accessGranted: Boolean)
