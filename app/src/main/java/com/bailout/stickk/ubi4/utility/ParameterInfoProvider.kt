package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.models.ParameterInfo

class ParameterInfoProvider {
    companion object {
        fun getParameterIDByCode(dataCode: Int, parameterIDSet: MutableSet<ParameterInfo<Int, Int, Int, Int>>): Int {
            parameterIDSet.forEach {
                if (it.dataCode == dataCode) {
                    return it.parameterID
                }
            }
            return 0
        }

        fun getDeviceAddressByDataCode(dataCode: Int,parameterIDSet: MutableSet<ParameterInfo<Int, Int, Int, Int>>): Int {
            parameterIDSet.forEach {
                if (it.dataCode == dataCode) {
                    return it.deviceAddress
                }
            }
            return 0
        }


        fun getDataOffsetByDataCode(dataCode: Int, parameterIDSet: MutableSet<ParameterInfo<Int, Int, Int, Int>>): Int {
            parameterIDSet.forEach {
                if (it.dataCode == dataCode) {
                    return it.dataOffset
                }
            }
            return 0
        }
    }
}