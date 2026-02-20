package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.SubDeviceManager
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.CRC_TABLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.HEADER_BLE_OFFSET

object BLECommandsV3 {
    fun requestDeviceData(): ByteArray {
        val header = byteArrayOf(
            0x00,
            BaseCommandsV3.SUB_DEVICE_MANAGER.number,
            SubDeviceManager.GET_ALL_SUB_DEVICE.number,
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    fun requestThresholdValue(): ByteArray {
        val header = byteArrayOf(
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    fun request(prosthesisModuleControl: Byte): ByteArray {
        val header = byteArrayOf(
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            prosthesisModuleControl,
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    fun requestKolbasa(): ByteArray {
        val header = byteArrayOf(
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
            0x00,
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number,
            0x00,
        )
        return header
    }
    fun sendCommand( moduleControlCommand: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number,
            moduleControlCommand.toByte(),
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    

    private fun calculateDataSize(message: ByteArray): Int {
        return message.size - HEADER_BLE_OFFSET
    }
    private fun calculationCRC(data: ByteArray): Int {
        var result = 0
        val limit = (data.size - 1).coerceAtLeast(0)

        for (i in 0 until limit) {
            result = CRC_TABLE[result xor (data[i].toInt() and 0xFF)]
        }
        return result
    }
    fun calculationCRCRange(data: ByteArray, offset: Int, length: Int): Int {
        var result = 0

        val safeOffset = offset.coerceIn(0, data.size)
        val endExclusive = (safeOffset + length).coerceIn(safeOffset, data.size)

        for (i in safeOffset until endExclusive) {
            result = CRC_TABLE[result xor (data[i].toInt() and 0xFF)]
        }
        return result
    }
}