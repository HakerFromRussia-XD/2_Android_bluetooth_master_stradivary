package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import kotlinx.coroutines.flow.Flow

data class V3SensorsWidgetsSnapshot(
    val deviceProfile: V3DeviceProfile,
    val deviceAddress: String,
    val widgets: List<V3SensorsWidget>,
    val animationsEnabled: Boolean = true,
)

interface V3SensorsWidgetsSource {
    val updates: Flow<Unit>
    fun snapshot(): V3SensorsWidgetsSnapshot
}
