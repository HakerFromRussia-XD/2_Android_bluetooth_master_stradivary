package com.bailout.stickk.new_electronic_by_Rodeon.models.userV2

import com.google.gson.annotations.SerializedName


data class Manager (
  @SerializedName("fio"   ) var fio   : String? = null,
  @SerializedName("email" ) var email : String? = null,
  @SerializedName("phone" ) var phone : String? = null,
  @SerializedName("photo" ) var photo : String? = null
)