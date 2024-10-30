package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseSubDevicesInfoStructSet

class ParameterProvider {
    companion object {
        fun getParameter(deviceAddress: Int, parameterID: Int): BaseParameterInfoStruct {
            if (deviceAddress == 0 ) {
                // значит мы ищем параметр на мастере
                baseParametrInfoStructArray.forEach {
                    if (it.ID == parameterID) return it
                }
            } else {
                // значит мы ищем параметр на сабдевайсах
                baseSubDevicesInfoStructSet.forEach { subDevice ->
                    if (subDevice.deviceAddress == deviceAddress) {
                        subDevice.parametersList.forEach { parameter ->
                            if (parameter.ID == parameterID) return parameter
                        }
                    }
                }
            }
            return BaseParameterInfoStruct(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, "")
        }
    }
}