package com.bailout.stickk.ubi4.models.ble

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


data class ParameterRef(
    val addressDevice: Int,
    val parameterID: Int,
    val dataCode: Int
)
data class ParameterRefV3(
    val parameterID: Int,
    val dataCode: Int
)

data class PlotParameterRef(
    val addressDevice: Int,
    val parameterID: Int,
    val dataPlots: ArrayList<Int>
)
@Serializable
data class ThresholdResult(
    var openThreshold: Int = 0,
    var closeThreshold: Int = 0
)
@Serializable
data class EMGGainResult(
    var openGain: Int = 0,
    var closeGain: Int = 0
)