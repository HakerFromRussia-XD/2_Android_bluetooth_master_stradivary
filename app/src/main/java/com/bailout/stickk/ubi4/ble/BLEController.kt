package com.bailout.stickk.ubi4.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.BIND_AUTO_CREATE
import androidx.appcompat.app.AppCompatActivity.BLUETOOTH_SERVICE
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.RECONNECT_BLE_PERIOD
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.lookup
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.state.BLEState.bleParser
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceAddress
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.BaseCommands
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll

class BLEController() {
    private val mContext: Context = main.applicationContext
    private var mBLEParser: BLEParser? = null


    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private var mCharacteristic: BluetoothGattCharacteristic? = null
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var progressDialog: Dialog? = null
    private var isUploading = false
    private var onDisconnectedListener: (() -> Unit)? = null
    private var reconnectThreadFlag = false
    private var scanWithoutConnectFlag = false
    private var mConnected = false
    private var endFlag = false
    private var mScanning = false
    private var firstNotificationRequestFlag = true

    private val bleJob = Job()
    private val bleScope = CoroutineScope(Dispatchers.Main + bleJob)
    private var mDisconnected = false

    private var receiverRegistered = false

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            mBluetoothLeService?.setReceiverCallback {state ->
                if(state == WRITE)
                synchronized(main.writeLock) {
                    canSendFlag = true
                    main.writeLock.notifyAll()
                }
            }
            if (!mBluetoothLeService?.initialize()!!) {
                main.finish()
            }
            if (!scanWithoutConnectFlag) {
                System.err.println("connectedDeviceAddress $connectedDeviceAddress")
//                mBluetoothLeService?.connect("DC:DA:0C:18:58:9E") // Лёшина плата
//                mBluetoothLeService?.connect("DC:DA:0C:18:0E:8E")       // Моя плата
//                mBluetoothLeService?.connect("DC:DA:0C:18:12:0A")       // Андрея плата
//                mBluetoothLeService?.connect("34:85:18:98:0F:D2")       // Mike плата
//                mBluetoothLeService?.connect("DC:DA:0C:18:1C:6A") // плата с оптикой Денис
//                mBluetoothLeService?.connect("F0:9E:9E:22:97:52")
//                mBluetoothLeService?.connect("F0:9E:9E:22:96:3E") // плата с оптикой с экраном
//                mBluetoothLeService?.connect("DC:DA:0C:18:58:9E")  // протез Макса
//                mBluetoothLeService?.connect("34:85:18:98:10:7E")
//                mBluetoothLeService?.connect("F0:9E:9E:22:97:36")
//                mBluetoothLeService?.connect("F0:9E:9E:22:97:52")
//                mBluetoothLeService?.connect("F0:9E:9E:22:96:3E") //fest FO3


                mBluetoothLeService?.connect(connectedDeviceAddress)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    internal fun initBLEStructure() {
        if (!main.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, "ошибка 1", Toast.LENGTH_SHORT).show()
            main.finish()
        }
        val bluetoothManager = main.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(mContext, "ошибка 2", Toast.LENGTH_SHORT).show()
            main.finish()
        } else {
//            Toast.makeText(mContext, "mBluetoothAdapter != null", Toast.LENGTH_SHORT).show()
        }
        val gattServiceIntent = Intent(mContext, BluetoothLeService::class.java)
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            LocalBroadcastManager.getInstance(mContext).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        }
        else {
            mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        }
        mBLEParser = bleParser
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("ResourceAsColor")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                BluetoothLeService.ACTION_GATT_CONNECTED == action -> {
                    System.err.println("Check BroadcastReceiver() ACTION_GATT_CONNECTED")
                    reconnectThreadFlag = false
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
                    if (mDisconnected) {
                        System.err.println("Устройство отключено намеренно, не переподключаемся")
                        return
                    }
                    mConnected = false
                    isUploading = false
                    endFlag = true
                    progressDialog?.dismiss()
                    progressDialog = null

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(mContext,
                            context.getString(R.string.bluetooth_connection_is_disabled), Toast.LENGTH_SHORT).show()
                    }

                    mBluetoothLeService?.disconnect()
                    mBluetoothLeService?.close()

                    if (!reconnectThreadFlag && !mScanning) {
                        reconnectThreadFlag = true
                        reconnectThread()
                    }
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
                    Log.d("BLE_CONN", "▶ ACTION_GATT_SERVICES_DISCOVERED, services count = ${mBluetoothLeService?.supportedGattServices?.size ?: 0}")
                    mConnected = true
                    Toast.makeText(context, "подключение установлено к $connectedDeviceAddress", Toast.LENGTH_SHORT).show()
                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService!!.supportedGattServices)

                        main.lifecycleScope.launch {
                            firstNotificationRequest()
                        }
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
                    if (intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL) != null) {
                        parseReceivedData(intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL))
                    }
                }
