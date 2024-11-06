package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
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
                0x20,
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
                0x20,
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
                0x20,
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
                0x20,
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
                0x20,
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
                0x20,
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
                0x20,
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

        fun sendTimestampInfo(addressDevice: Int, parameterID: Int, year: Int, month: Int, day: Int, weekDay: Int,  hour: Int, minutes: Int, seconds: Int): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val header = byteArrayOf(
                0x40,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
                year.toByte(),
                (year/256).toByte(),
                month.toByte(),
                day.toByte(),
                weekDay.toByte(),
                hour.toByte(),
                minutes.toByte(),
                seconds.toByte(),
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun sendRotationGroupInfo(addressDevice: Int, parameterID: Int, rotationGroup: RotationGroup): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val header = byteArrayOf(
                0x40,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
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
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }

        fun requestRotationGroup(addressDevice: Int, parameterID: Int): ByteArray {
            val header = byteArrayOf(
                0xE0.toByte(),
                parameterID.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            return header
        }
        fun requestGestureInfo(addressDevice: Int, parameterID: Int, gestureId: Int): ByteArray {
            val header = byteArrayOf(
                0xE0.toByte(),
                parameterID.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
                gestureId.toByte(),
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun sendGestureInfo(gestureWithAddress: GestureWithAddress): ByteArray {
            val code:Byte = (128 + gestureWithAddress.parameterID).toByte()
            val header = byteArrayOf(
                0x60,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                gestureWithAddress.deviceAddress.toByte()
            )
            val data = byteArrayOf(
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
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun sendOneButtonCommand(addressDevice: Int, parameterID: Int, command: Int): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val result = byteArrayOf(
                0x60,
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

        fun testDataTransfer(): ByteArray {
            val header = byteArrayOf(
                0x00,
                PreferenceKeysUBI4.BaseCommands.COMPLEX_PARAMETER_TRANSFER.number,
                0x00,
                0x00,//0x01
                0x00,
                0x00,
                0x00
            )
            val data = byteArrayOf(
                0x06,
                0x01,
                0x01,
                0x02
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        private fun calculateDataSize(massage: ByteArray): Int {
            return massage.size - HEADER_BLE_OFFSET
        }
    }
}