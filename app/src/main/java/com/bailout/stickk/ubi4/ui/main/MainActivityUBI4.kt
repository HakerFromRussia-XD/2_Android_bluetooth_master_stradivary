package com.bailout.stickk.ubi4.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.RECONNECT_BLE_PERIOD
import com.bailout.stickk.new_electronic_by_Rodeon.services.receivers.BlockingQueue
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFICATION_DATA
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.lookup
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.data.additionalParameter.AdditionalInfoSizeStruct
import com.bailout.stickk.ubi4.data.widget.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.data.BaseParametrInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.data.widget.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ADDITIONAL_INFO_SEG
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DataManagerCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.DeviceInformationCommand
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabelType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetType
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabel
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.HEADER_BLE_OFFSET
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.READ_DEVICE_ADDITIONAL_PARAMETR_DATA
import com.bailout.stickk.ubi4.ui.fragments.HomeFragment
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.REQUEST_ENABLE_BT
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates

class MainActivityUBI4 : AppCompatActivity(), NavigatorUBI4 {
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    // Очередь для задач работы с BLE
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private var mGattServicesList: ExpandableListView? = null
    private var mCharacteristic: BluetoothGattCharacteristic? = null
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var globalSemaphore = true // флаг, который преостанавливает отправку новой
    private val queue = BlockingQueue()
    private var dataSortSemaphore = "" // строчка, показывающая с каким регистром мы сейчас работаем, чтобы однозначно понять кому пердназначаются принятые данные

    private var readRegisterPointer: ByteArray? = null
    var reconnectThreadFlag = false
    private var scanWithoutConnectFlag = false
    private var firstNotificationRequestFlag = true
    private var mConnected = false
    private var endFlag = false
    var percentSynchronize = 0
    private var mScanning = false
    private val listName = "NAME"
    private val listUUID = "UUID"
    private var actionState = WRITE
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService?.initialize()!!) {
                finish()
            }
            if (!scanWithoutConnectFlag) {
                //TODO раскомментировать когда не нужно быстрое подключение
                mBluetoothLeService?.connect(connectedDeviceAddress)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }
    private lateinit var incrementTestSignalJob: Job

    private var testSignalInc = 0
    val chatFlow = MutableStateFlow<Int>(0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        mGattServicesList = findViewById(R.id.gatt_services_list)
        val view = binding.root
        setContentView(view)
        initAllVariables()

        // инициализация блютуз
        initBLEStructure()
        scanLeDevice(true)

        val worker = Thread {
            while (true) {
                val task: Runnable = queue.get()
                task.run()
            }
        }
        worker.start()


//        GlobalScope.launch() {
            testSignal = MutableStateFlow<Int>(0)
//        }

        System.err.println("MainActivityUBI4 do")
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer, HomeFragment())
            .commit()
        System.err.println("MainActivityUBI4 posle")

//        GlobalScope.launch() {
//            incrementTestSignal()
//        }


//        val myColor = Color(1, 255, 254)
//        val testString: String = "00ff00"

