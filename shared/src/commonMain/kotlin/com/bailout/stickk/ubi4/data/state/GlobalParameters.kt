package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct

object GlobalParameters {
    var baseParametrInfoStructArray: MutableList<BaseParameterInfoStruct> = arrayListOf()
    var baseSubDevicesInfoStructSet: MutableSet<BaseSubDeviceInfoStruct> = mutableSetOf()
}