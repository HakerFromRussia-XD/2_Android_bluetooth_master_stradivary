package com.bailout.stickk.new_electronic_by_Rodeon.models.userV2

import com.google.gson.annotations.SerializedName


data class UserV2 (
  @SerializedName("user_info" ) var userInfo : ClientDataV2? = ClientDataV2()
)