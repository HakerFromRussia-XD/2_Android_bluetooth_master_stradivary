package com.bailout.stickk.ubi4.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Client(
    @SerialName("client_id") val clientId: Int,
    @SerialName("corp_id")   val corpId:   Int,
    val name: String,
    val password: String
)