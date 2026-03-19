package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
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
    fun requestGestureInfo(gestureId: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            PROSTHESIS_MODULE_CONTROL.number,
            PWCE_GET_GESTURE_SETTING.number,
            gestureId.toByte(),
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
        header[header.size-1] = calculationCRC(header).toByte()
        data[data.size-1] = calculationCRC(data).toByte()
        return header + data
    }
    fun sendGestureInfo(gestureWithAddress: GestureWithAddress): ByteArray {
        val header = byteArrayOf(
            0x80.toByte(),
            PROSTHESIS_MODULE_CONTROL.number,
            0x00,
            0x00,
            0x00
        )
        val data = byteArrayOf(
            PWCE_SET_GESTURE_SETTING.number,
            gestureWithAddress.gesture.gestureId.toByte(),
            gestureWithAddress.gesture.openPosition1.toByte(),
            gestureWithAddress.gesture.openPosition2.toByte(),
            gestureWithAddress.gesture.openPosition3.toByte(),
            gestureWithAddress.gesture.openPosition4.toByte(),
            gestureWithAddress.gesture.openPosition5.toByte(),
            gestureWithAddress.gesture.openPosition6.toByte(),
            gestureWithAddress.gesture.closePosition1.toByte(),
            gestureWithAddress.gesture.closePosition2.toByte(),
            gestureWithAddress.gesture.closePosition3.toByte(),
            gestureWithAddress.gesture.closePosition4.toByte(),
            gestureWithAddress.gesture.closePosition5.toByte(),
            gestureWithAddress.gesture.closePosition6.toByte(),
            gestureWithAddress.gesture.openToCloseTimeShift1.toByte(),
            gestureWithAddress.gesture.openToCloseTimeShift2.toByte(),
            gestureWithAddress.gesture.openToCloseTimeShift3.toByte(),
            gestureWithAddress.gesture.openToCloseTimeShift4.toByte(),
            gestureWithAddress.gesture.openToCloseTimeShift5.toByte(),
            gestureWithAddress.gesture.openToCloseTimeShift6.toByte(),
            gestureWithAddress.gesture.closeToOpenTimeShift1.toByte(),
            gestureWithAddress.gesture.closeToOpenTimeShift2.toByte(),
            gestureWithAddress.gesture.closeToOpenTimeShift3.toByte(),
            gestureWithAddress.gesture.closeToOpenTimeShift4.toByte(),
            gestureWithAddress.gesture.closeToOpenTimeShift5.toByte(),
            gestureWithAddress.gesture.closeToOpenTimeShift6.toByte(),
            gestureWithAddress.gestureState.toByte(),
            0x00
        )
        header[2] = (data.size - 1).toByte()
        header[3] = (data.size / 256).toByte()
        header[header.size-1] = calculationCRC(header).toByte()
        data[data.size-1] = calculationCRC(data).toByte()
        return header + data
    }
    fun sendRotationGroupInfo(rotationGroup: RotationGroup): ByteArray {
        val header = byteArrayOf(
            0x80.toByte(),
            PROSTHESIS_MODULE_CONTROL.number,
            0x00,
            0x00,
            0x00
        )
        val data = byteArrayOf(
            PWCE_SET_GESTURE_GROUPE.number,
            rotationGroup.gesture1Id.toByte(),
            rotationGroup.gesture1ImageId.toByte(),
            rotationGroup.gesture2Id.toByte(),
            rotationGroup.gesture2ImageId.toByte(),
            rotationGroup.gesture3Id.toByte(),
            rotationGroup.gesture3ImageId.toByte(),
            rotationGroup.gesture4Id.toByte(),
            rotationGroup.gesture4ImageId.toByte(),
            rotationGroup.gesture5Id.toByte(),
            rotationGroup.gesture5ImageId.toByte(),
            rotationGroup.gesture6Id.toByte(),
            rotationGroup.gesture6ImageId.toByte(),
            rotationGroup.gesture7Id.toByte(),
            rotationGroup.gesture7ImageId.toByte(),
            rotationGroup.gesture8Id.toByte(),
            rotationGroup.gesture8ImageId.toByte(),
            0x00
        )
        header[2] = (data.size - 1).toByte()
        header[3] = (data.size / 256).toByte()
        header[header.size-1] = calculationCRC(header).toByte()
        data[data.size-1] = calculationCRC(data).toByte()
        return header + data
    }
    fun sendActiveGesture(activeGesture: Int): ByteArray {
        val header = byteArrayOf(
            0x80.toByte(),
            PROSTHESIS_MODULE_CONTROL.number,
            PWCE_SET_CURRENT_GESTURE_NUM.number,
            activeGesture.toByte(),
            0x00
        )
        header[header.size-1] = calculationCRC(header).toByte()
        return header
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