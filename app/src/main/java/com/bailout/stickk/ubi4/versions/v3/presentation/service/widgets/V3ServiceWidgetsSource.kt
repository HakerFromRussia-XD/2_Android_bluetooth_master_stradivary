package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import kotlinx.coroutines.flow.Flow

data class V3ServiceWidgetsSnapshot(
    val deviceProfile: V3DeviceProfile,
    val deviceAddress: String,
    val widgets: List<V3ServiceWidget>,
    val animationsEnabled: Boolean = true,
)

interface V3ServiceWidgetsSource {
    val updates: Flow<Unit>
    fun snapshot(): V3ServiceWidgetsSnapshot
}
