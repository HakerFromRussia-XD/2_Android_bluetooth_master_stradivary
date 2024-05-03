package com.bailout.stickk.new_electronic_by_Rodeon.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("client_data" ) var clientData : ClientData?        = ClientData(),
    @SerializedName("devices"     ) var devices    : ArrayList<Devices> = arrayListOf()
)