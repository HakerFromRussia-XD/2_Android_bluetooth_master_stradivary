package com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo

import com.google.gson.annotations.SerializedName


data class Version (
  @SerializedName("id"   ) var id   : Int?    = null,
  @SerializedName("name" ) var name : String? = null
)