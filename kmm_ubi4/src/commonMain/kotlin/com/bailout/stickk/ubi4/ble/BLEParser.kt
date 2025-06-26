package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.additionalParameter.AdditionalInfoSizeStruct
import com.bailout.stickk.ubi4.data.state.ConnectionState.fullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataTableSlotsCode.DTCE_FW_INFO_TYPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataTableSlotsCode.DTCE_DEVICE_INFO_TYPE
import com.bailout.stickk.ubi4.data.state.WidgetState.activeGestureFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.batteryPercentFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.bindingGroupFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.bmsStatusFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArray
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArrayFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.rotationGroupFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.selectGestureModeFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.slidersFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.switcherFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.thresholdFlow
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceArrayInfoDataStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceArrayInfoStruct
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
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.PlotParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.AdditionalParameterInfoType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabelType
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendNextChunkFlagFlow
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.FirmwareInfoStruct
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.utility.EncodeHexToInt.hexToBatteryPercent
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4Wrapper
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.ADDITIONAL_INFO_SEG
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.ADDITIONAL_INFO_SIZE_STRUCT_SIZE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.BASE_PARAMETER_INFO_STRUCT_SIZE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DATA_MANAGER_PAYLOAD_OFFSET
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.HEADER_BLE_OFFSET
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.READ_DEVICE_ADDITIONAL_PARAMETR_DATA
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.experimental.and

