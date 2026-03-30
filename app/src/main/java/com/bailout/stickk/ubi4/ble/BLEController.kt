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
import com.bailout.stickk.ubi4.ble.BLECommandsV3.request
import com.bailout.stickk.ubi4.ble.BLECommandsV3.requestWithCommand
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
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.guiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
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

class BLEController(private val bleManager: BleManagerKmm) {
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
    private var onNeedFullInitListener: (() -> Unit)? = null

    @Volatile private var isTransferFlowActive = false
    @Volatile private var productInfoRequested = false

    private val bleJob = Job()
    private val bleScope = CoroutineScope(Dispatchers.Main + bleJob)
    private var mDisconnected = false

    private var reconnectJob: Job? = null

    private var receiverRegistered = false

    @Volatile
    private var needReRequestTransferFlow = false
    private val pendingNotifyAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    @Volatile
    private var pendingDeviceDataResponseAck: CompletableDeferred<Boolean>? = null

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
        registerGattReceiverIfNeeded()
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
                                mBLEParserV3?.generatedHardcodeWidgets()
                                initRequestsV3()
                            } else {
                                //ветка инициализации протокола UBIv4
                                UiState.startupInProgress.value = true
                                // сброс прогресса ок, но "готово" НЕ эмитим
                                UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(0, 0)
                                requestProductInfoTypeOnceForUbiV4()
                                smartInitWithCrc()
                            }
                        }
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
                    if ((intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL) != null) && (!UiState.isInterfaceV3Activated)) {
                        parseReceivedData(intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL))
                    }
                    if (UiState.isInterfaceV3Activated) {
                        if (intent.getByteArrayExtra(BluetoothLeService.SENSORS_STREAM_V3) != null) {//стрим сенсоров, полностью совпадает с FEST-X
                            parseReceivedSensorsDataV3(intent.getByteArrayExtra(BluetoothLeService.SENSORS_STREAM_V3))
                        }
                        if (intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL_V3_SERIALPORTCHAR) != null) {//используем как порт в UBIv4
                            pendingDeviceDataResponseAck?.complete(true)
                            parseReceivedDataV3(intent.getByteArrayExtra(BluetoothLeService.MAIN_CHANNEL_V3_SERIALPORTCHAR))
                        }
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
    private suspend fun requestProductInfoTypeOnceForUbiV4() {
        val mainChannelNotifyEnabled = enableNotifyAndAwaitResponse(MAIN_CHANNEL_CHARACTERISTIC)
        if (!mainChannelNotifyEnabled) {
            main.showToast("Не включилась notify MAIN_CHANNEL_CHARACTERISTIC")
            platformLog("parseProductCRCInfo", "НЕ УСПЕШНО")
            requestProductInfoTypeOnceForUbiV4()
        } else {
            platformLog("parseProductCRCInfo", "УСПЕШНО")
            main.bleCommandWithQueue(
                BLECommands.requestProductInfoType(0x00.toByte()),
                MAIN_CHANNEL_CHARACTERISTIC,
                WRITE
            ) {}
        }
    }
    private suspend fun enableNotifyAndAwaitResponse(uuid: String, timeoutMs: Long = 250L, attempts: Int = 10, baseDelayMs: Long = 10L, onFailedAttempt: (suspend (attempt: Int, max: Int) -> Unit)? = null): Boolean {
        val key = uuid.lowercase()
        repeat(attempts) { index ->
            val attemptNo = index + 1
            val ack = CompletableDeferred<Boolean>()
            pendingNotifyAcks[key] = ack
            bleCommand(null, uuid, NOTIFY)
            val success = withTimeoutOrNull(timeoutMs) { ack.await() } ?: false
            pendingNotifyAcks.remove(key, ack)

            if (success) return true
            if (attemptNo < attempts) {
                kotlinx.coroutines.delay(baseDelayMs * attemptNo)
            }
        }
        return false
    }
    private suspend fun requestDeviceDataAndAwaitResponse(timeoutMs: Long = 250L): Boolean {
        val responseAck = CompletableDeferred<Boolean>()
        pendingDeviceDataResponseAck = responseAck
        platformLog( "requestDeviceDataAndAwaitResponse","requestDeviceData: ${EncodeByteToHex.bytesToHexString(BLECommandsV3.requestDeviceData())}")
        bleManager.sendBytesKmm(
            BLECommandsV3.requestDeviceData(),
            SERIALPORTCHAR_UUID,
            WRITE
        ) {}

        val responseReceived = withTimeoutOrNull(timeoutMs) { responseAck.await() } ?: false
        pendingDeviceDataResponseAck = null
        return responseReceived
    }
    private suspend fun firstRequestInicializeInformation() {
        main.bleCommandWithQueue(
            BLECommands.requestInicializeInformation(),
            MAIN_CHANNEL_CHARACTERISTIC,
            WRITE
        ) {}

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

    private suspend fun smartInitWithCrc() {
        Log.d("smartInitWithCrc", "▶ smartInitWithCrc")
        val masterAddr = 0

        val oldCrc: Long? = RoomPersistence.loadDeviceCrc(masterAddr)
        Log.d("BLE_CRC", "oldCrc from DB = $oldCrc, mac=${WidgetRepoProvider.mac()}")

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
        firstRequestInicializeInformation()
    }

    private fun parseReceivedData(data: ByteArray?) {
        if (data == null) { return }
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
            mBLEParserV3?.parseReceivedSensorsData(data)
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
            platformLog("connectToSavedDeviceNow", "scanLeDevice")
            scanLeDevice(true)
            return
        }
        platformLog("connectToSavedDeviceNow", "не scanLeDevice")

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
                Log.d("BLE_RECON", "reconnectThread() finished, connected=$mConnected")
            }
        }
    }
    private suspend fun reconnect() {
        // Выполняем unbindService и bindService на IO-потоке, если они действительно могут быть «тяжёлыми»
        withContext(Dispatchers.IO) {
            runCatching { mContext.unbindService(mServiceConnection) }
                .onFailure { Log.w("BLEController", "Не удалось отцепить сервис: ${it.message}") }

            mBluetoothLeService = null

            val gattServiceIntent = Intent(mContext, BluetoothLeService::class.java)
            mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        }

        // На главном потоке регистрируем ресивер (если требуется)
        withContext(Dispatchers.Main) {
            registerGattReceiverIfNeeded()
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerGattReceiverIfNeeded() {
        if (receiverRegistered) return
        runCatching {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                LocalBroadcastManager.getInstance(mContext)
                    .registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
            } else {
                mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
            }
            receiverRegistered = true
        }.onFailure {
            Log.w("BLEController", "registerReceiver failed: ${it.message}")
        }
    }

    private fun unregisterGattReceiverIfNeeded() {
        if (!receiverRegistered) return
        runCatching {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                LocalBroadcastManager.getInstance(mContext)
                    .unregisterReceiver(mGattUpdateReceiver)
            } else {
                mContext.unregisterReceiver(mGattUpdateReceiver)
            }
        }.onFailure {
            Log.w("BLEController", "unregisterReceiver failed: ${it.message}")
        }
        receiverRegistered = false
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
    internal fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String): Boolean  {
        Log.d("bleCommand", "Отправка команды: тип = $typeCommand, UUID = $uuid, данные = ${byteArray?.let { EncodeByteToHex.bytesToHexString(it) }}")
        var commandDispatched = false
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
                            commandDispatched = true
                            if (!commandDispatched) {
                                Log.w("bleCommand", "writeCharacteristic вернул false для UUID=$uuid")
                            }
                        }
                    }
                    if (typeCommand == READ){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                            mBluetoothLeService?.readCharacteristic(mCharacteristic)
                            commandDispatched = true
                        }
                    }
                    if (typeCommand == NOTIFY){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                            System.err.println("BLE debug попытка подписки на нотификацию")
                            mNotifyCharacteristic = mCharacteristic
                            mBluetoothLeService?.setCharacteristicNotification(mCharacteristic, true)
                            commandDispatched = true
                        }
                    }
                }
            }
        }
        return commandDispatched
    }
    fun cleanup() {
        reconnectThreadFlag = false
        reconnectJob?.cancel()
        bleJob.cancel()
        mDisconnected = true
        progressDialog?.dismiss()
        progressDialog = null
        onNeedFullInitListener = null
        onDisconnectedListener = null
        runCatching { mBluetoothLeService?.disconnect() }
        runCatching { mBluetoothLeService?.close() }
        runCatching { mContext.unbindService(mServiceConnection) }
            .onFailure { Log.w("BLEController", "unbindService failed: ${it.message}") }
        mBluetoothLeService = null
        unregisterGattReceiverIfNeeded()
    }

    private suspend fun initRequestsV3() {
        // 1) Поднимаем NOTIFY 1
        val serialNotifyEnabled = enableNotifyAndAwaitResponse(SERIALPORTCHAR_UUID)
        if (!serialNotifyEnabled) {
            Log.w("BLEParserV3", "Не удалось подтвердить включение notify для SERIALPORTCHAR_UUID")
            main.showToast("Не включилась notify SERIALPORTCHAR_UUID")
            initRequestsV3()
        } else {
            // 2) Запрашиваем информацию по девайсам и дожидаемся первого ответа
            val gotDeviceDataResponse = requestDeviceDataAndAwaitResponse()
            if (!gotDeviceDataResponse) {
                Log.w("BLEParserV3", "Ответ на requestDeviceData() не получен до включения MAIN_CHANNEL notify")
                main.showToast("Нет ответа requestDeviceData(), повторяем запрос")
                initRequestsV3()
            } else {
                // 3) Поднимаем NOTIFY 2
                val mainChannelNotifyEnabled = enableNotifyAndAwaitResponse(MAIN_CHANNEL_CHARACTERISTIC) { attempt, max ->
                    main.showToast("Не включилась notify MAIN_CHANNEL — попытка $attempt/$max")
                    Log.w("BLEParserV3", "Не включилась notify MAIN_CHANNEL — попытка $attempt/$max")
                }
                if (!mainChannelNotifyEnabled) {
                    Log.w("BLEParserV3", "Не удалось подтвердить включение notify для MAIN_CHANNEL_CHARACTERISTIC")
                    main.showToast("Не включилась notify MAIN_CHANNEL_CHARACTERISTIC")
                    initRequestsV3()
                } else {
                    main.bleCommandWithQueue(
                        request(PWCE_GET_THRESHOLD_VALUE.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                    main.bleCommandWithQueue(
                        request(PWCE_GET_EMG_GAIN_VALUE.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                    main.bleCommandWithQueue(
                        request(PWCE_GET_EMG_CHANGE_GESTURE.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                    main.bleCommandWithQueue(
                        requestWithCommand(GUI_CONTROL.number.toInt(),GMCE_GET_SCREEN_TIMEOUT.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                    main.bleCommandWithQueue(
                        request(PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                    main.bleCommandWithQueue(
                        requestWithCommand(GUI_CONTROL.number.toInt(),GMCE_GET_LEFT_RIGHT_HAND.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                    main.bleCommandWithQueue(
                        request(PWCE_GET_HAND_CONTROL_MODE.number.toInt()),
                        SERIALPORTCHAR_UUID,
                        WRITE){}
                }
            }
        }
    }

    fun refreshWidgetsV3BySwipe() {
        main.lifecycleScope.launch {
            initRequestsV3()
            UiState.fullInitInProgress.value = false
            UiState.widgetsLoadingFlow.tryEmit(Unit)
        }
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
