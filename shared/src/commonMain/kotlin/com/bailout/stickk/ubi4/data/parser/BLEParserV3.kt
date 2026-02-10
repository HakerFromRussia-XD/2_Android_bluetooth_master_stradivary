package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArray
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArrayFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.thresholdFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.widgetsMergeEventFlow
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ThresholdParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetLabelType
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParameterInfoStructArray
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BLEParserV3(
    private val coroutineScope: CoroutineScope,
    private val bleCommandExecutor: BleCommandExecutor,
    private val bleManager: BleManagerKmm
) {
    private var mConnected = false
    private var countErrors = 0
    private val deviceSize = 7
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
//        platformLog("BLEParserV3", "parseReceivedSensorsData=$receiveDataString")

        val parameter = ParameterProvider.getParameter(1, 1)
        parameter.data = receiveDataString
        val paddedData: String = receiveDataString.padEnd(12, '0')
        platformLog("updateAllUITest", "data = $data")
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

        parseSubDeviceManagerGetAllSubDevice(data.copyOfRange(5, data.size)).forEach { item ->
            platformLog("BLEParserV3", "item=$item")
        }
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
        val widget1 = BaseSubDeviceInfoStruct(
            deviceAddress = 1,
            parametersList = arrayListOf(BaseParameterInfoStruct(
                dataCode = 1,
                additionalInfoRefSet = mutableSetOf(BaseParameterWidgetStruct(
                    display = 1,
                    widgetPosition = 0,
                    widgetType = 1,
                    widgetCode = ParameterWidgetCode.PWCE_PLOT.number.toInt(),
                    deviceId = 1,
                    widgetId = 1,
                    parameterInfoSet = mutableSetOf(ParameterInfo(1, 1, 1, 1))
                ))
            )))
        baseSubDevicesInfoStructSet.add(widget1)
        parseWidgets(baseSubDevicesInfoStructSet.firstOrNull()?.parametersList?.firstOrNull()?.additionalInfoRefSet?.firstOrNull()!!)
        updateFlow.emit(1)
    }

    private fun parseWidgets(baseParameterWidgetStruct: BaseParameterWidgetStruct) {
        when (baseParameterWidgetStruct.widgetCode) {
            ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt() -> {}
            ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                val plotParameterWidgetEStruct = PlotParameterWidgetEStruct(baseParameterWidgetEStruct = BaseParameterWidgetEStruct(baseParameterWidgetStruct, 0))
                addToListWidgets(plotParameterWidgetEStruct, plotParameterWidgetEStruct.baseParameterWidgetEStruct)
            }
            ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> {}
            ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {}
            ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {}
            ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() -> {}
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
                            coroutineScope.launch { thresholdFlow.emit(ParameterRef(1, 1, 1)) }
                        }
                    }
                    is CommandParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(2, 2, 2)) }
                        }
                    }
                    is PlotParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(3, 3, 3)) }
                        }
                    }
                    is SliderParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(4, 4, 4)) }
                        }
                    }
                    is ToggleSliderParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(5, 5, 5)) }
                        }
                    }
                    is SwitchParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            coroutineScope.launch { widgetsMergeEventFlow.emit(ParameterRef(6, 6, 6)) }
                        }
                        platformLog("SwitchParameterWidgetEStruct_addToListWidgets", "combineWidgetId = $combineWidgetId")
                    }
                    else -> {
                        platformLog("addToListWidgets", "E it = $it")
                    }
                }
            }
        } else if (baseParameterWidgetStruct is BaseParameterWidgetSStruct) {
//            listWidgets.forEach {
//                when (it) {
//                    is CommandParameterWidgetSStruct -> {
//                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
//                            )
//                        }
//                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
//                            )
//                        }
////                        platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                    }
//                    is PlotParameterWidgetSStruct -> {
//                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
//                            )
//                        }
//                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
//                            )
//                        }
////                        platformLog("areEqualExcludingSetIdE", "ThresholdParameterWidgetSStruct ${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                    }
//                    is SliderParameterWidgetSStruct -> {
//                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
//                        platformLog("parseWidgets SLIDER", "Quadruple = ${ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.baseParameterWidgetStruct.dataOffset)}  $combineWidgetId = $combineWidgetIdIterated")
//                        if (combineWidgetId == combineWidgetIdIterated) {
//                            canAdd = false
//                            val set = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
//                            val boundAddr = set.firstOrNull()?.deviceAddress
//                            if (boundAddr == null || boundAddr == deviceAddress) {
//                                set.add(ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset))
//                            }
//                        }
//                    }
//
//                    is ToggleSliderParameterWidgetSStruct -> {
//                        val combineWidgetId =
//                            baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 +
//                                    baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//
//                        val combineWidgetIdIterated =
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 +
//                                    it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
//
//                        if (combineWidgetId == combineWidgetIdIterated) {
//                            canAdd = false
//
//                            val set = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
//                            val boundAddr = set.firstOrNull()?.deviceAddress
//                            if (boundAddr == null || boundAddr == deviceAddress) {
//                                set.add(ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset))
//                            }
//
//                            coroutineScope.launch {
//                                widgetsMergeEventFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode))
//                            }
//                        }
////                        platformLog("areEqualExcludingSetIdE", "ThresholdParameterWidgetSStruct ${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                    }
//                    is ThresholdParameterWidgetSStruct -> {
//                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
//                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
//                            )
//                        }
//                        if (combineWidgetId == combineWidgetIdIterated) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
//                            )
//                        }
////                        platformLog("areEqualExcludingSetIdE", "ThresholdParameterWidgetSStruct ${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                    }
//                    is SwitchParameterWidgetSStruct -> {
//                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
//                            canAdd = false
//                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
//                            )
//                        }
//                        platformLog("SwitchParameterWidgetEStruct_addToListWidgets не E а S", "combineWidgetId = $combineWidgetId")
//                    }
//                    else -> {
//                    }
//                }
//            }
        }
        if (canAdd) {
            listWidgets.add(widget)
            listWidgets.forEach { it ->
                platformLog("listWidgets", "listWidgets: $it")
            }
        }
    }
    private fun areEqualExcludingSetIdS(obj1: BaseParameterWidgetSStruct, obj2: BaseParameterWidgetSStruct): Boolean {
        val baseParameterWidgetStruct1 = obj1.baseParameterWidgetStruct.copy(parameterInfoSet = obj2.baseParameterWidgetStruct.parameterInfoSet)
        val baseParameterWidgetStruct2 = obj2.baseParameterWidgetStruct
        return baseParameterWidgetStruct1 == baseParameterWidgetStruct2
    }
    private fun areEqualExcludingSetIdE(obj1: BaseParameterWidgetEStruct, obj2: BaseParameterWidgetEStruct): Boolean {
        val baseParameterWidgetStruct1 = obj1.baseParameterWidgetStruct.copy(parameterInfoSet = obj2.baseParameterWidgetStruct.parameterInfoSet)
        val baseParameterWidgetStruct2 = obj2.baseParameterWidgetStruct
        return baseParameterWidgetStruct1 == baseParameterWidgetStruct2
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