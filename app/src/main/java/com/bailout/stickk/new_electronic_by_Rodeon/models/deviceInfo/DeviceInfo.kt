package com.bailout.stickk.new_electronic_by_Rodeon.models.deviceInfo

import com.google.gson.annotations.SerializedName


data class DeviceInfo (
  @SerializedName("id"                   ) var id                 :            Int?               = null,
  @SerializedName("serial_number       " ) var serialNumber       :            String?            = null,
  @SerializedName("model"                ) var model              :            Model?             = Model(),
  @SerializedName("version"              ) var version            :            Version?           = Version(),
  @SerializedName("status"               ) var status             :            Status?            = Status(),
  @SerializedName("device_model_version" ) var deviceModelVersion :            Int?               = null,
  @SerializedName("date_transfer"        ) var dateTransfer       :            String?            = null,
  @SerializedName("guarantee_period"     ) var guaranteePeriod    :            String?            = null,
  @SerializedName("side"                 ) var side               :            Side?              = Side(),
  @SerializedName("fingers"              ) var fingers            :            String?            = null,
  @SerializedName("size"                 ) var size               :            Size?              = Size(),
  @SerializedName("options"              ) var options            :            ArrayList<Options> = arrayListOf()
)