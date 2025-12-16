package com.bailout.stickk.ubi4.data.state


import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct


object RestoredState {
    var baseParamsFromDb: MutableList<BaseParameterInfoStruct> = mutableListOf()
    var subDevicesFromDb: MutableSet<BaseSubDeviceInfoStruct> = mutableSetOf()
}