package com.bailout.stickk.new_electronic_by_Rodeon.models

import com.google.gson.annotations.SerializedName

data class DeviceInList_DEV(
    @SerializedName("id"            ) var id           : Int?    = null,
    @SerializedName("image"         ) var image        : String? = null,
    @SerializedName("status"        ) var status       : Int?    = null,
    @SerializedName("serial_number" ) var serialNumber : String? = null,
    @SerializedName("name"          ) var name         : String? = null,
    @SerializedName("model_id"      ) var modelId      : Int?    = null,
    @SerializedName("model_name"    ) var modelName    : String? = null,
    @SerializedName("date_transfer" ) var dateTransfer : Int?    = null
)
