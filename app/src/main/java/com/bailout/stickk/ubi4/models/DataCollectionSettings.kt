package com.bailout.stickk.ubi4.models

data class DataCollectionSettings(
    val nCycles: Int,
    val baselineDuration: Int,
    val preGestDuration: Int,
    val atGestDuration: Int,
    val postGestDuration: Int
)