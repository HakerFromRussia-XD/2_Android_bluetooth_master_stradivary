package com.bailout.stickk.ubi4.data.network.model

import kotlinx.serialization.Serializable


@Serializable
data class TakeDataRequest(
    val texts: List<String>
)