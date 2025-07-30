package com.bailout.stickk.ubi4.models.network

import kotlinx.serialization.Serializable


@Serializable
data class TakeDataRequest(
    val texts: List<String>
)