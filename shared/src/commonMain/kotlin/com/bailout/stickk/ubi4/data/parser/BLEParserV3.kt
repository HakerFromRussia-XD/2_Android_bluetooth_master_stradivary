package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArray
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArrayFlow
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
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
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

    private fun Byte.toHex(): String =
        (this.toInt() and 0xFF)
            .toString(16)
            .uppercase()
            .padStart(2, '0')

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
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
        platformLog("BLEParserV3", "data = $receiveDataString")
        parseSubDeviceManagerGetAllSubDevice(data.copyOfRange(5, data.size)).forEach { item ->
            platformLog("BLEParserV3", "item=$item")
        }
        bleCommandExecutor.getQueueUBI4().allowNext(deviceAddress = 0,   parameterID = 0, receiveDataString = receiveDataString)
    }

    private fun parseSubDeviceManagerGetAllSubDevice(payload: ByteArray?): List<SubDeviceInfo> {
        val devices = mutableListOf<SubDeviceInfo>()

        if (payload == null || payload.isEmpty()) {
            // logger.debug("Ответ SUB_DEVICE_MANAGER: payload пуст")
            return devices
        }

        // Первый байт — подкоманда
        val subcommand = payload[0].toInt() and 0xFF

        if (payload.size <= 1) {
            // logger.debug("Ответ SUB_DEVICE_MANAGER: список устройств пуст (подкоманда=$subcommand)")
            return devices
        }

        // logger.debug("Парсим SUB_DEVICE_MANAGER ответ (подкоманда=$subcommand, ${payload.size - 1} байт данных)")

        var i = 1 // начинаем после подкоманды
        while (i + deviceSize <= payload.size) {
            val device = parseDevice(payload, i)
            if (device != null) {
                devices += device
                // logger.debug("Найдено устройство: адрес=${device.address}, тип=${device.deviceType}, версия=${device.fwVersion}")
            }
            i += deviceSize
        }

        return devices
    }
    private fun parseDevice(bytes: ByteArray, offset: Int): SubDeviceInfo? {
        if (offset < 0 || offset + deviceSize > bytes.size) return null

        val address = bytes[offset + 0].toInt() and 0xFF
        val deviceType = bytes[offset + 1].toInt() and 0xFF
        val deviceCode = bytes[offset + 2].toInt() and 0xFF
        val dfu = bytes[offset + 3].toInt() and 0xFF

        val major = bytes[offset + 4].toInt() and 0xFF
        val minor = bytes[offset + 5].toInt() and 0xFF
        val quickfix = bytes[offset + 6].toInt() and 0xFF

        val fwVersion = "$major.$minor.$quickfix"

        return SubDeviceInfo(
            address = address,
            deviceType = deviceType,
            deviceCode = deviceCode,
            dfu = dfu,
            fwVersion = fwVersion
        )
    }

    suspend fun generatedHardcodeWidgets() {
        baseParameterWidgetSStruct.add(
            BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetPosition = 0,
                widgetCode = ParameterWidgetCode.PWCE_PLOT.number.toInt(),
                deviceId = 1,
                widgetId = 1,
                parameterInfoSet = mutableSetOf(
                    ParameterInfo(1, 1, 1, 0),
                    ParameterInfo(2, PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number, 1, 0),
                    ParameterInfo(3, PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number, 1, 0))
            )
                ,"Графики"
            )
        )
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetPosition = 1,
                widgetCode = ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
                deviceId = 2,
                widgetId = 2,
                parameterInfoSet = mutableSetOf(ParameterInfo(2, 2, 2, 0))
            )
                ,"Чувствительность датчика открытия"
            )
        )
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetPosition = 2,
                widgetCode = ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
                deviceId = 3,
                widgetId = 3,
                parameterInfoSet = mutableSetOf(ParameterInfo(3, 3, 3, 0))
            )
                ,"Чувствительность датчика закрытия"
            )
        )
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetPosition = 3,
                widgetCode = ParameterWidgetCode.PWCE_BUTTON_V3.number.toInt(),
                deviceId = 4,
                widgetId = 4,
                parameterInfoSet = mutableSetOf(
                    ParameterInfo(4, 4, 4, 0),
                    ParameterInfo(5, 4, 4, 0),
                    ParameterInfo(6, 4, 4, 0)
                )
            )
                ,"Калибровка протеза"
            )
        )
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetPosition = 0,
                widgetCode = ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt(),
                deviceId = 1,
                widgetId = 1,
                parameterInfoSet = mutableSetOf(ParameterInfo(1, PreferenceKeysUbi4.ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number, 1, 0))
            )
                ,"Пороги"
            )
        )
        baseParameterWidgetSStruct.add(CommandParameterWidgetSStruct(
            clickCommand = 0,
            pressedCommand = 0,
            releasedCommand = 0,
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 1,
                widgetPosition = 4,
                widgetCode = ParameterWidgetCode.PWCE_BUTTON_V3.number.toInt(),
                deviceId = 5,
                widgetId = 5,
                parameterInfoSet = mutableSetOf(
                    ParameterInfo(BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number.toInt(), ProsthesisModuleControlEnum.PMCE_OPEN_COMMAND.number.toInt(), 5, 0),
                    ParameterInfo(BaseCommandsV3.PROSTHESIS_MODULE_CONTROL.number.toInt(), ProsthesisModuleControlEnum.PMCE_CLOSE_COMMAND.number.toInt(), 6, 1)
                )
            )
                ,"Открыть%Закрыть"
            )
        ))
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2,
            widgetPosition = 0,
            widgetCode = ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
            deviceId = 6,
            widgetId = 6,
            parameterInfoSet = mutableSetOf(ParameterInfo(2, 2, 2, 0))
        )
            ,"Чувствительность датчика открытия"
        )
        )
        baseParameterWidgetSStruct.add(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 3,
            widgetPosition = 0,
            widgetCode = ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
            deviceId = 7,
            widgetId = 7,
            parameterInfoSet = mutableSetOf(ParameterInfo(2, 2, 2, 0))
        )
            ,"Чувствительность датчика открытия"
        )
        )
        baseParameterWidgetSStruct.forEach { widget -> parseWidgets(widget) }
        updateFlow.emit(1)
    }

    private fun parseWidgets(widget: Any) {
        when (widget) {
            is BaseParameterWidgetSStruct -> {
                when (widget.baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        val commandParameterWidgetSStruct = CommandParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                        val switchParameterWidgetSStruct = SwitchParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(switchParameterWidgetSStruct, switchParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> {}
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                        val sliderParameterWidgetSStruct = SliderParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(sliderParameterWidgetSStruct, sliderParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt() -> {
                        val toggleSliderParameterWidgetSStruct = ToggleSliderParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(toggleSliderParameterWidgetSStruct, toggleSliderParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        val plotParameterWidgetSStruct = PlotParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(plotParameterWidgetSStruct, plotParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> {
                        val spinnerParameterWidgetSStruct = SpinnerParameterWidgetSStruct(baseParameterWidgetSStruct = widget)
                        addToListWidgets(spinnerParameterWidgetSStruct, spinnerParameterWidgetSStruct.baseParameterWidgetSStruct)
                    }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {}
                    ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {}
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
                when (it) {
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
            listWidgets.add(widget)
            listWidgets.forEach { it ->
                platformLog("listWidgets", "listWidgets: $it")
            }
        }
    }
    private fun Any.baseStructOrNull(): BaseParameterWidgetStruct? = when (this) {
        is BaseParameterWidgetEStruct -> this.baseParameterWidgetStruct
        is CommandParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is PlotParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SliderParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is ToggleSliderParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetEStruct -> this.baseParameterWidgetEStruct.baseParameterWidgetStruct

        is CommandParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is PlotParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SliderParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is ToggleSliderParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is ThresholdParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct
        is SwitchParameterWidgetSStruct -> this.baseParameterWidgetSStruct.baseParameterWidgetStruct

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