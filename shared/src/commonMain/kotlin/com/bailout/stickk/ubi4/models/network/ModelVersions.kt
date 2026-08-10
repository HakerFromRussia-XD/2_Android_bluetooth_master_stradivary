package com.bailout.stickk.ubi4.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelVersions(
    @SerialName("board_name") var boardName: String,
    @SerialName("board_code") var boardCode: Int,
    @SerialName("board_hardware_version") var boardHardwareVersion: String,
    @SerialName("board_software_version") var boardSoftwareVersion: String,
    @SerialName("model_code") var modelCode: Int,
    @SerialName("model_version") var modelVersion: String,
)