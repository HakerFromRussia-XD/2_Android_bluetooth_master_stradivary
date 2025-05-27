package com.bailout.stickk.ubi4.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SerialTokenRequest(
    @SerialName("serial_number") val serialNumber: String,
    val password: String
)
