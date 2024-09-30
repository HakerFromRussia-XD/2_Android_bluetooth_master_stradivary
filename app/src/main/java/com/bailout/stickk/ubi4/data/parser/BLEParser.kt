package com.bailout.stickk.ubi4.data.parser

import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.additionalParameter.AdditionalInfoSizeStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.AdditionalParameterInfoType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabelType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseParametrInfoStructArray
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
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BLEParser(main: AppCompatActivity) {
    private val mMain: MainActivityUBI4 = main as MainActivityUBI4
    private var mConnected = false
    private var count = 0


    internal fun parseReceivedData (data: ByteArray?) {
        if (data != null) {
            val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
            System.err.println("BLE debug TEST displayFirstNotify data.size = ${data.size}  $receiveDataString")
            val dataTransmissionDirection = data[0]
            val codeRequest = if (data.size > 1) data[1] else 0
            val dataLength = if (data.size > 3) { castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4])*256} else 0
            System.err.println("TEST dataLength = $dataLength  codeRequest = $codeRequest")
            val packageCodeRequest = if (data.size > 7) data[7] else 0
            val ID = if (data.size > 8) castUnsignedCharToInt(data[8]) else 0


            when (codeRequest){
                (0x00).toByte() -> { System.err.println("TEST parser DEFOULT") }
                BaseCommands.DEVICE_INFORMATION.number -> {
                    System.err.println("TEST parser DEVICE_INFORMATION (${packageCodeRequest})")
                    parseDeviceInformation(packageCodeRequest, ID, receiveDataString)
                }
                BaseCommands.DATA_MANAGER.number -> {
                    System.err.println("TEST parser DATA_MANAGER")
                    parseDataManger(packageCodeRequest)
                }
                BaseCommands.WRITE_FW_COMMAND.number -> {System.err.println("TEST parser WRITE_FW_COMMAND")}
                BaseCommands.DEVICE_ACCESS_COMMAND.number -> {System.err.println("TEST parser DEVICE_ACCESS_COMMAND")}
                BaseCommands.ECHO_COMMAND.number -> {System.err.println("TEST parser ECHO_COMMAND")}
                BaseCommands.SUB_DEVICE_MANAGER.number -> {System.err.println("TEST parser SUB_DEVICE_MANAGER")}
                BaseCommands.GET_DEVICE_STATUS.number -> {System.err.println("TEST parser GET_DEVICE_STATUS")}
                BaseCommands.DATA_TRANSFER_SETTINGS.number -> { System.err.println("TEST parser DATA_TRANSFER_SETTINGS") }
                BaseCommands.COMPLEX_PARAMETER_TRANSFER.number -> {
                    System.err.println("TEST parser COMPLEX_PARAMETER_TRANSFER $receiveDataString")
                    plotArray = arrayListOf(castUnsignedCharToInt(data[9]),castUnsignedCharToInt(data[10]),castUnsignedCharToInt(data[11]),castUnsignedCharToInt(data[12]),castUnsignedCharToInt(data[13]),castUnsignedCharToInt(data[14]))
                    plotArrayFlow.value = plotArray
                }
            }


//            System.err.println("TEST Serializer 1: ${Json.decodeFromString<FullInicializeConnectionStruct>("\"$testString\"").toString()}")
//            System.err.println("TEST Serializer 2: ${Json.encodeToString(myColor)}")
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
            DeviceInformationCommand.GET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 GET_SERIAL_NUMBER")}
            DeviceInformationCommand.SET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 SET_SERIAL_NUMBER")}
            DeviceInformationCommand.GET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 GET_DEVICE_NAME")}
            DeviceInformationCommand.SET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 SET_DEVICE_NAME")}
            DeviceInformationCommand.GET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 GET_DEVICE_ROLE")}
            DeviceInformationCommand.SET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 SET_DEVICE_ROLE")}
        }
    }
    private fun parseDataManger(packageCodeRequest: Byte) {
        when (packageCodeRequest) {
            (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
            DataManagerCommand.READ_AVAILABLE_SLOTS.number -> {System.err.println("TEST parser 2 READ_AVAILABLE_SLOTS")}
            DataManagerCommand.WRITE_SLOT.number -> {System.err.println("TEST parser 2 WRITE_SLOT")}
            DataManagerCommand.READ_DATA.number -> {System.err.println("TEST parser 2 READ_DATA")}
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
            mMain.bleCommand(
                BLECommands.requestAdditionalParametrInfo(
                    baseParametrInfoStructArray[0].ID.toByte()
                ), MAIN_CHANNEL, WRITE
            )
        }
//                                System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS ${baseParametrInfoStructArray.toString()}" )
        listWidgets.clear()
    }
    private fun parseReadDeviceAdditionalParameters(ID: Int, receiveDataString: String) {
        // читает каждый параметр отдельно по его ID
        // за один заход обрабатывает все ADDITIONAL_PARAMETR определённого параметра
        System.err.println("TEST parser 2 принятая посылка READ_DEVICE_ADDITIONAL_PARAMETRS $receiveDataString additionalInfoSize=${baseParametrInfoStructArray[ID].additionalInfoSize}")
        val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
        var dataOffset = 0
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
                        parseWidgets(receiveDataStringForParse, parameterID = ID)
                        GlobalScope.launch {
                            mMain.sendWidgetsArray()
                        }
                    }
                }
            }
        }

        //проход по остальным параметрам
        ID = getNextID(ID) //если у параметра additionalInfoSize = 0 то его пропустим

        if (ID != 0) {
            mMain.bleCommand(
                BLECommands.requestAdditionalParametrInfo(
                    baseParametrInfoStructArray[ID].ID.toByte()
                ), MAIN_CHANNEL, WRITE
            )
        }
    }

    private fun getNextID(ID: Int): Int{
        val result = 0
        for (i in baseParametrInfoStructArray.indices) {
            if (ID < baseParametrInfoStructArray[i].ID ) {
                if (baseParametrInfoStructArray[i].additionalInfoSize != 0) {
                    return baseParametrInfoStructArray[i].ID
                }
            }
        }
        return result
    }
    private fun parseWidgets(receiveDataStringForParse: String, parameterID: Int) {
        var baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${receiveDataStringForParse}\"")//+parameterID.toString()
        baseParameterWidgetStruct.parentParameterID = parameterID
        count += 1

        System.err.println("parseWidgets ID:${baseParameterWidgetStruct}")
        when (baseParameterWidgetStruct.widgetLabelType) {
            ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
                when (baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { System.err.println("parseWidgets UNKNOW") }
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        System.err.println("parseWidgets BUTTON CODE_LABEL")
                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parentParameterID = parameterID
                        listWidgets.add(commandParameterWidgetEStruct)
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> { System.err.println("parseWidgets SWITCH") }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { System.err.println("parseWidgets COMBOBOX") }
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> { System.err.println("parseWidgets SLIDER") }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        System.err.println("parseWidgets PLOT CODE_LABEL")
                        val plotParameterWidgetEStruct = Json.decodeFromString<PlotParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        plotParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parentParameterID = parameterID
                        listWidgets.add(plotParameterWidgetEStruct)
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { System.err.println("parseWidgets SPINBOX") }
                    ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets EMG_GESTURE_CHANGE_SETTINGS") }
                    ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets GESTURE_SETTINGS") }
                    ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { System.err.println("parseWidgets CALIB_STATUS") }
                    ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { System.err.println("parseWidgets CONTROL_MODE") }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        System.err.println("parseWidgets OPEN_CLOSE_THRESHOLD CODE_LABEL")
                        //TODO пока тестовая заглушка кнопкой
                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                        commandParameterWidgetEStruct.baseParameterWidgetEStruct.baseParameterWidgetStruct.parentParameterID = parameterID
                        listWidgets.add(commandParameterWidgetEStruct)
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_1_THRESHOLD") }
                    ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_2_THRESHOLD") }
                }
            }
            ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
                when (baseParameterWidgetStruct.widgetCode) {
                    ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { System.err.println("parseWidgets UNKNOW") }
                    ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                        System.err.println("parseWidgets BUTTON STRING_LABEL")
                        listWidgets.add(Json.decodeFromString<CommandParameterWidgetSStruct>("\"${receiveDataStringForParse}\""))
                    }
                    ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> { System.err.println("parseWidgets SWITCH") }
                    ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { System.err.println("parseWidgets COMBOBOX") }
                    ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> { System.err.println("parseWidgets SLIDER") }
                    ParameterWidgetCode.PWCE_PLOT.number.toInt() -> {
                        System.err.println("parseWidgets PLOT STRING_LABEL")
                        listWidgets.add(Json.decodeFromString<PlotParameterWidgetSStruct>("\"${receiveDataStringForParse}\""))
                    }
                    ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { System.err.println("parseWidgets SPINBOX") }
                    ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets EMG_GESTURE_CHANGE_SETTINGS") }
                    ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> { System.err.println("parseWidgets GESTURE_SETTINGS") }
                    ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { System.err.println("parseWidgets CALIB_STATUS") }
                    ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { System.err.println("parseWidgets CONTROL_MODE") }
                    ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> {
                        System.err.println("parseWidgets OPEN_CLOSE_THRESHOLD STRING_LABEL")
                        //TODO пока тестовая заглушка кнопкой
                        listWidgets.add(Json.decodeFromString<CommandParameterWidgetSStruct>("\"${receiveDataStringForParse}\""))
                    }
                    ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_1_THRESHOLD") }
                    ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { System.err.println("parseWidgets PLOT_AND_2_THRESHOLD") }
                }
            }
        }


//        listWidgets.forEach {
//            when (it) {
//                is CommandParameterWidgetEStruct -> {
//                    it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
//                }
//                is PlotParameterWidgetEStruct -> {
//                    it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
//                }
//            }
//        }
    }

    internal fun getStatusConnected() : Boolean { return mConnected }
}