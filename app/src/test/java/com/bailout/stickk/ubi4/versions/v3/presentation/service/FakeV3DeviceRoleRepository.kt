package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeV3DeviceRoleRepository : V3DeviceRoleRepository {
    override val interactionEnabled = MutableStateFlow(true)
    override val serviceEngineerAccess = MutableStateFlow(false)
    var selected = V3DeviceRole.USER
    var access = V3DeviceRole.USER
    val writes = mutableListOf<V3DeviceRole>()
    override fun getSelectedRole() = selected
    override fun updateRoleAccess(role: V3DeviceRole) {
        access = role
        serviceEngineerAccess.value = role == V3DeviceRole.SERVICE_ENGINEER
    }
    override fun isPinValid(pin: String) = pin == "1234"
    override fun setSelectedRole(role: V3DeviceRole) { selected = role; updateRoleAccess(role); writes += role }
}