//                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
//                    val raw = intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL)
//                    if (raw != null) {
//                        val hex = EncodeByteToHex.bytesToHexString(raw)
//                        Log.d("BLE_RX", "▶ DATA_AVAILABLE size=${raw.size}, data=$hex")
//                    } else {
//                        Log.d("BLE_RX", "▶ DATA_AVAILABLE, но MAIN_CHANNEL extra = null")
//                    }
//                    parseReceivedData(raw)
//                }

                }
            }
        }




//    private suspend fun firstNotificationRequest()  {
//        System.err.println("BLE debug firstNotificationRequest")
//        System.err.println("BLE debug DEVICE_INFORMATION = ${BaseCommands.DEVICE_INFORMATION.number}")
//
//        bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
//        bleCommand(null, MAIN_CHANNEL, NOTIFY)
//        delay(1000)
//
//        if (firstNotificationRequestFlag) {
//            firstNotificationRequest()
//        }
//    }
// 1. BLEController.firstNotificationRequest
    private suspend fun firstNotificationRequest() {
        var attempts = 0
        while (firstNotificationRequestFlag && attempts < 5) {
            Log.d("BLE_INIT", "▶ firstNotificationRequest попытка #${attempts+1}")
            bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
            bleCommand(null, MAIN_CHANNEL, NOTIFY)
            delay(1000)
            attempts++
        }
        if (firstNotificationRequestFlag) {
            Log.e("BLE_INIT", "✖ не получили уведомление после $attempts попыток")
        } else {
            Log.d("BLE_INIT", "✔ уведомление получено, выходим из цикла")
        }
    }
