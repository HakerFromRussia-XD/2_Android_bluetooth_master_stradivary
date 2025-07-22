package com.bailout.stickk.ubi4.models.device

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo (
    @SerialName("id") var id: Int? = null,
    @SerialName("serial_number") var serialNumber: String? = null,
    @SerialName("model") var model: Model? = Model(),
    @SerialName("version") var version: Version? = Version(),
    @SerialName("status") var status: Status? = Status(),
    @SerialName("device_model_version") var deviceModelVersion: Int? = null,
    @SerialName("date_transfer") var dateTransfer: String? = null,
    @SerialName("guarantee_period") var guaranteePeriod: String? = null,
    @SerialName("side") var side: Side? = Side(),
    @SerialName("fingers") var fingers: String? = null,
    @SerialName("size") var size: Size? = Size(),
    @SerialName("options") var options: List<Options> = emptyList()
)

@Serializable
data class Model(
    @SerialName("id") var id: Int? = null,
    @SerialName("name") var name: String? = null
)

@Serializable
data class Options(
    @SerialName("id") var id: Int? = null,
    @SerialName("name") var name: String? = null,
    @SerialName("type") var type: Int? = null,
    @SerialName("value") var value: Value? = Value()
)

@Serializable
data class Side(
    @SerialName("id") var id: String? = null,
    @SerialName("name") var name: String? = null
)

@Serializable
data class Size(
    @SerialName("id") var id: Int? = null,
    @SerialName("name") var name: String? = null
)

@Serializable
data class Status(
    @SerialName("id") var id: Int? = null,
    @SerialName("name") var name: String? = null
)

@Serializable
data class Value(
    @SerialName("id") var id: Int? = null,
    @SerialName("name") var name: String? = null
)

@Serializable
data class Version(
    @SerialName("id") var id: Int? = null,
    @SerialName("name") var name: String? = null
)