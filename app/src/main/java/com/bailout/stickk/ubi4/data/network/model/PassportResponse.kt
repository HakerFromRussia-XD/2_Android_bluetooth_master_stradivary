package com.bailout.stickk.ubi4.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class PassportResponse(
    val content:  String,
    val filename: String
)
