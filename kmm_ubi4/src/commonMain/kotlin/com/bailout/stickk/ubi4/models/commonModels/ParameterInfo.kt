package com.bailout.stickk.ubi4.models.commonModels


data class ParameterInfo<A, B, C, D>(
    val parameterID: A,
    val dataCode: B,
    val deviceAddress: C,
    val dataOffset: D
)