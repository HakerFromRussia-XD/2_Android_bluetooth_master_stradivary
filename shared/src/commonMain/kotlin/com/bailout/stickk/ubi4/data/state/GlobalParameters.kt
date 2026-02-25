package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct

object GlobalParameters {
    var baseParameterInfoStructArray: MutableList<BaseParameterInfoStruct> = arrayListOf()
    var baseSubDevicesInfoStructSet: MutableSet<BaseSubDeviceInfoStruct> = mutableSetOf()
    var baseSubDevicesInfoStructSetV3: MutableSet<BaseSubDeviceInfoStruct> = mutableSetOf()
}