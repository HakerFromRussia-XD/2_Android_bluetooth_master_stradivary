package com.bailout.stickk.ubi4.data.parser

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.additionalParameter.AdditionalInfoSizeStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceArrayInfoDataStruct
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceArrayInfoStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.AdditionalParameterInfoType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabelType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.fullInicializeConnectionStruct
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.plotArray
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.plotArrayFlow
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.ADDITIONAL_INFO_SEG
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.ADDITIONAL_INFO_SIZE_STRUCT_SIZE
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.BASE_PARAMETER_INFO_STRUCT_SIZE
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.HEADER_BLE_OFFSET
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.READ_DEVICE_ADDITIONAL_PARAMETR_DATA
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import kotlinx.serialization.json.Json
import android.util.Pair
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ParameterRef
import com.bailout.stickk.ubi4.models.PlotParameterRef
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.slidersFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.thresholdFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.experimental.and

class BLEParser(main: AppCompatActivity) {
    private val mMain: MainActivityUBI4 = main as MainActivityUBI4
    private var mConnected = false
    private var count = 0
    private var numberSubDevice = 0
    private var subDeviceCounter = 0
    private var subDeviceAdditionalCounter = 1


    internal fun parseReceivedData (data: ByteArray?) {
        if (data != null) {
            val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
            val dataTransmissionDirection = data[0]
            val bridgeIndicator = castUnsignedCharToInt(data[0] and 0b10000000.toByte())/128
            val requestType = castUnsignedCharToInt(data[0] and 0b01000000.toByte())/64
            val waitingAnsver = castUnsignedCharToInt(data[0] and 0b00100000.toByte())/32
            val codeRequest = if (data.size > 1) data[1] else 0
            val dataLength = if (data.size > 3) { castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4])*256} else 0
            val CRC = if (data.size > 5) castUnsignedCharToInt(data[5]) else 0
            val deviceAddress = if (data.size > 6) castUnsignedCharToInt(data[6]) else 0
            val packageCodeRequest = if (data.size > 7) data[7] else 0
            val ID = if (data.size > 8) castUnsignedCharToInt(data[8]) else 0
            System.err.println("BLE debug TEST displayFirstNotify data.size = ${data.size}  $receiveDataString  requestType=$requestType")

