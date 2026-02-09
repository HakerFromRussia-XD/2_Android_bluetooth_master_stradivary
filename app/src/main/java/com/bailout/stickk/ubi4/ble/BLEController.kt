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
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.lookup
import com.bailout.stickk.ubi4.data.local.bootstrap.WidgetBootstrapHydrator
import com.bailout.stickk.ubi4.data.local.db.RoomPersistence
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.data.state.BLEState.bleParser
import com.bailout.stickk.ubi4.data.state.BLEState.bleParserV3
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceAddress
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.listWidgets
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ControllerBleStatusConnection
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.internal.notifyAll
import java.util.concurrent.ConcurrentHashMap

class BLEController() {
    private val mContext: Context = main.applicationContext
    private var mBLEParser: BLEParser? = null
    private var mBLEParserV3: BLEParserV3? = null


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
    private var onNeedFullInitListener: (() -> Unit)? = null

    @Volatile private var isTransferFlowActive = false

    private val bleJob = Job()
    private val bleScope = CoroutineScope(Dispatchers.Main + bleJob)
    private var mDisconnected = false

    private var reconnectJob: Job? = null

    private var receiverRegistered = false

    @Volatile
    private var needReRequestTransferFlow = false
    private val pendingNotifyAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

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
        mBLEParserV3 = bleParserV3
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
                    isTransferFlowActive = false
                    if (mDisconnected) {
                        Log.d("BLE_DEBUG11", " isDisconnected = ${mDisconnected}")
                        System.err.println("Устройство отключено намеренно, не переподключаемся")
                        return
                    }
                    mConnected = false
                    isUploading = false
                    endFlag = true
                    progressDialog?.dismiss()
                    progressDialog = null

                    firstNotificationRequestFlag = true
                    needReRequestTransferFlow = true


                    WidgetState.dbSnapshotAppliedWithCrc = false

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

