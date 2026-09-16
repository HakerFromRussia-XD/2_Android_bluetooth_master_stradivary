package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

data class V3SensorsWidgetsSnapshot(
    val deviceProfile: V3DeviceProfile,
    val deviceAddress: String,
    val widgets: List<V3SensorsWidget>,
    val animationsEnabled: Boolean = true,
)

interface V3SensorsWidgetsSource {
    fun widgets(profile: V3DeviceProfile): List<V3SensorsWidget>
}
