package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.*

class BLECommands {
    companion object {
        fun requestInicializeInformation(): ByteArray {
            return byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x02,
                0x00,
                0x00,
                0x00,
                INICIALIZE_INFORMATION.number,
                0x02)
        }


    }

}