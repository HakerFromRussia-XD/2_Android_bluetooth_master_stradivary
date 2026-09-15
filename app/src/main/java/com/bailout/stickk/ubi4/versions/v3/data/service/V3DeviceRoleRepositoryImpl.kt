package com.bailout.stickk.ubi4.versions.v3.data.service

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository

class V3DeviceRoleRepositoryImpl(
    private val preferences: SharedPreferences,
    private val settings: V3SpinnerSettingsRepository,
) : V3DeviceRoleRepository {
    override val interactionEnabled = settings.spinnerInteractionEnabled

    // The disabled prosthetist role and unknown stored values display as User, as before.
    override fun getSelectedRole() = if (preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, 2) == 1) {
        V3DeviceRole.SERVICE_ENGINEER
    } else V3DeviceRole.USER

    override fun updateRoleAccess(role: V3DeviceRole) {
        UiState.isServiceEngineerRole.value = role == V3DeviceRole.SERVICE_ENGINEER
    }

    override fun isPinValid(pin: String) = pin == "1234" // Existing V3 PIN policy.

    override fun setSelectedRole(role: V3DeviceRole) {
        val value = if (role == V3DeviceRole.SERVICE_ENGINEER) 1 else 2
        preferences.edit().putInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, value).apply()
        updateRoleAccess(role)
        settings.setSpinnerValue(P_KEY_DEVICE_ROLE, value)
    }
}
