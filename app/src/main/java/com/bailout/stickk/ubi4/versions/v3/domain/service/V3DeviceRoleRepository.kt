package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

interface V3DeviceRoleRepository {
    val interactionEnabled: StateFlow<Boolean>
    fun getSelectedRole(): V3DeviceRole
    fun updateRoleAccess(role: V3DeviceRole)
    fun isPinValid(pin: String): Boolean
    fun setSelectedRole(role: V3DeviceRole)
}
