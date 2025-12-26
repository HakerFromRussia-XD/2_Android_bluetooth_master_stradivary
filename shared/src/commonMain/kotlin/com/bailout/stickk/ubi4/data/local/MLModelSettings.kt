package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.data.MLModelSettingsSerializer
import kotlinx.serialization.Serializable

@Serializable(with = MLModelSettingsSerializer::class)
data class MLModelSettings(
    var modelCode: Int = 0,
    var modelVersion: String = "0.0.2"
)