//        System.err.println("TEST Serializer 1: ${Json.decodeFromString<Color>("\"$testString\"").toString()}")
//        System.err.println("TEST Serializer 2: ${Json.encodeToString(myColor)}")
    }


    override fun onResume() {
        super.onResume()
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBluetoothLeService != null) {
            connectedDeviceName = getString(CONNECTED_DEVICE)
            connectedDeviceAddress =  getString(CONNECTED_DEVICE_ADDRESS)
        }
        if (!mConnected) {
            reconnectThreadFlag = true
            reconnectThread()
        }
    }
    private suspend fun incrementTestSignal()  {
        System.err.println("incrementTestSignal $testSignalInc")
        sendMessage(testSignalInc + (0..10).random())
        testSignalInc += 1

        if (testSignalInc == 245) { testSignalInc = 0 }
        delay(25)
        incrementTestSignal()
    }
    suspend fun sendMessage(message: Int) {
        testSignal.value = message
    }

    override fun getBackStackEntryCount(): Int { return supportFragmentManager.backStackEntryCount }
    override fun goingBack() { onBackPressed() }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        System.err.println("backStackEntryCount: ${supportFragmentManager.backStackEntryCount}")
        //эта хитрая конструкция отключает системную кнопку "назад", когда мы НЕ в меню помощи
        if (supportFragmentManager.backStackEntryCount != 0) {
            super.onBackPressed()
        }
        if (supportFragmentManager.backStackEntryCount == 0) {
        }
    }
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
    private fun getString(key: String) :String {
        return mSettings!!.getString(key, "NOT SET!").toString()
    }

    // работа с блютузом
    override fun initBLEStructure() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ошибка 1", Toast.LENGTH_SHORT).show()
            finish()
        }
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "ошибка 2", Toast.LENGTH_SHORT).show()
            finish()
        } else {
//            Toast.makeText(this, "mBluetoothAdapter != null", Toast.LENGTH_SHORT).show()
        }
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("ResourceAsColor")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                BluetoothLeService.ACTION_GATT_CONNECTED == action -> {
                    Toast.makeText(context, "подключение установлено к $connectedDeviceAddress", Toast.LENGTH_SHORT).show()
                    reconnectThreadFlag = false
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
                    mConnected = false
                    endFlag = true
                    //TODO тут отображать статус дисконнекта
                    invalidateOptionsMenu()
                    mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
                    percentSynchronize = 0

                    if(!reconnectThreadFlag && !mScanning && !inScanFragmentFlag){
                        reconnectThreadFlag = true
                        reconnectThread()
                    }
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
                    mConnected = true
                    //TODO тут отображать статус подключения

                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService!!.supportedGattServices)

                        GlobalScope.launch {
                            firstNotificationRequest()
                        }
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
//                    System.err.println("BLE debug ACTION_DATA_AVAILABLE")
                    if(intent.getByteArrayExtra(BluetoothLeService.NOTIFICATION_DATA) != null) {
                        val fakeData = byteArrayOf(0x00,0x01,0x00,0x02,0x01,0x00,0x00,0x01,0x02)
                        val fakeData2 = byteArrayOf(0x00, 0x01, 0x00, 0x4c, 0x00, 0x74, 0x00, 0x01, 0x02, 0x43, 0x50, 0x55, 0x20, 0x4d, 0x6f, 0x64, 0x75, 0x6c, 0x65, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                            0x83.toByte(),
                            0xf1.toByte(), 0x74, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x02,
                            0xff.toByte(), 0x43, 0x50, 0x55, 0x20, 0x4d, 0x4f, 0x44, 0x55, 0x4c, 0x45, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                            0x87.toByte(), 0xd6.toByte(), 0x12, 0x00, 0x01)
                        val fakeData3 = byteArrayOf(0x00, 0x01, 0x00, 0x12, 0x00, 0x16, 0x00, 0x02, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                            0xff.toByte(), 0x0b, 0x0c,
                            0xff.toByte(), 0x01, 0x02, 0x03)
                        displayReceivedData(intent.getByteArrayExtra(BluetoothLeService.NOTIFICATION_DATA))//fakeData2)//intent.getByteArrayExtra(BluetoothLeService.NOTIFICATION_DATA))
                    }
                }
            }
        }
    }
    private suspend fun firstNotificationRequest()  {
        System.err.println("BLE debug firstNotificationRequest")
        System.err.println("BLE debug DEVICE_INFORMATION = ${BaseCommands.DEVICE_INFORMATION.number}")

        bleCommand(BLECommands.requestInicializeInformation(), NOTIFICATION_DATA, WRITE)
        bleCommand(null, NOTIFICATION_DATA, NOTIFY)
        delay(1000)

        if (firstNotificationRequestFlag) {
            firstNotificationRequest()
        }
    }
    fun setActionState(value: String) { actionState = value }
    private fun displayReceivedData (data: ByteArray?) {
        if (data != null) {
            firstNotificationRequestFlag = false
            val receiveDataString2 = data.toString(Charsets.UTF_8)
            val receiveDataString = EncodeByteToHex.bytesToHexString(data)
            val dataTransmissionDirection = data[0]
            val codeRequest = data[1]
            val dataLenght = castUnsignedCharToInt(data[3]) + castUnsignedCharToInt(data[4])*256
            val packageCodeRequest = data[7]
            val typeRequest = data[8]
            System.err.println("TEST dataLenght = $dataLenght")

            when (codeRequest){
                (0x00).toByte() -> { System.err.println("TEST parser DEFOULT") }
                BaseCommands.DEVICE_INFORMATION.number -> {
                    System.err.println("TEST parser DEVICE_INFORMATION (${packageCodeRequest})")
                    when (packageCodeRequest) {
                        (0x00).toByte() -> { System.err.println("TEST parser 2 DEFOULT") }
                        DeviceInformationCommand.INICIALIZE_INFORMATION.number -> {
                            if (dataLenght > 2) {
                                fullInicializeConnectionStruct = Json.decodeFromString<FullInicializeConnectionStruct>("\"${receiveDataString.substring(18,receiveDataString.length)}\"")
                                System.err.println("TEST parser 2 INICIALIZE_INFORMATION ${fullInicializeConnectionStruct.toString()}" )
                                bleCommand(BLECommands.requestBaseParametrInfo(0x00, fullInicializeConnectionStruct.parametrsNum.toByte()), NOTIFICATION_DATA, WRITE)
                            }
                        }
                        DeviceInformationCommand.READ_DEVICE_PARAMETRS.number -> {
                            if (dataLenght > 3) {
                                val listA: ArrayList<BaseParametrInfoStruct> = ArrayList()
                                for(i in 0 until fullInicializeConnectionStruct.parametrsNum) {
                                    System.err.println("TEST parser 2 READ_DEVICE_PARAMETRS $i")
                                    listA.add(Json.decodeFromString<BaseParametrInfoStruct>("\"${receiveDataString.substring(20+i*30, 20+(i+1)*30)}\""))
                                }
                                baseParametrInfoStructArray = listA

                                bleCommand(
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
                            baseParametrInfoStructArray.size
                            if (dataLenght > READ_DEVICE_ADDITIONAL_PARAMETR_DATA) {
                                //показ только принятой посылки
//                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR $receiveDataString")
                                val offset = HEADER_BLE_OFFSET * 2 + READ_DEVICE_ADDITIONAL_PARAMETR_DATA * 2
                                var dataOffset = 0
                                var ID = castUnsignedCharToInt(data[8])


                                if (baseParametrInfoStructArray[ID].additionalInfoSize != 0) {
                                    for (i in 0 until baseParametrInfoStructArray[ID].additionalInfoSize) {
                                        val additionalInfoSizeStruct = Json.decodeFromString<AdditionalInfoSizeStruct>("\"${receiveDataString.substring(offset+i*16, offset+(i+1)*16)}\"")
                                        val receiveDataStringForParse = receiveDataString.substring(
                                            offset + //отступ на header + отправленные данные (отправленный запрос целиком)
                                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG*2 + //отступ на n кол-во additionalInfoSeg в конкретном параметре
                                            dataOffset*2, // отступ на кол-во байт в предыдущих dataSeg (важно если у нас больше одного сегмента, для первого сегмента 0)
                                            offset +
                                            baseParametrInfoStructArray[ID].additionalInfoSize*ADDITIONAL_INFO_SEG*2 +
                                            dataOffset*2 +
                                            additionalInfoSizeStruct.infoSize*2) // оступ на кол-во байт в считываемом сегменте
//                                        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${additionalInfoSizeStruct}")
                                        dataOffset = additionalInfoSizeStruct.infoSize


                                        val baseParameterWidgetStruct = Json.decodeFromString<BaseParameterWidgetStruct>("\"${receiveDataStringForParse}\"")
                                        when (baseParameterWidgetStruct.widgetLabelType) {
                                            ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt() -> {
                                                val baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                                                when (baseParameterWidgetEStruct.labelCode) {
                                                    ParameterWidgetLabel.PWLE_UNKNOW.number.toInt() -> {}
                                                    ParameterWidgetLabel.PWLE_OPEN.number.toInt() -> {}
                                                    ParameterWidgetLabel.PWLE_CLOSE.number.toInt() -> {}
                                                }
                                                when (baseParameterWidgetStruct.widgetType) {
                                                    ParameterWidgetType.PWTE_COMMAND.number.toInt() -> {
                                                        val commandParameterWidgetEStruct = Json.decodeFromString<CommandParameterWidgetEStruct>("\"${receiveDataStringForParse}\"")
                                                        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${commandParameterWidgetEStruct}")
                                                    }
                                                }
                                            }
                                            ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt() -> {
                                                val baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${receiveDataStringForParse}\"")
                                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR  ID=$ID  ${baseParameterWidgetSStruct}")
                                            }
                                        }
                                    }
                                }

                                //проход по остальным параметрам
                                ID = getNextID(ID) //если additionalInfoSize = 0 то мы пропустим несколько ID

//                                System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR baseParametrInfoStructArray.size-1=${baseParametrInfoStructArray.size-1} > ID=$ID")
                                if (ID != 0) {
                                    bleCommand(
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


            System.err.println("BLE debug TEST displayFirstNotify data.size = ${data.size}  $receiveDataString")
        }
    }

    private fun getNextID(ID: Int): Int{
        var result = 0
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
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
//        System.err.println("DeviceControlActivity------->   момент начала выстраивания списка параметров")
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString = ("unknown_service")
        val unknownCharaString =("unknown_characteristic")
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        mGattCharacteristics = java.util.ArrayList()


        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[listName] = lookup(uuid, unknownServiceString)
            currentServiceData[listUUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[listName] = lookup(uuid, unknownCharaString)
                currentCharaData[listUUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
                System.err.println("------->   ХАРАКТЕРИСТИКА: $uuid")
            }
            mGattCharacteristics.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(listName, listUUID), intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(listName, listUUID), intArrayOf(android.R.id.text1, android.R.id.text2))
        mGattServicesList!!.setAdapter(gattServiceAdapter)
        if (mScanning) { scanLeDevice(false) }
    }
    private fun reconnectThread() {
//        System.err.println("--> reconnectThread started")
        var j = 1
        val reconnectThread = Thread {
            while (reconnectThreadFlag) {
                runOnUiThread {
                    if(j % 5 == 0) {
                        reconnectThreadFlag = false
//                        scanLeDevice(true)
//                        System.err.println("DeviceControlActivity------->   Переподключение со сканированием №$j")
                    } else {
                        reconnect()
//                        System.err.println("DeviceControlActivity------->   Переподключение без сканирования №$j")
                    }
                    j++
                }
                try {
                    Thread.sleep(RECONNECT_BLE_PERIOD.toLong())
                } catch (ignored: Exception) { }
            }
        }
        reconnectThread.start()
    }
    override fun reconnect () {
        //полное завершение сеанса связи и создание нового в onResume
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection)
            mBluetoothLeService = null
        }

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

        //BLE
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            mBluetoothLeService!!.connect(getString(CONNECTED_DEVICE_ADDRESS))
        } else {
//            println("--> вызываем функцию коннекта к устройству $connectedDeviceName = null")
        }
    }
    override fun disconnect () {
        if (mBluetoothLeService != null) {
            println("--> дисконнектим всё к хуям и анбайндим")
            mBluetoothLeService!!.disconnect()
            unbindService(mServiceConnection)
            mBluetoothLeService = null
        }
        mConnected = false
        endFlag = true
        runOnUiThread {
            //TODO тут отображать статус дисконнекта
            mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        }
        invalidateOptionsMenu()
        percentSynchronize = 0

        if(!reconnectThreadFlag && !mScanning && !inScanFragmentFlag){
            reconnectThreadFlag = true
            reconnectThread()
        }
        scanWithoutConnectFlag = true
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }
    override fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
    }
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        runOnUiThread {
            if (device.name != null) {
                System.err.println("------->   ===============найден девайс: ${device.address} - ${device.name}  ищем $connectedDeviceAddress ==============")
                if (device.address == connectedDeviceAddress) {
                    System.err.println("------->   ==========это нужный нам девайс $device  $scanWithoutConnectFlag ==============")
                    if (!scanWithoutConnectFlag) {
                        scanLeDevice(false)
                        reconnectThreadFlag = true
                        reconnectThread()
                    }
                }
            }
        }
    }
    override fun bleCommand(byteArray: ByteArray?, command: String, typeCommand: String){
        for (i in mGattCharacteristics.indices) {
            for (j in mGattCharacteristics[i].indices) {
                if (mGattCharacteristics[i][j].uuid.toString() == command) {
                    mCharacteristic = mGattCharacteristics[i][j]
                    if (typeCommand == WRITE){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                            System.err.println("BLE debug попытка записи")
                            mCharacteristic?.value = byteArray
                            mBluetoothLeService?.writeCharacteristic(mCharacteristic)
                        }
                    }

                    if (typeCommand == READ){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                            mBluetoothLeService?.readCharacteristic(mCharacteristic)
                        }
                    }

                    if (typeCommand == NOTIFY){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                            System.err.println("BLE debug попытка подписки на нотификацию")
                            mNotifyCharacteristic = mCharacteristic
                            mBluetoothLeService!!.setCharacteristicNotification(
                                mCharacteristic, true)
                        }
                    }

                }
            }
        }
    }

    companion object {
        var  testSignal by Delegates.notNull<MutableStateFlow<Int>>()

        var fullInicializeConnectionStruct by Delegates.notNull<FullInicializeConnectionStruct>()
        var baseParametrInfoStructArray by Delegates.notNull<ArrayList<BaseParametrInfoStruct>>()

        var connectedDeviceName by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()

        var inScanFragmentFlag by Delegates.notNull<Boolean>()
    }
}