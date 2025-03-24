//package com.bailout.stickk.ubi4.data.parser
//
//import com.bailout.stickk.ubi4.ble.BLECommands
//import com.bailout.stickk.ubi4.ble.ParameterProvider
//import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
//import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
//import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
//import com.bailout.stickk.ubi4.data.DeviceInfoStructs
//import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
//import com.bailout.stickk.ubi4.data.additionalParameter.AdditionalInfoSizeStruct
//import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceArrayInfoStruct
//import com.bailout.stickk.ubi4.data.widget.endStructures.*
//import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
//import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
//import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
//import com.bailout.stickk.ubi4.models.ble.ParameterRef
//import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
//import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.AdditionalParameterInfoType
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabelType
//import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParametrInfoStructArray
//import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
//import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
//import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
//import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.ADDITIONAL_INFO_SEG
//import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.ADDITIONAL_INFO_SIZE_STRUCT_SIZE
//import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.BASE_PARAMETER_INFO_STRUCT_SIZE
//import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.HEADER_BLE_OFFSET
//import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.READ_DEVICE_ADDITIONAL_PARAMETR_DATA
//import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA
//import com.bailout.stickk.ubi4.utility.EncodeByteToHex
//import com.bailout.stickk.ubi4.utility.logging.platformLog
//import com.bailout.stickk.ubi4.utility.showToast
//import com.bailout.stickk.ubi4.utility.sleep
//import com.bailout.stickk.ubi4.utility.synchronized
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.json.Json
//import kotlin.experimental.and
//
///**
// * BLEParser – класс для разбора входящих BLE-данных.
// *
// * Этот класс оформлен как синглтон (object) и не имеет конструктора.
// * Он использует глобальную переменную main (например, MainActivityUBI4.main) для вызова методов:
// * - bleCommandWithQueue
// * - showToast
// * - getQueueUBI4, updateSerialNumber и т.д.
// *
// * Все платформозависимые вызовы (логирование, задержки, синхронизация) реализованы через
// * expect/actual-функции.
// */
//class BLEParser {
//    // Внутреннее состояние и счетчики
//    private var mConnected: Boolean = false
//    private var count: Int = 0
//    private var numberSubDevice: Int = 0
//    private var subDeviceCounter: Int = 0           // для переключения между сабдевайсами
//    private var subDeviceChankParametersCounter: Int = 0
//    private var subDeviceAdditionalCounter: Int = 1
//    private var countErrors: Int = 0
//
//    /**
//     * Основной метод разбора входящих данных.
//     */
//    fun parseReceivedData(data: ByteArray?) {
//        if (data == null) return
//
//        val receiveDataString = EncodeByteToHex.bytesToHexString(data)
//        val dataTransmissionDirection = data[0]
//        val bridgeIndicator = castUnsignedCharToInt(data[0] and 0b10000000.toByte()) / 128
//        val requestType = castUnsignedCharToInt(data[0] and 0b01000000.toByte()) / 64
//        val waitingAnswer = castUnsignedCharToInt(data[0] and 0b00100000.toByte()) / 32
//        val codeRequest = if (data.size > 1) data[1] else 0
//        val dataLength = if (data.size > 3)
//            castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4]) * 256
//        else 0
//        val CRC = if (data.size > 5) castUnsignedCharToInt(data[5]) else 0
//        val deviceAddress = if (data.size > 6) castUnsignedCharToInt(data[6]) else 0
//        val packageCodeRequest = if (data.size > 7) data[7] else 0
//        val ID = if (data.size > 8) castUnsignedCharToInt(data[8]) else 0
//
//        platformLog("BLEParser", "BLE debug: data.size=${data.size}, dataString=$receiveDataString, requestType=$requestType")
//
//        if (requestType == 1) {
//            // Разбор параметров
//            val parameterID = codeRequest.toInt()
//            val parameter = ParameterProvider.getParameter(deviceAddress, parameterID)
//            parameter.data = receiveDataString.substring(HEADER_BLE_OFFSET * 2)
//            updateAllUI(deviceAddress, parameterID, parameter.dataCode)
//        } else {
//            // Разбор команд
//            when (codeRequest) {
//                0.toByte() -> platformLog("BLEParser", "TEST parser DEFAULT")
//                BaseCommands.DEVICE_INFORMATION.number -> {
//                    platformLog("BLEParser", "TEST parser DEVICE_INFORMATION, packageCodeRequest=$packageCodeRequest")
//                    parseDeviceInformation(packageCodeRequest, ID, deviceAddress, receiveDataString)
//                }
//                BaseCommands.DATA_MANAGER.number -> {
//                    platformLog("BLEParser", "TEST parser DATA_MANAGER")
//                    parseDataManager(packageCodeRequest, receiveDataString)
//                }
//                BaseCommands.WRITE_FW_COMMAND.number ->
//                    platformLog("BLEParser", "TEST parser WRITE_FW_COMMAND")
//                BaseCommands.DEVICE_ACCESS_COMMAND.number ->
//                    platformLog("BLEParser", "TEST parser DEVICE_ACCESS_COMMAND")
//                BaseCommands.ECHO_COMMAND.number ->
//                    platformLog("BLEParser", "TEST parser ECHO_COMMAND")
//                BaseCommands.SUB_DEVICE_MANAGER.number ->
//                    platformLog("BLEParser", "TEST parser SUB_DEVICE_MANAGER")
//                BaseCommands.GET_DEVICE_STATUS.number ->
//                    platformLog("BLEParser", "TEST parser GET_DEVICE_STATUS")
//                BaseCommands.DATA_TRANSFER_SETTINGS.number ->
//                    platformLog("BLEParser", "TEST parser DATA_TRANSFER_SETTINGS")
//                BaseCommands.COMPLEX_PARAMETER_TRANSFER.number -> {
//                    platformLog("BLEParser", "TEST parser COMPLEX_PARAMETER_TRANSFER: data.size=${data.size}, dataLength=$dataLength")
//                    var dataLengthMax = dataLength
//                    var remainingDataLength = dataLength
//                    var counter = 1
//
//                    try {
//                        while (remainingDataLength > 0) {
//                            val dataHex = EncodeByteToHex.bytesToHexString(data)
//                            val offset = HEADER_BLE_OFFSET + (dataLengthMax - remainingDataLength)
//                            val deviceAddressFromHex = castUnsignedCharToInt(
//                                receiveDataString.substring(offset * 2, (offset + 1) * 2).toInt(16).toByte()
//                            )
//                            val parameterIDFromHex = castUnsignedCharToInt(
//                                receiveDataString.substring((offset + 1) * 2, (offset + 2) * 2).toInt(16).toByte()
//                            )
//                            val parameter = ParameterProvider.getParameter(deviceAddressFromHex, parameterIDFromHex)
//                            platformLog("BLEParser", "Complex transfer: counter=$counter, remainingDataLength=$remainingDataLength, data=$dataHex")
//                            val dataStart = (offset + 2) * 2
//                            val dataEnd = dataStart + parameter.parameterDataSize * 2
//                            parameter.data = receiveDataString.substring(dataStart, dataEnd)
//                            updateAllUI(deviceAddressFromHex, parameterIDFromHex, parameter.dataCode)
//                            remainingDataLength -= (parameter.parameterDataSize + 2)
//                            counter++
//                        }
//                    } catch (e: Exception) {
//                        showToast("Exception: ${e.message}")
//                    }
//                }
//            }
//        }
//
//        // Если не комплексный трансфер – разрешаем следующий пакет
//        if (requestType == 1 || codeRequest != BaseCommands.COMPLEX_PARAMETER_TRANSFER.number) {
//            main.getQueueUBI4().allowNext()
//        }
//    }
//
//    /**
//     * Обновляет UI в зависимости от dataCode.
//     */
//    private fun updateAllUI(deviceAddress: Int, parameterID: Int, dataCode: Int) {
//        platformLog("BLEParser", "updateAllUI: dataCode=$dataCode")
//        when (dataCode) {
//            ParameterDataCodeEnum.PDCE_UNIVERSAL_CONTROL_INPUT.number -> {
//                platformLog("BLEParser", "Universal Control Input: deviceAddress=$deviceAddress, parameterID=$parameterID, data=${ParameterProvider.getParameter(deviceAddress, parameterID).data}")
//            }
//            ParameterDataCodeEnum.PDCE_EMG_CH_1_3_VAL.number -> {
//                val parameter = ParameterProvider.getParameter(deviceAddress, parameterID)
//                val data = parameter.data
//                val paddedData = data.padEnd(12, '0')
//                try {
//                    val plotArray = arrayListOf(
//                        castUnsignedCharToInt(paddedData.substring(0, 2).toInt(16).toByte()),
//                        castUnsignedCharToInt(paddedData.substring(2, 4).toInt(16).toByte()),
//                        castUnsignedCharToInt(paddedData.substring(4, 6).toInt(16).toByte()),
//                        castUnsignedCharToInt(paddedData.substring(6, 8).toInt(16).toByte()),
//                        castUnsignedCharToInt(paddedData.substring(8, 10).toInt(16).toByte()),
//                        castUnsignedCharToInt(paddedData.substring(10, 12).toInt(16).toByte())
//                    )
//                    GlobalScope.launch {
//                        main.plotArrayFlow.emit(PlotParameterRef(deviceAddress, parameterID, plotArray))
//                    }
//                } catch (e: Exception) {
//                    main.showToast("Ошибка 113")
//                }
//            }
//            ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number -> {
//                RxUpdateMainEventUbi4.getInstance().updateUiGestureSettings(dataCode)
//            }
//            ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
//                RxUpdateMainEventUbi4.getInstance().updateUiRotationGroup(ParameterRef(deviceAddress, parameterID, dataCode))
//                GlobalScope.launch {
//                    main.rotationGroupFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode))
//                }
//            }
//            ParameterDataCodeEnum.PDCE_OPTIC_LEARNING_DATA.number -> {
//                RxUpdateMainEventUbi4.getInstance().updateUiOpticTraining(ParameterRef(deviceAddress, parameterID, dataCode))
//            }
//            ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number -> {
//                GlobalScope.launch {
//                    main.thresholdFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode))
//                }
//            }
//            else -> platformLog("BLEParser", "Unhandled dataCode: $dataCode")
//        }
//    }
//
//    // --- Методы разбора команд устройства ---
//    private fun parseDeviceInformation(packageCodeRequest: Byte, ID: Int, deviceAddress: Int, receiveDataString: String) {
//        when (packageCodeRequest) {
//            0.toByte() -> platformLog("BLEParser", "DeviceInformation: DEFAULT")
//            DeviceInformationCommand.INICIALIZE_INFORMATION.number -> parseInitializeInformation(receiveDataString)
//            DeviceInformationCommand.READ_DEVICE_PARAMETRS.number -> {
//                try {
//                    parseReadDeviceParameters(receiveDataString)
//                } catch (e: Exception) {
//                    main.showToast("Не удалось распарсить READ_DEVICE_PARAMETRS")
//                }
//            }
//            DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETRS.number -> {
//                parseReadDeviceAdditionalParameters(ID, receiveDataString, deviceAddress)
//            }
//            DeviceInformationCommand.READ_SUB_DEVICE_INFO.number -> {
//                platformLog("BLEParser", "READ_SUB_DEVICE_INFO")
//                parseReadSubDeviceInfo(receiveDataString)
//            }
//            DeviceInformationCommand.READ_SUB_DEVICE_PARAMETERS.number -> {
//                platformLog("BLEParser", "READ_SUB_DEVICE_PARAMETERS")
//                parseReadSubDeviceParameters(receiveDataString)
//            }
//            DeviceInformationCommand.READ_SUB_DEVICE_ADDITIONAL_PARAMETER.number -> {
//                platformLog("BLEParser", "READ_SUB_DEVICE_ADDITIONAL_PARAMETER")
//                val addressSubDevice = castUnsignedCharToInt(receiveDataString.substring(16, 18).toInt(16).toByte())
//                val parameterID = castUnsignedCharToInt(receiveDataString.substring(18, 20).toInt(16).toByte())
//                parseReadSubDeviceAdditionalParameters(addressSubDevice, parameterID, receiveDataString)
//            }
//            else -> platformLog("BLEParser", "Unknown DeviceInformation command: $packageCodeRequest")
//        }
//    }
//
//    private fun parseDataManager(packageCodeRequest: Byte, receiveDataString: String) {
//        platformLog("BLEParser", "parseDataManager: packageCodeRequest=$packageCodeRequest, dataString=$receiveDataString")
//        when (packageCodeRequest) {
//            0.toByte() -> platformLog("BLEParser", "DataManager: DEFAULT")
//            DataManagerCommand.READ_AVAILABLE_SLOTS.number -> platformLog("BLEParser", "READ_AVAILABLE_SLOTS")
//            DataManagerCommand.WRITE_SLOT.number -> platformLog("BLEParser", "WRITE_SLOT")
//            DataManagerCommand.READ_DATA.number -> {
//                platformLog("BLEParser", "READ_DATA")
//                parseProductInfoType(receiveDataString)
//            }
//            DataManagerCommand.WRITE_DATA.number -> platformLog("BLEParser", "WRITE_DATA")
//            DataManagerCommand.RESET_TO_FACTORY.number -> platformLog("BLEParser", "RESET_TO_FACTORY")
//            DataManagerCommand.SAVE_DATA.number -> platformLog("BLEParser", "SAVE_DATA")
//            else -> platformLog("BLEParser", "Unknown DataManager command: $packageCodeRequest")
//        }
//    }
//
//    private fun parseInitializeInformation(receiveDataString: String) {
//        fullInicializeConnectionStruct =
//            Json.decodeFromString<FullInicializeConnectionStruct>("\"${receiveDataString.substring(18)}\"")
//        platformLog("BLEParser", "INICIALIZE_INFORMATION: $fullInicializeConnectionStruct")
//        main.bleCommandWithQueue(
//            BLECommands.requestBaseParametrInfo(0x00, fullInicializeConnectionStruct.parametrsNum.toByte()),
//            MAIN_CHANNEL,
//            WRITE
//        ) {}
//        platformLog("BLEParser", "parametrsNum=${fullInicializeConnectionStruct.parametrsNum}")
//    }
//
//    private fun parseReadDeviceParameters(receiveDataString: String) {
//        val listA = arrayListOf<BaseParameterInfoStruct>()
//        platformLog("BLEParser", "READ_DEVICE_PARAMETRS: $receiveDataString")
//        for (i in 0 until fullInicializeConnectionStruct.parametrsNum) {
//            val start = 20 + i * BASE_PARAMETER_INFO_STRUCT_SIZE
//            val end = 20 + (i + 1) * BASE_PARAMETER_INFO_STRUCT_SIZE
//            listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"${receiveDataString.substring(start, end)}\""))
//        }
//        baseParametrInfoStructArray = listA
//        var widgetCount = 0
//        baseParametrInfoStructArray.forEach {
//            widgetCount += it.additionalInfoSize
//            platformLog("BLEParser", "Parameter: $it, widgetCount=$widgetCount")
//        }
//        if (baseParametrInfoStructArray.isNotEmpty()) {
//            platformLog("BLEParser", "Запрос дополнительного параметра")
//            if (baseParametrInfoStructArray[0].additionalInfoSize != 0) {
//                main.bleCommandWithQueue(
//                    BLECommands.requestAdditionalParametrInfo(baseParametrInfoStructArray[0].ID.toByte()),
//                    MAIN_CHANNEL,
//                    WRITE
//                ) {}
//            } else {
//                val nextID = getNextIDParameter(0)
//                if (nextID != 0) {
//                    main.bleCommandWithQueue(
//                        BLECommands.requestAdditionalParametrInfo(baseParametrInfoStructArray[nextID].ID.toByte()),
//                        MAIN_CHANNEL,
//                        WRITE
//                    ) {}
//                } else {
//                    main.bleCommandWithQueue(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE) {}
//                }
//            }
//        }
//    }
//
//    private fun parseReadDeviceAdditionalParameters(ID: Int, receiveDataString: String, deviceAddress: Int) {
//        platformLog("BLEParser", "READ_DEVICE_ADDITIONAL_PARAMETRS for ID=$ID")
//        val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
//        var dataOffset = 0
//        if (baseParametrInfoStructArray[ID].additionalInfoSize != 0) {
//            for (i in 0 until baseParametrInfoStructArray[ID].additionalInfoSize) {
//                val start = offset + i * ADDITIONAL_INFO_SIZE_STRUCT_SIZE
//                val end = offset + (i + 1) * ADDITIONAL_INFO_SIZE_STRUCT_SIZE
//                val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(start, end)}\"")
//                val dataStart = offset + baseParametrInfoStructArray[ID].additionalInfoSize * ADDITIONAL_INFO_SEG + dataOffset * 2
//                val dataEnd = dataStart + additionalInfoSizeStruct.infoSize * 2
//                val receiveDataStringForParse = receiveDataString.substring(dataStart, dataEnd)
//                dataOffset = additionalInfoSizeStruct.infoSize
//                when (additionalInfoSizeStruct.infoType) {
//                    AdditionalParameterInfoType.WIDGET.number.toInt() -> {
//                        parseWidgets(receiveDataStringForParse, parameterID = ID, dataCode = baseParametrInfoStructArray[ID].dataCode, deviceAddress)
//                        GlobalScope.launch { main.sendWidgetsArray() }
//                    }
//                    else -> platformLog("BLEParser", "Unhandled additional info type: ${additionalInfoSizeStruct.infoType}")
//                }
//            }
//        }
//        val nextID = getNextIDParameter(ID)
//        if (nextID != 0) {
//            platformLog("BLEParser", "Запрос следующего дополнительного параметра, ID=$nextID")
//            main.bleCommandWithQueue(
//                BLECommands.requestAdditionalParametrInfo(baseParametrInfoStructArray[nextID].ID.toByte()),
//                MAIN_CHANNEL,
//                WRITE
//            ) {}
//        } else {
//            platformLog("BLEParser", "Конец запроса дополнительных параметров")
//            main.bleCommandWithQueue(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE) {}
//        }
//    }
//
//    private fun parseReadSubDeviceInfo(receiveDataString: String) {
//        platformLog("BLEParser", "parseReadSubDeviceInfo: $receiveDataString")
//        val subDevices = Json.decodeFromString<BaseSubDeviceArrayInfoStruct>("\"${receiveDataString.substring(16)}\"")
//        baseSubDevicesInfoStructSet = subDevices.baseSubDeviceInfoStructArray
//        baseSubDevicesInfoStructSet.forEach {
//            platformLog("BLEParser", "SubDevice: $it")
//        }
//        numberSubDevice = subDevices.count
//        subDeviceCounter = 0
//        subDeviceChankParametersCounter = 0
//        subDeviceAdditionalCounter = 1
//        val parametrsNum = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum
//        if (baseSubDevicesInfoStructSet.isNotEmpty()) {
//            if (getNextSubDevice(subDeviceCounter) != -1) {
//                var numberCount = 10
//                if (subDeviceChankParametersCounter == (parametrsNum / 10)) {
//                    numberCount = parametrsNum - subDeviceChankParametersCounter * 10
//                }
//                main.bleCommandWithQueue(
//                    BLECommands.requestSubDeviceParametrs(
//                        baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress,
//                        subDeviceChankParametersCounter * 10,
//                        numberCount
//                    ), MAIN_CHANNEL, WRITE
//                ) {}
//            } else {
//                main.showToast("Нет сабдевайсов с параметрами")
//            }
//        } else {
//            main.showToast("Сабдевайсов нет")
//        }
//    }
//
//    private fun parseReadSubDeviceParameters(receiveDataString: String) {
//        var _deviceAddress = 0
//        var _parametrsNum = 0
//        var deviceAddress = 0
//        var startIndex = 0
//        var quantitiesReadParameters = 0
//        var numberCount = 10
//        if (subDeviceCounter < baseSubDevicesInfoStructSet.size) {
//            val listA = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList
//            if (receiveDataString.isEmpty() || receiveDataString.length < 22) return
//            _deviceAddress = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress
//            _parametrsNum = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum
//            deviceAddress = castUnsignedCharToInt(receiveDataString.substring(16, 18).toInt(16).toByte())
//            startIndex = castUnsignedCharToInt(receiveDataString.substring(18, 20).toInt(16).toByte())
//            quantitiesReadParameters = castUnsignedCharToInt(receiveDataString.substring(20, 22).toInt(16).toByte())
//            if (subDeviceChankParametersCounter == (_parametrsNum / 10)) {
//                numberCount = _parametrsNum - subDeviceChankParametersCounter * 10
//            }
//            if (_deviceAddress == deviceAddress && subDeviceChankParametersCounter * 10 == startIndex && quantitiesReadParameters == numberCount) {
//                for (i in 0 until numberCount) {
//                    val start = 22 + i * BASE_PARAMETER_INFO_STRUCT_SIZE
//                    val end = 22 + (i + 1) * BASE_PARAMETER_INFO_STRUCT_SIZE
//                    if (end <= receiveDataString.length) {
//                        try {
//                            val parameterJson = receiveDataString.substring(start, end)
//                            listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"$parameterJson\""))
//                        } catch (e: Exception) {
//                            platformLog("BLEParser", "Ошибка декодирования параметра: ${e.message}")
//                        }
//                    } else {
//                        platformLog("BLEParser", "Индексы $start-$end выходят за пределы строки длиной ${receiveDataString.length}")
//                        break
//                    }
//                }
//                baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList = listA
//            }
//            if (getNextSubDevice(subDeviceCounter) != -1) {
//                if (_parametrsNum > 10) {
//                    subDeviceChankParametersCounter++
//                }
//                main.bleCommandWithQueue(
//                    BLECommands.requestSubDeviceParametrs(
//                        _deviceAddress,
//                        subDeviceChankParametersCounter * 10,
//                        numberCount
//                    ), MAIN_CHANNEL, WRITE
//                ) {}
//            } else {
//                subDeviceCounter = 0
//                if (getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third == 0) {
//                    main.showToast("У сабдевайсов нет ни одного виджета")
//                    subDeviceAdditionalCounter = 1
//                } else {
//                    main.bleCommandWithQueue(
//                        BLECommands.requestSubDeviceAdditionalParametrs(
//                            getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).first,
//                            getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).second
//                        ), MAIN_CHANNEL, WRITE
//                    ) {}
//                    subDeviceAdditionalCounter++
//                }
//            }
//        }
//    }
//
//    private fun parseReadSubDeviceAdditionalParameters(addressSubDevice: Int, parameterID: Int, receiveDataString: String) {
//        val offset = HEADER_BLE_OFFSET * 2 + READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
//        var dataOffset = 0
//        baseSubDevicesInfoStructSet.forEach { subDevice ->
//            subDevice.parametersList.forEach { parametrSubDevice ->
//                if (subDevice.deviceAddress == addressSubDevice && parametrSubDevice.ID == parameterID) {
//                    for (i in 0 until parametrSubDevice.additionalInfoSize) {
//                        val start = offset + i * ADDITIONAL_INFO_SIZE_STRUCT_SIZE
//                        val end = offset + (i + 1) * ADDITIONAL_INFO_SIZE_STRUCT_SIZE
//                        val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(start, end)}\"")
//                        val dataStart = offset + parametrSubDevice.additionalInfoSize * ADDITIONAL_INFO_SEG + dataOffset * 2
//                        val dataEnd = dataStart + additionalInfoSizeStruct.infoSize * 2
//                        platformLog("BLEParser", "parseReadSubDeviceAdditionalParameters: start=$dataStart, end=$dataEnd, string.len=${receiveDataString.length}")
//                        val receiveDataStringForParse = if (dataEnd <= receiveDataString.length)
//                            receiveDataString.substring(dataStart, dataEnd)
//                        else ""
//                        dataOffset += additionalInfoSizeStruct.infoSize
//                        when (additionalInfoSizeStruct.infoType) {
//                            AdditionalParameterInfoType.WIDGET.number.toInt() -> {
//                                parseWidgets(receiveDataStringForParse, parameterID = parametrSubDevice.ID, dataCode = parametrSubDevice.dataCode, deviceAddress = subDevice.deviceAddress)
//                                GlobalScope.launch { main.sendWidgetsArray() }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        if (getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third != 0) {
//            main.bleCommandWithQueue(
//                BLECommands.requestSubDeviceAdditionalParametrs(
//                    getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).first,
//                    getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).second
//                ), MAIN_CHANNEL, WRITE
//            ) {}
//            subDeviceAdditionalCounter++
//        } else {
//            main.bleCommandWithQueue(BLECommands.requestTransferFlow(1), MAIN_CHANNEL, WRITE) {}
//            subDeviceAdditionalCounter = 1
//        }
//    }
//
//    private fun parseProductInfoType(receiveDataString: String) {
//        val deviceInfoStructs = Json.decodeFromString<DeviceInfoStructs>("\"${receiveDataString.substring(16)}\"")
//        platformLog("BLEParser", "DeviceInfoStructs = $deviceInfoStructs")
//        main.updateSerialNumber(deviceInfoStructs)
//    }
//
//    private fun getNextIDParameter(ID: Int): Int {
//        for (item in baseParametrInfoStructArray.indices) {
//            if (ID < baseParametrInfoStructArray[item].ID && baseParametrInfoStructArray[item].additionalInfoSize != 0) {
//                return baseParametrInfoStructArray[item].ID
//            }
//        }
//        return 0
//    }
//
//    private fun getNextSubDevice(subDeviceCounter: Int): Int {
//        for ((index, item) in baseSubDevicesInfoStructSet.withIndex()) {
//            if (index >= subDeviceCounter) {
//                if (item.parametrsNum > 10) {
//                    if (subDeviceChankParametersCounter == 0) {
//                        return item.deviceAddress
//                    } else {
//                        if (subDeviceChankParametersCounter * 10 >= item.parametrsNum) {
//                            this.subDeviceCounter++
//                            this.subDeviceChankParametersCounter = 0
//                        }
//                        return item.deviceAddress
//                    }
//                } else {
//                    this.subDeviceChankParametersCounter = 0
//                    if (item.parametrsNum != 0) {
//                        return item.deviceAddress
//                    }
//                }
//            }
//        }
//        return -1
//    }
//
//    private fun getSubDeviceParameterWithAdditionalParameters(itemPosition: Int): Triple<Int, Int, Int> {
//        var count = 1
//        baseSubDevicesInfoStructSet.forEach { subDevice ->
//            subDevice.parametersList.forEach { parameterSubDevice ->
//                if (subDevice.parametersList.isNotEmpty()) {
//                    if (parameterSubDevice.additionalInfoSize != 0 && count == itemPosition) {
//                        return Triple(subDevice.deviceAddress, parameterSubDevice.ID, itemPosition)
//                    }
//                    if (parameterSubDevice.additionalInfoSize != 0) {
//                        count++
//                    }
//                }
//            }
//        }
//        return Triple(0, 0, 0)
//    }
//
//    private fun parseWidgets(receiveDataStringForParse: String, parameterID: Int, dataCode: Int, deviceAddress: Int) {
//        val baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"$receiveDataStringForParse\"")
//        baseParameterWidgetStruct.parameterInfoSet.add(ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset))
//        count++
//        platformLog("BLEParser", "parseWidgets: dataCode=$dataCode, deviceAddress=$deviceAddress, parameterID=$parameterID, dataOffset=${baseParameterWidgetStruct.dataOffset}")
//        when (baseParameterWidgetStruct.widgetLabelType) {
//            ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
//                when (baseParameterWidgetStruct.widgetCode) {
//                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> platformLog("BLEParser", "parseWidgets: UNKNOW")
//                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
//                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"$receiveDataStringForParse\"")
//                        commandParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(commandParameterWidgetEStruct, commandParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
//                        val switchParameterWidgetEStruct = Json.decodeFromString<SwitchParameterWidgetEStruct>("\"$receiveDataStringForParse\"")
//                        switchParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(switchParameterWidgetEStruct, switchParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
//                        val sliderParameterWidgetEStruct = Json.decodeFromString<SliderParameterWidgetEStruct>("\"$receiveDataStringForParse\"")
//                        sliderParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(sliderParameterWidgetEStruct, sliderParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
//                        val plotParameterWidgetEStruct = Json.decodeFromString<PlotParameterWidgetEStruct>("\"$receiveDataStringForParse\"")
//                        plotParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(plotParameterWidgetEStruct, plotParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    else -> platformLog("BLEParser", "parseWidgets: Unhandled widgetCode ${baseParameterWidgetStruct.widgetCode}")
//                }
//            }
//            ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
//                when (baseParameterWidgetStruct.widgetCode) {
//                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> platformLog("BLEParser", "parseWidgets: UNKNOW STRING_LABEL")
//                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
//                        val commandParameterWidgetSStruct = Json.decodeFromString<CommandParameterWidgetSStruct>("\"$receiveDataStringForParse\"")
//                        commandParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
//                        val switchParameterWidgetSStruct = Json.decodeFromString<SwitchParameterWidgetSStruct>("\"$receiveDataStringForParse\"")
//                        switchParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(switchParameterWidgetSStruct, switchParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
//                        val sliderParameterWidgetSStruct = Json.decodeFromString<SliderParameterWidgetSStruct>("\"$receiveDataStringForParse\"")
//                        sliderParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(sliderParameterWidgetSStruct, sliderParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
//                        val plotParameterWidgetSStruct = Json.decodeFromString<PlotParameterWidgetSStruct>("\"$receiveDataStringForParse\"")
//                        plotParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                        )
//                        addToListWidgets(plotParameterWidgetSStruct, plotParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
//                    }
//                    else -> platformLog("BLEParser", "parseWidgets: Unhandled widgetCode STRING_LABEL ${baseParameterWidgetStruct.widgetCode}")
//                }
//            }
//        }
//    }
//
//    private fun addToListWidgets(widget: Any, baseParameterWidgetStruct: Any, parameterID: Int, dataCode: Int, deviceAddress: Int, dataOffset: Int) {
//        var canAdd = true
//        platformLog("BLEParser", "addToListWidgets: parameterID=$parameterID, dataCode=$dataCode, deviceAddress=$deviceAddress, dataOffset=$dataOffset")
//        if (baseParameterWidgetStruct is BaseParameterWidgetEStruct) {
//            listWidgets.forEach {
//                if (it is BaseParameterWidgetEStruct) {
//                    val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 +
//                            baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                    if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it)) {
//                        canAdd = false
//                        it.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetStruct.dataOffset)
//                        )
//                    }
//                    if (combineWidgetId == it.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetStruct.widgetId) {
//                        canAdd = false
//                        it.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetStruct.dataOffset)
//                        )
//                    }
//                }
//            }
//        } else if (baseParameterWidgetStruct is BaseParameterWidgetSStruct) {
//            listWidgets.forEach {
//                if (it is BaseParameterWidgetSStruct) {
//                    val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 +
//                            baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                    if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it)) {
//                        canAdd = false
//                        it.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetStruct.dataOffset)
//                        )
//                    }
//                    if (combineWidgetId == it.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetStruct.widgetId) {
//                        canAdd = false
//                        it.baseParameterWidgetStruct.parameterInfoSet.add(
//                            ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetStruct.dataOffset)
//                        )
//                    }
//                }
//            }
//        }
//        if (canAdd) {
//            listWidgets.add(widget)
//        }
//    }
//
//    private fun areEqualExcludingSetIdS(obj1: BaseParameterWidgetSStruct, obj2: BaseParameterWidgetSStruct): Boolean {
//        val widget1 = obj1.baseParameterWidgetStruct.copy(parameterInfoSet = obj2.baseParameterWidgetStruct.parameterInfoSet)
//        val widget2 = obj2.baseParameterWidgetStruct
//        return widget1 == widget2
//    }
//
//    private fun areEqualExcludingSetIdE(obj1: BaseParameterWidgetEStruct, obj2: BaseParameterWidgetEStruct): Boolean {
//        val widget1 = obj1.baseParameterWidgetStruct.copy(parameterInfoSet = obj2.baseParameterWidgetStruct.parameterInfoSet)
//        val widget2 = obj2.baseParameterWidgetStruct
//        return widget1 == widget2
//    }
//
//    fun getStatusConnected(): Boolean = mConnected
//
//    // Вспомогательная функция для преобразования байта в неотрицательное число
//    private fun castUnsignedCharToInt(byte: Byte): Int {
//        return byte.toInt() and 0xFF
//    }
//
//
//}