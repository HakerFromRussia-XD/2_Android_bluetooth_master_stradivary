package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.DATA_MANAGER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.DATA_TRANSFER_SETTINGS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands.DEVICE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand.READ_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataTableSlotsCode.DTCE_DEVICE_INFO_TYPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataTableSlotsEnum.DTE_SYSTEM_DEVICES
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.INICIALIZE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETRS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_DEVICE_PARAMETRS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_SUB_DEVICE_ADDITIONAL_PARAMETER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_SUB_DEVICE_INFO
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand.READ_SUB_DEVICE_PARAMETERS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.HEADER_BLE_OFFSET
import com.bailout.stickk.ubi4.utility.CrcCalc

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

        fun requestSubDeviceParametrs(subDeviceAddress: Int, startIndex: Int, quantitiesReadParameters: Int): ByteArray {
//            Log.d("requestSubDeviceParametrs", "$subDeviceAddress, $startIndex, $quantitiesReadParameters")
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
                quantitiesReadParameters.toByte(),
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            header[5] = CrcCalc.crcCalc(data)
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
            val header = byteArrayOf(
                0x20,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,//0x02
                0x00,
                0x00,
                0x00,
            )
            val data = byteArrayOf(
                INICIALIZE_INFORMATION.number,
                0x02)
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun requestBaseParametrInfo(startParametrNum: Byte, countReadParameters: Byte): ByteArray {
            val result = byteArrayOf(
                0x20,
                DEVICE_INFORMATION.number,
                0x00,
                0x00,
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
        fun requestBindingGroup(addressDevice: Int, parameterID: Int): ByteArray {
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
        fun requestActiveGesture(addressDevice: Int, parameterID: Int): ByteArray{
            val header = byteArrayOf(
                0x60.toByte(),
                parameterID.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            return header
        }
        fun requestSlider(addressDevice: Int, parameterID: Int): ByteArray {
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
        fun requestSwitcher(addressDevice: Int, parameterID: Int): ByteArray {
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
        fun requestThresholds(addressDevice: Int, parameterID: Int): ByteArray {
            val header = byteArrayOf(
                0x40.toByte(),
                parameterID.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            return header
        }
        fun requestProductInfoType(): ByteArray {
            val header = byteArrayOf(
                0x20.toByte(),
                DATA_MANAGER.number,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00
            )
            val data = byteArrayOf(
                READ_DATA.number,
                DTCE_DEVICE_INFO_TYPE.number
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
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
        fun sendBindingGroupInfo(addressDevice: Int, parameterID: Int, bindingGestureGroup: BindingGestureGroup): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val header = byteArrayOf(
                0x40.toByte(),
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
                bindingGestureGroup.gestureSpr1Id.toByte(),
                bindingGestureGroup.gesture1Id.toByte(),
                bindingGestureGroup.gestureSpr2Id.toByte(),
                bindingGestureGroup.gesture2Id.toByte(),
                bindingGestureGroup.gestureSpr3Id.toByte(),
                bindingGestureGroup.gesture3Id.toByte(),
                bindingGestureGroup.gestureSpr4Id.toByte(),
                bindingGestureGroup.gesture4Id.toByte(),
                bindingGestureGroup.gestureSpr5Id.toByte(),
                bindingGestureGroup.gesture5Id.toByte(),
                bindingGestureGroup.gestureSpr6Id.toByte(),
                bindingGestureGroup.gesture6Id.toByte(),
                bindingGestureGroup.gestureSpr7Id.toByte(),
                bindingGestureGroup.gesture7Id.toByte(),
                bindingGestureGroup.gestureSpr8Id.toByte(),
                bindingGestureGroup.gesture8Id.toByte(),
                bindingGestureGroup.gestureSpr9Id.toByte(),
                bindingGestureGroup.gesture9Id.toByte(),
                bindingGestureGroup.gestureSpr10Id.toByte(),
                bindingGestureGroup.gesture10Id.toByte(),
                bindingGestureGroup.gestureSpr11Id.toByte(),
                bindingGestureGroup.gesture11Id.toByte(),
                bindingGestureGroup.gestureSpr12Id.toByte(),
                bindingGestureGroup.gesture12Id.toByte(),
            )
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun sendActiveGesture(addressDevice: Int, parameterID: Int, activeGesture: Int): ByteArray {
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
                activeGesture.toByte(),
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
                gestureWithAddress.addressDevice.toByte()
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
        fun sendSliderCommand(addressDevice: Int, parameterID: Int,  progress: ArrayList<Int>): ByteArray {
            val code:Byte = (128 + parameterID).toByte()
            val header = byteArrayOf(
                0x60,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = progress.map { it.toByte() }.toByteArray()
            header[3] = data.size.toByte()
            header[4] = (data.size/256).toByte()
            val result = header + data
            return result
        }
        fun sendSwitcherCommand(addressDevice: Int, parameterID: Int, switchState: Boolean): ByteArray {
            val code: Byte = (128 + parameterID).toByte()
            val state: Byte = if (switchState) 1 else 0
            val result = byteArrayOf(
                0x60,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte(),
                state
            )
            result[3] = calculateDataSize(result).toByte()
            result[4] = (calculateDataSize(result) / 256).toByte()
            return result
        }
        fun sendThresholdsCommand(addressDevice: Int, parameterID: Int,  thresholds: ArrayList<Int>): ByteArray {
            val code: Byte = (128 + parameterID).toByte()
            val header = byteArrayOf(
                0x60,
                code,
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = thresholds.map { it.toByte() }.toByteArray()
            header[3] = data.size.toByte()
            header[4] = (data.size / 256).toByte()
            val result = header + data
            return result
        }

        fun openCheckpointFileInSDCard(name: String, addressDevice: Int, parameterID: Int, indexPackage: Int): ByteArray {
            val header = byteArrayOf(
                0x40.toByte(),
                (128 + parameterID).toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
                0x01,
                name.length.toByte(),
                0x00,
                0x01,
            ) + name.toByteArray(charset = Charsets.UTF_8)
            data[2] = (indexPackage).toByte()
            data[3] = (indexPackage / 256).toByte()
            header[3] = data.size.toByte()
            header[4] = (data.size / 256).toByte()
            val result = header + data
            return result
        }
        fun writeDataInCheckpointFileInSDCard(modifiedChunkArray: ByteArray, addressDevice: Int, parameterID: Int, indexPackage: Int) : ByteArray {
            val header = byteArrayOf(
                0x40.toByte(),
                (128 + parameterID).toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
                0x02,
                modifiedChunkArray.size.toByte(),
                0x00,
                0x02,
            ) + modifiedChunkArray
            data[2] = (indexPackage).toByte()
            data[3] = (indexPackage / 256).toByte()
            header[3] = data.size.toByte()
            header[4] = (data.size / 256).toByte()
            val result = header + data
            return result
        }
        fun closeCheckpointFileInSDCard(addressDevice: Int, parameterID: Int, indexPackage: Int): ByteArray {
            val header = byteArrayOf(
                0x40.toByte(),
                (128 + parameterID).toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                addressDevice.toByte()
            )
            val data = byteArrayOf(
                0x03,
                0x00,
                0x00,
                0x03,
            )
            data[2] = (indexPackage).toByte()
            data[3] = (indexPackage / 256).toByte()
            header[3] = data.size.toByte()
            header[4] = (data.size / 256).toByte()
            val result = header + data
            return result
        }


        private fun calculateDataSize(massage: ByteArray): Int {
            return massage.size - HEADER_BLE_OFFSET
        }
    }
}