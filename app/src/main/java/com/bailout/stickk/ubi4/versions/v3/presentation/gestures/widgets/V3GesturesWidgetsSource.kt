package com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

fun interface V3GesturesWidgetsSource {
    fun widgets(profile: V3DeviceProfile): List<V3GesturesWidget>
}