            if (requestType == 1) {
                // парсим параметры
                Log.d("uiGestureSettingsObservable", "парсим параметры вход")
                val parameterID = codeRequest.toInt()
                ParameterProvider.getParameter(deviceAddress, parameterID).data = receiveDataString.substring(HEADER_BLE_OFFSET*2, receiveDataString.length)
                updateAllUI(deviceAddress, parameterID, ParameterProvider.getParameter(deviceAddress, parameterID).dataCode)//
            } else {
                // парсим команды
                when (codeRequest){
                    (0x00).toByte() -> { System.err.println("TEST parser DEFOULT") }
                    BaseCommands.DEVICE_INFORMATION.number -> {
                        System.err.println("TEST parser DEVICE_INFORMATION (${packageCodeRequest})")
                        parseDeviceInformation(packageCodeRequest, ID, receiveDataString)
                    }
                    BaseCommands.DATA_MANAGER.number -> {
                        System.err.println("TEST parser DATA_MANAGER")
                        parseDataManger(packageCodeRequest, receiveDataString)
                    }
                    BaseCommands.WRITE_FW_COMMAND.number -> {System.err.println("TEST parser WRITE_FW_COMMAND")}
                    BaseCommands.DEVICE_ACCESS_COMMAND.number -> {System.err.println("TEST parser DEVICE_ACCESS_COMMAND")}
                    BaseCommands.ECHO_COMMAND.number -> {System.err.println("TEST parser ECHO_COMMAND")}
                    BaseCommands.SUB_DEVICE_MANAGER.number -> {System.err.println("TEST parser SUB_DEVICE_MANAGER")}
                    BaseCommands.GET_DEVICE_STATUS.number -> {System.err.println("TEST parser GET_DEVICE_STATUS")}
                    BaseCommands.DATA_TRANSFER_SETTINGS.number -> { System.err.println("TEST parser DATA_TRANSFER_SETTINGS") }
                    BaseCommands.COMPLEX_PARAMETER_TRANSFER.number -> {
                        System.err.println("TEST parser COMPLEX_PARAMETER_TRANSFER data.size = ${data.size}   dataLength = $dataLength")
                        var dataLengthMax = dataLength
                        var dataLength = dataLength
                        var counter = 1

                        while (dataLength > 0) {
                            Log.d("uiGestureSettingsObservableCP", "counter = $counter dataLength = $dataLength")
                            val deviceAddress = castUnsignedCharToInt(receiveDataString.substring((HEADER_BLE_OFFSET+(dataLengthMax-dataLength))*2, (HEADER_BLE_OFFSET+(dataLengthMax-dataLength)+1)*2).toInt(16).toByte())
                            val parameterID = castUnsignedCharToInt(receiveDataString.substring((HEADER_BLE_OFFSET+(dataLengthMax-dataLength)+1)*2, (HEADER_BLE_OFFSET+(dataLengthMax-dataLength)+2)*2).toInt(16).toByte())
                            val parameter = ParameterProvider.getParameter(deviceAddress, parameterID)
                            parameter.data = receiveDataString.substring((HEADER_BLE_OFFSET+(dataLengthMax-dataLength)+2)*2, (HEADER_BLE_OFFSET+(dataLengthMax-dataLength)+2+parameter.parameterDataSize)*2)
                            updateAllUI(deviceAddress, parameterID, parameter.dataCode)
                            dataLength -= (parameter.parameterDataSize + 2)
                            counter += 1
                        }
                    }
                }
            }
        }
    }

    private fun updateAllUI(deviceAddress: Int, parameterID: Int, dataCode: Int) {
        Log.d("uiGestureSettingsObservable", "dataCode = $dataCode")
        when (dataCode) {
            ParameterDataCodeEnum.PDCE_EMG_CH_1_3_VAL.number -> {
                Log.d("uiGestureSettingsObservable", "dataCode = $dataCode")
                val parameter = ParameterProvider.getParameter(deviceAddress, parameterID)
                val data = ParameterProvider.getParameter(deviceAddress, parameterID).data
                try {
                    if (parameter.parameterDataSize == 1) {
                        plotArray =
                            arrayListOf(castUnsignedCharToInt(data.substring(0, 2).toInt(16).toByte()))
                    }
                    if (parameter.parameterDataSize == 2) {
                        plotArray = arrayListOf(
                            castUnsignedCharToInt(data.substring(0, 2).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(2, 4).toInt(16).toByte())
                        )
                    }
                    if (parameter.parameterDataSize == 3) {
                        plotArray = arrayListOf(
                            castUnsignedCharToInt(data.substring(0, 2).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(2, 4).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(4, 6).toInt(16).toByte())
                        )
                    }
                    if (parameter.parameterDataSize == 4) {
                        plotArray = arrayListOf(
                            castUnsignedCharToInt(data.substring(0, 2).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(2, 4).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(4, 6).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(6, 8).toInt(16).toByte())
                        )
                    }
                    if (parameter.parameterDataSize == 5) {
                        plotArray = arrayListOf(
                            castUnsignedCharToInt(data.substring(0, 2).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(2, 4).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(4, 6).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(6, 8).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(8, 10).toInt(16).toByte())
                        )
                    }
                    if (parameter.parameterDataSize == 6) {
                        plotArray = arrayListOf(
                            castUnsignedCharToInt(data.substring(0, 2).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(2, 4).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(4, 6).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(6, 8).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(8, 10).toInt(16).toByte()),
                            castUnsignedCharToInt(data.substring(10, 12).toInt(16).toByte())
                        )
                    }
                } catch (e: Error) {
                    mMain.showToast("Ошибка 113")
                }
                CoroutineScope(Dispatchers.Default).launch { plotArrayFlow.emit(PlotParameterRef(deviceAddress, parameterID, plotArray))}
            }
            ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number -> {
                Log.d("parameter sliderCollect","deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode")
                CoroutineScope(Dispatchers.Default).launch { thresholdFlow.emit(ParameterRef(deviceAddress, parameterID)) }
            }
            ParameterDataCodeEnum.PDCE_GESTURE_SETTINGS.number -> {
                Log.d("uiGestureSettingsObservable", "dataCode = $dataCode")
                RxUpdateMainEventUbi4.getInstance().updateUiGestureSettings(dataCode) }
            ParameterDataCodeEnum.PDCE_GESTURE_GROUP.number -> {
                Log.d("uiRotationGroupObservable", "dataCode = $dataCode")
                RxUpdateMainEventUbi4.getInstance().updateUiRotationGroup(ParameterRef(deviceAddress, parameterID))
                CoroutineScope(Dispatchers.Default).launch { rotationGroupFlow.emit((0..1000).random()) } }
            ParameterDataCodeEnum.PDCE_OPTIC_LEARNING_DATA.number -> {
                Log.d("TestOptic"," dataCode: $dataCode")
                RxUpdateMainEventUbi4.getInstance().updateUiOpticTraining(dataCode) }
            ParameterDataCodeEnum.PDCE_GLOBAL_SENSITIVITY.number -> {
                Log.d("parameter sliderCollect","deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode")
                CoroutineScope(Dispatchers.Default).launch { slidersFlow.emit(ParameterRef(deviceAddress, parameterID)) }
            }
            ParameterDataCodeEnum.PDCE_INTERFECE_ERROR_COUNTER.number -> {
                Log.d("parameter sliderCollect","deviceAddress: $deviceAddress  parameterID: $parameterID   dataCode: $dataCode")
                CoroutineScope(Dispatchers.Default).launch { slidersFlow.emit(ParameterRef(deviceAddress, parameterID)) }
            }
            ParameterDataCodeEnum.PDCE_CALIBRATION_CURRENT_PERCENT.number -> {
                Log.d("TestOptic"," dataCode: $dataCode")
                CoroutineScope(Dispatchers.Default).launch { slidersFlow.emit(ParameterRef(deviceAddress, parameterID)) }
            }
            //TODO поставить актуальный dataCode
            ParameterDataCodeEnum.PDCE_GLOBAL_SENSITIVITY.number -> {
                Log.d("TestOptic", "dataCode: $dataCode")
                CoroutineScope(Dispatchers.Default).launch { switcherFlow.emit((0..1).random()) }
            }
        }
    }

    // All data parsers
    private fun parseDeviceInformation(packageCodeRequest: Byte, ID: Int, receiveDataString: String) {
        when (packageCodeRequest) {
            (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
            DeviceInformationCommand.INICIALIZE_INFORMATION.number -> {
                parseInitializeInformation(receiveDataString)
            }
            DeviceInformationCommand.READ_DEVICE_PARAMETRS.number -> {
                parseReadDeviceParameters(receiveDataString)
            }
            DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETRS.number -> {
                parseReadDeviceAdditionalParameters(ID, receiveDataString)
            }

            DeviceInformationCommand.READ_SUB_DEVICES_FIRST_INFO.number -> {System.err.println("TEST parser 2 READ_SUB_DEVICES_FIRST_INFO")}
            DeviceInformationCommand.READ_SUB_DEVICE_INFO.number -> {
                System.err.println("TEST parser 2 READ_SUB_DEVICE_INFO")
                parseReadSubDeviceInfo(receiveDataString)
            }
            DeviceInformationCommand.READ_SUB_DEVICE_PARAMETERS.number -> {
                System.err.println("TEST parser 2 READ_SUB_DEVICE_PARAMETERS")
                parseReadSubDeviceParameters(receiveDataString)
            }
            DeviceInformationCommand.READ_SUB_DEVICE_ADDITIONAL_PARAMETER.number -> {
                System.err.println("TEST parser 2 READ_SUB_DEVICE_ADDITIONAL_PARAMETER")
                val addressSubDevice = castUnsignedCharToInt(receiveDataString.substring(16, 18).toInt(16).toByte()) // хедер 7 + 1 байта данные до addressSubDevice (адрес сабдевайса передаётся в возращаемых данных вторым байтом)
                val parameterID = castUnsignedCharToInt(receiveDataString.substring(18, 20).toInt(16).toByte()) // хедер 7 + 2 байта данные до ID (ID-параметра передаётся в возращаемых данных третьим байтом)
                parseReadSubDeviceAdditionalParameters(addressSubDevice, parameterID, receiveDataString)
            }
            DeviceInformationCommand.SUB_DEVICE_PARAMETER_INIT_READ.number -> {System.err.println("TEST parser 2 SUB_DEVICE_PARAMETER_INIT_READ")}
            DeviceInformationCommand.SUB_DEVICE_PARAMETER_INIT_WRITE.number -> {System.err.println("TEST parser 2 SUB_DEVICE_PARAMETER_INIT_WRITE")}

            DeviceInformationCommand.GET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 GET_SERIAL_NUMBER")}
            DeviceInformationCommand.SET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 SET_SERIAL_NUMBER")}
            DeviceInformationCommand.GET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 GET_DEVICE_NAME")}
            DeviceInformationCommand.SET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 SET_DEVICE_NAME")}
            DeviceInformationCommand.GET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 GET_DEVICE_ROLE")}
            DeviceInformationCommand.SET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 SET_DEVICE_ROLE")}
        }
    }
    private fun parseDataManger(packageCodeRequest: Byte, receiveDataString: String) {
        when (packageCodeRequest) {
            (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
            DataManagerCommand.READ_AVAILABLE_SLOTS.number -> {System.err.println("TEST parser 2 READ_AVAILABLE_SLOTS")}
            DataManagerCommand.WRITE_SLOT.number -> {System.err.println("TEST parser 2 WRITE_SLOT")}
            DataManagerCommand.READ_DATA.number -> { System.err.println("TEST parser 2 READ_DATA")
                parseReadSubDeviceInfoData(receiveDataString)
            }
            DataManagerCommand.WRITE_DATA.number -> {System.err.println("TEST parser 2 WRITE_DATA")}
            DataManagerCommand.RESET_TO_FACTORY.number -> {System.err.println("TEST parser 2 RESET_TO_FACTORY")}
            DataManagerCommand.SAVE_DATA.number -> {System.err.println("TEST parser 2 SAVE_DATA")}
        }
    }

    // DeviceInformation parsers
    private fun parseInitializeInformation(receiveDataString: String) {
        fullInicializeConnectionStruct = Json.decodeFromString<FullInicializeConnectionStruct>("\"${receiveDataString.substring(18,receiveDataString.length)}\"")
        System.err.println("TEST parser 2 INICIALIZE_INFORMATION $fullInicializeConnectionStruct" )
        mMain.bleCommand(BLECommands.requestBaseParametrInfo(0x00, fullInicializeConnectionStruct.parametrsNum.toByte()), MAIN_CHANNEL, WRITE)
    }
    private fun parseReadDeviceParameters(receiveDataString: String) {
        val listA: ArrayList<BaseParameterInfoStruct> = ArrayList()
        System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS $receiveDataString" )
        for(i in 0 until fullInicializeConnectionStruct.parametrsNum) {
            listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"${receiveDataString.substring(20+i*BASE_PARAMETER_INFO_STRUCT_SIZE, 20+(i+1)*BASE_PARAMETER_INFO_STRUCT_SIZE)}\""))
        }
        baseParametrInfoStructArray = listA
        var widgetCount = 0
        baseParametrInfoStructArray.forEach {
            widgetCount += it.additionalInfoSize
            println("READ_DEVICE_PARAMETRS $it $widgetCount")
        }


        if (baseParametrInfoStructArray.size != 0) {
            // если у запрашиваемого параметра нет адишнл параметров, то на этом алгоритм опроса остановится
            Log.d("getNextIDParameter", "запрос адшнл параметра")
            if (baseParametrInfoStructArray[0].additionalInfoSize != 0) {
                mMain.bleCommand(
                    BLECommands.requestAdditionalParametrInfo(
                        baseParametrInfoStructArray[0].ID.toByte()
                    ), MAIN_CHANNEL, WRITE
                )
            } else {
                //проход по остальным параметрам
                val ID =
                    getNextIDParameter(0) //если у параметра additionalInfoSize = 0 то его пропустим
                if (ID != 0) {
                    Log.d("getNextIDParameter", "запроса адшнл параметра")
                    mMain.bleCommand(
                        BLECommands.requestAdditionalParametrInfo(
                            baseParametrInfoStructArray[ID].ID.toByte()
                        ), MAIN_CHANNEL, WRITE
                    )
                } else {
                    Log.d("getNextIDParameter", "конец запроса параметров")
                    mMain.bleCommand(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE)
                }
            }
        }
    }
    private fun parseReadDeviceAdditionalParameters(ID: Int, receiveDataString: String) {
        // читает каждый параметр отдельно по его ID
        // за один заход обрабатывает все ADDITIONAL_PARAMETR определённого параметра
        System.err.println("TEST parser 2 принятая посылка READ_DEVICE_ADDITIONAL_PARAMETRS $receiveDataString additionalInfoSize=${baseParametrInfoStructArray[ID].additionalInfoSize}")
        val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
        var dataOffset = 0
        // инициализация ID происходит здесь потому
        // что в ответе есть ID обрабатываемого параметра
        var ID = ID

        if (baseParametrInfoStructArray[ID].additionalInfoSize != 0) {
            for (i in 0 until baseParametrInfoStructArray[ID].additionalInfoSize) {
                //каждый новый цикл вычитываем данные следующего сегмента (следующий addInfoSeg)
                val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(offset+i*ADDITIONAL_INFO_SIZE_STRUCT_SIZE, offset+(i+1)*ADDITIONAL_INFO_SIZE_STRUCT_SIZE)}\"")
                val receiveDataStringForParse = receiveDataString.substring(
                    offset + //отступ на header + отправленные данные (отправленный запрос целиком)
                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG + //отступ на n кол-во additionalInfoSeg в конкретном параметре
                            dataOffset*2, // отступ на кол-во байт в предыдущих dataSeg (важно если у нас больше одного сегмента, для первого сегмента 0)
                    offset +
                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG +
                            dataOffset*2 +
                            additionalInfoSizeStruct.infoSize*2) // оступ на кол-во байт в считываемом сегменте
//                                        System.err.println("testSignal 0 $receiveDataStringForParse")
                dataOffset = additionalInfoSizeStruct.infoSize


                when (additionalInfoSizeStruct.infoType) {
                    AdditionalParameterInfoType.WIDGET.number.toInt() -> {
                        parseWidgets(receiveDataStringForParse, parameterID = ID, dataCode = baseParametrInfoStructArray[ID].dataCode)
                        GlobalScope.launch {
                            mMain.sendWidgetsArray()
                        }
                    }
                }
            }
        }

        //проход по остальным параметрам
        ID = getNextIDParameter(ID) //если у параметра additionalInfoSize = 0 то его пропустим
        if (ID != 0) {
            Log.d("getNextIDParameter", "запроса адшнл параметра")
            mMain.bleCommand(
                BLECommands.requestAdditionalParametrInfo(
                    baseParametrInfoStructArray[ID].ID.toByte()
                ), MAIN_CHANNEL, WRITE
            )
        } else {
            Log.d("getNextIDParameter", "конец запроса адшнл параметров")
            mMain.bleCommand(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE)
        }
    }
    private fun parseReadSubDeviceInfo(receiveDataString: String) {
        Log.d("SubDeviceSubDevice", "receiveDataString=$receiveDataString")
        val subDevices = Json.decodeFromString<BaseSubDeviceArrayInfoStruct>("\"${receiveDataString.substring(16,receiveDataString.length)}\"") // 8 байт заголовок и отправленные данные
        baseSubDevicesInfoStructSet = subDevices.baseSubDeviceInfoStructArray
        numberSubDevice = subDevices.count


        // тут нам нужно запустить цепную реакцию сабдевайсов (читаем параметры первого сабдевайса)
        Log.d("SubDeviceSubDevice", "subDevices=$baseSubDevicesInfoStructSet  numberSubDevice=$numberSubDevice")
        if (baseSubDevicesInfoStructSet.size != 0) {
            mMain.bleCommand(
                BLECommands.requestSubDeviceParametrs(
                    baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress,
                    0,
                    baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum
                ), MAIN_CHANNEL, WRITE
            )
        } else {
            mMain.showToast("Сабдевайсов нет")
        }
    }
    private fun parseReadSubDeviceParameters(receiveDataString: String) {
        // пробегаемся по всем параметрам, формируя их список
        val listA: ArrayList<BaseParameterInfoStruct> = ArrayList()
        for (i in 0 until baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum) {
            val start = 22 + i * BASE_PARAMETER_INFO_STRUCT_SIZE
            val end = 22 + (i + 1) * BASE_PARAMETER_INFO_STRUCT_SIZE

            if (end <= receiveDataString.length) {
                try {
                    val parameterJson = receiveDataString.substring(start, end)
                    listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"$parameterJson\""))
                } catch (e: Exception) {}
            } else {
                Log.e("error", "Индексы $start-$end выходят за пределы строки длиной ${receiveDataString.length}")
                break
            }
            listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"${receiveDataString.substring(22 + i * BASE_PARAMETER_INFO_STRUCT_SIZE, 22 + (i + 1) * BASE_PARAMETER_INFO_STRUCT_SIZE)}\""))
        }

        // присваиваем этот список соответствующему полю сабдевайса parametrsList
        baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList = listA
        Log.d(
            "SubDeviceAdditionalParameters",
            "прочитали параметры из сабдевайса ${
                baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress
            } их ${baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList.size} listA=${listA.size} subDeviceCounter=$subDeviceCounter"
        )

        // берём следующий сабдевайс у которого количество параметров не равно 0
        if (getNextSubDevice(subDeviceCounter) != 0) {
            mMain.bleCommand(
                BLECommands.requestSubDeviceParametrs(
                    baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).deviceAddress,
                    0,
                    baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametrsNum
                ), MAIN_CHANNEL, WRITE
            )
        } else {
            Log.d(
                "SubDeviceAdditionalParameters",
                "закончили чтение всех параметров во всех сабдевайсах"
            )
            Log.d("SubDeviceAdditionalParameters", "subDeviceCounter = $subDeviceCounter")
            subDeviceCounter = 0

            Log.d("SubDeviceAdditionalParameters", "6 = ${getSubDeviceParameterWithAdditionalParameters(1).first}  0 = ${getSubDeviceParameterWithAdditionalParameters(1).second}")
            if (getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third == 0) {
                Log.d("SubDeviceAdditionalParameters", "у сабдевайсов нет ни одного виджета")
                Log.d("SubDeviceAdditionalParameters", "конец запроса параметров сабдевайса")
                subDeviceAdditionalCounter = 1
            } else {
                Log.d("SubDeviceAdditionalParameters", "запроса адишнл параметра")
                mMain.bleCommand(
                    BLECommands.requestSubDeviceAdditionalParametrs(
                        getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).first,
                        getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).second
                    ), MAIN_CHANNEL, WRITE
                )
                subDeviceAdditionalCounter ++
            }
        }
    }
    private fun parseReadSubDeviceAdditionalParameters(addressSubDevice: Int, parameterID: Int, receiveDataString: String) {
        val offset = HEADER_BLE_OFFSET * 2 + READ_SUB_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
        var dataOffset = 0


        // читает каждый параметр отдельно по его ID
        // за один заход обрабатывает все ADDITIONAL_PARAMETR определённого параметра
        baseSubDevicesInfoStructSet.forEach { subDevice ->
            subDevice.parametersList.forEach { parametrSubDevice ->
                if (subDevice.deviceAddress == addressSubDevice) {
                    if (parametrSubDevice.ID == parameterID) {
                        Log.d("parseReadSubDeviceAdditionalParameters", "deviceAddress=${subDevice.deviceAddress}   additionalInfoSize=${parametrSubDevice.additionalInfoSize}  receiveDataString=$receiveDataString")
                        for (i in 0 until parametrSubDevice.additionalInfoSize) {
                            //каждый новый цикл вычитываем данные следующего сегмента (следующий addInfoSeg)
                            val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(offset+i*ADDITIONAL_INFO_SIZE_STRUCT_SIZE, offset+(i+1)*ADDITIONAL_INFO_SIZE_STRUCT_SIZE)}\"")
                            Log.d("parseReadSubDeviceAdditionalParameters", "additionalInfoSizeStruct = $additionalInfoSizeStruct")
                            val start = offset + //отступ на header + отправленные данные (отправленный запрос целиком)
                                             parametrSubDevice.additionalInfoSize*ADDITIONAL_INFO_SEG + //отступ на n кол-во additionalInfoSeg в конкретном параметре
                                             dataOffset*2 // отступ на кол-во байт в предыдущих dataSeg (важно если у нас больше одного сегмента, для первого сегмента 0)
                            val end = offset +
                                      parametrSubDevice.additionalInfoSize*ADDITIONAL_INFO_SEG +
                                      dataOffset*2 +
                                      additionalInfoSizeStruct.infoSize*2

                            Log.d("parseReadSubDeviceAdditionalParameters", "start = $start    end = $end  receiveDataString.length = ${receiveDataString.length}")
                            var receiveDataStringForParse = ""
                            if (end <= receiveDataString.length) {
                                receiveDataStringForParse = receiveDataString.substring(start, end)
                            }
                            dataOffset += additionalInfoSizeStruct.infoSize
                            Log.d("parseReadSubDeviceAdditionalParameters", "receiveDataStringForParse = $receiveDataStringForParse")


                            when (additionalInfoSizeStruct.infoType) {
                                AdditionalParameterInfoType.WIDGET.number.toInt() -> {
                                    parseWidgets(receiveDataStringForParse, parameterID = parametrSubDevice.ID, dataCode = parametrSubDevice.dataCode)
                                    GlobalScope.launch {
                                        mMain.sendWidgetsArray()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        //проход по остальным параметрам
        if (getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).third != 0) {
            Log.d("SubDeviceAdditionalParameters", "запроса адишнл параметра")
            mMain.bleCommand(
                BLECommands.requestSubDeviceAdditionalParametrs(
                    getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).first,
                    getSubDeviceParameterWithAdditionalParameters(subDeviceAdditionalCounter).second
                ), MAIN_CHANNEL, WRITE
            )
            subDeviceAdditionalCounter ++
        } else {
            subDeviceAdditionalCounter = 1
            Log.d("SubDeviceAdditionalParameters", "конец запроса адишнл параметров сабдевайса")
        }
    }

    // DataManger parsers
    private fun parseReadSubDeviceInfoData(receiveDataString: String) {
        System.err.println("TEST parser 2 READ_DATA befo parse test $receiveDataString")
        val test = Json.decodeFromString<BaseSubDeviceArrayInfoDataStruct>("\"${receiveDataString.substring(16,receiveDataString.length)}\"") // 8 байт заголовок и отправленные данные
        System.err.println("TEST parser 2 READ_DATA $test")
    }

    private fun getNextIDParameter(ID: Int): Int{
        for (item in baseParametrInfoStructArray.indices) {
            if (ID < baseParametrInfoStructArray[item].ID ) {
                if (baseParametrInfoStructArray[item].additionalInfoSize != 0) {
                    return baseParametrInfoStructArray[item].ID
                }
            }
        }
        return 0
    }
    private fun getNextSubDevice(subDeviceCounter: Int):Int {
        // TODO функция не проверена в бою на множестве сабдевайсов, есть подозрение что возникнет рассинхрон subDeviceCounter
        for ((index, item) in baseSubDevicesInfoStructSet.withIndex()) {
            Log.d("SubDeviceSubDevice", "index=$index item=$item")
            if (index > subDeviceCounter) {
                this.subDeviceCounter ++
                if (item.parametrsNum != 0) {
                   return item.deviceAddress
                }
            }
        }
        return 0
    }
    private fun getNextSubDeviceParameter(subDeviceParameterCounter: Int):Int {
        // TODO функция не проверена в бою на множестве сабдевайсов, есть подозрение что возникнет рассинхрон subDeviceCounter
        Log.d("SubDeviceSubDevice", "baseSubDevicesInfoStructArray=${baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList.size}")
//        baseSubDevicesInfoStructArray[subDeviceCounter].parametrsList
        for (index in baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList.indices) {
            if (subDeviceParameterCounter < baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList[index].ID ) {
                if (baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList[index].additionalInfoSize != 0) {
                    return baseSubDevicesInfoStructSet.elementAt(subDeviceCounter).parametersList[index].ID
                }
            }
        }
        return 0
    }
    private fun areEqualExcludingSetIdS(obj1: BaseParameterWidgetSStruct, obj2: BaseParameterWidgetSStruct): Boolean {
        // Сравниваем объекты, исключив поле parametersIDAndDataCodes
        val baseParameterWidgetStruct1 = obj1.baseParameterWidgetStruct.copy(parametersIDAndDataCodes = obj2.baseParameterWidgetStruct.parametersIDAndDataCodes)
        val baseParameterWidgetStruct2 = obj2.baseParameterWidgetStruct

        return  baseParameterWidgetStruct1 == baseParameterWidgetStruct2
    }
    private fun areEqualExcludingSetIdE(obj1: BaseParameterWidgetEStruct, obj2: BaseParameterWidgetEStruct): Boolean {
        // Сравниваем объекты, исключив поле parametersIDAndDataCodes
        val baseParameterWidgetStruct1 = obj1.baseParameterWidgetStruct.copy(parametersIDAndDataCodes = obj2.baseParameterWidgetStruct.parametersIDAndDataCodes)
        val baseParameterWidgetStruct2 = obj2.baseParameterWidgetStruct

        return  baseParameterWidgetStruct1 == baseParameterWidgetStruct2
    }
    private fun parseWidgets(receiveDataStringForParse: String, parameterID: Int, dataCode: Int) {
        var baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${receiveDataStringForParse}\"")//+parameterID.toString()
        // тут надо проверить есть-ли в сете такой же объект за исключением поля parametersIDAndDataCodes

//        if (listWidgets  )

        baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
        count += 1

        System.err.println("parseWidgets ID:${baseParameterWidgetStruct}")
        when (baseParameterWidgetStruct.widgetLabelType) {
            ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
                when (baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { System.err.println("parseWidgets UNKNOW") }
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(commandParameterWidgetEStruct, commandParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {
                        val switchParameterWidgetEStruct = Json.decodeFromString<SwitchParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        switchParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(switchParameterWidgetEStruct,switchParameterWidgetEStruct.baseParameterWidgetEStruct,parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { System.err.println("parseWidgets COMBOBOX") }
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                        System.err.println("parseWidgets SLIDER")
                        val sliderParameterWidgetEStruct = Json.decodeFromString<SliderParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        sliderParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(sliderParameterWidgetEStruct, sliderParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        System.err.println("parseWidgets PLOT CODE_LABEL")
                        val plotParameterWidgetEStruct = Json.decodeFromString<PlotParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        plotParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(plotParameterWidgetEStruct, plotParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { System.err.println("parseWidgets SPINBOX") }
                    ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets EMG_GESTURE_CHANGE_SETTINGS") }
                    ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> {System.err.println("parseWidgets GESTURE_SETTINGS")}
                    ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { System.err.println("parseWidgets CALIB_STATUS") }
                    ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { System.err.println("parseWidgets CONTROL_MODE") }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        System.err.println("parseWidgets OPEN_CLOSE_THRESHOLD CODE_LABEL")
                        //TODO пока тестовая заглушка кнопкой
                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(commandParameterWidgetEStruct, commandParameterWidgetEStruct.baseParameterWidgetEStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_1_THRESHOLD") }
                    ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_2_THRESHOLD") }
                    ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {
                        System.err.println("parseWidgets PWCE_GESTURES_WINDOW")
                        val gesturesParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        gesturesParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(gesturesParameterWidgetEStruct, gesturesParameterWidgetEStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() -> {
                        System.err.println("parseWidgets PWCE_OPTIC_LERNING_WIDGET $receiveDataStringForParse")
                        val opticParameterWidgetEStruct = Json.decodeFromString<OpticStartLearningWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        opticParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(opticParameterWidgetEStruct,opticParameterWidgetEStruct.baseParameterWidgetEStruct,parameterID, dataCode)
                    }
                }
            }
            ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
                when (baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { System.err.println("parseWidgets UNKNOW") }
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        System.err.println("parseWidgets BUTTON STRING_LABEL")
                        val commandParameterWidgetSStruct = Json.decodeFromString<CommandParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> {  System.err.println("parseWidgets SWITCH")
                        val switchParameterWidgetSStruct = Json.decodeFromString<SwitchParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        switchParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(switchParameterWidgetSStruct,switchParameterWidgetSStruct.baseParameterWidgetSStruct,parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { System.err.println("parseWidgets COMBOBOX") }
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> {
                        System.err.println("parseWidgets SLIDER")
                        val sliderParameterWidgetSStruct = Json.decodeFromString<SliderParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        sliderParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(sliderParameterWidgetSStruct, sliderParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        System.err.println("parseWidgets PLOT STRING_LABEL")
                        val plotParameterWidgetSStruct = Json.decodeFromString<CommandParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        plotParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(plotParameterWidgetSStruct, plotParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { System.err.println("parseWidgets SPINBOX") }
                    ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets EMG_GESTURE_CHANGE_SETTINGS") }
                    ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets GESTURE_SETTINGS") }
                    ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { System.err.println("parseWidgets CALIB_STATUS") }
                    ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { System.err.println("parseWidgets CONTROL_MODE") }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        System.err.println("parseWidgets OPEN_CLOSE_THRESHOLD STRING_LABEL")
                        //TODO пока тестовая заглушка кнопкой
                        val commandParameterWidgetSStruct = Json.decodeFromString<CommandParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(commandParameterWidgetSStruct, commandParameterWidgetSStruct.baseParameterWidgetSStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_1_THRESHOLD") }
                    ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_2_THRESHOLD") }
                    ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> {
                        System.err.println("parseWidgets PWCE_GESTURES_WINDOW")
                        val gesturesParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        gesturesParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(gesturesParameterWidgetSStruct, gesturesParameterWidgetSStruct, parameterID, dataCode)
                    }
                    ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() -> { System.err.println("parseWidgets PWCE_OPTIC_LEARNING_WIDGET")
                        val opticParameterWidgetSStruct = Json.decodeFromString<OpticStartLearningWidgetSStruct>("\"${receiveDataStringForParse}\"")
                        opticParameterWidgetSStruct.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        addToListWidgets(opticParameterWidgetSStruct,opticParameterWidgetSStruct.baseParameterWidgetSStruct,parameterID, dataCode)
                    }
                }
            }
        }
    }
    private fun getSubDeviceParameterWithAdditionalParameters(itemPosition: Int): Triple<Int,Int,Int>  {
        // в itemPosition мы передаём номер того параметра у которго мы заберём адишнл
        // параметры. Например, если у нас есть три параметра с адишнл параметрами, то
        // передав этой функции itemPosition = 3 мы получим на выходе первым параметром
        // subDevice.deviceAddress и вторым параметром parameterSubDevice.ID для запроса
        // адишнл параметров именно у третьего параметра с адишнл параметрами. Если передаём
        // itemPosition = 1, то функция выдаст параметры для запроса адишнл параметров у первого
        // параметра с адишнл параметрами
        var count = 1
        baseSubDevicesInfoStructSet.forEach { subDevice ->
            subDevice.parametersList.forEach { parameterSubDevice ->
                if (subDevice.parametersList.size != 0 ) {
                    if (parameterSubDevice.additionalInfoSize != 0 && count == itemPosition) {
                        return Triple(subDevice.deviceAddress, parameterSubDevice.ID, itemPosition)
                    }
                    if (parameterSubDevice.additionalInfoSize != 0) {
                        // мы инкриментируем count каждый раз, когда встречаем параметр у которого
                        // есть адишнл параметры (виджеты)
                        count ++
                    }
                }
            }
        }
        return Triple(0, 0, 0)
    }
    private fun addToListWidgets(widget: Any, baseParameterWidgetStruct: Any, parameterID: Int, dataCode: Int) {
        // добавление нового виджета не происходит если у нас уже есть такой виджет
        // у другого параметра. Вместо этого добавляется ссылка на новый параметр с
        // этим виджетом в parametersIDAndDataCodes
        var canAdd = true
        if (baseParameterWidgetStruct is BaseParameterWidgetEStruct) {
            listWidgets.forEach {
                when (it) {
                    is BaseParameterWidgetEStruct -> {
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it)) {
                            canAdd = false
                            it.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                    is CommandParameterWidgetEStruct -> {
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                    is PlotParameterWidgetEStruct -> {
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                    is SliderParameterWidgetEStruct -> {
                        if (areEqualExcludingSetIdE(baseParameterWidgetStruct, it.baseParameterWidgetEStruct)) {
                            canAdd = false
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                }
            }
        } else if (baseParameterWidgetStruct is BaseParameterWidgetSStruct) {
            listWidgets.forEach {
                when (it) {
                    is CommandParameterWidgetSStruct -> {
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                    is PlotParameterWidgetSStruct -> {
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                    is SliderParameterWidgetSStruct -> {
                        if (areEqualExcludingSetIdS(baseParameterWidgetStruct, it.baseParameterWidgetSStruct)) {
                            canAdd = false
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.add(Pair(parameterID, dataCode))
                        }
                    }
                }
            }
        }
        if (canAdd) {
            listWidgets.add(widget)
        }
    }

    internal fun getStatusConnected() : Boolean { return mConnected }
}