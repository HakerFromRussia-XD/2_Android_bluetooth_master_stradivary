package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import kotlinx.coroutines.flow.Flow

data class V3SpecialSettingsWidgetsSnapshot(
    val deviceProfile: V3DeviceProfile,
    val deviceAddress: String,
    val widgets: List<V3SpecialSettingsWidget>,
    val animationsEnabled: Boolean = true,
)

/** A presentation source of composition, independent of BLE reads and writes. */
interface V3SpecialSettingsWidgetsSource {
    val updates: Flow<Unit>
    fun snapshot(section: V3SpecialSettingsSection): V3SpecialSettingsWidgetsSnapshot
}
