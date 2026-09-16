package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection

data class V3SpecialSettingsWidgetsSnapshot(
    val deviceProfile: V3DeviceProfile,
    val deviceAddress: String,
    val widgets: List<V3SpecialSettingsWidget>,
    val animationsEnabled: Boolean = true,
)

/** A presentation source of composition, independent of BLE reads and writes. */
interface V3SpecialSettingsWidgetsSource {
    fun widgets(profile: V3DeviceProfile, section: V3SpecialSettingsSection): List<V3SpecialSettingsWidget>
}
