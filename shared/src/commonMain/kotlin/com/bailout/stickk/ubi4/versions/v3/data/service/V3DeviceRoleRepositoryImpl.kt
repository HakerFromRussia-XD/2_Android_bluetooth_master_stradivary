package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Service and account repositories share access for the lifetime of the process.
internal val serviceEngineerAccessState = MutableStateFlow(false)

class V3DeviceRoleRepositoryImpl(
    private val readSavedRole: () -> Int,
    private val saveRole: (Int) -> Unit,
    private val settings: V3SpinnerSettingsRepository,
    private val allowProsthetist: Boolean = false,
    private val updateSharedAccess: Boolean = true,
) : V3DeviceRoleRepository {
    override val interactionEnabled = settings.spinnerInteractionEnabled
    override val serviceEngineerAccess = serviceEngineerAccessState.asStateFlow()

    // Android keeps its disabled prosthetist role normalized for display; iOS retains all three roles.
    override fun getSelectedRole() = when (readSavedRole()) {
        V3DeviceRole.SERVICE_ENGINEER.wireValue -> V3DeviceRole.SERVICE_ENGINEER
        V3DeviceRole.PROSTHETIST.wireValue -> if (allowProsthetist) V3DeviceRole.PROSTHETIST else V3DeviceRole.USER
        else -> V3DeviceRole.USER
    }

    override fun updateRoleAccess(role: V3DeviceRole) {
        if (updateSharedAccess) serviceEngineerAccessState.value = role == V3DeviceRole.SERVICE_ENGINEER
    }

    override fun isPinValid(pin: String) = pin == "1234" // Existing V3 PIN policy.

    override fun setSelectedRole(role: V3DeviceRole) {
        require(allowProsthetist || role != V3DeviceRole.PROSTHETIST) { "Prosthetist role is unavailable" }
        val value = role.wireValue
        saveRole(value)
        updateRoleAccess(role)
        settings.setSpinnerValue(P_KEY_DEVICE_ROLE, value)
    }
}
