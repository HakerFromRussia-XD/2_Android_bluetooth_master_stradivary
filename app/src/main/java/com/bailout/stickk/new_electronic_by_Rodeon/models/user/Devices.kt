package com.bailout.stickk.new_electronic_by_Rodeon.models.user

import com.google.gson.annotations.SerializedName


data class Devices (
    @SerializedName("id"            ) var id           : String? = null,
    @SerializedName("serial_number" ) var serialNumber : String? = null
)