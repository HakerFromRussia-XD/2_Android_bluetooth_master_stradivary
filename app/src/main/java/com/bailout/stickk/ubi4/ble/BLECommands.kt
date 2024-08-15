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

        fun requestBaseParametrInfo(startParametrNum: Byte, countReadParameters: Byte): ByteArray {
            return byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x03,
                0x00,
                0x00,
                0x00,
                READ_DEVICE_PARAMETRS.number,
                startParametrNum,
                countReadParameters)
        }

        fun requestAdditionalParametrInfo(idParameter: Byte): ByteArray {
            System.err.println("TEST idParameter = $idParameter")
            return byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x02,
                0x00,
                0x00,
                0x00,
                READ_DEVICE_ADDITIONAL_PARAMETR.number,
                idParameter
            )
        }
    }

}