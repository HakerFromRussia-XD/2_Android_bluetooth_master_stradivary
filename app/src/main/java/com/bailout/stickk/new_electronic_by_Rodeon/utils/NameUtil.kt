package com.bailout.stickk.new_electronic_by_Rodeon.utils

import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager

object NameUtil {
    fun getCleanName(deviceName: String): String {
        if (deviceName.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            if ( deviceName.length == 15 ) {
                if (deviceName.contains(" ")) {
                    return deviceName
                }
                var newName = ""
                val namePrefix = deviceName.substring(6, 10)
                val nameCode = deviceName.substring(10, deviceName.length)
                System.err.println("Test getCleanName() deviceName: $deviceName")
                System.err.println("Test getCleanName() namePrefix: $namePrefix")
                System.err.println("Test getCleanName() nameCode: $nameCode")
                newName = when (namePrefix) {
                    ConstantManager.NEW_DEVICE_TYPE_FEST_F -> {
                        "FEST-F-$nameCode"
                    }

                    ConstantManager.NEW_DEVICE_TYPE_FEST_H -> {
                        "FEST-H-$nameCode"
                    }

                    ConstantManager.NEW_DEVICE_TYPE_FEST_F_O -> {
                        "FEST-FO-$nameCode"
                    }

                    ConstantManager.NEW_DEVICE_TYPE_FEST_H_O -> {
                        "FEST-HO-$nameCode"
                    }

                    ConstantManager.NEW_DEVICE_TYPE_FEST_EP -> {
                        "FEST-EP-$nameCode"
                    }

                    ConstantManager.NEW_DEVICE_TYPE_FEST_EB -> {
                        "FEST-EB-$nameCode"
                    }

                    else -> {
                        "$namePrefix-$nameCode"
                    }
                }
                return newName
            }
        }
        return deviceName
    }
}