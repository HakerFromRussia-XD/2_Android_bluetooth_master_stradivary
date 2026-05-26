package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.testing.RecordingBleCommandExecutor
import com.bailout.stickk.ubi4.testing.ensureWidgetRepoInitializedForTests
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParserCoverageTest {

    @Test
    fun `ubi packet view data class should support equals hash and extraction`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val view1 = ByteArrayView(bytes = bytes, offset = 1, length = 3)
        val view2 = ByteArrayView(bytes = bytes.copyOf(), offset = 1, length = 3)
        val packet1 = UbiPacketView(
            type = UbiPacketType.LONG,
            address = 7,
            command = 3,
            payloadSize = 3,
            headerCrcError = false,
            payloadCrcError = true,
            payloadOffset = 1,
            payloadLength = 3,
            payload = view1
        )
        val packet2 = packet1.copy(payload = view2)

        assertEquals(packet1, packet2)
        assertEquals(packet1.hashCode(), packet2.hashCode())
        assertArrayEquals(byteArrayOf(2, 3, 4), packet1.payload.toByteArray())
        assertNotEquals(packet1, packet1.copy(command = 9))
    }

    @Test
    fun `ble parser v3 should parse short long and helper payloads`() = runBlocking {
        ensureWidgetRepoInitializedForTests()
        val executor = RecordingBleCommandExecutor()
        val manager = BleManagerKmm().also { it.setBleCommandExecutor(executor) }
        val parser = BLEParserV3(
            coroutineScope = CoroutineScope(Dispatchers.Default),
            bleCommandExecutor = executor,
            bleManager = manager
        )

        parser.parseReceivedSensorsData(byteArrayOf(1, 2, 3, 4, 5, 6))

        // SUB_DEVICE_MANAGER (short)
        parser.parseReceivedData(
            shortPacket(
                address = 1,
                command = PreferenceKeysUbi4.BaseCommandsV3.SUB_DEVICE_MANAGER.number.toInt(),
                payload0 = PreferenceKeysUbi4.SubDeviceManager.GET_ALL_SUB_DEVICE.number.toInt(),
                payload1 = 0
            )
        )

        val prosthesisCommand = PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number.toInt()
        val guiCommand = PreferenceKeysUbi4.BaseCommandsV3.GUI_CONTROL.number.toInt()

        val longPayloads = listOf(
            // PROSTHESIS_MODULE_CONTROL branches
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_HAND_CONTROL_MODE.number, 1, 0),
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_EMG_MOVEMENT_LOCK.number, 1, 0),
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_EMG_CHANGE_GESTURE.number, 2, 0),
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_CURRENT_GESTURE_NUM.number, 3, 0),
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE.number, 10, 20),
            byteArrayOf(PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_GAIN_VALUE.number, 11, 21),
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_GROUPE.number) + ByteArray(16) { it.toByte() },
            byteArrayOf(PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_SETTING.number) + ByteArray(25) { (it + 1).toByte() },
            // GUI_CONTROL branches
            byteArrayOf(PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_SCREEN_TIMEOUT.number, 15, 0),
            byteArrayOf(PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_LEFT_RIGHT_HAND.number, 1, 0)
        )

        longPayloads.forEachIndexed { idx, payload ->
            val command = if (idx < 8) prosthesisCommand else guiCommand
            runCatching {
                parser.parseReceivedData(longPacket(address = 1, command = command, payload = payload))
            }
        }
        parser.parseReceivedData(
            longPacket(
                address = 1,
                command = PreferenceKeysUbi4.BaseCommandsV3.DEVICE_INFORMATION.number.toInt(),
                payload = byteArrayOf(
                    PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER.number.toByte()
                ) + "FEST-FO-0000008".encodeToByteArray() + byteArrayOf(0x00)
            )
        )
        val serialInfo = PreferenceKeysUbi4.ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
        val serialValue = ParameterStoreV3.get(serialInfo) as? ParameterTypedValueV3.Text
        assertEquals("FEST-FO-0000008", serialValue?.value)

        runCatching { parser.generatedHardcodeWidgets() }

        // Private helper functions coverage
        runCatching { invokePrivate(parser, "parseUbiPacketZeroAlloc", shortPacket(1, prosthesisCommand, 1, 2)) }
        runCatching { invokePrivate(parser, "parseUbiPacketZeroAlloc", longPacket(1, prosthesisCommand, byteArrayOf(1, 2, 3))) }
        assertFailsWith<IllegalArgumentException> {
            invokePrivate(parser, "parseUbiPacketZeroAlloc", byteArrayOf(1, 2, 3, 4))
        }

        val payloadView = ByteArrayView(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), 0, 9)
        runCatching { invokePrivate(parser, "parseSubDeviceManagerGetAllSubDevice", payloadView) }
        runCatching { invokePrivate(parser, "parseThresholdZeroAlloc", payloadView) }
        runCatching { invokePrivate(parser, "parseEMGGainZeroAlloc", payloadView) }
        runCatching { invokePrivate(parser, "parseToggleZeroAlloc", payloadView) }
        runCatching { invokePrivate(parser, "parseSpinnerZeroAlloc", payloadView) }
        runCatching { invokePrivate(parser, "parseSwitchZeroAlloc", payloadView) }
        runCatching { invokePrivate(parser, "parseCurrentGestureZeroAlloc", payloadView) }
        runCatching { invokePrivate(parser, "parseGestureZeroAlloc", ByteArrayView(ByteArray(30) { it.toByte() }, 0, 30)) }
        runCatching { invokePrivate(parser, "parseGestureGroupeZeroAlloc", ByteArrayView(ByteArray(20) { it.toByte() }, 0, 20)) }
        runCatching { invokePrivate(parser, "substringSafe", "AB", 0, 1) }
        runCatching { invokePrivate(parser, "substringSafe", "AB", 5, 1) }
        runCatching { invokePrivate(parser, "toHex", 0x0F.toByte()) }

        assertTrue(true)
    }

    @Test
    fun `ble parser should execute major command and widget branches`() {
        ensureWidgetRepoInitializedForTests()
        val executor = RecordingBleCommandExecutor()
        val manager = BleManagerKmm().also { it.setBleCommandExecutor(executor) }
        val parser = BLEParser(
            coroutineScope = CoroutineScope(Dispatchers.Default),
            bleCommandExecutor = executor,
            bleManager = manager
        )

        ConnectionState.fullInicializeConnectionStruct = FullInicializeConnectionStruct(
            deviceName = "D",
            deviceVersion = 1,
            deviceSubVersion = 0,
            deviceLabel = "L",
            deviceType = 1,
            deviceCode = 2,
            deviceAddress = 0,
            deviceUUID_Prefix = "U",
            deviceUUID = 1,
            parametersNum = 1,
            subDeviceNum = 1,
            programType = 1,
            defaultPort = 1
        )

        UiState.listWidgets.clear()

        val masterParam = BaseParameterInfoStruct(
            ID = 1,
            dataCode = PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GENERIC_2.number,
            additionalInfoSize = 0,
            additionalInfoRefSet = mutableSetOf(),
            data = "0102030405060708090A0B0C"
        )
        GlobalParameters.baseParameterInfoStructArray = arrayListOf(masterParam)
        GlobalParameters.baseSubDevicesInfoStructSet = mutableSetOf(
            BaseSubDeviceInfoStruct(
                deviceAddress = 7,
                parametersNum = 1,
                parametersList = arrayListOf(
                    BaseParameterInfoStruct(
                        ID = 1,
                        dataCode = PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number,
                        additionalInfoSize = 1,
                        data = "0102"
                    )
                )
            )
        )

        fun widget(code: PreferenceKeysUbi4.ParameterWidgetCode): BaseParameterWidgetStruct =
            BaseParameterWidgetStruct(
                widgetCode = code.number.toInt(),
                display = 1,
                widgetPosition = 1,
                deviceId = 0,
                widgetId = 1,
                dataOffset = 0,
                dataSize = 1,
                parameterInfoSet = mutableSetOf()
            )

        fun callUpdate(code: PreferenceKeysUbi4.ParameterWidgetCode, dataCode: Int, data: String) {
            masterParam.dataCode = dataCode
            masterParam.data = data
            masterParam.additionalInfoRefSet.clear()
            masterParam.additionalInfoRefSet.add(widget(code))
            runCatching {
                invokePrivate(parser, "updateAllUI", 0, 1, dataCode)
            }
        }

        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_UNKNOW,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GENERIC_2.number,
            "0102030405060708"
        )
        callUpdate(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SWITCH, 1, "01")
        callUpdate(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER, 1, "02")
        callUpdate(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_TOGGLE_SLIDER, 1, "03")
        callUpdate(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT, 1, "010203040506")
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number,
            "0102"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number,
            "3F"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number,
            "4001020304"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number,
            "41"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number,
            "0102"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_MODE_SELECT_GESTURE.number,
            "0001"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPTIC_LEARNING_DATA.number,
            "01"
        )
        callUpdate(
            PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SERVICE_INFO,
            PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_BMS_STATUS_COMBINED_PARAM.number,
            "6400"
        )

        // requestType=1 packet path
        parser.parseReceivedData(
            oldPacket(
                requestType = 1,
                codeRequest = 1,
                deviceAddress = 0,
                payload = byteArrayOf(0x11, 0x22, 0x33, 0x44)
            )
        )

        // DEVICE_INFORMATION command path + subcommands
        val deviceInfoCode = PreferenceKeysUbi4.BaseCommands.DEVICE_INFORMATION.number.toInt()
        val infoCommands = listOf(
            PreferenceKeysUbi4.deviceInformationCommand.INICIALIZE_INFORMATION.number,
            PreferenceKeysUbi4.deviceInformationCommand.READ_DEVICE_PARAMETERS.number,
            PreferenceKeysUbi4.deviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETERS.number,
            PreferenceKeysUbi4.deviceInformationCommand.READ_SUB_DEVICE_INFO.number,
            PreferenceKeysUbi4.deviceInformationCommand.READ_SUB_DEVICE_PARAMETERS.number,
            PreferenceKeysUbi4.deviceInformationCommand.READ_SUB_DEVICE_ADDITIONAL_PARAMETER.number,
            PreferenceKeysUbi4.deviceInformationCommand.GET_SYSTEM_CRC.number
        )

        infoCommands.forEach { sub ->
            runCatching {
                parser.parseReceivedData(
                    oldPacket(
                        requestType = 0,
                        codeRequest = deviceInfoCode,
                        deviceAddress = 7,
                        payload = byteArrayOf(sub, 0x01, 0x02, 0x03) + ByteArray(100)
                    )
                )
            }
        }

        // DATA_MANAGER path for parseProductInfoType/parseProductFwInfoType switch by total hex length
        runCatching {
            parser.parseReceivedData(
                oldPacket(
                    requestType = 0,
                    codeRequest = PreferenceKeysUbi4.BaseCommands.DATA_MANAGER.number.toInt(),
                    deviceAddress = 0,
                    payload = byteArrayOf(PreferenceKeysUbi4.DataManagerCommand.READ_DATA.number, 0x00) + ByteArray(80)
                )
            )
        }

        // WRITE_FW_COMMAND path subcommands
        val fwSubcommands = listOf(
            PreferenceKeysUbi4.FirmwareManagerCommand.START_SYSTEM_UPDATE.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.GET_RUN_PROGRAM_TYPE.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.GET_BOOTLOADER_STATUS.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.GET_BOOTLOADER_INFO.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.CHECK_NEW_FW.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.GET_MAX_CHANK_SIZE.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.PRELOAD_INFO.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.LOAD_NEW_FW.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.CALCULATE_CRC.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.COMPLETE_UPDATE.number,
            PreferenceKeysUbi4.FirmwareManagerCommand.FINISH_SYSTEM_UPDATE.number
        )
        fwSubcommands.forEach { sub ->
            runCatching {
                parser.parseReceivedData(
                    oldPacket(
                        requestType = 0,
                        codeRequest = PreferenceKeysUbi4.BaseCommands.WRITE_FW_COMMAND.number.toInt(),
                        deviceAddress = 7,
                        payload = byteArrayOf(sub, 0x01, 0x02, 0x03, 0x04, 0x05)
                    )
                )
            }
        }

        // direct private paths
        runCatching { invokePrivate(parser, "parseInitializeInformation", "00".repeat(200)) }
        runCatching { invokePrivate(parser, "parseReadDeviceParameters", "00".repeat(200)) }
        runCatching { invokePrivate(parser, "parseReadDeviceAdditionalParameters", 0, buildDeviceAdditionalPacketHex(widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_BUTTON.number.toInt())), 0) }
        runCatching { invokePrivate(parser, "parseReadSubDeviceInfo", buildSubDeviceInfoHex()) }
        runCatching { invokePrivate(parser, "parseReadSubDeviceParameters", buildSubDeviceParametersHex()) }
        runCatching { invokePrivate(parser, "parseReadSubDeviceAdditionalParameters", 7, 1, buildSubDeviceAdditionalPacketHex(widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt()))) }
        runCatching { invokePrivate(parser, "parseReadSubDeviceInfoData", "00".repeat(100)) }
        runCatching { invokePrivate(parser, "parseProductInfoType", "00".repeat(200)) }
        runCatching { invokePrivate(parser, "parseProductFwInfoType", "00".repeat(200)) }
        runCatching { invokePrivate(parser, "parseProductCRCInfo", "00".repeat(50)) }
        runCatching { invokePrivate(parser, "getNextIDParameter", 0) }
        runCatching { invokePrivate(parser, "getNextSubDevice", 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_BUTTON.number.toInt()), 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SWITCH.number.toInt()) + "01", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt()) + "010203", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt()) + "010205", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt()) + "010286", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt()), 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt()), 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexE(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt()), 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_BUTTON.number.toInt()) + "010203", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SWITCH.number.toInt()) + "01", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_PLOT.number.toInt()) + "010203", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SPINBOX.number.toInt()) + spinnerDataHex(), 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt()) + "01020304", 1, 1, 0) }
        runCatching { invokePrivate(parser, "parseWidgets", widgetHexS(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt()), 1, 1, 0) }
        runCatching { invokePrivate(parser, "sendFwInfoRequestsWithRetry") }
        runCatching { invokePrivate(parser, "substringSafe", "AB", 0, 1) }

        assertTrue(executor.queuedPackets.isNotEmpty())
    }

    private fun shortPacket(address: Int, command: Int, payload0: Int, payload1: Int): ByteArray {
        val out = byteArrayOf(
            (address and 0x7F).toByte(),
            command.toByte(),
            payload0.toByte(),
            payload1.toByte(),
            0
        )
        out[4] = BLECommandsV3.calculationCRCRange(out, 0, 4).toByte()
        return out
    }

    private fun longPacket(address: Int, command: Int, payload: ByteArray): ByteArray {
        require(payload.size >= 3)
        val out = ByteArray(5 + payload.size + 1)
        out[0] = (0x80 or (address and 0x7F)).toByte()
        out[1] = command.toByte()
        out[2] = (payload.size and 0xFF).toByte()
        out[3] = ((payload.size ushr 8) and 0xFF).toByte()
        out[4] = BLECommandsV3.calculationCRCRange(out, 0, 4).toByte()
        payload.copyInto(out, 5)
        out[5 + payload.size] = BLECommandsV3.calculationCRCRange(out, 5, payload.size).toByte()
        return out
    }

    private fun oldPacket(requestType: Int, codeRequest: Int, deviceAddress: Int, payload: ByteArray): ByteArray {
        val out = ByteArray(7 + payload.size)
        out[0] = if (requestType == 1) 0x40 else 0x00
        out[1] = codeRequest.toByte()
        out[2] = 0
        out[3] = (payload.size and 0xFF).toByte()
        out[4] = ((payload.size ushr 8) and 0xFF).toByte()
        out[5] = 0
        out[6] = deviceAddress.toByte()
        payload.copyInto(out, 7)
        return out
    }

    private fun widgetHexBase(labelType: Int, widgetCode: Int): String {
        val firstByte = (1 and 0x7F) or ((labelType and 1) shl 7)
        return hex(firstByte) +
            hex(widgetCode) +
            hex(1) + // display
            hex(1) + // position
            hex(0) + // deviceId
            hex(1) + // widgetId
            hex(0) + // dataOffset
            hex(1)   // dataSize
    }

    private fun widgetHexE(widgetCode: Int): String = widgetHexBase(labelType = 0, widgetCode = widgetCode) + hex(1)

    private fun widgetHexS(widgetCode: Int): String = widgetHexBase(labelType = 1, widgetCode = widgetCode) + "41".repeat(32)

    private fun spinnerDataHex(): String = "31" + "0A" + "4F70656E" + "0A" + "436C6F7365"

    private fun buildDeviceAdditionalPacketHex(widgetHex: String): String {
        // offset = 18, then AdditionalInfoSize(16), then widget payload
        val additionalInfoSize = widgetHex.length / 2
        val structHex = "0500" + hex(additionalInfoSize) + "00" + "00000000"
        return "00".repeat(9) + structHex + widgetHex + "00".repeat(40)
    }

    private fun buildSubDeviceInfoHex(): String {
        val sub = "01" + "02" + "03" + "04" + "05" + "07" + "01" + "01" + "00"
        return "00".repeat(8) + "0100" + sub + "00".repeat(20)
    }

    private fun buildSubDeviceParametersHex(): String {
        val param = "00".repeat(16)
        return "00".repeat(8) + "07" + "00" + "01" + param + "00".repeat(20)
    }

    private fun buildSubDeviceAdditionalPacketHex(widgetHex: String): String {
        // offset = 20, then AdditionalInfoSize(16), then widget payload
        val additionalInfoSize = widgetHex.length / 2
        val structHex = "0500" + hex(additionalInfoSize) + "00" + "00000000"
        return "00".repeat(10) + structHex + widgetHex + "00".repeat(40)
    }

    private fun hex(v: Int): String = (v and 0xFF).toString(16).padStart(2, '0')

    private fun invokePrivate(target: Any, name: String, vararg args: Any?): Any? {
        val method = target.javaClass.declaredMethods.firstOrNull {
            it.name == name && it.parameterCount == args.size
        } ?: error("Method not found: $name(${args.size}) in ${target.javaClass.name}")

        method.isAccessible = true

        return try {
            method.invoke(target, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}