//    private fun parseReceivedData (data: ByteArray?) {
//        if (data != null) {
//            firstNotificationRequestFlag = false
//            mBLEParser?.parseReceivedData(data)
//        }
//    }
private fun parseReceivedData(data: ByteArray?) {
    val hex = data?.let { EncodeByteToHex.bytesToHexString(it) } ?: "null"
    Log.d("BLE_GOGO", "▶ parseReceivedData(data=$hex)")
    val requestType = data?.let { ((it[0].toInt() and 0x40) shr 6) } ?: -1
    val codeRequest = data?.getOrNull(1)?.toInt() ?: -1
    Log.d("BLE_PARSER", "▶ parseReceivedData: type=$requestType code=$codeRequest size=${data?.size ?: 0} data=$hex")
    if (data != null) {
        firstNotificationRequestFlag = false
        mBLEParser?.parseReceivedData(data)
    }
}

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        System.err.println("DeviceControlActivity------->   момент начала выстраивания списка параметров")
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
            currentServiceData["NAME"] = lookup(uuid, unknownServiceString)
            currentServiceData["UUID"] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val characteristicsList = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                characteristicsList.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData["NAME"] = lookup(uuid, unknownCharaString)
                currentCharaData["UUID"] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
                System.err.println("------->   ХАРАКТЕРИСТИКА: $uuid")
            }
            mGattCharacteristics.add(characteristicsList)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        if (mScanning) { scanLeDevice(false) }
    }
    fun reconnectThread() {
        var j = 1
        bleScope.launch {
            while (reconnectThreadFlag) {
                if (j % 5 == 0) {
                    reconnectThreadFlag = false
                    scanLeDevice(true)
                    System.err.println("DeviceControlActivity-------> Переподключение со сканированием №$j")
                } else {
                    reconnect()
                    System.err.println("DeviceControlActivity-------> Переподключение без сканирования №$j")
                }
                j++
                delay(RECONNECT_BLE_PERIOD.toLong())
            }
        }
    }

    private suspend fun reconnect() {
        // Выполняем unbindService и bindService на IO-потоке, если они действительно могут быть «тяжёлыми»
        withContext(Dispatchers.IO) {
            try {
                mContext.unbindService(mServiceConnection)
            } catch (ex: Exception) {
                // Если не был привязан, можно игнорировать ошибку
                Log.w("BLEController", "Не удалось отцепить сервис: ${ex.message}")
            }
            mBluetoothLeService = null

            val gattServiceIntent = Intent(mContext, BluetoothLeService::class.java)
            mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        }

        // На главном потоке регистрируем ресивер (если требуется)
        withContext(Dispatchers.Main) {
            try {
                // Проверяем, что ресивер ещё не зарегистрирован (будет показан пример ниже)
                mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
            } catch (e: IllegalArgumentException) {
                // Если уже зарегистрирован, игнорируем
                Log.w("BLEController", "Ресивер уже зарегистрирован")
            }
            mBluetoothLeService?.connect(connectedDeviceAddress)
        }
    }
    fun disconnect() {
        if (mDisconnected) return
        reconnectThreadFlag = false
        mDisconnected = true
            println("--> дисконнектим всё к хуям и анбайндим")

            bleScope.launch(Dispatchers.IO) {
                mBluetoothLeService?.disconnect()
                runCatching { mContext.unbindService(mServiceConnection) }
                withContext(Dispatchers.Main){
                    mConnected = false
                    listWidgets.clear()
                    main.openScanActivity()
                }
            }

    }


    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }
    internal fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mScanning = true
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) { return }
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
    }
    @SuppressLint("MissingPermission")
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        main.runOnUiThread {
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
    internal fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String) {
        val hex = byteArray?.let { EncodeByteToHex.bytesToHexString(it) } ?: "null"
        Log.d("BLE_SEND", "▶ bleCommand type=$typeCommand, uuid=$uuid, data=$hex")
        Log.d("BLEController", "Отправка команды: тип = $typeCommand, UUID = $uuid, данные = ${byteArray?.let { EncodeByteToHex.bytesToHexString(it) }}")
        System.err.println("BLE debug")
        for (i in mGattCharacteristics.indices) {
            for (j in mGattCharacteristics[i].indices) {
                Log.d("bleCommand", "Характеристика $i-$j UUID: ${mGattCharacteristics[i][j].uuid}")
                if (mGattCharacteristics[i][j].uuid.toString() == uuid) {
                    mCharacteristic = mGattCharacteristics[i][j]
                    if (typeCommand == WRITE){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                            Log.d("bleCommand", "Отправка команды: ${byteArray?.let {
                                EncodeByteToHex.bytesToHexString(
                                    it
                                )
                            }} на UUID: $uuid")
                            System.err.println("BLE debug запись ${EncodeByteToHex.bytesToHexString(byteArray!!)}")
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
                            mBluetoothLeService?.setCharacteristicNotification(mCharacteristic, true)
                        }
                    }

                }
            }
        }
    }
    fun setOnDisconnectedListener(listener: () -> Unit) {
        // Сохраняйте listener и вызывайте его в `ACTION_GATT_DISCONNECTED`
        onDisconnectedListener = listener
    }

    fun cleanup() {
        // Отменяем запущенные корутины
        bleJob.cancel()
        try {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mGattUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("BLEController", "Ресивер уже отписан")
        }
    }


    internal fun setUploadingState(state: Boolean) { isUploading = state }
    internal fun isCurrentlyUploading(): Boolean { return isUploading }
    internal fun setProgressDialog(dialog: Dialog?) { progressDialog = dialog }
    internal fun getBluetoothLeService() : BluetoothLeService? { return mBluetoothLeService }
    internal fun getBluetoothAdapter() : BluetoothAdapter? { return mBluetoothAdapter }
    internal fun getStatusConnected() : Boolean { return mConnected }

    internal fun setReconnectThreadFlag(reconnectThreadFlag: Boolean) {
        this.reconnectThreadFlag = reconnectThreadFlag
    }
}