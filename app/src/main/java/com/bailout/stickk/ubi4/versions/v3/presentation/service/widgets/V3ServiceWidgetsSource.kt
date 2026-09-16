package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

data class V3ServiceWidgetsSnapshot(
    val deviceProfile: V3DeviceProfile,
    val deviceAddress: String,
    val widgets: List<V3ServiceWidget>,
    val animationsEnabled: Boolean = true,
)

interface V3ServiceWidgetsSource {
    fun widgets(profile: V3DeviceProfile): List<V3ServiceWidget>
}
