package com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo

import com.google.gson.annotations.SerializedName


data class Side (
  @SerializedName("id"   ) var id   : String? = null,
  @SerializedName("name" ) var name : String? = null
)