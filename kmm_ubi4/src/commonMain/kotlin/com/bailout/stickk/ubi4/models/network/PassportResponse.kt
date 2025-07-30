package com.bailout.stickk.ubi4.models.network

import kotlinx.serialization.Serializable

@Serializable
data class PassportResponse(
    val content:  String,
    val filename: String
)
