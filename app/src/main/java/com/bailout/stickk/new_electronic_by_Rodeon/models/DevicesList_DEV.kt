package com.bailout.stickk.new_electronic_by_Rodeon.models

import com.google.gson.annotations.SerializedName

data class DevicesList_DEV(
    @SerializedName("devices") var devices : ArrayList<DeviceInList_DEV?>   = arrayListOf()
)
