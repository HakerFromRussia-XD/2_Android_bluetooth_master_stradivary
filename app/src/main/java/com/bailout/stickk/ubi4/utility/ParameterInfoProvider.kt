package com.bailout.stickk.ubi4.utility

import android.util.Log
import com.bailout.stickk.ubi4.models.ParameterInfo
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main

class ParameterInfoProvider {
    companion object {
        fun getParameterIDByCode(dataCode: Int, parameterIDSet: MutableSet<ParameterInfo<Int, Int, Int, Int>>): Int {
            var count = 0
            parameterIDSet.forEach { if (it.dataCode == dataCode) count++ }
            if (count > 1) {
                main.showToast("НЕШТАТНАЯ СИТУАЦИЯ в getParameterIDByCode()")
                parameterIDSet.forEach { if (it.dataCode == dataCode) Log.d("getParameterIDByCode", "$it") }
            }
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