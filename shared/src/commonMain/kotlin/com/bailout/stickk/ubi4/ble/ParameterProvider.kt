package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.utility.logging.platformLog

class ParameterProvider {
    companion object {
        fun getParameter(deviceAddress: Int, parameterID: Int): BaseParameterInfoStruct {
            if (baseParametrInfoStructArray.size != 0){
//                Log.d("TestOptic","baseSubDevicesInfoStructSet.size != 0")
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
            } else {
//                Log.d("TestOptic","baseSubDevicesInfoStructSet.size == 0")
            }
            return BaseParameterInfoStruct()
        }

        fun getParameterDeprecated(dataCode: Int): BaseParameterInfoStruct {
            baseParametrInfoStructArray.forEach { parameter ->
                if (parameter.dataCode == dataCode) return parameter
            }
            baseSubDevicesInfoStructSet.forEach { subDevice ->
                subDevice.parametersList.forEach { parameter ->
                    if (parameter.dataCode == dataCode) return parameter
                }
            }
            return BaseParameterInfoStruct(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, mutableSetOf(),"")
        }











        //TODO удалить внизу две фунеции они для теста
//        fun findAddressAndIdByDataCode(dataCode: Int): Pair<Int, Int>? {
//            // master (deviceAddress = 0)
//            baseParametrInfoStructArray.firstOrNull { it.dataCode == dataCode }?.let { p ->
//                return 0 to p.ID
//            }
//            // sub-devices
//            baseSubDevicesInfoStructSet.forEach { sub ->
//                sub.parametersList.firstOrNull { it.dataCode == dataCode }?.let { p ->
//                    return sub.deviceAddress to p.ID
//                }
//            }
//            return null
//        }
//
//        /** Записать hex-данные в точный параметр по address+ID. */
//        fun setParameterData(deviceAddress: Int, parameterID: Int, hexData: String): Boolean {
//            if (deviceAddress == 0) {
//                baseParametrInfoStructArray.firstOrNull { it.ID == parameterID }?.let {
//                    it.data = hexData
//                    return true
//                }
//            } else {
//                baseSubDevicesInfoStructSet.firstOrNull { it.deviceAddress == deviceAddress }?.let { sub ->
//                    sub.parametersList.firstOrNull { it.ID == parameterID }?.let {
//                        it.data = hexData
//                        return true
//                    }
//                }
//            }
//            return false
//        }
    }
    }