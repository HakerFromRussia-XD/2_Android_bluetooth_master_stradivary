package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.SubDeviceManager
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.CRC_TABLE
import com.bailout.stickk.ubi4.utility.logging.platformLog

object BLECommandsV3 {

    fun requestDeviceData(): ByteArray {
        val header = byteArrayOf(
            0x00,
            SUB_DEVICE_MANAGER.number,
            SubDeviceManager.GET_ALL_SUB_DEVICE.number,
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    fun request(subcommand: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            PROSTHESIS_MODULE_CONTROL.number,
            subcommand.toByte(),
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    fun sendCommand( moduleControlCommand: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            PROSTHESIS_MODULE_CONTROL.number,
            moduleControlCommand.toByte(),
            0x00,
            0x00
        )
        header[4] = calculationCRC(header).toByte()
        return header
    }
    fun sendThresholds(thresholdOpen: Int, thresholdClose: Int): ByteArray {
        platformLog("Thresholds", "sendThresholds $thresholdOpen  $thresholdClose")
        val header = byteArrayOf(
            0x80.toByte(),
            PROSTHESIS_MODULE_CONTROL.number,
            0x00,
            0x00,
            0x00
        )
        val data = byteArrayOf(
            PWCE_SET_THRESHOLD_VALUE.number,
            thresholdOpen.toByte(),
            thresholdClose.toByte(),
            0x00
        )
        header[2] = (data.size - 1).toByte()
        header[3] = (data.size / 256).toByte()
        header[4] = calculationCRC(header).toByte()
        data[3] = calculationCRC(data).toByte()
        return header + data
    }
    fun sendGaines(gainOpen: Int, gainClose: Int): ByteArray {
        val header = byteArrayOf(
            0x80.toByte(),
            PROSTHESIS_MODULE_CONTROL.number,
            0x00,
            0x00,
            0x00
        )
        val data = byteArrayOf(
            PWCE_SET_EMG_GAIN_VALUE.number,
            gainOpen.toByte(),
            gainClose.toByte(),
            0x00
        )
        header[2] = (data.size - 1).toByte()
        header[3] = (data.size / 256).toByte()
        header[4] = calculationCRC(header).toByte()
        data[3] = calculationCRC(data).toByte()
        return header + data
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