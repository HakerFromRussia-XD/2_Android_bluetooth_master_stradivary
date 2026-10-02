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
) : V3DeviceRoleRepository {
    override val interactionEnabled = settings.spinnerInteractionEnabled
    override val serviceEngineerAccess = serviceEngineerAccessState.asStateFlow()

    // The disabled prosthetist role and unknown stored values display as User, as before.
    override fun getSelectedRole() = if (readSavedRole() == 1) {
        V3DeviceRole.SERVICE_ENGINEER
    } else V3DeviceRole.USER

    override fun updateRoleAccess(role: V3DeviceRole) {
        serviceEngineerAccessState.value = role == V3DeviceRole.SERVICE_ENGINEER
    }

    override fun isPinValid(pin: String) = pin == "1234" // Existing V3 PIN policy.

    override fun setSelectedRole(role: V3DeviceRole) {
        val value = if (role == V3DeviceRole.SERVICE_ENGINEER) 1 else 2
        saveRole(value)
        updateRoleAccess(role)
        settings.setSpinnerValue(P_KEY_DEVICE_ROLE, value)
    }
}
