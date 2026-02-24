package com.bailout.stickk.ubi4.models.ble

import kotlinx.serialization.Serializable


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

data class ThresholdResult(
    val openThreshold: Int = 0,
    val closeThreshold: Int = 0
)
@Serializable
data class EMGGainResult(
    val openGain: Int = 0,
    val closeGain: Int = 0
)