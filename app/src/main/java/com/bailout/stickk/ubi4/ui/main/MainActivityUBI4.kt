package com.bailout.stickk.ubi4.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.contract.TransmitterUBI4
import com.bailout.stickk.ubi4.data.BaseParametrInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.ui.fragments.HomeFragment
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.REQUEST_ENABLE_BT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates


class MainActivityUBI4 : AppCompatActivity(), NavigatorUBI4, TransmitterUBI4 {
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private lateinit var mBLEController: BLEController

    private var testSignalInc = 0
    val chatFlow = MutableStateFlow<Int>(0)


    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        val view = binding.root
        setContentView(view)
        initAllVariables()

        // инициализация блютуз
        mBLEController = BLEController(this)
        mBLEController.initBLEStructure()
        mBLEController.scanLeDevice(true)


        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer, HomeFragment())
            .commit()


        littleFun()
    }
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (!mBLEController.getBluetoothAdapter()?.isEnabled!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBLEController.getBluetoothLeService() != null) {
            connectedDeviceName = getString(CONNECTED_DEVICE)
            connectedDeviceAddress =  getString(CONNECTED_DEVICE_ADDRESS)
        }
        if (!mBLEController.getStatusConnected()) {
            mBLEController.setReconnectThreadFlag(true)
            mBLEController.reconnectThread()
        }
    }
    private fun littleFun() {
        listWidgets = arrayListOf()
        updateFlow = MutableStateFlow(0)
        binding.buttonFlow.setOnClickListener {
            sendWidgetsArray()
        }
    }
    private fun sendWidgetsArray() {
        //событие эммитится только в случае если size отличается от предыдущего
        updateFlow.value = listWidgets.size
    }


    override fun getBackStackEntryCount(): Int { return supportFragmentManager.backStackEntryCount }
    override fun goingBack() { onBackPressed() }
    override fun goToMenu() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun initAllVariables() {
        connectedDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME).orEmpty()
        connectedDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS).orEmpty()

        //settings
    }

    // сохранение и загрузка данных
    override fun saveString(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    fun getString(key: String) :String {
        return mSettings!!.getString(key, "NOT SET!").toString()
    }

    // работа с блютузом

//    private fun displayReceivedData (data: ByteArray?) {
//        if (data != null) {
//            firstNotificationRequestFlag = false
//            val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
//            val dataTransmissionDirection = data[0]
//            val codeRequest = data[1]
//            val dataLength = castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4])*256
//            val packageCodeRequest = data[7]
//            var ID = castUnsignedCharToInt(data[8])
//            System.err.println("TEST dataLength = $dataLength")
//
//            when (codeRequest){
//                (0x00).toByte() -> { System.err.println("TEST parser DEFOULT") }
//                BaseCommands.DEVICE_INFORMATION.number -> {
//                    System.err.println("TEST parser DEVICE_INFORMATION (${packageCodeRequest})")
//                    when (packageCodeRequest) {
//                        (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
//                        DeviceInformationCommand.INICIALIZE_INFORMATION.number -> {
//                            if (dataLength > 2) {
//                                fullInicializeConnectionStruct = Json.decodeFromString<FullInicializeConnectionStruct>("\"${receiveDataString.substring(18,receiveDataString.length)}\"")
//                                System.err.println("TEST parser 2 INICIALIZE_INFORMATION ${fullInicializeConnectionStruct.toString()}" )
//                                bleCommand(BLECommands.requestBaseParametrInfo(0x00, fullInicializeConnectionStruct.parametrsNum.toByte()), NOTIFICATION_DATA, WRITE)
//                            }
//                        }
//                        DeviceInformationCommand.READ_DEVICE_PARAMETRS.number -> {
//                            if (dataLength > 3) {
//                                val listA: ArrayList<BaseParametrInfoStruct> = ArrayList()
//                                for(i in 0 until fullInicializeConnectionStruct.parametrsNum) {
//                                    System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS $i")
//                                    listA.add(Json.decodeFromString<BaseParametrInfoStruct>("\"${receiveDataString.substring(20+i*30, 20+(i+1)*30)}\""))
//                                }
//                                baseParametrInfoStructArray = listA
//
//                                bleCommand(
//                                    BLECommands.requestAdditionalParametrInfo(
//                                        baseParametrInfoStructArray[0].ID.toByte()
//                                    ), NOTIFICATION_DATA, WRITE
//                                )
//                                System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS ${baseParametrInfoStructArray.toString()}" )
//                            }
//                        }
//                        DeviceInformationCommand.READ_DEVICE_ADDITIONAL_PARAMETR.number -> {
//                            // показ отправленной и принятой посылки
////                            System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR $receiveDataString")
//                            if (dataLength > READ_DEVICE_ADDITIONAL_PARAMETR_DATA) {
//                                //показ только принятой посылки
////                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR $receiveDataString")
//                                val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
//                                var dataOffset = 0
//
//                                if (baseParametrInfoStructArray[ID].additionalInfoSize != 0) {
//                                    for (i in 0 until baseParametrInfoStructArray[ID].additionalInfoSize) {
//                                        //каждый новый цикл вычитываем следующий addInfoSeg
//                                        val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(offset+i*16, offset+(i+1)*16)}\"")
//                                        //каждый новый цикл вычитываем данные следующего сегмента
//                                        val receiveDataStringForParse = receiveDataString.substring(
//                                            offset + //отступ на header + отправленные данные (отправленный запрос целиком)
//                                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG*2 + //отступ на n кол-во additionalInfoSeg в конкретном параметре
//                                            dataOffset*2, // отступ на кол-во байт в предыдущих dataSeg (важно если у нас больше одного сегмента, для первого сегмента 0)
//                                            offset +
//                                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG*2 +
//                                            dataOffset*2 +
//                                            additionalInfoSizeStruct.infoSize*2) // оступ на кол-во байт в считываемом сегменте
////                                        System.err.println("TEST parser READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID     infoType: ${additionalInfoSizeStruct.infoType}")
//                                        dataOffset = additionalInfoSizeStruct.infoSize
//
//
//                                        val baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${receiveDataStringForParse}\"")
//                                        when (additionalInfoSizeStruct.infoType) {
//                                            AdditionalParameterInfoType.WIDGET.number.toInt() -> {
//                                                when (baseParameterWidgetStruct.widgetLabelType) {
//                                                    ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
//                                                        val baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
//                                                        when (baseParameterWidgetEStruct.labelCode) {
//                                                            ParameterWidgetLabel.PWLE_UNKNOW.number.toInt() -> {}
//                                                            ParameterWidgetLabel.PWLE_OPEN.number.toInt() -> {}
//                                                            ParameterWidgetLabel.PWLE_CLOSE.number.toInt() -> {}
//                                                        }
//                                                        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID    widgetType: ${baseParameterWidgetStruct.widgetType}   widgetCode: ${baseParameterWidgetStruct.widgetCode} $receiveDataString $receiveDataStringForParse")
//                                                        when (baseParameterWidgetStruct.widgetType) {
//                                                            ParameterWidgetType.PWTE_COMMAND.number.toInt() -> {
//                                                                val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
//                                                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${commandParameterWidgetEStruct}")
//                                                            }
//                                                        }
//                                                        listWidgets.add(receiveDataString)
//                                                        GlobalScope.launch {
//                                                            sendWidgetsArray()
//                                                        }
//                                                    }
//                                                    ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
//                                                        val baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
//                                                        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${baseParameterWidgetSStruct}")
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//
//                                //проход по остальным параметрам
//                                ID = getNextID(ID) //если additionalInfoSize = 0 то мы пропустим несколько ID
//
//                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR baseParametrInfoStructArray.size-1=${baseParametrInfoStructArray.size-1} > ID=$ID")
//                                if (ID != 0) {
//                                    bleCommand(
//                                        BLECommands.requestAdditionalParametrInfo(
//                                            baseParametrInfoStructArray[ID].ID.toByte()
//                                        ), NOTIFICATION_DATA, WRITE
//                                    )
//                                }
//                            }
//                        }
//                        DeviceInformationCommand.GET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 GET_SERIAL_NUMBER")}
//                        DeviceInformationCommand.SET_SERIAL_NUMBER.number -> {System.err.println("TEST parser 2 SET_SERIAL_NUMBER")}
//                        DeviceInformationCommand.GET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 GET_DEVICE_NAME")}
//                        DeviceInformationCommand.SET_DEVICE_NAME.number -> {System.err.println("TEST parser 2 SET_DEVICE_NAME")}
//                        DeviceInformationCommand.GET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 GET_DEVICE_ROLE")}
//                        DeviceInformationCommand.SET_DEVICE_ROLE.number -> {System.err.println("TEST parser 2 SET_DEVICE_ROLE")}
//                    }
//                }
//                BaseCommands.DATA_MANAGER.number -> {
//                    System.err.println("TEST parser DATA_MANAGER")
//                    when (packageCodeRequest) {
//                        (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
//                        DataManagerCommand.READ_AVAILABLE_SLOTS.number -> {System.err.println("TEST parser 2 READ_AVAILABLE_SLOTS")}
//                        DataManagerCommand.WRITE_SLOT.number -> {System.err.println("TEST parser 2 WRITE_SLOT")}
//                        DataManagerCommand.READ_DATA.number -> {System.err.println("TEST parser 2 READ_DATA")}
//                        DataManagerCommand.WRITE_DATA.number -> {System.err.println("TEST parser 2 WRITE_DATA")}
//                        DataManagerCommand.RESET_TO_FACTORY.number -> {System.err.println("TEST parser 2 RESET_TO_FACTORY")}
//                        DataManagerCommand.SAVE_DATA.number -> {System.err.println("TEST parser 2 SAVE_DATA")}
//                    }
//                }
//                BaseCommands.WRITE_FW_COMMAND.number -> {System.err.println("TEST parser WRITE_FW_COMMAND")}
//                BaseCommands.DEVICE_ACCESS_COMMAND.number -> {System.err.println("TEST parser DEVICE_ACCESS_COMMAND")}
//                BaseCommands.ECHO_COMMAND.number -> {System.err.println("TEST parser ECHO_COMMAND")}
//                BaseCommands.SUB_DEVICE_MANAGER.number -> {System.err.println("TEST parser SUB_DEVICE_MANAGER")}
//                BaseCommands.GET_DEVICE_STATUS.number -> {System.err.println("TEST parser GET_DEVICE_STATUS")}
//                BaseCommands.DATA_TRANSFER_SETTINGS.number -> {System.err.println("TEST parser DATA_TRANSFER_SETTINGS")}
//            }
//
//
////            System.err.println("TEST Serializer 1: ${Json.decodeFromString<FullInicializeConnectionStruct>("\"$testString\"").toString()}")
////            System.err.println("TEST Serializer 2: ${Json.encodeToString(myColor)}")
//
//
//            System.err.println("BLE debug TEST displayFirstNotify data.size = ${data.size}  $receiveDataString")
//        }
//    }
    override fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String) {
        mBLEController.bleCommand( byteArray, uuid, typeCommand )
    }

    companion object {
        var updateFlow by Delegates.notNull<MutableStateFlow<Int>>()
        var listWidgets by Delegates.notNull<ArrayList<String>>()

        var fullInicializeConnectionStruct by Delegates.notNull<FullInicializeConnectionStruct>()
        var baseParametrInfoStructArray by Delegates.notNull<ArrayList<BaseParametrInfoStruct>>()


        var connectedDeviceName by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()

        var inScanFragmentFlag by Delegates.notNull<Boolean>()
    }
}