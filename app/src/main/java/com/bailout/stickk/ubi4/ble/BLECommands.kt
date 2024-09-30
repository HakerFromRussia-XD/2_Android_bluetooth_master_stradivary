package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.*
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.HEADER_BLE_OFFSET

class BLECommands {
    companion object {
        fun requestPlotFlow(): ByteArray {
            val result = byteArrayOf(
                0x00,
                DATA_TRANSFER_SETTINGS.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00,
                0x01)
            result[3] = calculateDataSize(result).toByte()
            result[4] = (calculateDataSize(result)/256).toByte()
            return result
        }

        fun requestInicializeInformation(): ByteArray {
            val result = byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x02
                0x00,
                0x00,
                0x00,
                INICIALIZE_INFORMATION.number,
                0x02)
            result[3] = calculateDataSize(result).toByte()
            result[4] = (calculateDataSize(result)/256).toByte()
            return result
        }

        fun requestBaseParametrInfo(startParametrNum: Byte, countReadParameters: Byte): ByteArray {
            val result = byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x03
                0x00,
                0x00,
                0x00,
                READ_DEVICE_PARAMETRS.number,
                startParametrNum,
                countReadParameters)
            result[3] = calculateDataSize(result).toByte()
            result[4] = (calculateDataSize(result)/256).toByte()
            return result
        }

        fun requestAdditionalParametrInfo(idParameter: Byte): ByteArray {
            val result = byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x02
                0x00,
                0x00,
                0x00,
                READ_DEVICE_ADDITIONAL_PARAMETRS.number,
                idParameter
            )
            result[3] = calculateDataSize(result).toByte()
            result[4] = (calculateDataSize(result)/256).toByte()
            return result
        }


        fun oneButtonCommand(parameterID: Int, command: Int): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val result = byteArrayOf(
                0x40,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                command.toByte()
            )
            result[3] = calculateDataSize(result).toByte()
            result[4] = (calculateDataSize(result)/256).toByte()
            return result
        }

        private fun calculateDataSize(massage: ByteArray): Int {
            return massage.size - HEADER_BLE_OFFSET
        }
    }
}