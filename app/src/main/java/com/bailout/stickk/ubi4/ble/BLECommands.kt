package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.DATA_MANAGER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.DATA_TRANSFER_SETTINGS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.DEVICE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand.READ_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataTableSlotsEnum.DTE_SYSTEM_DEVICES
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.INICIALIZE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETRS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_DEVICE_PARAMETRS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_SUB_DEVICE_ADDITIONAL_PARAMETER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_SUB_DEVICE_INFO
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_SUB_DEVICE_PARAMETERS
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.HEADER_BLE_OFFSET

class BLECommands {
    companion object {
        // чтение слота сабдевайсов
        fun requestSubDevices(): ByteArray {
            val header = byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00
            )
            val data = byteArrayOf(
                READ_SUB_DEVICE_INFO.number,
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun requestSubDevicesOld(): ByteArray {
            val header = byteArrayOf(
                0x00,
                DATA_MANAGER.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00
            )
            val data = byteArrayOf(
                READ_DATA.number,
                DTE_SYSTEM_DEVICES.number
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }

        fun requestSubDeviceParametrs(subDeviceAddress: Int, startIndex: Int, readNum: Int): ByteArray {
            val header = byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00
            )
            val data = byteArrayOf(
                READ_SUB_DEVICE_PARAMETERS.number,
                subDeviceAddress.toByte(),
                startIndex.toByte(),
                readNum.toByte(),
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }

        fun requestSubDeviceAdditionalParametrs(subDeviceAddress: Int, idParameter: Int): ByteArray {
            val header = byteArrayOf(
                0x00,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00
            )
            val data = byteArrayOf(
                READ_SUB_DEVICE_ADDITIONAL_PARAMETER.number,
                subDeviceAddress.toByte(),
                idParameter.toByte(),
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }

        fun requestTransferFlow(startTransfer: Int): ByteArray {
            val result = byteArrayOf(
                0x00,
                DATA_TRANSFER_SETTINGS.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00,
                startTransfer.toByte()) // 1 - start     2 - stop
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

        fun oneButtonCommand(addressDevice: Int, parameterID: Int, command: Int): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val result = byteArrayOf(
                0x40,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte(),
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