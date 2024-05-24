package com.bailout.stickk.new_electronic_by_Rodeon.models.userV2

import com.google.gson.annotations.SerializedName


data class ClientDataV2 (
  @SerializedName("username"     ) var username    : String?  = null,
  @SerializedName("email"        ) var email       : String?  = null,
  @SerializedName("sex"          ) var sex         : Int?     = null,
  @SerializedName("phone"        ) var phone       : String?  = null,
  @SerializedName("fio"          ) var fio         : String?  = null,
  @SerializedName("country_code" ) var countryCode : String?  = null,
  @SerializedName("photo"        ) var photo       : String?  = null,
  @SerializedName("fname"        ) var fname       : String?  = null,
  @SerializedName("sname"        ) var sname       : String?  = null,
  @SerializedName("city"         ) var city        : String?  = null,
  @SerializedName("birth_date"   ) var birthDate   : Int?     = null,
  @SerializedName("client_id"    ) var clientId    : Int?     = null,
  @SerializedName("manager"      ) var manager     : Manager? = Manager()
)