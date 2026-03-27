package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.SUB_DEVICE_MANAGER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_SETTING
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_EMG_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_SETTING
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_THRESHOLD_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.SubDeviceManager.GET_ALL_SUB_DEVICE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.CRC_TABLE
import kotlin.test.Test
import kotlin.test.assertEquals

class CrcAndBleCommandsV3Test {

    @Test
    fun `requestDeviceData should contain expected command and crc`() {
        val packet = BLECommandsV3.requestDeviceData()

        assertEquals(5, packet.size)
        assertEquals(SUB_DEVICE_MANAGER.number.toInt(), packet[1].toInt() and 0xFF)
        assertEquals(GET_ALL_SUB_DEVICE.number.toInt(), packet[2].toInt() and 0xFF)
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
        val requestWithCommand = BLECommandsV3.requestWithCommand(command = 0x55, subcommand = 0x66)
        val gestureInfoRequest = BLECommandsV3.requestGestureInfo(gestureId = 12)
        val sendSubcommand = BLECommandsV3.sendSubcommand(subcommand = 0x21, parameter = 0xAB)
        val sendCommand = BLECommandsV3.sendCommand(command = 0x50, subcommand = 0x51, parameter = 0x52)

        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), request[1].toInt() and 0xFF)
        assertEquals(0x33, request[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(request), request.last().toInt() and 0xFF)

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
        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), header[1].toInt() and 0xFF)
        assertEquals(PWCE_SET_EMG_GAIN_VALUE.number.toInt(), payload[0].toInt() and 0xFF)
        assertEquals(88, payload[1].toInt() and 0xFF)
        assertEquals(77, payload[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header.last().toInt() and 0xFF)
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)
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
