package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParameterInfoStructArray
import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSetV3
import com.bailout.stickk.ubi4.utility.logging.platformLog

class ParameterProvider {
    companion object {
        fun getParameterV3(parameterInfo: ParameterInfo<Int, Int, Int, Int>): BaseParameterInfoStruct {
            baseSubDevicesInfoStructSetV3.forEach { subDevice ->
                if (subDevice.deviceAddress == parameterInfo.deviceAddress) {
                    subDevice.parametersList.forEach { p ->
                        platformLog("baseSubDevicesInfoStructSet", "ищем ${parameterInfo.dataCode} а находим ${p.dataCode}  его дата ${p.data}")
                        if (p.ID == parameterInfo.parameterID &&
                            p.dataCode == parameterInfo.dataCode) {
//                            platformLog("baseSubDevicesInfoStructSet", "Возвращаем валидный параметр")
                            return p
                        }
                    }
                }
            }
            return BaseParameterInfoStruct()
        }
        fun getParameterV3(parameterRef: ParameterRef): BaseParameterInfoStruct {
            baseSubDevicesInfoStructSetV3.forEach { subDevice ->
                if (subDevice.deviceAddress == parameterRef.addressDevice) {
                    subDevice.parametersList.forEach { parameter ->
                        if (parameter.ID == parameterRef.parameterID) return parameter
                    }
                }
            }
            return BaseParameterInfoStruct()
        }
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