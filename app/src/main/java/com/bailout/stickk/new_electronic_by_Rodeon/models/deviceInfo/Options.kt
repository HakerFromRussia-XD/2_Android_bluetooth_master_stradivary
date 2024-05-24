package com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo

import com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo.Value
import com.google.gson.annotations.SerializedName


data class Options (
  @SerializedName("id"    ) var id    : Int?    = null,
  @SerializedName("name"  ) var name  : String? = null,
  @SerializedName("type"  ) var type  : Int?    = null,
  @SerializedName("value" ) var value : Value?  = Value()
)