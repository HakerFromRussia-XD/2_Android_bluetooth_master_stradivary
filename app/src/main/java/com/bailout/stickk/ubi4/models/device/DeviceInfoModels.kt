package com.bailout.stickk.ubi4.models.device

import com.google.gson.annotations.SerializedName

data class DeviceInfo (
    @SerializedName("id") var id: Int? = null,
    @SerializedName("serial_number       ") var serialNumber: String? = null,
    @SerializedName("model") var model: Model? = Model(),
    @SerializedName("version") var version: Version? = Version(),
    @SerializedName("status") var status: Status? = Status(),
    @SerializedName("device_model_version") var deviceModelVersion: Int? = null,
    @SerializedName("date_transfer") var dateTransfer: String? = null,
    @SerializedName("guarantee_period") var guaranteePeriod: String? = null,
    @SerializedName("side") var side: Side? = Side(),
    @SerializedName("fingers") var fingers: String? = null,
    @SerializedName("size") var size: Size? = Size(),
    @SerializedName("options") var options: ArrayList<Options> = arrayListOf()
)
data class Model(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null
)

data class Options(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("type") var type: Int? = null,
    @SerializedName("value") var value: Value? = Value()
)

data class Side(
    @SerializedName("id") var id: String? = null,
    @SerializedName("name") var name: String? = null
)

data class Size(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null
)

data class Status(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null
)

data class Value(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null
)

data class Version(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null
)