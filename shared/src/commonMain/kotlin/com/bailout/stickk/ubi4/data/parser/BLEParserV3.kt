package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BLECommandsV3.calculationCRCRange
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.WidgetState.batteryPercentFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArray
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArrayFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.thresholdFlowV3
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.ble.ThresholdsV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
//import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSetV3
import com.bailout.stickk.ubi4.data.state.WidgetState.currentGestureFlowV3
import com.bailout.stickk.ubi4.data.state.WidgetState.gestureGroupFlowV3
import com.bailout.stickk.ubi4.data.state.WidgetState.spinnerFlowV3
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.ble.SwitcherV3
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4Wrapper
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_PLOT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_START_CALIBRATE_COMMAND
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BLEParserV3(
    private val coroutineScope: CoroutineScope,
    private val bleCommandExecutor: BleCommandExecutor,
    private val bleManager: BleManagerKmm
) {
    private var mConnected = false
    private var countErrors = 0
    private val deviceSize = 7
    var baseParameterWidgetSStruct: MutableSet<Any> = mutableSetOf()
    data class SubDeviceInfo(
        val address: Int,        // 0..255
        val deviceType: Int,     // 0..255
        val deviceCode: Int,     // 0..255
        val dfu: Int,            // 0..255  // 0 - нельзя прошить, 1 - можно шить
        val fwVersion: String    // "major.minor.quickfix"
    )
    private data class BmsStatusCombinedV3(
        val batLevel: Int,
        val chargeStatus: Int,
        val chargeCurrent: Int
    )

    fun parseReceivedSensorsData(data: ByteArray) {
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)

        val parameter = ParameterProvider.getParameter(1, 1)
        parameter.data = receiveDataString
        val paddedData: String = receiveDataString.padEnd(12, '0')
        try {
            plotArray = arrayListOf(
                castUnsignedCharToInt(paddedData.substringSafe(0, 2).toInt(16).toByte()),
                castUnsignedCharToInt(paddedData.substringSafe(2, 4).toInt(16).toByte()),
                castUnsignedCharToInt(paddedData.substringSafe(4, 6).toInt(16).toByte()),
                castUnsignedCharToInt(paddedData.substringSafe(6, 8).toInt(16).toByte()),
                castUnsignedCharToInt(paddedData.substringSafe(8, 10).toInt(16).toByte()),
                castUnsignedCharToInt(paddedData.substringSafe(10, 12).toInt(16).toByte())
            )
        } catch (e: Exception) {
            showToast("Ошибка 113")
            plotArray = arrayListOf(0, 0, 0, 0, 0, 0)
        }
        coroutineScope.launch { plotArrayFlow.emit(PlotParameterRef(1, 1, plotArray)) }
    }
    fun parseReceivedData(data: ByteArray) {
        // [new widgets V3] тут добавляем обработку ответа устройства: route -> decode codec -> ParameterStore -> emitTarget
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
        val receivePacket = runCatching { parseUbiPacketZeroAlloc(data) }
            .getOrElse { error ->
                platformLog(
                    "[parseReceivedData]",
                    "invalid packet ignored: ${error.message}; data=$receiveDataString"
                )
                bleCommandExecutor.getQueueUBI4().allowNext(
                    deviceAddress = 0,
                    parameterID = 0,
                    receiveDataString = receiveDataString
                )
                platformLog(
                    "sendBytesKmm",
                    "А тут разрешаем протолкнуть следующую команду allowNextV3 "
                )
                return
            }
        val payload = receivePacket.payload
        platformLog("[parseReceivedData]", "command = ${receivePacket.command} receiveDataString = $receiveDataString")
        when (receivePacket.command) {
            SUB_DEVICE_MANAGER.number.toInt() -> {
                val devices = parseSubDeviceManagerGetAllSubDevice(payload)
                coroutineScope.launch {
                    baseSubDevicesInfoStructSet.clear()

                    devices.forEach { d ->
                        baseSubDevicesInfoStructSet.add(
                            BaseSubDeviceInfoStruct(
                                deviceAddress = d.address,
                                deviceType = d.deviceType,
                                deviceCode = d.deviceCode,
                                parametersList = arrayListOf()
                            )
                        )
                    }

                    // ВАЖНО: один снапшот версий на все платы
                    val versionsByAddr: Map<Int, String> = devices.associate { it.address to it.fwVersion }
                    FirmwareInfoState.emitFirmwareInfoV3(versionsByAddr)

                    updateFlow.emit(1)
                }
            }
            else -> {
                if (payload.length == 0) {
                    platformLog("[parseReceivedData]", "payload empty for command=${receivePacket.command}")
                } else {
                    val responseSubcommand = payload.u8(0)
                    if (receivePacket.command == GUI_CONTROL.number.toInt() &&
                        responseSubcommand == GMCE_GET_BATTERY.number.toInt()
                    ) {
                        handleBatteryStatus(payload)
                        bleCommandExecutor.getQueueUBI4().allowNext(deviceAddress = 0,   parameterID = 0, receiveDataString = receiveDataString)
                        platformLog(
                            "sendBytesKmm",
                            "А тут разрешаем протолкнуть следующую команду allowNextV3 "
                        )
                        return
                    }
                    val route = WidgetResponseRoutesV3.find(
                        command = receivePacket.command,
                        responseSubcommand = responseSubcommand
                    )
                    if (route == null) {
                        platformLog(
                            "[parseReceivedData]",
                            "route not found for command=${receivePacket.command} subcommand=$responseSubcommand"
                        )
                    } else {
                        handleWidgetRoute(route, payload)
                    }
                }
            }
        }
        bleCommandExecutor.getQueueUBI4().allowNext(deviceAddress = 0,   parameterID = 0, receiveDataString = receiveDataString)
        platformLog(
            "sendBytesKmm",
            "А тут разрешаем протолкнуть следующую команду allowNextV3 "
        )
    }

    private fun handleWidgetRoute(
        route: WidgetResponseRouteV3,
        payload: ByteArrayView
    ) {
        val parameterMeta = ParameterInfoRegistry.requireMeta(route.parameterKey)
        val parameterInfo = parameterMeta.parameterInfo
        val typedValue = ParameterCodecRegistryV3.decodeFromPayload(
            codecId = parameterMeta.codecId,
            payload = payload
        ) ?: run {
            platformLog(
                "[parseReceivedData]",
                "decode failed for parameter=${route.parameterKey} codec=${parameterMeta.codecId}"
            )
            return
        }

        ParameterStoreV3.put(parameterInfo, typedValue)

        ParameterCodecRegistryV3
            .encodeToSerialized(parameterMeta.codecId, typedValue)
            ?.let { encoded ->
                ParameterProvider.getParameterV3(parameterInfo).data = encoded
            }

        coroutineScope.launch {
            when (route.emitTarget) {
                WidgetEmitTargetV3.SPINNER_FLOW -> spinnerFlowV3.emit(parameterInfo)
                WidgetEmitTargetV3.SLIDER_FLOW -> sliderFlowV3.emit(parameterInfo)
                WidgetEmitTargetV3.THRESHOLD_FLOW -> thresholdFlowV3.emit(parameterInfo)
                WidgetEmitTargetV3.CURRENT_GESTURE_FLOW -> currentGestureFlowV3.emit(parameterInfo)
                WidgetEmitTargetV3.GESTURE_GROUP_FLOW -> gestureGroupFlowV3.emit(parameterInfo)
                WidgetEmitTargetV3.GESTURE_SETTINGS_EVENT ->
                    RxUpdateMainEventUbi4Wrapper.updateUiGestureSettingsV3(parameterInfo)
            }
        }
    }
    private fun handleBatteryStatus(payload: ByteArrayView) {
        val battery = parseBmsStatusCombinedZeroAlloc(payload)
        val normalizedBatLevel = if (battery.batLevel == 0) 1 else battery.batLevel
        val percent = normalizedBatLevel.coerceIn(0, 100)
        platformLog(
            "BatteryParserV3",
            "bat_level=${battery.batLevel}, normalized_bat_level=$normalizedBatLevel, charge_status=${battery.chargeStatus}, charge_current=${battery.chargeCurrent}"
        )
        coroutineScope.launch { batteryPercentFlow.emit(percent) }
    }
    private fun parseBmsStatusCombinedZeroAlloc(payload: ByteArrayView?): BmsStatusCombinedV3 {
        // payload: [subcommand, int8 bat_level, uint8 charge_status, uint16 charge_current(LE)]
        if (payload == null || payload.length < 5) {
            return BmsStatusCombinedV3(
                batLevel = 0,
                chargeStatus = 0,
                chargeCurrent = 0
            )
        }
        val batLevel = payload.s8(1)
        val chargeStatus = payload.u8(2)
        val chargeCurrent = (payload.u8(4) shl 8) or payload.u8(3)
        return BmsStatusCombinedV3(
            batLevel = batLevel,
            chargeStatus = chargeStatus,
            chargeCurrent = chargeCurrent
        )
    }

    private fun parseUbiPacketZeroAlloc(data: ByteArray): UbiPacketView {
        require(data.size >= 5) { "Пакет слишком короткий" }

        val b0 = data[0].toInt() and 0xFF
        val typeBit = (b0 shr 7) and 0x01
        val type = if (typeBit == 1) UbiPacketType.LONG else UbiPacketType.SHORT
        val address = b0 and 0x7F
        val command = data[1].toInt() and 0xFF

        val headerCrc = data[4].toInt() and 0xFF
        val expectedHeaderCrc = calculationCRCRange(data, 0, 4)
        val headerCrcError = headerCrc != expectedHeaderCrc

        return if (type == UbiPacketType.SHORT) {
            val payloadOffset = 2
            val payloadSize = 2

            // SHORT по твоей логике всегда полный (5 байт)
            val payloadView = ByteArrayView(data, payloadOffset, payloadSize)

            UbiPacketView(
                type = type,
                address = address,
                command = command,
                payloadSize = payloadSize,
                payloadOffset = payloadOffset,
                payloadLength = payloadSize,
                headerCrcError = headerCrcError,
                payloadCrcError = false,
                payload = payloadView
            )
        } else {
            val sizeLow = data[2].toInt() and 0xFF
            val sizeHigh = data[3].toInt() and 0xFF
            val payloadSize = (sizeHigh shl 8) or sizeLow

            require(payloadSize >= 3) { "LONG пакет: payloadSize < 3: $payloadSize" }

            val payloadOffset = 5

            // ✅ Строго требуем полный LONG: header(5) + payload + payloadCRC(1)
            val needed = payloadOffset + payloadSize + 1
            require(data.size >= needed) {
                "LONG пакет неполный: size=${data.size}, нужно минимум=$needed (payloadSize=$payloadSize)"
            }

            val payloadView = ByteArrayView(data, payloadOffset, payloadSize)

            val payloadCrcIndex = payloadOffset + payloadSize
            val payloadCrc = data[payloadCrcIndex].toInt() and 0xFF
            val expectedPayloadCrc = calculationCRCRange(data, payloadOffset, payloadSize)
            val payloadCrcError = payloadCrc != expectedPayloadCrc

            UbiPacketView(
                type = type,
                address = address,
                command = command,
                payloadSize = payloadSize,
                payloadOffset = payloadOffset,
                payloadLength = payloadSize,
                headerCrcError = headerCrcError,
                payloadCrcError = payloadCrcError,
                payload = payloadView
            )
        }
    }
    private fun parseSubDeviceManagerGetAllSubDevice(payload: ByteArrayView?): List<SubDeviceInfo> {
        val devices = mutableListOf<SubDeviceInfo>()

        if (payload == null || payload.length == 0) {
            // logger.debug("Ответ SUB_DEVICE_MANAGER: payload пуст")
            return devices
        }


        val subcommand = payload.u8(0)

        if (payload.length <= 1) {
            // logger.debug("Ответ SUB_DEVICE_MANAGER: список устройств пуст (подкоманда=$subcommand)")
            return devices
        }

        var i = 1 // начинаем после подкоманды
        while (i + deviceSize <= payload.length) {
            val device = parseDevice(payload, i)
            if (device != null) devices += device
            i += deviceSize
        }

        return devices
    }
    private fun parseDevice(payload: ByteArrayView, offset: Int): SubDeviceInfo? {
        if (offset < 0 || offset + deviceSize > payload.length) return null

        val address = payload.u8(offset + 0)
        val deviceType = payload.u8(offset + 1)
        val deviceCode = payload.u8(offset + 2)
        val dfu = payload.u8(offset + 3)

        val major = payload.u8(offset + 4)
        val minor = payload.u8(offset + 5)
        val quickfix = payload.u8(offset + 6)

        val fwVersion = "$major.$minor.$quickfix"

        return SubDeviceInfo(
            address = address,
            deviceType = deviceType,
            deviceCode = deviceCode,
            dfu = dfu,
            fwVersion = fwVersion
        )
    }
    private fun parseThresholdZeroAlloc(payload: ByteArrayView?): ThresholdsV3 {
        //парсинг PWCE_GET_THRESHOLD_VALUE
        if (payload == null || payload.length < 3) { return ThresholdsV3() }

        val subcommand = payload.u8(0)
        val openThreshold = payload.u8(1)
        val closeThreshold = payload.u8(2)

        return ThresholdsV3(
            openThreshold = openThreshold,
            closeThreshold = closeThreshold
        )
    }
    private fun parseEMGGainZeroAlloc(payload: ByteArrayView?): EMGGainsV3 {
        // парсинг PWCE_GET_EMG_GAIN
        if (payload == null || payload.length < 3) { return EMGGainsV3() }

        val subcommand = payload.u8(0)
        val openGain = payload.u8(1)
        val closeGain = payload.u8(2)

        return EMGGainsV3(
            openGain = openGain,
            closeGain = closeGain
        )
    }
    private fun parseToggleZeroAlloc(payload: ByteArrayView?): ToggleV3 {
        // парсинг PWCE_GET_CURRENT_GESTURE_NUM
        if (payload == null || payload.length < 2) { return ToggleV3() }
        val subcommand = payload.u8(0)

        return ToggleV3(
            toggleValue = payload.u8(1)
        )
    }
    private fun parseSpinnerZeroAlloc(payload: ByteArrayView?): SpinnerV3 {
        if (payload == null || payload.length < 2) { return SpinnerV3() }
        val subcommand = payload.u8(0)
        return SpinnerV3(
            spinnerValue = payload.u8(1)
        )
    }
    private fun parseSwitchZeroAlloc(payload: ByteArrayView?): SwitcherV3 {
        if (payload == null || payload.length < 2) { return SwitcherV3() }
        val subcommand = payload.u8(0)
        return SwitcherV3(
            checked = payload.u8(1) != 0
        )
    }
    private fun parseCurrentGestureZeroAlloc(payload: ByteArrayView?): CurrentGestureV3 {
        // парсинг PWCE_GET_CURRENT_GESTURE_NUM
        if (payload == null || payload.length < 2) { return CurrentGestureV3(0) }
        val subcommand = payload.u8(0)
        val currentGesture = payload.u8(1)

        return CurrentGestureV3(currentGesture)
    }
    private fun parseGestureZeroAlloc(payload: ByteArrayView?): GestureV3 {
        // парсинг PWCE_GET_GESTURE_SETTING
        if (payload == null || payload.length < 26) {
            return GestureV3()
        }
        val subcommand = payload.u8(0)

        return GestureV3(
            gestureId = payload.u8(1),

            openPosition1 = payload.u8(2),
            openPosition2 = payload.u8(3),
            openPosition3 = payload.u8(4),
            openPosition4 = payload.u8(5),
            openPosition5 = payload.u8(6),
            openPosition6 = payload.u8(7),

            closePosition1 = payload.u8(8),
            closePosition2 = payload.u8(9),
            closePosition3 = payload.u8(10),
            closePosition4 = payload.u8(11),
            closePosition5 = payload.u8(12),
            closePosition6 = payload.u8(13),

            openToCloseTimeShift1 = payload.u8(14),
            openToCloseTimeShift2 = payload.u8(15),
            openToCloseTimeShift3 = payload.u8(16),
            openToCloseTimeShift4 = payload.u8(17),
            openToCloseTimeShift5 = payload.u8(18),
            openToCloseTimeShift6 = payload.u8(19),

            closeToOpenTimeShift1 = payload.u8(20),
            closeToOpenTimeShift2 = payload.u8(21),
            closeToOpenTimeShift3 = payload.u8(22),
            closeToOpenTimeShift4 = payload.u8(23),
            closeToOpenTimeShift5 = payload.u8(24),
            closeToOpenTimeShift6 = payload.u8(25)
        )
    }
    private fun parseGestureGroupeZeroAlloc(payload: ByteArrayView?): RotationGroupV3 {
        // парсинг PWCE_GET_CURRENT_GESTURE_NUM
        if (payload == null || payload.length < 17) { return RotationGroupV3() }
        val subcommand = payload.u8(0)

        return RotationGroupV3(
            gesture1Id = payload.u8(1)     ,
            gesture1ImageId = payload.u8(2),
            gesture2Id = payload.u8(3)     ,
            gesture2ImageId = payload.u8(4),
            gesture3Id = payload.u8(5)     ,
            gesture3ImageId = payload.u8(6),
            gesture4Id = payload.u8(7)     ,
            gesture4ImageId = payload.u8(8),
            gesture5Id = payload.u8(9)     ,
            gesture5ImageId = payload.u8(10),
            gesture6Id = payload.u8(11)     ,
            gesture6ImageId = payload.u8(12),
            gesture7Id = payload.u8(13)     ,
            gesture7ImageId = payload.u8(14),
            gesture8Id = payload.u8(15)     ,
            gesture8ImageId = payload.u8(16),
        )
    }

    suspend fun generatedHardcodeWidgets() {
        // [new widgets V3] тут описываем состав виджетов на экранах (display/widgetPosition/widgetCode/parameterInfoSet)
        baseParameterWidgetSStruct.clear()
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 1,
            widgetCode = PWCE_PLOT_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(
                ParameterInfoRegistry.require(P_KEY_PLOT),
                ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD))
        ),"Графики"))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 1,
            widgetCode = PWCE_SLIDER_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_EMG_GAIN_OPEN_VALUE))
        ),"Чувствительность датчика открытия"))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 1,
            widgetCode = PWCE_SLIDER_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_EMG_GAIN_CLOSE_VALUE))
        ),"Чувствительность датчика закрытия"))
        baseParameterWidgetSStruct.add(CommandParameterWidgetSStruct(
            clickCommand = 0,
            pressedCommand = 0,
            releasedCommand = 0,
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetCode = PWCE_BUTTON_V3.number.toInt(),
                parameterInfoSet = mutableSetOf(
                    ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PMCE_OPEN_COMMAND.number.toInt(), 5, 0),
                    ParameterInfo(PROSTHESIS_MODULE_CONTROL.number.toInt(), PMCE_CLOSE_COMMAND.number.toInt(), 6, 1))
            ),"Открыть%Закрыть")))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 0,
            widgetCode = PWCE_GESTURES_WINDOW_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(
                ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE),
                ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING),
                ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
            )
        ),"Жесты"))
        baseParameterWidgetSStruct.add(ToggleSliderParameterWidgetSStruct(
            minProgress = 20,
            maxProgress = 100,
            increment = 0.1f,
            unitLabel = "сек",
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 2,
                widgetCode = PWCE_TOGGLE_SLIDER_V3.number.toInt(),
                parameterInfoSet = mutableSetOf(
                    ParameterInfoRegistry.require(P_KEY_EMG_CHANGE_GESTURE),
                )
            ),"Переключение жестов сенсорами")))
        baseParameterWidgetSStruct.add(SpinnerParameterWidgetSStruct(
            dataSpinnerParameterWidgetStruct = DataSpinnerParameterWidgetStruct(listOf("ЕМГ 4.0","ЕМГ 3.0","Первый старт"),0),
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 2,
                widgetCode = PWCE_SPINBOX_V3.number.toInt(),
                parameterInfoSet = mutableSetOf(
                    ParameterInfoRegistry.require(P_KEY_EMG_CONTROL_MODE),
                )
            ),"Режим работы ЕМГ")))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2,
            widgetCode = PWCE_SLIDER_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_EMG_MAX_GAIN_VALUE))
        ),"Максимальная чувтсвительность датчиков"))
        baseParameterWidgetSStruct.add(ToggleSliderParameterWidgetSStruct(
            minProgress = 20,
            maxProgress = 100,
            increment = 0.1f,
            unitLabel = "сек",
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 2,
                widgetCode = PWCE_TOGGLE_SLIDER_V3.number.toInt(),
                parameterInfoSet = mutableSetOf(
                    ParameterInfoRegistry.require(P_KEY_EMG_MOVEMENT_LOCK),
                )
            ),"Блокировка движения с ЕМГ")))
        baseParameterWidgetSStruct.add(SpinnerParameterWidgetSStruct(
            dataSpinnerParameterWidgetStruct = DataSpinnerParameterWidgetStruct(listOf("Нормальный","Спортивный","Плавное управление силой","Плавное управление скоростью","Плавное управление силой и скоростью"),0),
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2,
            widgetCode = PWCE_SPINBOX_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(
                ParameterInfoRegistry.require(P_KEY_HAND_CONTROL_MODE),
            )
        ),"Режим работы протеза")))
        baseParameterWidgetSStruct.add(SpinnerParameterWidgetSStruct(
            dataSpinnerParameterWidgetStruct = DataSpinnerParameterWidgetStruct(listOf("Левая","Правая"),0),
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2,
            widgetCode = PWCE_SPINBOX_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(
                ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND),
            )
        ),"Сторона руки")))
        baseParameterWidgetSStruct.add(ToggleSliderParameterWidgetSStruct(
            minProgress = 20,
            maxProgress = 100,
            increment = 0.1f,
            unitLabel = "сек",
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 2,
                widgetCode = PWCE_TOGGLE_SLIDER_V3.number.toInt(),
                parameterInfoSet = mutableSetOf(
                    ParameterInfoRegistry.require(P_KEY_SCREEN_TIMEOUT),
                )
            ),"Время работы экрана")))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2,
            widgetCode = PWCE_TEXT_INPUT_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(
                ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME),
            )
        ),"Имя протеза%Записать"))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2,
            widgetCode = PWCE_BUTTON_V3.number.toInt(),
            parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_START_CALIBRATE_COMMAND))
        ),"Калибровка протеза"))


        baseParameterWidgetSStruct = assignWidgetOrder(baseParameterWidgetSStruct)

        generatedParameters()
        baseParameterWidgetSStruct.forEach { widget -> parseWidgets(widget) }
        updateFlow.emit(1)
    }
    private fun generatedParameters() {
        val parametersByDevice = linkedMapOf<Int, LinkedHashMap<ParameterInfo<Int,Int,Int,Int>, BaseParameterInfoStruct>>()

        baseParameterWidgetSStruct.forEach { widget ->
            val baseStruct = widget.baseStructOrNull() ?: return@forEach

            baseStruct.parameterInfoSet.forEach { parameterInfo ->
                val key = ParameterInfo(
                    (parameterInfo.parameterID as Number).toInt(),
                    (parameterInfo.dataCode as Number).toInt(),
                    (parameterInfo.deviceAddress as Number).toInt(),
                    (parameterInfo.dataOffsets as Number).toInt()
                )

                val deviceMap =
                    parametersByDevice.getOrPut(key.deviceAddress) { LinkedHashMap() }

                deviceMap[key] = BaseParameterInfoStruct(
                    ID = key.parameterID,
                    dataCode = key.dataCode
                )
            }
        }

        baseSubDevicesInfoStructSetV3.clear()
        parametersByDevice.forEach { (deviceAddress, paramsById) ->
            baseSubDevicesInfoStructSetV3.add(
                BaseSubDeviceInfoStruct(
                    deviceAddress = deviceAddress,
                    parametersNum = paramsById.size,
                    parametersList = ArrayList(paramsById.values)
                )
            )
            platformLog("baseSubDevicesInfoStructSetV3", "it: $deviceAddress $paramsById")
        }
        baseSubDevicesInfoStructSetV3.forEach { platformLog("baseSubDevicesInfoStructSetV3", "до $it") }
    }
    private fun parseWidgets(widget: Any) {
        when (widget) {
            is BaseParameterWidgetSStruct -> {
                platformLog(
                    "PWCE_GESTURES_WINDOW_V3",
                    "expected=${PWCE_GESTURES_WINDOW_V3.number} actual=${widget.baseParameterWidgetStruct.widgetCode}"
                )
                when (widget.baseParameterWidgetStruct.widgetCode) {
                    PWCE_BUTTON.number.toInt(),
                    PWCE_BUTTON_V3.number.toInt(),
                    PWCE_TEXT_INPUT_V3.number.toInt() -> {
                        val commandParameterWidgetSStruct = CommandParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    PWCE_SWITCH.number.toInt(),
                    PWCE_SWITCH_V3.number.toInt() -> {
                        val switchParameterWidgetSStruct = SwitchParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(switchParameterWidgetSStruct, switchParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    PWCE_COMBOBOX.number.toInt() -> {}
                    PWCE_SLIDER.number.toInt(),
                    PWCE_SLIDER_V3.number.toInt() -> {
                        val sliderParameterWidgetSStruct = SliderParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(sliderParameterWidgetSStruct, sliderParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    PWCE_TOGGLE_SLIDER.number.toInt(),
                    PWCE_TOGGLE_SLIDER_V3.number.toInt()-> {
                        val toggleSliderParameterWidgetSStruct = ToggleSliderParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(toggleSliderParameterWidgetSStruct, toggleSliderParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    PWCE_PLOT.number.toInt(),
                    PWCE_PLOT_V3.number.toInt() -> {
                        val plotParameterWidgetSStruct = PlotParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(plotParameterWidgetSStruct, plotParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    PWCE_SPINBOX.number.toInt(),
                    PWCE_SPINBOX_V3.number.toInt()-> {
                        val spinnerParameterWidgetSStruct = SpinnerParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(spinnerParameterWidgetSStruct, spinnerParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {}
                    PWCE_GESTURES_WINDOW.number.toInt(),
                    PWCE_GESTURES_WINDOW_V3.number.toInt() -> {
                        addToListWidgets(widget, widget)
                    }
                }
            }
            is CommandParameterWidgetSStruct -> {
                val commandParameterWidgetSStruct = CommandParameterWidgetSStruct(
                    clickCommand = widget.clickCommand,
                    pressedCommand = widget.pressedCommand,
                    releasedCommand = widget.releasedCommand,
                    baseParameterWidgetSStruct = widget.baseParameterWidgetSStruct)
                addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct)
            }
            is ToggleSliderParameterWidgetSStruct -> {
                val toggleSliderParameterWidgetSStruct = ToggleSliderParameterWidgetSStruct(
                    minProgress = widget.minProgress,
                    maxProgress = widget.maxProgress,
                    increment = widget.increment,
                    unitLabel = widget.unitLabel,
                    baseParameterWidgetSStruct = widget.baseParameterWidgetSStruct)
                addToListWidgets(toggleSliderParameterWidgetSStruct, toggleSliderParameterWidgetSStruct.baseParameterWidgetSStruct)
            }
            is SpinnerParameterWidgetSStruct -> {
                val spinnerParameterWidgetSStruct = SpinnerParameterWidgetSStruct(
                    dataSpinnerParameterWidgetStruct = widget.dataSpinnerParameterWidgetStruct,
                    baseParameterWidgetSStruct = widget.baseParameterWidgetSStruct)
                addToListWidgets(spinnerParameterWidgetSStruct, spinnerParameterWidgetSStruct.baseParameterWidgetSStruct)
            }
        }
    }
    private fun addToListWidgets(widget: Any, baseParameterWidgetStruct: Any) {
        var canAdd = true
        if (baseParameterWidgetStruct is BaseParameterWidgetEStruct) {
            listWidgets.forEach {
                when (it) {
                    is BaseParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
//                            coroutineScope.launch { thresholdFlow.emit(ParameterRef(1, 1, 1)) }
                        }
                    }
                    is CommandParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
//                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(2, 2, 2)) }
                        }
                    }
                    is PlotParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
//                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(3, 3, 3)) }
                        }
                    }
                    is SliderParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
//                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(4, 4, 4)) }
                        }
                    }
                    is ToggleSliderParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
//                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(5, 5, 5)) }
                        }
                    }
                    is SwitchParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
//                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(6, 6, 6)) }
                        }
                    }
                    else -> {}
                }
            }
        } else if (baseParameterWidgetStruct is BaseParameterWidgetSStruct) {
            listWidgets.forEach {
//                platformLog("PWCE_GESTURES_WINDOW_V3", "пошли по S  it = $it")
                when (it) {
                    is BaseParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetStruct.widgetId
                        platformLog("PWCE_GESTURES_WINDOW_V3", "пошли по S BaseParameterWidgetSStruct")
                        if (combineWidgetId == combineWidgetIdIterated) { canAdd = false }
                    }
                    is CommandParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                        }
                    }
                    is PlotParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                        }
                    }
                    is SliderParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                        }
                    }

                    is ToggleSliderParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false

                        }
                    }
                    is ThresholdParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                        }
                    }
                    is SwitchParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                        }
                   }
                    else -> {}
                }
            }
        }
        if (canAdd) {
//            platformLog("PWCE_GESTURES_WINDOW_V3", "смогли добавить")
            listWidgets.add(widget)
            listWidgets.forEach { it ->
                platformLog("listWidgets", "listWidgets: $it")
            }
        } else {
            platformLog("PWCE_GESTURES_WINDOW_V3", "не смогли добавить")
        }
    }

    private fun Byte.toHex(): String =
        (this.toInt() and 0xFF)
            .toString(16)
            .uppercase()
            .padStart(2, '0')
    private fun ByteArrayView.u8(i: Int): Int = bytes[offset + i].toInt() and 0xFF
    private fun ByteArrayView.s8(i: Int): Int = bytes[offset + i].toInt()
    // при добавлении нового виджета в generatedHardcodeWidgets
    // не нужно вручную выставлять widgetPosition/widgetId — они назначатся здесь по порядку.
    private fun assignWidgetOrder(widgets: MutableSet<Any>): MutableSet<Any> {
        val nextPositionByDisplay = mutableMapOf<Int, Int>()
        var nextWidgetId = 1
        val orderedWidgets = linkedSetOf<Any>()

        widgets.forEach { widget ->
            val baseStruct = widget.baseStructOrNull()
            if (baseStruct == null) {
                orderedWidgets.add(widget)
                return@forEach
            }

            val assignedPosition = nextPositionByDisplay[baseStruct.display] ?: 0
            nextPositionByDisplay[baseStruct.display] = assignedPosition + 1

            val assignedBaseStruct = baseStruct.copy(
                widgetPosition = assignedPosition,
                widgetId = nextWidgetId++
            )
            orderedWidgets.add(widget.withBaseStruct(assignedBaseStruct))
        }

        return orderedWidgets
    }
    private fun Any.withBaseStruct(baseStruct: BaseParameterWidgetStruct): Any = when (this) {
        is BaseParameterWidgetSStruct -> copy(baseParameterWidgetStruct = baseStruct)
        is CommandParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        is PlotParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        is SliderParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        is ToggleSliderParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        is ThresholdParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        is SwitchParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        is SpinnerParameterWidgetSStruct -> copy(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct.copy(baseParameterWidgetStruct = baseStruct)
        )
        else -> this
    }
    private fun Any.baseStructOrNull(): BaseParameterWidgetStruct? = when (this) {
        is BaseParameterWidgetEStruct -> this.baseParameterWidgetStruct
        is CommandParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is PlotParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SliderParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is ToggleSliderParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SpinnerParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct

        is BaseParameterWidgetSStruct -> this.baseParameterWidgetStruct
        is CommandParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is PlotParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SliderParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is ToggleSliderParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is ThresholdParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SpinnerParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        else -> null
    }
    private fun String.substringSafe(startIndex: Int, endIndex: Int): String {
        // корректный диапазон — отдаём подстроку, но гарантируем чётную длину (для hex)
        if (startIndex >= 0 && endIndex <= length && startIndex < endIndex) {
            val s = substring(startIndex, endIndex)
            return if (s.length % 2 == 1) "0$s" else s
        }
        // некорректный диапазон — больше не возвращаем "", чтобы не падать в toInt(16)
        platformLog(
            "substringSafe",
            "Невалидные индексы: ожидали [$startIndex, $endIndex), но длина строки = $length"
        )
        return "00"
    }
}
