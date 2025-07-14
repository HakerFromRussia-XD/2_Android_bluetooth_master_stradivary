package com.bailout.stickk.ubi4.models.deviceList

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInList_DEV(
    @SerialName("id") var id: Int? = null,
    @SerialName("image") var image: String? = null,
    @SerialName("status") var status: Int? = null,
    @SerialName("serial_number") var serialNumber: String? = null,
    @SerialName("name") var name: String? = null,
    @SerialName("model_id") var modelId: Int? = null,
    @SerialName("model_name") var modelName: String? = null,
    @SerialName("date_transfer") var dateTransfer: Int? = null
)

@Serializable
data class DevicesList_DEV(
    @SerialName("devices") var devices: List<DeviceInList_DEV?> = emptyList()
)