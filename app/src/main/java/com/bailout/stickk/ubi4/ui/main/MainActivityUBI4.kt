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
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFICATION_TEST_MTU
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.lookup
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.ui.fragments.HomeFragment
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.REQUEST_ENABLE_BT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    var reconnectThreadFlag: Boolean = false
    private var mConnected = false
    private var endFlag = false
    var percentSynchronize = 0
    private var mScanning = false
    private val listName = "NAME"
    private val listUUID = "UUID"
    private var actionState = WRITE
    private var flagScanWithoutConnect = false
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService?.initialize()!!) {
                finish()
            }
            if (!flagScanWithoutConnect) {
                //TODO раскомментировать когда не нужно быстрое подключение
//                mBluetoothLeService?.connect(connectedDeviceAddress)
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

        GlobalScope.launch() {
            incrementTestSignal()
        }
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
//                        bleCommand(null, NOTIFICATION_TEST_MTU, NOTIFY)

//                        bleCommand(byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x02) ,NOTIFICATION_TEST_MTU, WRITE)
//                        bleCommand(byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x02) ,NOTIFICATION_TEST_MTU, WRITE)
//                        bleCommand(byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x02) ,NOTIFICATION_TEST_MTU, WRITE)

                        System.err.println("BLE debug bleCommand")
                        bleCommand(byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x02) ,NOTIFICATION_TEST_MTU, WRITE)
                        bleCommand(null, NOTIFICATION_TEST_MTU, NOTIFY)
                        bleCommand(byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x02) ,NOTIFICATION_TEST_MTU, WRITE)
                        bleCommand(null, NOTIFICATION_TEST_MTU, NOTIFY)
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
                    System.err.println("BLE debug ACTION_DATA_AVAILABLE")
                    if(intent.getByteArrayExtra(BluetoothLeService.NOTIFICATION_TEST_MTU) != null) displayFirstNotify (intent.getByteArrayExtra(BluetoothLeService.NOTIFICATION_TEST_MTU))
                }
            }
        }
    }
    fun setActionState(value: String) { actionState = value }
    private fun displayFirstNotify (data: ByteArray?) {
        if (data != null) {
            val logString = String(data, charset("UTF-8"))

            System.err.println("BLE debug displayFirstNotify data.size = ${data.size}  $logString")
        }
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
        flagScanWithoutConnect = true
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
                    System.err.println("------->   ==========это нужный нам девайс $device  $flagScanWithoutConnect ==============")
                    if (!flagScanWithoutConnect) {
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
                            System.err.println("BLE debug попытка подписки записи")
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

        var connectedDeviceName by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()

        var inScanFragmentFlag by Delegates.notNull<Boolean>()
    }
}