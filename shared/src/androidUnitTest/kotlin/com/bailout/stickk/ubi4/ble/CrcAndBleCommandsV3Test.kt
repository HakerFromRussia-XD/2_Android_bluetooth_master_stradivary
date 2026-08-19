package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.data.parser.ByteArrayView
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.DATA_MANAGER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.DEVICE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.EMG_MASTER_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.SUB_DEVICE_MANAGER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DataManagerCommand.READ_AVAILABLE_SLOTS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_SET_EMG_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_SETTING
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_TELEMETRY_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_SETTING
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_THRESHOLD_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.SubDeviceManager.GET_ALL_SUB_DEVICE
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.CRC_TABLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CrcAndBleCommandsV3Test {

    @Test
    fun `global closed position packets should use dedicated one byte commands`() {
        val getCommands = listOf(
            PWCE_GET_PINCH_THUMB_POSITION.number.toInt() to 0x3B,
            PWCE_GET_PINCH_FINGER_POSITION.number.toInt() to 0xAA
        )
        val setCommands = listOf(
            PWCE_SET_PINCH_THUMB_POSITION.number.toInt() to mapOf(0 to 0xFF, 37 to 0xE3, 100 to 0xFB),
            PWCE_SET_PINCH_FINGER_POSITION.number.toInt() to mapOf(0 to 0x6E, 37 to 0x72, 100 to 0x6A)
        )

        getCommands.forEach { (command, expectedCrc) ->
            val packet = BLECommandsV3.request(command)
            assertContentEquals(
                byteArrayOf(
                    0,
                    PROSTHESIS_MODULE_CONTROL.number,
                    command.toByte(),
                    0,
                    expectedCrc.toByte()
                ),
                packet
            )
        }

        setCommands.forEach { (command, crcByValue) ->
            listOf(0, 37, 100).forEach { value ->
                val packet = BLECommandsV3.sendCommand(
                    command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                    subcommand = command,
                    parameter = value
                )
                assertContentEquals(
                    byteArrayOf(
                        0,
                        PROSTHESIS_MODULE_CONTROL.number,
                        command.toByte(),
                        value.toByte(),
                        crcByValue.getValue(value).toByte()
                    ),
                    packet
                )
            }
        }
    }

    @Test
    fun `global closed position bridge should map metadata to read and set commands`() {
        val cases = listOf(
            P_KEY_GLOBAL_THUMB_CLOSED_POSITION to PWCE_GET_PINCH_THUMB_POSITION,
            P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION to PWCE_GET_PINCH_FINGER_POSITION
        )

        cases.forEach { (key, getCommand) ->
            val info = com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
                .ParameterInfoRegistry.require(key)
            val read = WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode)!!
            assertEquals(getCommand.number.toInt(), read[2].toInt() and 0xFF)

            val set = WidgetCommandBridgeV3.buildSetInt(
                parameterID = info.parameterID,
                dataCode = getCommand.number.toInt(),
                deviceAddress = info.deviceAddress,
                dataOffset = 0,
                value = 64
            )!!
            assertEquals(info.dataCode, set[2].toInt() and 0xFF)
            assertEquals(64, set[3].toInt() and 0xFF)
            assertEquals(crcExcludeLast(set), set.last().toInt() and 0xFF)
        }
    }

    @Test
    fun `global closed position values decode independently and survive gesture changes`() {
        val thumbInfo = com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
            .ParameterInfoRegistry.require(P_KEY_GLOBAL_THUMB_CLOSED_POSITION)
        val fingersInfo = com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
            .ParameterInfoRegistry.require(P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION)
        val gestureInfo = com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
            .ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)

        val decoded = ParameterCodecRegistryV3.decodeFromPayload(
            ParameterCodecIdV3.SLIDER,
            ByteArrayView(
                byteArrayOf(PWCE_GET_PINCH_THUMB_POSITION.number, 73),
                offset = 0,
                length = 2
            )
        ) as ParameterTypedValueV3.Slider
        assertEquals(73, decoded.value.sliderValue)

        ParameterStoreV3.clear()
        ParameterStoreV3.put(thumbInfo, decoded)
        ParameterStoreV3.put(fingersInfo, ParameterTypedValueV3.Slider(SliderV3(41)))
        ParameterStoreV3.put(
            gestureInfo,
            ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(currentGesture = 3))
        )
        ParameterStoreV3.put(
            gestureInfo,
            ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(currentGesture = 10))
        )

        assertEquals(73, (ParameterStoreV3.get(thumbInfo) as ParameterTypedValueV3.Slider).value.sliderValue)
        assertEquals(41, (ParameterStoreV3.get(fingersInfo) as ParameterTypedValueV3.Slider).value.sliderValue)
        assertEquals(3, ParameterStoreV3.values.value.size)
        assertEquals(false, P_KEY_GLOBAL_THUMB_CLOSED_POSITION.contains("GESTURE", ignoreCase = true))
        assertEquals(false, P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION.contains("GESTURE", ignoreCase = true))
    }

    @Test
    fun `requestDeviceData should contain expected command and crc`() {
        val packet = BLECommandsV3.requestDeviceData()

        assertEquals(5, packet.size)
        assertEquals(SUB_DEVICE_MANAGER.number.toInt(), packet[1].toInt() and 0xFF)
        assertEquals(GET_ALL_SUB_DEVICE.number.toInt(), packet[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(packet), packet[4].toInt() and 0xFF)
    }

    @Test
    fun `requestAvailableSlots should use short data manager packet`() {
        val packet = BLECommandsV3.requestAvailableSlots(deviceAddress = 8)

        assertEquals(5, packet.size)
        assertEquals(0x00, packet[0].toInt() and 0xFF)
        assertEquals(DATA_MANAGER.number.toInt(), packet[1].toInt() and 0xFF)
        assertEquals(READ_AVAILABLE_SLOTS.number.toInt(), packet[2].toInt() and 0xFF)
        assertEquals(8, packet[3].toInt() and 0xFF)
        assertEquals(crcExcludeLast(packet), packet[4].toInt() and 0xFF)
    }

    @Test
    fun `sendSwitcher should encode boolean flag and crc`() {
        val packetTrue = BLECommandsV3.sendSwitcher(subcommand = 0x12, checked = true)
        val packetFalse = BLECommandsV3.sendSwitcher(subcommand = 0x12, checked = false)

        assertEquals(1, packetTrue[3].toInt() and 0xFF)
        assertEquals(0, packetFalse[3].toInt() and 0xFF)
        assertEquals(crcExcludeLast(packetTrue), packetTrue.last().toInt() and 0xFF)
        assertEquals(crcExcludeLast(packetFalse), packetFalse.last().toInt() and 0xFF)
    }

    @Test
    fun `sendThresholds should build long packet with header and payload crc`() {
        val packet = BLECommandsV3.sendThresholds(thresholdOpen = 120, thresholdClose = 55)
        val header = packet.copyOfRange(0, 5)
        val payload = packet.copyOfRange(5, packet.size)

        assertEquals(9, packet.size)
        assertEquals(0x80, header[0].toInt() and 0xFF)
        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), header[1].toInt() and 0xFF)
        assertEquals(payload.size - 1, header[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header[4].toInt() and 0xFF)

        assertEquals(PWCE_SET_THRESHOLD_VALUE.number.toInt(), payload[0].toInt() and 0xFF)
        assertEquals(120, payload[1].toInt() and 0xFF)
        assertEquals(55, payload[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)
    }

    @Test
    fun `basic command builders should encode subcommands and crc`() {
        val request = BLECommandsV3.request(subcommand = 0x33)
        val telemetryRequest = BLECommandsV3.requestTelemetryData()
        val requestWithCommand = BLECommandsV3.requestWithCommand(command = 0x55, subcommand = 0x66)
        val gestureInfoRequest = BLECommandsV3.requestGestureInfo(gestureId = 12)
        val sendSubcommand = BLECommandsV3.sendSubcommand(subcommand = 0x21, parameter = 0xAB)
        val sendCommand = BLECommandsV3.sendCommand(command = 0x50, subcommand = 0x51, parameter = 0x52)

        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), request[1].toInt() and 0xFF)
        assertEquals(0x33, request[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(request), request.last().toInt() and 0xFF)

        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), telemetryRequest[1].toInt() and 0xFF)
        assertEquals(PWCE_GET_TELEMETRY_DATA.number.toInt(), telemetryRequest[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(telemetryRequest), telemetryRequest.last().toInt() and 0xFF)

        assertEquals(0x55, requestWithCommand[1].toInt() and 0xFF)
        assertEquals(0x66, requestWithCommand[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(requestWithCommand), requestWithCommand.last().toInt() and 0xFF)

        assertEquals(PWCE_GET_GESTURE_SETTING.number.toInt(), gestureInfoRequest[2].toInt() and 0xFF)
        assertEquals(12, gestureInfoRequest[3].toInt() and 0xFF)
        assertEquals(crcExcludeLast(gestureInfoRequest), gestureInfoRequest.last().toInt() and 0xFF)

        assertEquals(0x21, sendSubcommand[2].toInt() and 0xFF)
        assertEquals(0xAB, sendSubcommand[3].toInt() and 0xFF)
        assertEquals(crcExcludeLast(sendSubcommand), sendSubcommand.last().toInt() and 0xFF)

        assertEquals(0x50, sendCommand[1].toInt() and 0xFF)
        assertEquals(0x51, sendCommand[2].toInt() and 0xFF)
        assertEquals(0x52, sendCommand[3].toInt() and 0xFF)
        assertEquals(crcExcludeLast(sendCommand), sendCommand.last().toInt() and 0xFF)
    }

    @Test
    fun `sendGaines should build payload and crc`() {
        val packet = BLECommandsV3.sendGaines(gainOpen = 88, gainClose = 77)
        val header = packet.copyOfRange(0, 5)
        val payload = packet.copyOfRange(5, packet.size)

        assertEquals(9, packet.size)
        assertEquals(EMG_MASTER_CONTROL.number.toInt(), header[1].toInt() and 0xFF)
        assertEquals(EMCE_SET_EMG_GAIN_VALUE.number.toInt(), payload[0].toInt() and 0xFF)
        assertEquals(88, payload[1].toInt() and 0xFF)
        assertEquals(77, payload[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header.last().toInt() and 0xFF)
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)
    }

    @Test
    fun `serial number widget command should encode full text`() {
        val serialNumber = "FEST-FO-0000008"
        val packet = WidgetCommandBridgeV3.buildSetText(
            parameterID = DEVICE_INFORMATION.number.toInt(),
            dataCode = SET_SERIAL_NUMBER.number,
            deviceAddress = 1,
            text = serialNumber
        )!!
        val header = packet.copyOfRange(0, 5)
        val payload = packet.copyOfRange(5, packet.size)

        assertEquals(0x80, header[0].toInt() and 0xFF)
        assertEquals(DEVICE_INFORMATION.number.toInt(), header[1].toInt() and 0xFF)
        assertEquals(payload.size - 1, header[2].toInt() and 0xFF)
        assertEquals(SET_SERIAL_NUMBER.number, payload[0].toInt() and 0xFF)
        assertEquals(serialNumber, payload.copyOfRange(1, payload.size - 2).decodeToString())
        assertEquals(0x00, payload[payload.size - 2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header.last().toInt() and 0xFF)
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)

        val readPacket = WidgetCommandBridgeV3.buildReadRequest(
            parameterID = DEVICE_INFORMATION.number.toInt(),
            dataCode = SET_SERIAL_NUMBER.number
        )!!
        assertEquals(DEVICE_INFORMATION.number.toInt(), readPacket[1].toInt() and 0xFF)
        assertEquals(GET_SERIAL_NUMBER.number, readPacket[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(readPacket), readPacket.last().toInt() and 0xFF)
    }

    @Test
    fun `sendGestureInfo should preserve gesture bytes and crc`() {
        val gesture = Gesture(
            gestureId = 5,
            openPosition1 = 1, openPosition2 = 2, openPosition3 = 3, openPosition4 = 4, openPosition5 = 5, openPosition6 = 6,
            closePosition1 = 7, closePosition2 = 8, closePosition3 = 9, closePosition4 = 10, closePosition5 = 11, closePosition6 = 12,
            openToCloseTimeShift1 = 13, openToCloseTimeShift2 = 14, openToCloseTimeShift3 = 15,
            openToCloseTimeShift4 = 16, openToCloseTimeShift5 = 17, openToCloseTimeShift6 = 18,
            closeToOpenTimeShift1 = 19, closeToOpenTimeShift2 = 20, closeToOpenTimeShift3 = 21,
            closeToOpenTimeShift4 = 22, closeToOpenTimeShift5 = 23, closeToOpenTimeShift6 = 24
        )
        val packet = BLECommandsV3.sendGestureInfo(
            GestureWithAddress(
                addressDevice = 1,
                parameterID = 2,
                gesture = gesture,
                gestureState = 9
            )
        )
        val header = packet.copyOfRange(0, 5)
        val payload = packet.copyOfRange(5, packet.size)

        assertEquals(PWCE_SET_GESTURE_SETTING.number.toInt(), payload[0].toInt() and 0xFF)
        assertEquals(5, payload[1].toInt() and 0xFF)
        assertEquals(1, payload[2].toInt() and 0xFF)
        assertEquals(12, payload[13].toInt() and 0xFF)
        assertEquals(24, payload[25].toInt() and 0xFF)
        assertEquals(9, payload[26].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header.last().toInt() and 0xFF)
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)
    }

    @Test
    fun `sendRotationGroup should keep payload order and crc`() {
        val packet = BLECommandsV3.sendRotationGroup(
            RotationGroup(
                gesture1Id = 1, gesture1ImageId = 11,
                gesture2Id = 2, gesture2ImageId = 12,
                gesture3Id = 3, gesture3ImageId = 13,
                gesture4Id = 4, gesture4ImageId = 14,
                gesture5Id = 5, gesture5ImageId = 15,
                gesture6Id = 6, gesture6ImageId = 16,
                gesture7Id = 7, gesture7ImageId = 17,
                gesture8Id = 8, gesture8ImageId = 18,
            )
        )

        val header = packet.copyOfRange(0, 5)
        val payload = packet.copyOfRange(5, packet.size)

        assertEquals(PWCE_SET_GESTURE_GROUPE.number.toInt(), payload[0].toInt() and 0xFF)
        assertEquals(1, payload[1].toInt() and 0xFF)
        assertEquals(11, payload[2].toInt() and 0xFF)
        assertEquals(8, payload[15].toInt() and 0xFF)
        assertEquals(18, payload[16].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header.last().toInt() and 0xFF)
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)
    }

    @Test
    fun `calculationCRCRange should safely clamp boundaries`() {
        val data = byteArrayOf(0x10, 0x20, 0x30, 0x40)

        val fromStart = BLECommandsV3.calculationCRCRange(data, offset = -5, length = 99)
        val fromPastEnd = BLECommandsV3.calculationCRCRange(data, offset = 10, length = 4)

        assertEquals(crcRangeManual(data, 0, data.size), fromStart)
        assertEquals(0, fromPastEnd)
    }

    private fun crcExcludeLast(data: ByteArray): Int = crcRangeManual(data, 0, (data.size - 1).coerceAtLeast(0))

    private fun crcRangeManual(data: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        val safeOffset = offset.coerceIn(0, data.size)
        val endExclusive = (safeOffset + length).coerceIn(safeOffset, data.size)
        for (i in safeOffset until endExclusive) {
            result = CRC_TABLE[result xor (data[i].toInt() and 0xFF)]
        }
        return result
    }
}
