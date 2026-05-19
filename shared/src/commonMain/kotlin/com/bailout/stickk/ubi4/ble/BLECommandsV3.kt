package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.FirmwareManagerCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.SubDeviceManager
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.CRC_TABLE
import com.bailout.stickk.ubi4.utility.logging.platformLog

object BLECommandsV3 {
    fun requestRunProgramTypeFw(deviceAddress: Int): ByteArray {
        return sendCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.GET_RUN_PROGRAM_TYPE.number.toInt(),
            deviceAddress
        )
    }

    fun jumpToBootloaderFw(deviceAddress: Int): ByteArray {
        return sendCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.JUMP_TO_BOOTLOADER.number.toInt(),
            deviceAddress
        )
    }

    fun requestUploadAttributeFw(deviceAddress: Int): ByteArray {
        return sendCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.GET_MAX_CHANK_SIZE.number.toInt(),
            deviceAddress
        )
    }

    fun requestCheckNewFw(deviceAddress: Int, fwDesc: ByteArray): ByteArray {
        return sendLongCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.CHECK_NEW_FW.number.toInt(),
            byteArrayOf(deviceAddress.toByte()) + fwDesc
        )
    }

    fun requestPreloadInfoFw(deviceAddress: Int, fwSize: Int): ByteArray {
        return sendLongCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.PRELOAD_INFO.number.toInt(),
            byteArrayOf(deviceAddress.toByte()) + fwSize.toUInt32Le()
        )
    }

    fun sendLoadNewFw(deviceAddress: Int, offset: Int, chunk: ByteArray): ByteArray {
        return sendLongCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.LOAD_NEW_FW.number.toInt(),
            byteArrayOf(deviceAddress.toByte()) + offset.toUInt32Le() + chunk
        )
    }

    fun requestCalculateCrcFw(deviceAddress: Int): ByteArray {
        return sendCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.CALCULATE_CRC.number.toInt(),
            deviceAddress
        )
    }

    fun requestCompleteUpdateFw(deviceAddress: Int): ByteArray {
        return sendCommand(
            WRITE_FW_COMMAND.number.toInt(),
            FirmwareManagerCommand.COMPLETE_UPDATE.number.toInt(),
            deviceAddress
        )
    }

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
    fun requestTelemetryData(): ByteArray {
        return request(PWCE_GET_TELEMETRY_DATA.number.toInt())
    }
    fun requestWithCommand(command: Int, subcommand: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            command.toByte(),
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
    fun sendSubcommand(subcommand: Int, parameter: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            PROSTHESIS_MODULE_CONTROL.number,
            subcommand.toByte(),
            parameter.toByte(),
            0x00
        )
        header[header.size-1] = calculationCRC(header).toByte()
        return header
    }
    fun sendCommand(command: Int,subcommand: Int, parameter: Int): ByteArray {
        val header = byteArrayOf(
            0x00,
            command.toByte(),
            subcommand.toByte(),
            parameter.toByte(),
            0x00
        )
        header[header.size-1] = calculationCRC(header).toByte()
        return header
    }
    fun sendLongCommand(command: Int,subcommand: Int, data: ByteArray): ByteArray {
        val header = byteArrayOf(
            0x80.toByte(),
            command.toByte(),
            0x00,
            0x00,
            0x00
        )
        val dataRepack = byteArrayOf(subcommand.toByte()) + data + byteArrayOf(0x00)
        header[2] = (dataRepack.size - 1).toByte()
        header[3] = (dataRepack.size / 256).toByte()
        header[header.size-1] = calculationCRC(header).toByte()
        dataRepack[dataRepack.size-1] = calculationCRC(dataRepack).toByte()
        return header + dataRepack
    }

    fun sendSwitcher(subcommand: Int, checked: Boolean): ByteArray {
        val checkedByte: Byte = if (checked) 0x01 else 0x00
        val header = byteArrayOf(
            0x00,
            PROSTHESIS_MODULE_CONTROL.number,
            subcommand.toByte(),
            checkedByte,
            0x00
        )
        header[header.size-1] = calculationCRC(header).toByte()
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
        header[header.size-1] = calculationCRC(header).toByte()
        data[data.size-1] = calculationCRC(data).toByte()
        return header + data
    }
    fun sendGaines(gainOpen: Int, gainClose: Int): ByteArray {
        val header = byteArrayOf(
            0x80.toByte(),
            EMG_MASTER_CONTROL.number,
            0x00,
            0x00,
            0x00
        )
        val data = byteArrayOf(
            EMCE_SET_EMG_GAIN_VALUE.number,
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
    fun sendRotationGroup(rotationGroup: RotationGroup): ByteArray {
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

    private fun Int.toUInt32Le(): ByteArray =
        byteArrayOf(
            (this and 0xFF).toByte(),
            ((this shr 8) and 0xFF).toByte(),
            ((this shr 16) and 0xFF).toByte(),
            ((this shr 24) and 0xFF).toByte()
        )
}