class BLEParser(
    private val coroutineScope: CoroutineScope,
    private val bleCommandExecutor: BleCommandExecutor
) {
    private var mConnected = false
    private var count = 0
    private var numberSubDevice = 0
    private var subDeviceCounter = 0 //чтоб при проверке первого сабдевайса индекс перещёлкнулся на 0
    private var subDeviceChankParametersCounter = 0
    private var subDeviceAdditionalCounter = 1
    private var countErrors = 0

    private val deviceProgramTypeMap = mutableMapOf<Int, Int>()
    fun parseReceivedData(data: ByteArray?) {
        if (data != null) {

            val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
            platformLog("BLEParser", "data.size=${data.size}")
            platformLog("BLEParser", "dataString=$receiveDataString")
            platformLog("BLEParser", "requestType=${((data[0].toInt() and 0xFF) and 0x40) / 64}")
            val dataTransmissionDirection = data[0]
            val bridgeIndicator = castUnsignedCharToInt(data[0] and 0b10000000.toByte()) / 128
            val requestType = castUnsignedCharToInt(data[0] and 0b01000000.toByte()) / 64
            val waitingAnsver = castUnsignedCharToInt(data[0] and 0b00100000.toByte()) / 32
            val codeRequest = if (data.size > 1) data[1] else 0
            val dataLength =
                if (data.size > 3) {
                    castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4]) * 256
                } else 0
            val CRC = if (data.size > 5) castUnsignedCharToInt(data[5]) else 0
            val deviceAddress = if (data.size > 6) castUnsignedCharToInt(data[6]) else 0
            val packageCodeRequest = if (data.size > 7) data[7] else 0
            val ID = if (data.size > 8) castUnsignedCharToInt(data[8]) else 0
            platformLog("BLEParser", "BLE debug TEST displayFirstNotify data.size = ${data.size}  $receiveDataString  requestType=$requestType")

            if (requestType == 1) {
                // парсим параметры
                val parameterID = codeRequest.toInt()
                if ((deviceAddress == 8 && parameterID == 1) || (deviceAddress == 6 && parameterID == 2) || (deviceAddress == 6 && parameterID == 16)) {
                    platformLog(
                        "receiveTestQuality",
                        "парсим параметры вход deviceAddress=$deviceAddress parameterID=$parameterID  data=${receiveDataString.substring(
                            HEADER_BLE_OFFSET * 2,
                            receiveDataString.length
                        )}"
                    )
                }
                ParameterProvider.getParameter(deviceAddress, parameterID).data =
                    receiveDataString.substring(HEADER_BLE_OFFSET * 2, receiveDataString.length)
                ParameterProvider.getParameter(deviceAddress, parameterID).firstReceiveDataFlag = false
                platformLog("CheckUpdateAllUI","data = ${receiveDataString.substring(HEADER_BLE_OFFSET * 2, receiveDataString.length)}")
                updateAllUI(
                    deviceAddress,
                    parameterID,
                    ParameterProvider.getParameter(deviceAddress, parameterID).dataCode
                )
            } else {
                // парсим команды
                when (codeRequest) {
                    (0x00).toByte() -> {
                        platformLog("BLEParser", "TEST parser DEFOULT")
                    }
                    BaseCommands.DEVICE_INFORMATION.number -> {
                        platformLog("BLEParser", "TEST parser DEVICE_INFORMATION (${packageCodeRequest})")
                        parseDeviceInformation(packageCodeRequest, ID, deviceAddress, receiveDataString)
                    }
                    BaseCommands.DATA_MANAGER.number -> {
                        platformLog("BLEParser", "TEST parser DATA_MANAGER")
                        parseDataManger(packageCodeRequest, receiveDataString)
                    }
                    BaseCommands.WRITE_FW_COMMAND.number -> {
                        platformLog("BLEParser", "TEST parser WRITE_FW_COMMAND")
                    }
                    BaseCommands.DEVICE_ACCESS_COMMAND.number -> {
                        platformLog("BLEParser", "TEST parser DEVICE_ACCESS_COMMAND")
                    }
                    BaseCommands.ECHO_COMMAND.number -> {
                        platformLog("BLEParser", "TEST parser ECHO_COMMAND")
                    }
                    BaseCommands.SUB_DEVICE_MANAGER.number -> {
                        platformLog("BLEParser", "TEST parser SUB_DEVICE_MANAGER")
                    }
                    BaseCommands.GET_DEVICE_STATUS.number -> {
                        platformLog("BLEParser", "TEST parser GET_DEVICE_STATUS")
                    }
                    BaseCommands.DATA_TRANSFER_SETTINGS.number -> {
                        platformLog("BLEParser", "TEST parser DATA_TRANSFER_SETTINGS")
                    }
                    BaseCommands.COMPLEX_PARAMETER_TRANSFER.number -> {
                        platformLog(
                            "BLEParser",
                            "TEST parser COMPLEX_PARAMETER_TRANSFER data.size = ${data.size}   dataLength = $dataLength"
                        )
                        var dataLengthMax = dataLength
                        var dataLength = dataLength
                        var counter = 1

                        try {
                            while (dataLength > 0) {
                                val dataHex = EncodeByteToHex.bytesToHexString(data)
                                val deviceAddress = castUnsignedCharToInt(
                                    receiveDataString.substring(
                                        (HEADER_BLE_OFFSET + (dataLengthMax - dataLength)) * 2,
                                        (HEADER_BLE_OFFSET + (dataLengthMax - dataLength) + 1) * 2
                                    ).toInt(16).toByte()
                                )
                                val parameterID = castUnsignedCharToInt(
                                    receiveDataString.substring(
                                        (HEADER_BLE_OFFSET + (dataLengthMax - dataLength) + 1) * 2,
                                        (HEADER_BLE_OFFSET + (dataLengthMax - dataLength) + 2) * 2
                                    ).toInt(16).toByte()
                                )
                                val parameter = ParameterProvider.getParameter(deviceAddress, parameterID)
                                platformLog("uiGestureSettingsObservableCP", "dataCode = ${parameter.dataCode}")
                                platformLog("uiGestureSettingsObservableCP", "counter = $counter dataLength = $dataLength {data = $dataHex }")

                                parameter.data = receiveDataString.substring(
                                    (HEADER_BLE_OFFSET + (dataLengthMax - dataLength) + 2) * 2,
                                    (HEADER_BLE_OFFSET + (dataLengthMax - dataLength) + 2 + parameter.parameterDataSize) * 2
                                )
                                parameter.firstReceiveDataFlag = false
                                platformLog("CheckUpdateAllUI","data2 = ${parameter.data} deviceAddress = $deviceAddress, parameterId = $parameterID")
                                updateAllUI(deviceAddress, parameterID, parameter.dataCode)
                                dataLength -= (parameter.parameterDataSize + 2)
                                counter += 1
                            }
                        } catch (e: Exception) {
                            showToast("Exception ${e.message}")
                        }
                    }
                }
            }

            if (requestType == 1 || codeRequest != BaseCommands.COMPLEX_PARAMETER_TRANSFER.number)
                bleCommandExecutor.getQueueUBI4().allowNext()
        }
    }

    private fun updateAllUI(deviceAddress: Int, parameterID: Int, dataCode: Int) {
        platformLog("updateAllUITest", "deviceAddress =$deviceAddress, parameterID = $parameterID, dataCode = $dataCode")
        ParameterProvider.getParameter(deviceAddress, parameterID).additionalInfoRefSet.forEach {

            platformLog("updateAllUITest", "widgetCode = ${it.widgetCode}")
            when (it.widgetCode) {
                ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> {
                    when(dataCode) {
                        //TODO проверить!
                        ParameterDataCodeEnum.PDCE_GENERIC_2.number -> {
                            platformLog("StatusWriteFlash", "deviceAddress: $deviceAddress    parameterID: $parameterID    dataCode: $dataCode")
                            val newStatusExist = castUnsignedCharToInt(
                                ParameterProvider.getParameter(deviceAddress, parameterID).data.substring(0, 2).toInt(16).toByte()
                            )
                            val errorStatus = castUnsignedCharToInt(
                                ParameterProvider.getParameter(deviceAddress, parameterID).data.substring(8, 10).toInt(16).toByte()
                            )
                            val packIndex = castUnsignedCharToInt(
                                ParameterProvider.getParameter(deviceAddress, parameterID).data.substring(6, 8).toInt(16).toByte()
                            ) * 256 + castUnsignedCharToInt(
                                ParameterProvider.getParameter(deviceAddress, parameterID).data.substring(4, 6).toInt(16).toByte()
                            )
                            if (errorStatus != 0 && errorStatus != 255) {
                                countErrors++
                            }
                            if (newStatusExist == 1 && errorStatus == 0)
                                coroutineScope.launch { canSendNextChunkFlagFlow.emit(packIndex) }
                            platformLog("StatusWriteFlash", "data = ${ParameterProvider.getParameter(deviceAddress, parameterID).data} countErrors = $countErrors")
                        }

                    }
                }
                ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {}
                ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                    //TODO проверить!
                    ParameterProvider.getParameter(deviceAddress, parameterID)
                    platformLog("parameter swichCollect PDCE_ENERGY_SAVE_MODE", "deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode data = ${ParameterProvider.getParameter(deviceAddress, parameterID).data}")
                    coroutineScope.launch { switcherFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) }
                }
                ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> {}
                ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                    coroutineScope.launch { slidersFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) } }
                ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                    val parameter = ParameterProvider.getParameter(deviceAddress, parameterID)
                    val data = parameter.data
                    val paddedData: String = data.padEnd(12, '0')
                    platformLog("updateAllUITest", "data = $data")
                    try {
                        plotArray = arrayListOf(
                            castUnsignedCharToInt(paddedData.substring(0, 2).toInt(16).toByte()),
                            castUnsignedCharToInt(paddedData.substring(2, 4).toInt(16).toByte()),
                            castUnsignedCharToInt(paddedData.substring(4, 6).toInt(16).toByte()),
                            castUnsignedCharToInt(paddedData.substring(6, 8).toInt(16).toByte()),
                            castUnsignedCharToInt(paddedData.substring(8, 10).toInt(16).toByte()),
                            castUnsignedCharToInt(paddedData.substring(10, 12).toInt(16).toByte())
                        )
                    } catch (e: Error) {
                        showToast("Ошибка 113")
                    }
                    coroutineScope.launch { plotArrayFlow.emit(PlotParameterRef(deviceAddress, parameterID, plotArray)) }
                }
                ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> {}
                ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> {}
                ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> {}
                ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> {}
                ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> {}
                ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                    platformLog("parameter sliderCollect PDCE_OPEN_CLOSE_THRESHOLD", "deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode")
                    coroutineScope.launch { thresholdFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) }
                }
                ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> {}
                ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> {}
                ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {
                    when (dataCode){
                        ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
                            platformLog("uiRotationGroupObservable", "dataCode = $dataCode")
                            RxUpdateMainEventUbi4Wrapper.updateUiRotationGroup(ParameterRef(deviceAddress, parameterID, dataCode))
                            coroutineScope.launch { rotationGroupFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) }
                            val parameter =
                                ParameterProvider.getParameter(deviceAddress, parameterID)
                            platformLog("uiRotationGroupObservable", "parameter.data = ${parameter.data}")
                        }
                        ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number -> {
                            val raw = ParameterProvider
                                .getParameter(deviceAddress, parameterID)
                                .data                     // hex‑строка без заголовка

                            platformLog("GestureSettings‑RX", "raw=$raw")

                            if (raw.length >= 2) {
                                val idHex = raw.substring(0, 2)
                                val idDec = idHex.toInt(16)
                                platformLog(
                                    "GestureSettings‑RX",
                                    "byte0=0x$idHex  ->  id=$idDec (i=${idDec - 0x3F})"
                                )
                                // ↓ здесь же можно парсить остальные байты настройки, если протокол известен
                            }
                            platformLog("uiGestureSettingsObservable", "перед RX dataCode = $dataCode")
                            RxUpdateMainEventUbi4Wrapper.updateUiGestureSettings(dataCode)
                        }
                        ParameterDataCodeEnum.PDCE_SELECT_GESTURE.number -> {
                            val paramData = ParameterProvider.getParameter(deviceAddress, parameterID).data
                            val idHex = paramData.substring(0, 2)
                            val idDec = idHex.toInt(16)
                            platformLog("ActiveGesture‑RX", "byte=0x$idHex  ->  id=$idDec (i=${idDec - 0x3F})")
                            platformLog("parameter PDCE_SELECT_GESTURE", "deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode data: $paramData")
                            coroutineScope.launch { activeGestureFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) }
                        }
                        ParameterDataCodeEnum.PDCE_OPTIC_BINDING_DATA.number -> {
                            platformLog("parameter PDCE_OPTIC_BINDING_DATA", "deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode")
                            coroutineScope.launch { bindingGroupFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) }
                        }
                        ParameterDataCodeEnum.PDCE_OPTIC_MODE_SELECT_GESTURE.number -> {
                            val paramData = ParameterProvider.getParameter(deviceAddress, parameterID).data
                            platformLog("BorderAnimator", "deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode data: $paramData")
                            coroutineScope.launch { selectGestureModeFlow.emit(ParameterRef(deviceAddress, parameterID, dataCode)) }
                        }
                    }
                }
                ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() -> {
                    when (dataCode) {
                        ParameterDataCodeEnum.PDCE_OPTIC_LEARNING_DATA.number -> {
                            //TODO проверить!
                            platformLog("TestOptic", " dataCode: $dataCode")
                            platformLog("FileInfoWriteFile", "recive ok")
                            RxUpdateMainEventUbi4Wrapper.updateUiOpticTraining(ParameterRef(deviceAddress, parameterID, dataCode))
                        }

                    }
                }
                ParameterWidgetCode.PWCE_SERVICE_INFO.number.toInt() -> {
                    platformLog("updateAllUITest", "here!")

                    when(dataCode){
                        ParameterDataCodeEnum.PDCE_BMS_STATUS_COMBINED_PARAM.number -> {
                            val paramData = ParameterProvider.getParameter(deviceAddress,parameterID).data
                            platformLog("updateAllUITest", "deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode data: $paramData")
                            val percent = paramData.hexToBatteryPercent()
                            platformLog(
                                "BatteryParser",
                                "raw=$paramData → percent=$percent%"
                            )
                            coroutineScope.launch { bmsStatusFlow.emit(ParameterRef(deviceAddress,parameterID, dataCode))
                            }
                            coroutineScope.launch { batteryPercentFlow.emit(percent) }

                        }

                    }

                }
            }

        }
    }


    private fun parseDeviceInformation(packageCodeRequest: Byte, ID: Int, deviceAddress: Int, receiveDataString: String) {
        when (packageCodeRequest) {
            (0x00).toByte() -> {
                platformLog("BLEParser", "TEST parser 2 DEFOULT")
            }
            DeviceInformationCommand.INICIALIZE_INFORMATION.number -> {
                platformLog("BLE_DEVINFO", "▶ INITIALIZE_INFORMATION пакет, packageByte=$packageCodeRequest, ID=$ID, addr=$deviceAddress, rawData=$receiveDataString")
                parseInitializeInformation(receiveDataString)
//                parseInitializeInformation(deviceAddress, receiveDataString)
            }
            DeviceInformationCommand.READ_DEVICE_PARAMETRS.number -> {
                try {
                    parseReadDeviceParameters(receiveDataString)
                } catch (e: Exception) {
                    showToast("Неудалось распарсить READ_DEVICE_PARAMETRS")
                }
            }
            DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETRS.number -> {
                parseReadDeviceAdditionalParameters(ID, receiveDataString, deviceAddress)
            }
            DeviceInformationCommand.READ_SUB_DEVICES_FIRST_INFO.number -> {
                platformLog("BLEParser", "TEST parser 2 READ_SUB_DEVICES_FIRST_INFO")
            }
            DeviceInformationCommand.READ_SUB_DEVICE_INFO.number -> {
                platformLog("BLEParser", "TEST parser 2 READ_SUB_DEVICE_INFO")
                parseReadSubDeviceInfo(receiveDataString)
            }
            DeviceInformationCommand.READ_SUB_DEVICE_PARAMETERS.number -> {
                platformLog("BLEParser", "TEST parser 2 READ_SUB_DEVICE_PARAMETERS старт вызовов")
                parseReadSubDeviceParameters(receiveDataString)
            }
            DeviceInformationCommand.READ_SUB_DEVICE_ADDITIONAL_PARAMETER.number -> {
                platformLog("BLEParser", "TEST parser 2 READ_SUB_DEVICE_ADDITIONAL_PARAMETER")
                val addressSubDevice = castUnsignedCharToInt(
                    receiveDataString.substring(16, 18).toInt(16).toByte()
                ) // хедер 7 + 1 байта данные до addressSubDevice (адрес сабдевайса передаётся в возращаемых данных вторым байтом)
                val parameterID = castUnsignedCharToInt(
                    receiveDataString.substring(18, 20).toInt(16).toByte()
                ) // хедер 7 + 2 байта данные до ID (ID-параметра передаётся в возращаемых данных третьим байтом)
                parseReadSubDeviceAdditionalParameters(addressSubDevice, parameterID, receiveDataString)
            }
            DeviceInformationCommand.SUB_DEVICE_PARAMETER_INIT_READ.number -> {
                platformLog("BLEParser", "TEST parser 2 SUB_DEVICE_PARAMETER_INIT_READ")
            }
            DeviceInformationCommand.SUB_DEVICE_PARAMETER_INIT_WRITE.number -> {
                platformLog("BLEParser", "TEST parser 2 SUB_DEVICE_PARAMETER_INIT_WRITE")
            }
            DeviceInformationCommand.GET_SERIAL_NUMBER.number -> {
                platformLog("BLEParser", "TEST parser 2 GET_SERIAL_NUMBER")
            }
            DeviceInformationCommand.SET_SERIAL_NUMBER.number -> {
                platformLog("BLEParser", "TEST parser 2 SET_SERIAL_NUMBER")
            }
            DeviceInformationCommand.GET_DEVICE_NAME.number -> {
                platformLog("BLEParser", "TEST parser 2 GET_DEVICE_NAME")
            }
            DeviceInformationCommand.SET_DEVICE_NAME.number -> {
                platformLog("BLEParser", "TEST parser 2 SET_DEVICE_NAME")
            }
            DeviceInformationCommand.GET_DEVICE_ROLE.number -> {
                platformLog("BLEParser", "TEST parser 2 GET_DEVICE_ROLE")
            }
            DeviceInformationCommand.SET_DEVICE_ROLE.number -> {
                platformLog("BLEParser", "TEST parser 2 SET_DEVICE_ROLE")
            }
        }
    }

    private fun parseDataManger(packageCodeRequest: Byte, receiveDataString: String) {
        platformLog("parseProductInfoType", "packageCodeRequest = $packageCodeRequest")
        platformLog("parseDataManger", "packageCodeRequest = $packageCodeRequest, receiveDataString = $receiveDataString")
        when (packageCodeRequest) {
            (0x00).toByte() -> {
                platformLog("BLEParser", "TEST parser 2 DEFOULT")
            }
            DataManagerCommand.READ_AVAILABLE_SLOTS.number -> {
                platformLog("BLEParser", "TEST parser 2 READ_AVAILABLE_SLOTS")
            }
            DataManagerCommand.WRITE_SLOT.number -> {
                platformLog("BLEParser", "TEST parser 2 WRITE_SLOT")
            }
            DataManagerCommand.READ_DATA.number -> {
                platformLog("BLEParser", "TEST parser 2 READ_DATA")
                parseProductInfoType(receiveDataString)
                parseProductFwInfoType(receiveDataString)
            }
            DataManagerCommand.WRITE_DATA.number -> {
                platformLog("BLEParser", "TEST parser 2 WRITE_DATA")
            }
            DataManagerCommand.RESET_TO_FACTORY.number -> {
                platformLog("BLEParser", "TEST parser 2 RESET_TO_FACTORY")
            }
            DataManagerCommand.SAVE_DATA.number -> {
                platformLog("BLEParser", "TEST parser 2 SAVE_DATA")
            }
        }

    }

    private fun parseInitializeInformation(receiveDataString: String) {
        fullInicializeConnectionStruct =
            Json.decodeFromString<FullInicializeConnectionStruct>("\"${receiveDataString.substring(18, receiveDataString.length)}\"")

        platformLog("BLE_PARSER", "▶ parseInitializeInformation → $fullInicializeConnectionStruct")
        bleCommandExecutor.bleCommandWithQueue(
            BLECommands.requestBaseParametrInfo(0x00, fullInicializeConnectionStruct.parametrsNum.toByte()),
            MAIN_CHANNEL,
            WRITE
        ) {}


        platformLog("BLEParser", "parametrsNum = ${fullInicializeConnectionStruct.parametrsNum}")
    }


    private fun parseReadDeviceParameters(receiveDataString: String) {
        platformLog("BLEParserTest", "▶️ parseReadDeviceParameters start, raw=${receiveDataString.take(40)}…")
        val listA: ArrayList<BaseParameterInfoStruct> = ArrayList()
        platformLog("BLEParser", "TEST parser 2 READ_DEVICE_PARAMETRS $receiveDataString")
        for (i in 0 until fullInicializeConnectionStruct.parametrsNum) {
            listA.add(
                Json.decodeFromString<BaseParameterInfoStruct>(
                    "\"${receiveDataString.substring(20 + i * BASE_PARAMETER_INFO_STRUCT_SIZE, 20 + (i + 1) * BASE_PARAMETER_INFO_STRUCT_SIZE)}\""
                )
            )
        }

        baseParametrInfoStructArray = listA
        var widgetCount = 0
        baseParametrInfoStructArray.forEach {
            widgetCount += it.additionalInfoSize
            println("READ_DEVICE_PARAMETRS $it $widgetCount")
        }

        if (baseParametrInfoStructArray.size != 0) {
            platformLog("getNextIDParameter", "запрос адшнл параметра")
            if (baseParametrInfoStructArray[0].additionalInfoSize != 0) {
                bleCommandExecutor.bleCommandWithQueue(
                    BLECommands.requestAdditionalParametrInfo(baseParametrInfoStructArray[0].ID.toByte()),
                    MAIN_CHANNEL,
                    WRITE
                ) {}
            } else {
                val ID = getNextIDParameter(0)
                if (ID != 0) {
                    platformLog("getNextIDParameter", "запроса адшнл параметра")
                    bleCommandExecutor.bleCommandWithQueue(
                        BLECommands.requestAdditionalParametrInfo(baseParametrInfoStructArray[ID].ID.toByte()),
                        MAIN_CHANNEL,
                        WRITE
                    ) {}
                } else {
                    platformLog("getNextIDParameter", "конец запроса параметров")
                    bleCommandExecutor.bleCommandWithQueue(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE) {}
                }
            }
        }

    }

    private fun parseReadDeviceAdditionalParameters(ID: Int, receiveDataString: String, deviceAddress: Int) {
        platformLog("BLEParserTest", "▶️ parseReadDeviceAdditionalParameters start for ID=$ID")
        val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
        var dataOffset = 0
        var ID = ID

        if (baseParametrInfoStructArray[ID].additionalInfoSize != 0) {
            for (i in 0 until baseParametrInfoStructArray[ID].additionalInfoSize) {
                val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>(
                    "\"${receiveDataString.substring(offset + i * ADDITIONAL_INFO_SIZE_STRUCT_SIZE, offset + (i + 1) * ADDITIONAL_INFO_SIZE_STRUCT_SIZE)}\""
                )
                platformLog("BLEParser", "testSignal 0 ")
                val receiveDataStringForParse = receiveDataString.substring(
                    offset + baseParametrInfoStructArray[ID].additionalInfoSize * ADDITIONAL_INFO_SEG + dataOffset * 2,
                    offset + baseParametrInfoStructArray[ID].additionalInfoSize * ADDITIONAL_INFO_SEG + dataOffset * 2 + additionalInfoSizeStruct.infoSize * 2
                )
                dataOffset = additionalInfoSizeStruct.infoSize

                when (additionalInfoSizeStruct.infoType) {
                    AdditionalParameterInfoType.WIDGET.number.toInt() -> {
                        val parsedWidget = parseWidgets(receiveDataStringForParse, parameterID = ID, dataCode = baseParametrInfoStructArray[ID].dataCode, deviceAddress)
                        platformLog("parsedWidget", "▶️widgetcode - ${parsedWidget.widgetCode}")

                        coroutineScope.launch {
                            platformLog("BLEParserTest", "▶️ sendWidgetsArray() called, total widgets=${listWidgets.size}")
                            platformLog("sendWidgetsArray", "▶️ sendWidgetsArray()  called, total widgets=${listWidgets.size}")
                            bleCommandExecutor.sendWidgetsArray()
                        }
                    }
                }
            }
        }

        ID = getNextIDParameter(ID)
        if (ID != 0) {
            platformLog("getNextIDParameter", "запроса адшнл параметра")
            bleCommandExecutor.bleCommandWithQueue(
                BLECommands.requestAdditionalParametrInfo(baseParametrInfoStructArray[ID].ID.toByte()),
                MAIN_CHANNEL,
                WRITE
            ) {}
        } else {
            platformLog("getNextIDParameter", "конец запроса адшнл параметров")
            bleCommandExecutor.bleCommandWithQueue(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE) {}
        }
    }

    private fun parseReadSubDeviceInfo(receiveDataString: String) {
        platformLog("SubDeviceSubDevice", "receiveDataString=$receiveDataString")
        val subDevices = Json.decodeFromString<BaseSubDeviceArrayInfoStruct>(
            "\"${receiveDataString.substring(16, receiveDataString.length)}\""
        )

        baseSubDevicesInfoStructSet = subDevices.baseSubDeviceInfoStructArray
        baseSubDevicesInfoStructSet.forEach {
            platformLog("SubDeviceSubDevice", "$it")
        }

        numberSubDevice = subDevices.count
        subDeviceCounter = 0
        subDeviceChankParametersCounter = 0
        subDeviceAdditionalCounter = 1
        val parametrsNum = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum


        if (baseSubDevicesInfoStructSet.size != 0) {
            platformLog("getNextSubDevice", "baseSubDevicesInfoStructSet.size=${baseSubDevicesInfoStructSet.size} baseSubDevicesInfoStructSet=$baseSubDevicesInfoStructSet")
            if (getNextSubDevice(subDeviceCounter) != -1) {
                var numberCount = 10
                if (subDeviceChankParametersCounter == (parametrsNum / 10)) {
                    numberCount = parametrsNum - subDeviceChankParametersCounter * 10
                }
                if (numberCount != 0) {
                    bleCommandExecutor.bleCommandWithQueue(
                        BLECommands.requestSubDeviceParametrs(
                            baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress,
                            subDeviceChankParametersCounter * 10,
                            numberCount
                        ),
                        MAIN_CHANNEL,
                        WRITE
                    ) {}
                }


                bleCommandExecutor.bleCommandWithQueue(
                    BLECommands.requestProductInfoType(),
                    MAIN_CHANNEL, WRITE) {}

                baseSubDevicesInfoStructSet.forEach { sub ->
                    bleCommandExecutor.bleCommandWithQueue(
                        BLECommands.requestProductFWInfoType(sub.deviceAddress),
                        MAIN_CHANNEL, WRITE) {}
                }
//                val test = subDeviceChankParametersCounter*10
//                val test2 = numberCount
//                val test3 = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress
//                platformLog("requestSubDeviceParametrs","")
//                platformLog("requestSubDeviceParametrs", "startIndex = ${subDeviceChankParametersCounter*10}   numberCount = $numberCount  subDeviceCounter=$subDeviceCounter из $numberSubDevice  parametrsNum = ${parametrsNum}")
//                val localSubDeviceChankParametersCounter = subDeviceChankParametersCounter
//                if (parametrsNum > 10) {
//                    subDeviceChankParametersCounter ++
//                    platformLog("getNextSubDevice", "инкрементировали subDeviceChankParametersCounter=$subDeviceChankParametersCounter")
//                } else {platformLog("getNextSubDevice", "не проинкрементировали subDeviceChankParametersCounter=$subDeviceChankParametersCounter")}
//                if (parametrsNum <= 10 || (parametrsNum-localSubDeviceChankParametersCounter*10 <= 10)) {
//                    platformLog("getNextSubDevice", "инкрементировали subDeviceCounter parametrsNum=${parametrsNum}  parametrsNum-subDeviceChankParametersCounter*10 = ${parametrsNum-subDeviceChankParametersCounter*10}")
//                    this.subDeviceCounter ++
//                } else {platformLog("getNextSubDevice", "не инкрементировали subDeviceCounter parametrsNum=${parametrsNum}  parametrsNum-subDeviceChankParametersCounter*10 = ${parametrsNum-subDeviceChankParametersCounter*10}")}
            } else {
                showToast("Нет сабдевайсов с параметрами")
            }
        } else {
            showToast("Сабдевайсов нет")
        }
    }

    private fun parseReadSubDeviceParameters(receiveDataString: String) {
        var _deviceAddress = 0
        var _parametrsNum = 0
        var deviceAddress = 0
        var startIndex = 0
        var quantitiesReadParameters = 0
        var numberCount = 10

        if (subDeviceCounter < baseSubDevicesInfoStructSet.size) {
            val listA: ArrayList<BaseParameterInfoStruct> = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList
            if (receiveDataString.isEmpty() || receiveDataString.length < 22) return
            _deviceAddress = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress
            _parametrsNum = baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum
            deviceAddress = castUnsignedCharToInt(receiveDataString.substring(16, 18).toInt(16).toByte())
            startIndex = castUnsignedCharToInt(receiveDataString.substring(18, 20).toInt(16).toByte())
            quantitiesReadParameters = castUnsignedCharToInt(receiveDataString.substring(20, 22).toInt(16).toByte())
            platformLog("SubDeviceAdditionalParameters", "$_deviceAddress $_parametrsNum $deviceAddress $startIndex $quantitiesReadParameters")

            if (subDeviceChankParametersCounter == (_parametrsNum / 10)) {
                numberCount = _parametrsNum - subDeviceChankParametersCounter * 10
            }

            platformLog("listA", "listA=${listA.size}")
            platformLog("listA", "deviceAddress = ${_deviceAddress}   0 <= i < $numberCount")
            if (_deviceAddress == deviceAddress && subDeviceChankParametersCounter * 10 == startIndex && quantitiesReadParameters == numberCount) {
                platformLog("SubDeviceAdditionalParameters", "FILTER $_deviceAddress $_parametrsNum $deviceAddress $startIndex $quantitiesReadParameters")

                for (i in 0 until numberCount) {
                    val start = 22 + (i) * BASE_PARAMETER_INFO_STRUCT_SIZE
                    val end = 22 + (i + 1) * BASE_PARAMETER_INFO_STRUCT_SIZE
                    platformLog("SubDeviceAdditionalParameters", "start=$start   end=$end  receiveDataString=$receiveDataString")

                    if (end <= receiveDataString.length) {
                        try {
                            val parameterJson = receiveDataString.substring(start, end)
                            listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"$parameterJson\""))
                        } catch (e: Exception) {}
                    } else {
                        platformLog("SubDeviceAdditionalParameters", "Индексы $start-$end выходят за пределы строки длиной ${receiveDataString.length}")
                        break
                    }
                }
                baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList = listA
            }
            platformLog(
                "SubDeviceAdditionalParameters",
                "прочитали параметры из сабдевайса ${_deviceAddress} их ${baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList.size} listA=${listA.size} subDeviceCounter=$subDeviceCounter из $numberSubDevice"
            )

            baseSubDevicesInfoStructSet.forEach {
                println("READ_SUB_DEVICE_PARAMETRS $it")
            }

            platformLog("SubDeviceSubDevice2", "numerSubDevice=$numberSubDevice  _parametrsNum = ${_parametrsNum}")
        }

        if (getNextSubDevice(subDeviceCounter) != -1) {
            numberCount = 10
            if (subDeviceChankParametersCounter == (_parametrsNum / 10)) {
                numberCount = _parametrsNum - subDeviceChankParametersCounter * 10
            }
            if (_deviceAddress == deviceAddress && subDeviceChankParametersCounter * 10 == startIndex && quantitiesReadParameters == numberCount) {
                val localSubDeviceChankParametersCounter = subDeviceChankParametersCounter
                if (_parametrsNum > 10) {
                    subDeviceChankParametersCounter++
                    if ((_parametrsNum - localSubDeviceChankParametersCounter * 10 <= 10)) {
                        subDeviceChankParametersCounter = 0
                    }
                    platformLog("getNextSubDevice", "инкрементировали subDeviceChankParametersCounter=$subDeviceChankParametersCounter")
                } else {
                    platformLog("getNextSubDevice", "не проинкрементировали subDeviceChankParametersCounter=$subDeviceChankParametersCounter")
                }
                if ((_parametrsNum <= 10 || (_parametrsNum - localSubDeviceChankParametersCounter * 10 <= 10))) {
                    platformLog("getNextSubDevice", "инкрементировали subDeviceCounter parametrsNum=${_parametrsNum}  parametrsNum-subDeviceChankParametersCounter*10 = ${_parametrsNum - subDeviceChankParametersCounter * 10}")
                    this.subDeviceCounter++
                } else {
                    platformLog("getNextSubDevice", "не инкрементировали subDeviceCounter parametrsNum=${_parametrsNum}  parametrsNum-subDeviceChankParametersCounter*10 = ${_parametrsNum - subDeviceChankParametersCounter * 10}")
                }
            } else {
                // здесь можно добавить отладочные сообщения, если нужно
//                if (_deviceAddress != deviceAddress) showToast("обнаружено несоответствие deviceAddress ${_deviceAddress} != $deviceAddress")
//                if (subDeviceChankParametersCounter*10 != startIndex) showToast("обнаружено несоответствие startIndex ${subDeviceChankParametersCounter*10} != $startIndex")
//                if (quantitiesReadParameters != numberCount) showToast("обнаружено несоответствие numberCount $quantitiesReadParameters != $numberCount")
            }

            if (numberCount != 0){
                bleCommandExecutor.bleCommandWithQueue(
                    BLECommands.requestSubDeviceParametrs(_deviceAddress, subDeviceChankParametersCounter * 10, numberCount),
                    MAIN_CHANNEL,
                    WRITE
                ) {}
            }

//            val test = subDeviceChankParametersCounter*10
//            val test2 = numberCount
//            val test3 = _deviceAddress
//            platformLog("requestSubDeviceParametrs","")
//            platformLog("requestSubDeviceParametrs", "startIndex = ${subDeviceChankParametersCounter*10}   numberCount = $numberCount  subDeviceCounter=$subDeviceCounter из $numberSubDevice  parametrsNum = ${_parametrsNum}")
        } else {
            platformLog("SubDeviceAdditionalParameterss", "закончили чтение всех параметров во всех сабдевайсах")
            platformLog("SubDeviceAdditionalParameterss", "subDeviceCounter = $subDeviceCounter")
            platformLog(
                "SubDeviceAdditionalParameterss",
                "10 = ${getSubDeviceParameterWithAdditionalParameters(1).first}  0 = ${getSubDeviceParameterWithAdditionalParameters(1).second}  1 = ${getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third}"
            )
            if (getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third == 0) {
                platformLog("SubDeviceAdditionalParameterss", "у сабдевайсов нет ни одного виджета")
                platformLog("SubDeviceAdditionalParameterss", "конец запроса параметров сабдевайса")
                subDeviceAdditionalCounter = 1
            } else {
                platformLog("SubDeviceAdditionalParameterss", "запроса адишнл параметра")
                bleCommandExecutor.bleCommandWithQueue(
                    BLECommands.requestSubDeviceAdditionalParametrs(
                        getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).first,
                        getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).second
                    ),
                    MAIN_CHANNEL,
                    WRITE
                ) {}
                subDeviceAdditionalCounter++
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun parseReadSubDeviceAdditionalParameters(addressSubDevice: Int, parameterID: Int, receiveDataString: String) {
        val offset = HEADER_BLE_OFFSET * 2 + READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
        var dataOffset = 0

        baseSubDevicesInfoStructSet.forEach { subDevice ->
            subDevice.parametersList.forEach { parametrSubDevice ->
                if (subDevice.deviceAddress == addressSubDevice) {
                    if (parametrSubDevice.ID == parameterID) {
                        platformLog(
                            "parseReadSubDeviceAdditionalParameters",
                            "deviceAddress=${subDevice.deviceAddress}  parameterID = ${parametrSubDevice.ID}  list = ${subDevice.parametersList.size} additionalInfoSize=${parametrSubDevice.additionalInfoSize}  receiveDataString=$receiveDataString"
                        )
                        for (i in 0 until parametrSubDevice.additionalInfoSize) {
                            val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>(
                                "\"${receiveDataString.substring(offset + i * ADDITIONAL_INFO_SIZE_STRUCT_SIZE, offset + (i + 1) * ADDITIONAL_INFO_SIZE_STRUCT_SIZE)}\""
                            )
                            platformLog("parseReadSubDeviceAdditionalParameters", "additionalInfoSizeStruct = $additionalInfoSizeStruct")
                            val start = offset + parametrSubDevice.additionalInfoSize * ADDITIONAL_INFO_SEG + dataOffset * 2
                            val end = offset + parametrSubDevice.additionalInfoSize * ADDITIONAL_INFO_SEG + dataOffset * 2 + additionalInfoSizeStruct.infoSize * 2

                            platformLog("parseReadSubDeviceAdditionalParameters", "start = $start    end = $end  receiveDataString.length = ${receiveDataString.length}")
                            var receiveDataStringForParse = ""
                            if (end <= receiveDataString.length) {
                                receiveDataStringForParse = receiveDataString.substring(start, end)
                            }
                            dataOffset += additionalInfoSizeStruct.infoSize
                            platformLog("parseReadSubDeviceAdditionalParameters", "receiveDataStringForParse = $receiveDataStringForParse")

                            when (additionalInfoSizeStruct.infoType) {
                                AdditionalParameterInfoType.WIDGET.number -> {
                                    val widgetStruct = parseWidgets(receiveDataStringForParse, parameterID = parametrSubDevice.ID, dataCode = parametrSubDevice.dataCode, addressSubDevice)
                                    if (widgetStruct.widgetCode == 16){
                                        platformLog("parsedWidget", "▶️ parsedWidget run")
                                        bleCommandExecutor.bleCommandWithQueue(
                                            BLECommands.requestBatteryStatus(7,0),
                                            MAIN_CHANNEL,
                                            WRITE
                                        ) {}
                                    }
                                    parametrSubDevice.additionalInfoRefSet.add(widgetStruct)
                                    coroutineScope.launch {
                                        platformLog("sendWidgetsArray", "▶\uFE0F sendWidgetsArray() called, total widgets=${listWidgets.size}")
                                        bleCommandExecutor.sendWidgetsArray()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third != 0) {
            platformLog("parseReadSubDeviceAdditionalParameters", "запроса адишнл параметра")
            bleCommandExecutor.bleCommandWithQueue(
                BLECommands.requestSubDeviceAdditionalParametrs(
                    getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).first,
                    getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).second
                ),
                MAIN_CHANNEL,
                WRITE
            ) {}
            subDeviceAdditionalCounter++
        } else {
            bleCommandExecutor.bleCommandWithQueue(BLECommands.requestTransferFlow(1), MAIN_CHANNEL, WRITE) {}
            subDeviceAdditionalCounter = 1
            platformLog("parseReadSubDeviceAdditionalParameters", "конец запроса адишнл параметров сабдевайса")
        }
    }

    private fun parseReadSubDeviceInfoData(receiveDataString: String) {
        platformLog("BLEParser", "TEST parser 2 READ_DATA befo parse test $receiveDataString")
        val test = Json.decodeFromString<BaseSubDeviceArrayInfoDataStruct>(
            "\"${receiveDataString.substring(16, receiveDataString.length)}\""
        )
        platformLog("BLEParser", "TEST parser 2 READ_DATA $test")
    }

    private fun parseProductInfoType(receiveDataString: String) {
        val deviceInfoStructs = Json.decodeFromString<DeviceInfoStructs>(
            "\"${receiveDataString.substring(16, receiveDataString.length)}\""
        )
        platformLog("parseProductInfoType", "deviceInfoStructs = $deviceInfoStructs")
        bleCommandExecutor.updateSerialNumber(deviceInfoStructs)

    }


//    private fun parseProductFwInfoType(hex: String) {
//
//        val payload = hex.substring(16)                               // 7-байт header -> вон
//        val fwInfo  = Json.decodeFromString<FirmwareInfoStruct>("\"$payload\"")
//
//        platformLog("parseProductFwInfoType", "fwInfoStruct = $fwInfo")
//
//        platformLog("FW_INFO_RX", "code=${fwInfo.fwCode}  ver=${fwInfo.fwVersion}")
//
//        // ➜  единая точка выхода наружу
//        bleCommandExecutor.updateFirmwareInfo(fwInfo)
//    }
    private fun parseProductFwInfoType(hex: String) {
        val deviceAddr = castUnsignedCharToInt(hex.substring(12, 14).toInt(16).toByte())
        val payload = hex.substring(16)

        val fw = Json.decodeFromString<FirmwareInfoStruct>("\"$payload\"")
            .copy(deviceAddress = deviceAddr)

    platformLog("parseProductInfoTypefw", "fw = $fw")
    platformLog("FW_INFO_RX", "addr=$deviceAddr code=${fw.fwCode} ver=${fw.fwVersion}")
//        bleCommandExecutor.updateFirmwareInfo(fw)
        FirmwareInfoState.emitFirmwareInfo(fw)
    }



    private fun getNextIDParameter(ID: Int): Int {
        for (item in baseParametrInfoStructArray.indices) {
            if (ID < baseParametrInfoStructArray[item].ID) {
                if (baseParametrInfoStructArray[item].additionalInfoSize != 0) {
                    return baseParametrInfoStructArray[item].ID
                }
            }
        }
        return 0
    }

    private fun getNextSubDevice(subDeviceCounter: Int): Int {
        for ((index, item) in baseSubDevicesInfoStructSet.withIndex()) {
            platformLog("getNextSubDevice", "index=$index  subDeviceCounter=$subDeviceCounter из $numberSubDevice  subDeviceChankParametersCounter=$subDeviceChankParametersCounter  deviceAddress=${item.deviceAddress}")
            if (index >= subDeviceCounter) {
                if (item.parametrsNum > 10) {
                    if (subDeviceChankParametersCounter == 0) {
                        platformLog("getNextSubDevice", "1 subDeviceCounter=${this.subDeviceCounter} return=${item.deviceAddress}  subDeviceChankParametersCounter=$subDeviceChankParametersCounter  baseSubDevicesInfoStructSet.size=${baseSubDevicesInfoStructSet.size}  parametrsNum=${item.parametrsNum}")
                        return item.deviceAddress
                    } else {
                        if (subDeviceChankParametersCounter * 10 >= item.parametrsNum) {
                            this.subDeviceCounter++
                            this.subDeviceChankParametersCounter = 0
                            platformLog("getNextSubDevice", "произвели сброс subDeviceChankParametersCounter=0")
                        }
                        platformLog("getNextSubDevice", "2 subDeviceCounter=${this.subDeviceCounter} return=${item.deviceAddress}  subDeviceChankParametersCounter=$subDeviceChankParametersCounter  baseSubDevicesInfoStructSet.size=${baseSubDevicesInfoStructSet.size}  parametrsNum=${item.parametrsNum}")
                        return item.deviceAddress
                    }
                } else {
                    this.subDeviceChankParametersCounter = 0
                    platformLog("getNextSubDevice", "произвели сброс subDeviceChankParametersCounter=0")
                    if (item.parametrsNum != 0) {
                        platformLog("getNextSubDevice", "3 subDeviceCounter=${this.subDeviceCounter} return=${item.deviceAddress}  subDeviceChankParametersCounter=$subDeviceChankParametersCounter  baseSubDevicesInfoStructSet.size=${baseSubDevicesInfoStructSet.size}  parametrsNum=${item.parametrsNum}")
                        return item.deviceAddress
                    }
                }
            }
        }
        platformLog("getNextSubDevice", "return -1")
        platformLog("getNextSubDevice", "4 subDeviceCounter=${this.subDeviceCounter} return=${-1}  subDeviceChankParametersCounter=$subDeviceChankParametersCounter  baseSubDevicesInfoStructSet.size=${baseSubDevicesInfoStructSet.size}")
        return -1
    }

    private fun areEqualExcludingSetIdS(obj1: BaseParameterWidgetSStruct, obj2: BaseParameterWidgetSStruct): Boolean {
        val baseParameterWidgetStruct1 = obj1.baseParameterWidgetStruct.copy(parameterInfoSet = obj2.baseParameterWidgetStruct.parameterInfoSet)
        val baseParameterWidgetStruct2 = obj2.baseParameterWidgetStruct
        return baseParameterWidgetStruct1 == baseParameterWidgetStruct2
    }

    private fun areEqualExcludingSetIdE(obj1: BaseParameterWidgetEStruct, obj2: BaseParameterWidgetEStruct): Boolean {
//        platformLog("areEqualExcludingSetIdE", "obj1 = $obj1  obj2 = $obj2")
        val baseParameterWidgetStruct1 = obj1.baseParameterWidgetStruct.copy(parameterInfoSet = obj2.baseParameterWidgetStruct.parameterInfoSet)
        val baseParameterWidgetStruct2 = obj2.baseParameterWidgetStruct
        return baseParameterWidgetStruct1 == baseParameterWidgetStruct2
    }

    private fun parseWidgets(receiveDataStringForParse: String, parameterID: Int, dataCode: Int, deviceAddress: Int):BaseParameterWidgetStruct {
        var baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${receiveDataStringForParse}\"")
        baseParameterWidgetStruct.widgetId
        platformLog("OPEN_CLOSE_THRESHOLD CODE_LABEL parametersIDAndDataCodes", "0 Quadruple = ${ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)} ")
        baseParameterWidgetStruct.parameterInfoSet.add(ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset))
        count += 1
        platformLog("BLEParser", "dataCode=$dataCode  deviceAddress = $deviceAddress parameterID = $parameterID  parseWidgets ID:  dataOffset = ${baseParameterWidgetStruct.dataOffset}")

        when (baseParameterWidgetStruct.widgetLabelType) {
            ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
                when (baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets UNKNOW")
                    }
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(commandParameterWidgetEStruct, commandParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                        val switchParameterWidgetEStruct = Json.decodeFromString<SwitchParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        switchParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(switchParameterWidgetEStruct, switchParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets COMBOBOX")
                    }
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets SLIDER")
                        val sliderParameterWidgetEStruct = Json.decodeFromString<SliderParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        sliderParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(sliderParameterWidgetEStruct, sliderParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PLOT CODE_LABEL")
                        val plotParameterWidgetEStruct = Json.decodeFromString<PlotParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        plotParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(plotParameterWidgetEStruct, plotParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets SPINBOX")
                    }
                    ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets EMG_GESTURE_CHANGE_SETTINGS")
                    }
                    ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets GESTURE_SETTINGS")
                    }
                    ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets CALIB_STATUS")
                    }
                    ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets CONTROL_MODE")
                    }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets OPEN_CLOSE_THRESHOLD CODE_LABEL")
                        val thresholdParameterWidgetEStruct = Json.decodeFromString<ThresholdParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        thresholdParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode = 5
                        val plotParameterWidgetEStruct = PlotParameterWidgetEStruct(baseParameterWidgetEStruct = thresholdParameterWidgetEStruct.baseParameterWidgetEStruct)
                        platformLog("OPEN_CLOSE_THRESHOLD CODE_LABEL parametersIDAndDataCodes_1", "1 Quadruple = ${ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)} thresholdParameterWidgetEStruct = $thresholdParameterWidgetEStruct")
                        plotParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(plotParameterWidgetEStruct, plotParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PLOT_AND_1_THRESHOLD")
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PLOT_AND_2_THRESHOLD")
                    }
                    ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PWCE_GESTURES_WINDOW")
                        val gesturesParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        gesturesParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(gesturesParameterWidgetEStruct, gesturesParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PWCE_OPTIC_LERNING_WIDGET $receiveDataStringForParse")
                        val opticParameterWidgetEStruct = Json.decodeFromString<OpticStartLearningWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        opticParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(opticParameterWidgetEStruct, opticParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> {
                        val spinnerParameterWidgetEStruct = Json.decodeFromString<SpinnerParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        spinnerParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(spinnerParameterWidgetEStruct, spinnerParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                }
            }
            ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
                when (baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets UNKNOW")
                    }
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets BUTTON STRING_LABEL")
                        val commandParameterWidgetSStruct = Json.decodeFromString<CommandParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets SWITCH")
                        val switchParameterWidgetSStruct = Json.decodeFromString<SwitchParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        switchParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(switchParameterWidgetSStruct, switchParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets COMBOBOX")
                    }
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets SLIDER S dataOffset = ${baseParameterWidgetStruct.dataOffset} dataCode = $dataCode  deviceAddress = $deviceAddress    parameterID = $parameterID")
                        val sliderParameterWidgetSStruct = Json.decodeFromString<SliderParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        sliderParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(sliderParameterWidgetSStruct, sliderParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PLOT STRING_LABEL")
                        val plotParameterWidgetSStruct = Json.decodeFromString<PlotParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        plotParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(plotParameterWidgetSStruct, plotParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets SPINBOX")
                        val spinnerParameterWidgetSStruct = Json.decodeFromString<SpinnerParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        spinnerParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(spinnerParameterWidgetSStruct, spinnerParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets EMG_GESTURE_CHANGE_SETTINGS")
                    }
                    ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets GESTURE_SETTINGS")
                    }
                    ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets CALIB_STATUS")
                    }
                    ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets CONTROL_MODE")
                    }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets OPEN_CLOSE_THRESHOLD STRING_LABEL")
                        val thresholdParameterWidgetSStruct = Json.decodeFromString<ThresholdParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        val plotParameterWidgetSStruct = PlotParameterWidgetSStruct(
                            openThresholdUpper = thresholdParameterWidgetSStruct.openThresholdUpper,
                            openThresholdLower = thresholdParameterWidgetSStruct.openThresholdLower,
                            closeThresholdUpper = thresholdParameterWidgetSStruct.closeThresholdUpper,
                            closeThresholdLower = thresholdParameterWidgetSStruct.closeThresholdLower,
                        )
                        plotParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(plotParameterWidgetSStruct, plotParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PLOT_AND_1_THRESHOLD")
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PLOT_AND_2_THRESHOLD")
                    }
                    ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PWCE_GESTURES_WINDOW")
                        val gesturesParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        gesturesParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(gesturesParameterWidgetSStruct, gesturesParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                    ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() -> {
                        platformLog("BLEParser", "parseWidgets PWCE_OPTIC_LEARNING_WIDGET")
                        val opticParameterWidgetSStruct = Json.decodeFromString<OpticStartLearningWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        opticParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                            ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                        )
                        addToListWidgets(opticParameterWidgetSStruct, opticParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.dataOffset)
                    }
                }
            }
        }
        return baseParameterWidgetStruct
    }

    private fun getSubDeviceParameterWithAdditionalParameters(itemPosition: Int): Triple<Int, Int, Int> {
        var count = 1
        baseSubDevicesInfoStructSet.forEach { subDevice ->
            subDevice.parametersList.forEach { parameterSubDevice ->
                if (subDevice.parametersList.size != 0) {
                    if (parameterSubDevice.additionalInfoSize != 0 && count == itemPosition) {
                        return Triple(subDevice.deviceAddress, parameterSubDevice.ID, itemPosition)
                    }
                    if (parameterSubDevice.additionalInfoSize != 0) {
                        count++
                    }
                }
            }
        }
        return Triple(0, 0, 0)
    }

    private fun addToListWidgets(widget: Any, baseParameterWidgetStruct: Any, parameterID: Int, dataCode: Int, deviceAddress: Int, dataOffset: Int) {
        platformLog("BLEParserTest", "▶️ addToListWidgets widgetCode=${baseParameterWidgetStruct} dataOffset=$dataOffset")
        var canAdd = true
        platformLog("addToListWidgets", "dataCode  = $dataCode  deviceAddress = $deviceAddress  parameterID = $parameterID  dataOffset = $dataOffset  parseWidgets")
        if (baseParameterWidgetStruct is BaseParameterWidgetEStruct) {
            listWidgets.forEach {
                when (it) {
                    is BaseParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
//                        platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdE(baseParameterWidgetStruct, it)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it)) {
                            canAdd = false
                            it.baseParameterWidgetStruct.parameterInfoSet.add(ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetStruct.dataOffset))
                        }
                        if (combineWidgetId == it.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetStruct.parameterInfoSet.add(ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetStruct.dataOffset))
                        }
                    }
                    is CommandParameterWidgetEStruct -> {
//                        platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                        platformLog("addToListWidgets", "E CommandParameterWidgetEStruct = $it")
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetEStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        if (combineWidgetId == it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetEStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                    }
                    is PlotParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId
//                        if (deviceAddress == 34 && parameterID == 3) {
//                            platformLog("areEqualExcludingSetIdE", "dataCode  = $dataCode  deviceAddress = $deviceAddress  parameterID = $parameterID  dataOffset = $dataOffset  parseWidgets")
//                            platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                            platformLog("areEqualExcludingSetIdE", "combineWidgetId = $combineWidgetId    combineWidgetIdIterated = $combineWidgetIdIterated")
//                        }
//                        if (deviceAddress == 6 && parameterID == 3) {
//                            platformLog("areEqualExcludingSetIdE", "dataCode  = $dataCode  deviceAddress = $deviceAddress  parameterID = $parameterID  dataOffset = $dataOffset  parseWidgets")
//                            platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
//                            platformLog("areEqualExcludingSetIdE", "combineWidgetId = $combineWidgetId    combineWidgetIdIterated = $combineWidgetIdIterated")
//                        }
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)) {
                            canAdd = false
                            platformLog("OPEN_CLOSE_THRESHOLD CODE_LABEL parametersIDAndDataCodes", "2 Quadruple = ${ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)} ")
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
                            )
                        }
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            platformLog("OPEN_CLOSE_THRESHOLD CODE_LABEL parametersIDAndDataCodes", "3 Quadruple = ${ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)} ")
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
                            )
                        }
                    }
                    is SliderParameterWidgetEStruct -> {
//                        platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetEStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        if (combineWidgetId == it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetEStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                    }
                    is SwitchParameterWidgetEStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == it.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetEStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        platformLog("SwitchParameterWidgetEStruct_addToListWidgets", "combineWidgetId = $combineWidgetId")
                    }
                    else -> {
                        platformLog("addToListWidgets", "E it = $it")
                    }
                }
            }
            listWidgets.forEachIndexed { index, it ->
//                platformLog("areEqualExcludingSetIdE", "widget №$index = $it")
            }
        } else if (baseParameterWidgetStruct is BaseParameterWidgetSStruct) {
            listWidgets.forEach {
                when (it) {
                    is CommandParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
//                        platformLog("areEqualExcludingSetIdE", "${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
                    }
                    is PlotParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
//                        platformLog("areEqualExcludingSetIdE", "ThresholdParameterWidgetSStruct ${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
                    }
                    is SliderParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        platformLog("parseWidgets SLIDER", "Quadruple = ${ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.baseParameterWidgetStruct.dataOffset)}  $combineWidgetId = $combineWidgetIdIterated")
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, baseParameterWidgetStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
//                        platformLog("areEqualExcludingSetIdE", "ThresholdParameterWidgetSStruct ${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
                    }
                    is ThresholdParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        val combineWidgetIdIterated = it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
                            )
                        }
                        if (combineWidgetId == combineWidgetIdIterated) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
                            )
                        }
//                        platformLog("areEqualExcludingSetIdE", "ThresholdParameterWidgetSStruct ${areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)}  baseParameterWidgetStruct = $baseParameterWidgetStruct")
                    }
                    is SwitchParameterWidgetSStruct -> {
                        val combineWidgetId = baseParameterWidgetStruct.baseParameterWidgetStruct.deviceId * 256 + baseParameterWidgetStruct.baseParameterWidgetStruct.widgetId
                        if (combineWidgetId == it.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId * 256 + it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetId) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.add(
                                ParameterInfo(parameterID, dataCode, deviceAddress, it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataOffset)
                            )
                        }
                        platformLog("SwitchParameterWidgetEStruct_addToListWidgets не E а S", "combineWidgetId = $combineWidgetId")
                    }
                    else -> {
                    }
                }
            }
        }
        if (canAdd) {
            listWidgets.add(widget)
        }
    }

    internal fun getStatusConnected(): Boolean {
        return mConnected
    }
}