package com.bailout.stickk.new_electronic_by_Rodeon.models

import com.google.gson.annotations.SerializedName


data class ClientData (
    @SerializedName("id"  ) var id  : Int?    = null,
    @SerializedName("fio" ) var fio : String? = null
)