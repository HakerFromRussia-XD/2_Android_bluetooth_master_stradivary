package com.bailout.stickk.ubi4.models.ble


data class ParameterRef(
    val addressDevice: Int,
    val parameterID: Int,
    val dataCode: Int
)

data class PlotParameterRef(
    val addressDevice: Int,
    val parameterID: Int,
    val dataPlots: ArrayList<Int>
)