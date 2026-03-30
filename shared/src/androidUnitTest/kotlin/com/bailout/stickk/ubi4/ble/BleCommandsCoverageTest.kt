package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BleCommandsCoverageTest {

    @Test
    fun `request packets should have expected command bytes`() {
        val packets = listOf(
            BLECommands.requestSubDevices(),
            BLECommands.requestSubDevicesOld(),
            BLECommands.requestSubDeviceParameters(subDeviceAddress = 7, startIndex = 4, quantitiesReadParameters = 10),
            BLECommands.requestSystemCrc(),
            BLECommands.requestSubDeviceAdditionalParameters(subDeviceAddress = 3, idParameter = 9),
            BLECommands.requestTransferFlow(startTransfer = 1),
            BLECommands.requestInicializeInformation(),
            BLECommands.requestBaseParametrInfo(startParametrNum = 2, countReadParameters = 9),
            BLECommands.requestAdditionalParametrInfo(idParameter = 5),
            BLECommands.requestRotationGroup(addressDevice = 1, parameterID = 2),
            BLECommands.requestBindingGroup(addressDevice = 1, parameterID = 3),
            BLECommands.requestGestureInfo(addressDevice = 1, parameterID = 4, gestureId = 7),
            BLECommands.requestActiveGesture(addressDevice = 1, parameterID = 5),
            BLECommands.requestSlider(addressDevice = 1, parameterID = 6),
            BLECommands.requestBatteryStatus(addressDevice = 1, parameterID = 7),
            BLECommands.requestSwitcher(addressDevice = 1, parameterID = 8),
            BLECommands.requestThresholds(addressDevice = 1, parameterID = 9),
            BLECommands.requestProductInfoType(),
            BLECommands.requestProductInfoType(deviceAddress = 2),
            BLECommands.requestProductFWInfoType(0),
            BLECommands.requestProductFWInfoType(8),
            BLECommands.requestDataSlots(0),
            BLECommands.requestDataSlots(8),
            BLECommands.requestRunProgramType(deviceAddress = 7),
            BLECommands.jumpToBootloader(deviceAddress = 7),
            BLECommands.requestStartSystemUpdate(),
            BLECommands.requestStartSystemUpdate(deviceAddress = 7),
            BLECommands.getBootloaderInfo(deviceAddress = 7),
            BLECommands.requestMaxChunkSize(deviceAddress = 7),
            BLECommands.requestCheckNewFw(deviceAddress = 7, fwDesc = byteArrayOf(0x01, 0x02, 0x03)),
            BLECommands.requestPreloadInfo(deviceAddress = 7),
            BLECommands.requestBootloaderStatus(deviceAddress = 7),
            BLECommands.sendLoadNewFw(deviceAddress = 7, offset = 0x12345678.toInt(), chunk = byteArrayOf(0x0A, 0x0B)),
            BLECommands.requestCalculateCrc(deviceAddress = 7),
            BLECommands.requestCompleteUpdate(deviceAddress = 7),
            BLECommands.requestFinishSystemUpdate(),
            BLECommands.requestFinishSystemUpdate(deviceAddress = 7),
            BLECommands.sendTimestampInfo(
                addressDevice = 1,
                parameterID = 2,
                year = 2026,
                month = 3,
                day = 27,
                weekDay = 5,
                hour = 10,
                minutes = 44,
                seconds = 12
            ),
            BLECommands.sendRotationGroupInfo(
                addressDevice = 1,
                parameterID = 3,
                rotationGroup = RotationGroup(
                    gesture1Id = 1,
                    gesture1ImageId = 11,
                    gesture2Id = 2,
                    gesture2ImageId = 12,
                    gesture3Id = 3,
                    gesture3ImageId = 13,
                    gesture4Id = 4,
                    gesture4ImageId = 14,
                    gesture5Id = 5,
                    gesture5ImageId = 15,
                    gesture6Id = 6,
                    gesture6ImageId = 16,
                    gesture7Id = 7,
                    gesture7ImageId = 17,
                    gesture8Id = 8,
                    gesture8ImageId = 18,
                )
            ),
            BLECommands.sendBindingGroupInfo(
                addressDevice = 1,
                parameterID = 4,
                bindingGestureGroup = BindingGestureGroup(
                    gestureSpr1Id = 1, gesture1Id = 11,
                    gestureSpr2Id = 2, gesture2Id = 12,
                    gestureSpr3Id = 3, gesture3Id = 13,
                    gestureSpr4Id = 4, gesture4Id = 14,
                    gestureSpr5Id = 5, gesture5Id = 15,
                    gestureSpr6Id = 6, gesture6Id = 16,
                    gestureSpr7Id = 7, gesture7Id = 17,
                    gestureSpr8Id = 8, gesture8Id = 18,
                    gestureSpr9Id = 9, gesture9Id = 19,
                    gestureSpr10Id = 10, gesture10Id = 20,
                    gestureSpr11Id = 11, gesture11Id = 21,
                    gestureSpr12Id = 12, gesture12Id = 22,
                )
            ),
            BLECommands.sendActiveGesture(addressDevice = 1, parameterID = 5, activeGesture = 8),
            BLECommands.sendGestureInfo(
                GestureWithAddress(
                    addressDevice = 1,
                    parameterID = 6,
                    gesture = Gesture(
                        gestureId = 3,
                        openPosition1 = 1,
                        openPosition2 = 2,
                        openPosition3 = 3,
                        openPosition4 = 4,
                        openPosition5 = 5,
                        openPosition6 = 6,
                        closePosition1 = 7,
                        closePosition2 = 8,
                        closePosition3 = 9,
                        closePosition4 = 10,
                        closePosition5 = 11,
                        closePosition6 = 12,
                        openToCloseTimeShift1 = 13,
                        openToCloseTimeShift2 = 14,
                        openToCloseTimeShift3 = 15,
                        openToCloseTimeShift4 = 16,
                        openToCloseTimeShift5 = 17,
                        openToCloseTimeShift6 = 18,
                        closeToOpenTimeShift1 = 19,
                        closeToOpenTimeShift2 = 20,
                        closeToOpenTimeShift3 = 21,
                        closeToOpenTimeShift4 = 22,
                        closeToOpenTimeShift5 = 23,
                        closeToOpenTimeShift6 = 24,
                    ),
                    gestureState = 1
                )
            ),
            BLECommands.sendOneButtonCommand(addressDevice = 1, parameterID = 7, command = 2),
            BLECommands.sendSliderCommand(addressDevice = 1, parameterID = 8, progress = listOf(10, 20, 30)),
            BLECommands.sendSwitcherCommand(addressDevice = 1, parameterID = 9, switchState = true),
            BLECommands.sendSwitcherCommand(addressDevice = 1, parameterID = 9, switchState = false),
            BLECommands.sendThresholdsCommand(addressDevice = 1, parameterID = 10, thresholds = listOf(40, 50)),
            BLECommands.openCheckpointFileInSDCard(
                name = "checkpoint.bin",
                addressDevice = 1,
                parameterID = 11,
                indexPackage = 513
            ),
            BLECommands.writeDataInCheckpointFileInSDCard(
                modifiedChunkArray = byteArrayOf(0x55, 0x66, 0x77),
                addressDevice = 1,
                parameterID = 11,
                indexPackage = 514
            ),
            BLECommands.closeCheckpointFileInSDCard(
                addressDevice = 1,
                parameterID = 11,
                indexPackage = 515
            )
        )

        assertTrue(packets.all { it.isNotEmpty() })
        assertTrue(packets.any { (it[0].toInt() and 0xFF) == 0xE0 })
        assertTrue(packets.any { (it[0].toInt() and 0xFF) == 0xA0 })
        assertTrue(packets.any { (it[0].toInt() and 0xFF) == 0x60 })
        assertTrue(packets.any { (it[0].toInt() and 0xFF) == 0x40 })

        val fwRoot = BLECommands.requestProductFWInfoType(0)
        val fwSub = BLECommands.requestProductFWInfoType(2)
        assertEquals(0x20, fwRoot[0].toInt() and 0xFF)
        assertEquals(0xA0, fwSub[0].toInt() and 0xFF)

        val slotRoot = BLECommands.requestDataSlots(0)
        val slotSub = BLECommands.requestDataSlots(2)
        assertEquals(0x20, slotRoot[0].toInt() and 0xFF)
        assertEquals(0xA0, slotSub[0].toInt() and 0xFF)
    }

    @Test
    fun `sendLoadNewFw should encode little endian offset and payload size`() {
        val chunk = byteArrayOf(0x11, 0x22, 0x33)
        val packet = BLECommands.sendLoadNewFw(
            deviceAddress = 5,
            offset = 0x78563412,
            chunk = chunk
        )

        val payload = packet.copyOfRange(7, packet.size)
        assertEquals(PreferenceKeysUbi4.FirmwareManagerCommand.LOAD_NEW_FW.number.toInt(), payload[0].toInt() and 0xFF)
        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), payload.copyOfRange(1, 5))
        assertContentEquals(chunk, payload.copyOfRange(5, payload.size))
        assertEquals(payload.size, packet[3].toInt() and 0xFF)
    }
}
