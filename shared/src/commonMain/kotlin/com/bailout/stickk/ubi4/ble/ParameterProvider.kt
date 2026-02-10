package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParameterInfoStructArray
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet

class ParameterProvider {
    companion object {
        fun getParameter(deviceAddress: Int, parameterID: Int): BaseParameterInfoStruct {
            if (baseParameterInfoStructArray.size != 0) {
//                Log.d("TestOptic","baseSubDevicesInfoStructSet.size != 0")
                if (deviceAddress == 0) {
                    // значит мы ищем параметр на мастере
                    baseParameterInfoStructArray.forEach {
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
            } else {
//                Log.d("TestOptic","baseSubDevicesInfoStructSet.size == 0")
                baseSubDevicesInfoStructSet.forEach { subDevice ->
                    if (subDevice.deviceAddress == deviceAddress) {
                        subDevice.parametersList.forEach { parameter ->
                            if (parameter.ID == parameterID) return parameter
                        }
                    }
                }
            }
            return BaseParameterInfoStruct()
        }

        fun getParameterDeprecated(dataCode: Int): BaseParameterInfoStruct {
            baseParameterInfoStructArray.forEach { parameter ->
                if (parameter.dataCode == dataCode) return parameter
            }
            baseSubDevicesInfoStructSet.forEach { subDevice ->
                subDevice.parametersList.forEach { parameter ->
                    if (parameter.dataCode == dataCode) return parameter
                }
            }
            return BaseParameterInfoStruct(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                mutableSetOf(),
                ""
            )
        }


    }
}