                    WidgetRepoProvider.setCurrentMac(connectedDeviceAddress)
                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService!!.supportedGattServices)

                        main.lifecycleScope.launch {
                            if (UiState.isInterfaceV3Activated) {
                                //закрытие прелоадера синхронизации
                                UiState.startupInProgress.value = false
                                ensureTransferFlowV3Active()
                            } else {
                                //ветка инициализации протокола UBIv4
                                UiState.startupInProgress.value = true
                                // сброс прогресса ок, но "готово" НЕ эмитим
                                UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(0, 0)
                                ensureTransferFlowActive()
                                smartInitWithCrc()
                            }
                        }
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
                    if (intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL) != null) {
                        parseReceivedData(intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL))
                    }
                    if (intent.getByteArrayExtra(BluetoothLeService.SENSORS_STREAM_V3) != null) {//стрим сенсоров, полностью совпадает с FEST-X
                        parseReceivedSensorsDataV3(intent.getByteArrayExtra(BluetoothLeService.SENSORS_STREAM_V3))
                    }
                    if (intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL_V3_SERIALPORTCHAR) != null) {//используем как порт в UBIv4
                        parseReceivedDataV3(intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL_V3_SERIALPORTCHAR))
                    }
                }
                BluetoothLeService.ACTION_NOTIFICATION_SUBSCRIBED == action -> {
                    val uuid = intent.getStringExtra(BluetoothLeService.EXTRA_NOTIFICATION_UUID)?.lowercase()
                    val enabled = intent.getBooleanExtra(BluetoothLeService.EXTRA_NOTIFICATION_ENABLED, false)
                    val status = intent.getIntExtra(BluetoothLeService.EXTRA_GATT_STATUS, -1)
                    Log.d("BLE_NOTIFY", "Notify subscribe result: uuid=$uuid enabled=$enabled status=$status")
                    pendingNotifyAcks.remove(uuid)?.complete(enabled)
                }
            }
        }
    }
    private suspend fun enableNotifyAndAwaitAck(uuid: String, timeoutMs: Long = 2500L): Boolean {
        val key = uuid.lowercase()
        val ack = CompletableDeferred<Boolean>()
        pendingNotifyAcks[key] = ack
        bleCommand(null, uuid, NOTIFY)

        val success = withTimeoutOrNull(timeoutMs) { ack.await() } ?: false
        pendingNotifyAcks.remove(key, ack)
        return success
    }
    private suspend fun firstNotificationRequestFull() {
        var attempts = 0

        while (firstNotificationRequestFlag && attempts < 5) {
            Log.d("BLE_INIT", "▶ firstNotificationRequestFull попытка #${attempts + 1}")

            bleCommand(
                BLECommands.requestInicializeInformation(),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            )
            bleCommand(null, MAIN_CHANNEL_CHARACTERISTIC, NOTIFY)
            main.bleCommandWithQueue(
                BLECommands.requestSystemCrc(),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}

            delay(1000)
            attempts++
        }

        if (firstNotificationRequestFlag) {
            Log.e("BLE_INIT", "✖ не получили уведомление после $attempts попыток")
            // да, оставляем рекурсивный ретрай как был
            firstNotificationRequestFull()
        } else {
            Log.d("BLE_INIT", "✔ уведомление получено, выходим из цикла")

            // 1) Проверяем, есть ли кеш к этому моменту
            val cacheCount = WidgetRepoProvider.get().count()
            val hasCache = cacheCount > 0

            if (hasCache) {
                WidgetBootstrapHydrator.restoreFromDb(0)
                WidgetBootstrapHydrator.hydrateParameterProviderFromDb(0)
                WidgetBootstrapHydrator.replayWidgetEventsFromDb(0)
                updateFlow.emit(0)
            }

            if (needReRequestTransferFlow) {
                Log.d("BLE_INIT", "→ re-request transfer flow after reconnect")
                bleCommand(
                    BLECommands.requestTransferFlow(1),
                    MAIN_CHANNEL_CHARACTERISTIC,
                    WRITE
                )
                needReRequestTransferFlow = false
            }

            UiState.widgetsLoadingFlow.tryEmit(Unit)
        }
    }

    private suspend fun smartInitWithCrc() {
        Log.d("smartInitWithCrc", "▶ smartInitWithCrc")
        val masterAddr = 0

        val oldCrc: Long? = RoomPersistence.loadDeviceCrc(masterAddr)
        Log.d("BLE_CRC", "oldCrc from DB = $oldCrc, mac=${WidgetRepoProvider.mac()}")

        // Включаем NOTIFY и запрашиваем CRC
        bleCommand(null, MAIN_CHANNEL_CHARACTERISTIC, NOTIFY)
        delay(150)
        //TODO переписать без delay

        Log.d("BLE_INIT", "smartInitWithCrc → send requestSystemCrc()")
        main.bleCommandWithQueue(
            BLECommands.requestSystemCrc(),
            MAIN_CHANNEL_CHARACTERISTIC,
            WRITE
        ) {}

        // 2) ждём, пока parseProductCRCInfo сохранит CRC в БД
        var newCrc: Long? = null
        repeat(5) { attempt ->
            delay(200)
            newCrc = RoomPersistence.loadDeviceCrc(masterAddr)
            Log.d("BLE_CRC", "poll[$attempt] newCrc = $newCrc, mac=${WidgetRepoProvider.mac()}")
            if (newCrc != null) return@repeat
        }

        Log.d("BLE_CRC", "final newCrc = $newCrc, mac=${WidgetRepoProvider.mac()}")

        val crcSame = oldCrc != null && newCrc != null && oldCrc == newCrc
        val cacheCount = WidgetRepoProvider.get().count()
        val hasCache = cacheCount > 0

        Log.d(
            "BLE_INIT",
            "DECISION: crcSame=$crcSame, hasCache=$hasCache, old=$oldCrc, new=$newCrc, cacheCount=$cacheCount, mac=${WidgetRepoProvider.mac()}"
        )

        // ---------- ТЁПЛЫЙ СТАРТ ----------
        if (crcSame && hasCache) {
            Log.d(
                "BLE_INIT",
                "WARM START (old=$oldCrc, new=$newCrc, cacheCount=$cacheCount, mac=${WidgetRepoProvider.mac()})"
            )

            WidgetState.dbSnapshotAppliedWithCrc = true
            // Полной инициализации НЕТ
            UiState.fullInitInProgress.value = false

            // Сбрасываем прогресс
            UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(
                current = 0,
                total = 0
            )

//            UiState.widgetsLoadingFlow.tryEmit(Unit)

            // Гидратация из кеша
            WidgetBootstrapHydrator.restoreFromDb(masterAddr)
            WidgetBootstrapHydrator.hydrateParameterProviderFromDb(masterAddr)
            WidgetBootstrapHydrator.rebuildParameterLinksFromDb(masterAddr)
            WidgetBootstrapHydrator.replayWidgetEventsFromDb(masterAddr)
            updateFlow.emit(0)
            UiState.startupInProgress.value = false
            UiState.widgetsLoadingFlow.tryEmit(Unit)

            // Запускаем живой поток
            main.bleCommandWithQueue(
                BLECommands.requestTransferFlow(1),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}

            WidgetBootstrapHydrator.requestParametersDataKmm { cmd ->
                bleCommand(cmd, MAIN_CHANNEL_CHARACTERISTIC, WRITE)
            }

            main.bleCommandWithQueue(
                BLECommands.requestProductInfoType(0x00.toByte()),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}

            main.bleCommandWithQueue(
                BLECommands.requestBatteryStatus(7, 0),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}

            needReRequestTransferFlow = false
            return
        }

        // ---------- ХОЛОДНЫЙ СТАРТ ----------
        Log.d(
            "BLE_INIT",
            "COLD START (old=$oldCrc, new=$newCrc, hasCache=$hasCache, mac=${WidgetRepoProvider.mac()}) → full init"
        )

        WidgetState.dbSnapshotAppliedWithCrc = false

        UiState.fullInitInProgress.value = true
        UiState.startupInProgress.value = false

        withContext(Dispatchers.Main) {
            onNeedFullInitListener?.invoke()
        }
        firstNotificationRequestFull()
    }

    private fun parseReceivedData(data: ByteArray?) {
        if (data == null) { return }
        firstNotificationRequestFlag = false
        runCatching {
            mBLEParser?.parseReceivedData(data)
        }.onFailure { t ->
            main.showToast("ошибка парсинга в mBLEParser")
        }
    }
    private fun parseReceivedDataV3(data: ByteArray?) {
        if (data == null) { return }
        runCatching {
            mBLEParserV3?.parseReceivedData(data)
        }.onFailure { t ->
            main.showToast("ошибка парсинга в mBLEParserV3")
        }
    }
    private fun parseReceivedSensorsDataV3(data: ByteArray?) {
        if (data == null) { return }
        runCatching {
            mBLEParserV3?.parseReceivedData(data)
        }.onFailure { t ->
            main.showToast("ошибка парсинга в mBLEParserV3")
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
    fun connectToSavedDeviceNow() {
        val hasTarget = connectedDeviceAddress.isNotBlank() && connectedDeviceAddress != "null"
        if (!hasTarget) {
            scanLeDevice(true)
            return
        }

        reconnectThreadFlag = true
        reconnectThread()
    }
    fun reconnectThread() {
        if (reconnectJob?.isActive == true) return

        reconnectJob = bleScope.launch {
            var j = 1
            try {
                while (reconnectThreadFlag) {
                    if (j % 5 == 0) {
                        scanLeDevice(true)
                    } else {
                        reconnect()
                    }
                    j++
                    delay(RECONNECT_BLE_PERIOD.toLong())
                }
            } finally {
                if (!mConnected) {
                    ControllerBleStatusConnection.UiBridges.bleStatusController?.stopReconnecting()
                }
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
        reconnectJob?.cancel()
        ControllerBleStatusConnection.UiBridges.bleStatusController?.stopReconnecting()
        mDisconnected = true
        println("--> дисконнектим всё к хуям и анбайндим")
        bleScope.launch(Dispatchers.IO) {
            mBluetoothLeService?.disconnect()
            runCatching { mContext.unbindService(mServiceConnection) }
            withContext(Dispatchers.Main) {
                mConnected = false
                listWidgets.clear()
                UiState.resetWidgetRequests()
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
        intentFilter.addAction(BluetoothLeService.ACTION_NOTIFICATION_SUBSCRIBED)
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
        Log.d("bleCommand", "Отправка команды: тип = $typeCommand, UUID = $uuid, данные = ${byteArray?.let { EncodeByteToHex.bytesToHexString(it) }}")
        for (i in mGattCharacteristics.indices) {
            for (j in mGattCharacteristics[i].indices) {
                Log.d("bleCommand", "Характеристика $i-$j UUID: ${mGattCharacteristics[i][j].uuid} ищем: UUID = $uuid")
                if(mGattCharacteristics[i][j].uuid.toString().equals(uuid, ignoreCase = true)){
                    Log.d("bleCommand", "НАШЛИ!!! UUID = $uuid")
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
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Там, где регистрировали через LocalBroadcastManager
                LocalBroadcastManager.getInstance(mContext)
                    .unregisterReceiver(mGattUpdateReceiver)
            } else {
                // Там, где регистрировали через обычный Context
                mContext.unregisterReceiver(mGattUpdateReceiver)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("BLEController", "Ресивер уже отписан: ${e.message}")
        }
    }

    private suspend fun ensureTransferFlowActive() {
        //старый вариант подключения для UBIv4
        if (isTransferFlowActive) return

        // 1) Поднимаем NOTIFY
        bleCommand(null, MAIN_CHANNEL_CHARACTERISTIC, NOTIFY)
        delay(200) // лучше ждать onDescriptorWrite, но это быстрый фикс

        // 2) Запрашиваем стрим
        main.bleCommandWithQueue(
            BLECommands.requestTransferFlow(1),
            MAIN_CHANNEL_CHARACTERISTIC,
            WRITE
        ) {}

        isTransferFlowActive = true
    }
    private suspend fun ensureTransferFlowV3Active() {
        //старый вариант подключения для UBIv4
        if (isTransferFlowActive) return

        // 1) Поднимаем NOTIFY 1
        val serialNotifyEnabled = enableNotifyAndAwaitAck(SERIALPORTCHAR_UUID)
        if (!serialNotifyEnabled) {
            Log.w("BLEParserV3", "Не удалось подтвердить включение notify для SERIALPORTCHAR_UUID")
            main.showToast("Не включилась notify SERIALPORTCHAR_UUID")
            return
        }

        // 2) Поднимаем NOTIFY 2
        val mainChannelNotifyEnabled = enableNotifyAndAwaitAck(MAIN_CHANNEL_CHARACTERISTIC)
        if (!mainChannelNotifyEnabled) {
            Log.w("BLEParserV3", "Не удалось подтвердить включение notify для MAIN_CHANNEL_CHARACTERISTIC")
            main.showToast("Не включилась notify MAIN_CHANNEL_CHARACTERISTIC")
            return
        }

        platformLog("BLEParserV3", "send command requestDeviceData")
        // 3) Запрашиваем информацию по девайсам
        main.bleCommandWithQueue(
            BLECommandsV3.requestDeviceData(),
            SERIALPORTCHAR_UUID,
            WRITE
        ) {}

        isTransferFlowActive = true
    }

    fun setOnNeedFullInitListener(listener: () -> Unit) {
        onNeedFullInitListener = listener
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