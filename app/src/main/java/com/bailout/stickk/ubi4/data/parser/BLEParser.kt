package com.bailout.stickk.ubi4.data.parser

import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFICATION_DATA
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.additionalParameter.AdditionalInfoSizeStruct
import com.bailout.stickk.ubi4.data.widget.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ADDITIONAL_INFO_SEG
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.AdditionalParameterInfoType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BASE_PARAMETER_INFO_STRUCT_SIZE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabelType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabel
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.HEADER_BLE_OFFSET
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.READ_DEVICE_ADDITIONAL_PARAMETR_DATA
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.fullInicializeConnectionStruct
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BLEParser(main: AppCompatActivity) {
    private val mMain: MainActivityUBI4 = main as MainActivityUBI4
    private var mConnected = false


    internal fun parseReceivedData (data: ByteArray?) {
        if (data != null) {
            val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
//            System.err.println("BLE debug TEST displayFirstNotify data.size = ${data.size}  $receiveDataString")
            val dataTransmissionDirection = data[0]
            val codeRequest = data[1]
            val dataLength = castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4])*256
            System.err.println("TEST dataLength = $dataLength")
            val packageCodeRequest = data[7]
            var ID = 0
            if (dataLength > 8 ) { ID = castUnsignedCharToInt(data[8])}


            when (codeRequest){
                (0x00).toByte() -> { System.err.println("TEST parser DEFOULT") }
                BaseCommands.DEVICE_INFORMATION.number -> {
                    System.err.println("TEST parser DEVICE_INFORMATION (${packageCodeRequest})")
                    when (packageCodeRequest) {
                        (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
                        DeviceInformationCommand.INICIALIZE_INFORMATION.number -> {
                            if (dataLength > 2) {
                                fullInicializeConnectionStruct = Json.decodeFromString<FullInicializeConnectionStruct>("\"${receiveDataString.substring(18,receiveDataString.length)}\"")
                                System.err.println("TEST parser 2 INICIALIZE_INFORMATION $fullInicializeConnectionStruct" )
                                mMain.bleCommand(BLECommands.requestBaseParametrInfo(0x00, fullInicializeConnectionStruct.parametrsNum.toByte()), NOTIFICATION_DATA, WRITE)
                            }
                        }
                        DeviceInformationCommand.READ_DEVICE_PARAMETRS.number -> {
                            if (dataLength > 3) {
                                val listA: ArrayList<BaseParameterInfoStruct> = ArrayList()
                                for(i in 0 until fullInicializeConnectionStruct.parametrsNum) {
                                    listA.add(Json.decodeFromString<BaseParameterInfoStruct>("\"${receiveDataString.substring(20+i*BASE_PARAMETER_INFO_STRUCT_SIZE, 20+(i+1)*BASE_PARAMETER_INFO_STRUCT_SIZE)}\""))
                                    System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS $i ")
                                }
                                baseParametrInfoStructArray = listA
                                baseParametrInfoStructArray.forEach { println("READ_DEVICE_PARAMETRS $it") }

                                mMain.bleCommand(
                                    BLECommands.requestAdditionalParametrInfo(
                                        baseParametrInfoStructArray[0].ID.toByte()
                                    ), NOTIFICATION_DATA, WRITE
                                )
                                System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS ${baseParametrInfoStructArray.toString()}" )
                            }
                        }
                        DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETR.number -> {
                            // показ отправленной и принятой посылки
//                            System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR $receiveDataString")
                            if (dataLength > READ_DEVICE_ADDITIONAL_PARAMETR_DATA) {
                                //показ только принятой посылки
                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR $receiveDataString additionalInfoSize=${baseParametrInfoStructArray[ID].additionalInfoSize}")
                                val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
                                var dataOffset = 0

                                if (baseParametrInfoStructArray[ID].additionalInfoSize != 0) {
                                    for (i in 0 until baseParametrInfoStructArray[ID].additionalInfoSize) {
                                        //каждый новый цикл вычитываем следующий addInfoSeg
                                        val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(offset+i*16, offset+(i+1)*16)}\"")
                                        //каждый новый цикл вычитываем данные следующего сегмента
                                        val receiveDataStringForParse = receiveDataString.substring(
                                            offset + //отступ на header + отправленные данные (отправленный запрос целиком)
                                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG*2 + //отступ на n кол-во additionalInfoSeg в конкретном параметре
                                            dataOffset*2, // отступ на кол-во байт в предыдущих dataSeg (важно если у нас больше одного сегмента, для первого сегмента 0)
                                            offset +
                                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG*2 +
                                            dataOffset*2 +
                                            additionalInfoSizeStruct.infoSize*2) // оступ на кол-во байт в считываемом сегменте
//                                        System.err.println("TEST parser READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID     infoType: ${additionalInfoSizeStruct.infoType}")
//                                        System.err.println("testSignal 0 $receiveDataStringForParse")
                                        dataOffset = additionalInfoSizeStruct.infoSize


                                        val baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${receiveDataStringForParse}\"")
                                        when (additionalInfoSizeStruct.infoType) {
                                            AdditionalParameterInfoType.WIDGET.number.toInt() -> {
                                                when (baseParameterWidgetStruct.widgetLabelType) {
                                                    ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
                                                        val baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                                                        when (baseParameterWidgetEStruct.labelCode) {
                                                            ParameterWidgetLabel.PWLE_UNKNOW.number.toInt() -> {}
                                                            ParameterWidgetLabel.PWLE_OPEN.number.toInt() -> {}
                                                            ParameterWidgetLabel.PWLE_CLOSE.number.toInt() -> {}
                                                        }
//                                                        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID    widgetType: ${baseParameterWidgetStruct.widgetType}   widgetCode: ${baseParameterWidgetStruct.widgetCode} $receiveDataString $receiveDataStringForParse")
                                                        when (baseParameterWidgetStruct.widgetType) {
                                                            ParameterWidgetType.PWTE_COMMAND.number.toInt() -> {
                                                                val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
//                                                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${commandParameterWidgetEStruct}")
                                                            }
                                                        }
                                                        listWidgets.add(receiveDataString)
                                                        GlobalScope.launch {
                                                            mMain.sendWidgetsArray()
                                                        }
                                                    }
                                                    ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
                                                        val baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
//                                                        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${baseParameterWidgetSStruct}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                //проход по остальным параметрам
                                ID = getNextID(ID) //если additionalInfoSize = 0 то мы пропустим несколько ID

//                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR baseParametrInfoStructArray.size-1=${baseParametrInfoStructArray.size-1} > ID=$ID")
                                if (ID != 0) {
                                    mMain.bleCommand(
                                        BLECommands.requestAdditionalParametrInfo(
                                            baseParametrInfoStructArray[ID].ID.toByte()
                                        ), NOTIFICATION_DATA, WRITE
                                    )
                                }
                            }
                        }
                        DeviceInformationCommand.GET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 GET_SERIAL_NUMBER")}
                        DeviceInformationCommand.SET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 SET_SERIAL_NUMBER")}
                        DeviceInformationCommand.GET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 GET_DEVICE_NAME")}
                        DeviceInformationCommand.SET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 SET_DEVICE_NAME")}
                        DeviceInformationCommand.GET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 GET_DEVICE_ROLE")}
                        DeviceInformationCommand.SET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 SET_DEVICE_ROLE")}
                    }
                }
                BaseCommands.DATA_MANAGER.number -> {
                    System.err.println("TEST parser DATA_MANAGER")
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
                BaseCommands.WRITE_FW_COMMAND.number -> {System.err.println("TEST parser WRITE_FW_COMMAND")}
                BaseCommands.DEVICE_ACCESS_COMMAND.number -> {System.err.println("TEST parser DEVICE_ACCESS_COMMAND")}
                BaseCommands.ECHO_COMMAND.number -> {System.err.println("TEST parser ECHO_COMMAND")}
                BaseCommands.SUB_DEVICE_MANAGER.number -> {System.err.println("TEST parser SUB_DEVICE_MANAGER")}
                BaseCommands.GET_DEVICE_STATUS.number -> {System.err.println("TEST parser GET_DEVICE_STATUS")}
                BaseCommands.DATA_TRANSFER_SETTINGS.number -> {System.err.println("TEST parser DATA_TRANSFER_SETTINGS")}
            }


//            System.err.println("TEST Serializer 1: ${Json.decodeFromString<FullInicializeConnectionStruct>("\"$testString\"").toString()}")
//            System.err.println("TEST Serializer 2: ${Json.encodeToString(myColor)}")
        }
    }
    private fun getNextID(ID: Int): Int{
        val result = 0
        for (i in baseParametrInfoStructArray.indices) {
//            System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR ID=$ID")
            if (ID < baseParametrInfoStructArray[i].ID ) {
//                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR ID=$ID")
                if (baseParametrInfoStructArray[i].additionalInfoSize != 0) {
                    return baseParametrInfoStructArray[i].ID
                }
            }
        }
//        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR ID=$ID")
        return result
    }


    internal fun getStatusConnected() : Boolean { return mConnected }
}