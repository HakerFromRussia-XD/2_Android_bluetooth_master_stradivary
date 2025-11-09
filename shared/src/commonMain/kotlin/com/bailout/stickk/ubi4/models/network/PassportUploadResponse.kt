package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PassportUploadResponse(
    @SerialName("message")
    val message: String,
    @SerialName("serial")
    val serial: String? = null,
    @SerialName("model_file_url")
    val modelFileUrl: String? = null
)