package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.logging.platformLog
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

class ParameterInfoProvider {
    companion object {

        fun getParameterIDByCode(
            dataCode: Int,
            parameterIDSet: Set<ParameterInfo<Int, Int, Int, Int>>
        ): Int {
            val matchingItems = parameterIDSet.filter { it.dataCode == dataCode }
            if (matchingItems.size > 1) {
                // Логирование через кроссплатформенную функцию
                matchingItems.forEach { platformLog("getParameterIDByCode", it.toString()) }
            }
            return matchingItems.firstOrNull()?.parameterID ?: 0
        }

        fun getDeviceAddressByDataCode(
            dataCode: Int,
            parameterIDSet: Set<ParameterInfo<Int, Int, Int, Int>>
        ): Int {
            return parameterIDSet.firstOrNull { it.dataCode == dataCode }?.deviceAddress ?: 0
        }

        fun getDataOffsetByDataCode(
            dataCode: Int,
            parameterIDSet: Set<ParameterInfo<Int, Int, Int, Int>>
        ): Int {
            return parameterIDSet.firstOrNull { it.dataCode == dataCode }?.dataOffset ?: 0
        }
    }
}