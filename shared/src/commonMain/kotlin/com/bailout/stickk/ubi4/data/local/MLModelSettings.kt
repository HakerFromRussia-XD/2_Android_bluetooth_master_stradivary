package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.data.MLModelSettingsSerializer
import com.bailout.stickk.ubi4.models.device.Version
import kotlinx.serialization.Serializable

@Serializable(with = MLModelSettingsSerializer::class)
data class MLModelSettings(
    var modelCode: Int = 0,
    var majorModelVersion: Int = 0,
    var minorModelVersion: Int = 0,
    var quickfixModelVersion: Int = 0
)