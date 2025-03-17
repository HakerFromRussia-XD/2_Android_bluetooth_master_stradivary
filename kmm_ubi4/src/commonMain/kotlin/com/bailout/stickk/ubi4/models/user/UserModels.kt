package com.bailout.stickk.ubi4.models.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientData(
    @SerialName("id") var id: Int? = null,
    @SerialName("fio") var fio: String? = null,
    @SerialName("fname") var fname: String? = null,
    @SerialName("sname") var sname: String? = null
)

@Serializable
data class Devices(
    @SerialName("id") var id: String? = null,
    @SerialName("serial_number") var serialNumber: String? = null
)

@Serializable
data class User(
    @SerialName("client_data") var clientData: ClientData? = ClientData(),
    @SerialName("devices") var devices: List<Devices> = emptyList()
)

@Serializable
data class ClientDataV2(
    @SerialName("username") var username: String? = null,
    @SerialName("email") var email: String? = null,
    @SerialName("sex") var sex: Int? = null,
    @SerialName("phone") var phone: String? = null,
    @SerialName("fio") var fio: String? = null,
    @SerialName("country_code") var countryCode: String? = null,
    @SerialName("photo") var photo: String? = null,
    @SerialName("fname") var fname: String? = null,
    @SerialName("sname") var sname: String? = null,
    @SerialName("city") var city: String? = null,
    @SerialName("birth_date") var birthDate: Int? = null,
    @SerialName("client_id") var clientId: Int? = null,
    @SerialName("manager") var manager: Manager? = Manager()
)

@Serializable
data class Manager(
    @SerialName("fio") var fio: String? = null,
    @SerialName("email") var email: String? = null,
    @SerialName("phone") var phone: String? = null,
    @SerialName("photo") var photo: String? = null
)

@Serializable
data class UserV2(
    @SerialName("user_info") var userInfo: ClientDataV2? = ClientDataV2()
)