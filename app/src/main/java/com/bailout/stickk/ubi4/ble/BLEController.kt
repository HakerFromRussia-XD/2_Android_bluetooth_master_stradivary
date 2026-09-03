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
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.network.SettingsProfileUploadWorkScheduler
import com.bailout.stickk.ubi4.data.network.Ubi4SettingsProfileReceiver
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
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendFlag
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.mainOrNull
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
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class BLEController(private val bleManager: BleManagerKmm) {
    private data class V3InitRequest(
        val packet: ByteArray,
        val expectedResponseCommand: Int,
        val expectedResponseSubcommand: Int
    )

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
    @Volatile private var dfuReconnectActive = false
    @Volatile private var firmwareUpdateSessionActive = false
    @Volatile private var gattServicesGeneration = 0L
    private var scanWithoutConnectFlag = false
    private var mConnected = false
    private var endFlag = false
    private var mScanning = false
    private var onConnectedListener: (() -> Unit)? = null
    private var onNeedFullInitListener: (() -> Unit)? = null
    private val settingsProfileReceiver = Ubi4SettingsProfileReceiver()

    @Volatile private var isTransferFlowActive = false
    @Volatile private var productInfoRequested = false
    @Volatile private var settingsProfileDownloadedForConnection = false

    private val bleJob = Job()
    private val bleScope = CoroutineScope(Dispatchers.Main + bleJob)
    private var mDisconnected = false

    private var reconnectJob: Job? = null

    private var receiverRegistered = false

    @Volatile
    private var needReRequestTransferFlow = false
    private val pendingNotifyAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    @Volatile
    private var pendingFirmwareControlWrite: CompletableDeferred<Boolean>? = null
    @Volatile
    private var pendingDeviceDataResponseAck: CompletableDeferred<Boolean>? = null
    private val v3InitProgressLock = Any()
    private val v3InitExpectedResponses = mutableSetOf<String>()
    private var v3InitProgressTotal: Int = 0
    private var v3InitProgressCurrent: Int = 0
    private var v3InitTrackingActive: Boolean = false
    private var v3InitCompletionEmitted: Boolean = false

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            mBluetoothLeService?.setReceiverCallback {state ->
                if(state == WRITE) {
                    pendingFirmwareControlWrite?.complete(true)
                    val currentMain = mainOrNull ?: return@setReceiverCallback
                    synchronized(currentMain.writeLock) {
                        canSendFlag = true
                        currentMain.writeLock.notifyAll()
                    }
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
            Toast.makeText(mContext, mContext.getString(SharedRes.strings.ble_le_not_supported.resourceId), Toast.LENGTH_SHORT).show()
            main.finish()
        }
        val bluetoothManager = main.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(mContext, mContext.getString(SharedRes.strings.bluetooth_adapter_unavailable.resourceId), Toast.LENGTH_SHORT).show()
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
                    Log.i(
                        DFU_TRACE_TAG,
                        "controller event=GATT_CONNECTED connected=$mConnected generation=$gattServicesGeneration " +
                            "reconnect_flag=$reconnectThreadFlag dfu_active=$dfuReconnectActive"
                    )
                    System.err.println("Check BroadcastReceiver() ACTION_GATT_CONNECTED")
                    reconnectThreadFlag = false
                    settingsProfileDownloadedForConnection = false
                    SettingsProfileUploadWorkScheduler.onConnected(mContext)
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
                    Log.i(
                        DFU_TRACE_TAG,
                        "controller event=GATT_DISCONNECTED connected_before=$mConnected generation=$gattServicesGeneration " +
                            "reconnect_flag=$reconnectThreadFlag dfu_active=$dfuReconnectActive intentional=$mDisconnected"
                    )
                    isTransferFlowActive = false
                    if (mDisconnected) {
                        Log.d("BLE_DEBUG11", " isDisconnected = ${mDisconnected}")
                        System.err.println("Устройство отключено намеренно, не переподключаемся")
                        return
                    }
                    mConnected = false
                    isUploading = false
                    endFlag = true
                    SettingsProfileUploadWorkScheduler.enqueueDisconnectUpload(
                        context = mContext,
                        reason = "ble_disconnect"
                    )
                    progressDialog?.dismiss()
                    progressDialog = null
                    needReRequestTransferFlow = true


                    WidgetState.dbSnapshotAppliedWithCrc = false

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(mContext,
                            context.getString(R.string.bluetooth_connection_is_disabled), Toast.LENGTH_SHORT).show()
                    }

                    mBluetoothLeService?.close()

                    if (!reconnectThreadFlag && !mScanning) {
                        reconnectThreadFlag = true
                        reconnectThread()
                    }
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
                    gattServicesGeneration++
                    Log.i(
                        DFU_TRACE_TAG,
                        "controller event=SERVICES_DISCOVERED generation=$gattServicesGeneration " +
                            "services=${mBluetoothLeService?.supportedGattServices?.size ?: 0} " +
                            "serial_wwr=${mBluetoothLeService?.supportsWriteWithoutResponse(SERIALPORTCHAR_UUID)} " +
                            "max_wwr=${mBluetoothLeService?.maximumWriteWithoutResponseSize()} dfu_active=$dfuReconnectActive"
                    )
                    Log.d("BLE_CONN", "▶ ACTION_GATT_SERVICES_DISCOVERED, services count = ${mBluetoothLeService?.supportedGattServices?.size ?: 0}")
                    mConnected = true
                    Toast.makeText(
                        context,
                        context.getString(SharedRes.strings.connected_device.resourceId, connectedDeviceAddress),
                        Toast.LENGTH_SHORT
                    ).show()

                    WidgetRepoProvider.setCurrentMac(connectedDeviceAddress)
                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService!!.supportedGattServices)

                        val bootloaderV2Transport =
                            mBluetoothLeService?.supportsWriteWithoutResponse(SERIALPORTCHAR_UUID) == true
                        if (!dfuReconnectActive && !firmwareUpdateSessionActive && !bootloaderV2Transport) {
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
                        } else {
                            Log.i(
                                DFU_TRACE_TAG,
                                "controller normal_init suppressed dfu_active=$dfuReconnectActive " +
                                    "firmware_session=$firmwareUpdateSessionActive " +
                                    "bootloader_v2_transport=$bootloaderV2Transport"
                            )
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
            main.showToast(main.getString(SharedRes.strings.notify_enable_failed.resourceId, "MAIN_CHANNEL_CHARACTERISTIC"))
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
            if (uuid.equals(SERIALPORTCHAR_UUID, ignoreCase = true)) {
                Log.d(
                    DFU_TRACE_TAG,
                    "notify subscribe attempt=$attemptNo/$attempts uuid=$uuid timeout_ms=$timeoutMs " +
                        "connected=$mConnected generation=$gattServicesGeneration"
                )
            }
            bleCommand(null, uuid, NOTIFY)
            val success = withTimeoutOrNull(timeoutMs) { ack.await() } ?: false
            pendingNotifyAcks.remove(key, ack)

            if (uuid.equals(SERIALPORTCHAR_UUID, ignoreCase = true)) {
                Log.d(DFU_TRACE_TAG, "notify subscribe result attempt=$attemptNo success=$success uuid=$uuid")
            }

            if (success) return true
            if (attemptNo < attempts) {
                kotlinx.coroutines.delay(baseDelayMs * attemptNo)
            }
        }
        return false
    }

    suspend fun prepareFirmwareSessionNotifications(): Boolean {
        val ready = enableNotifyAndAwaitResponse(
            uuid = SERIALPORTCHAR_UUID,
            timeoutMs = 1_000L,
            attempts = 3,
            baseDelayMs = 100L
        )
        Log.i(DFU_TRACE_TAG, "firmware_session serial_notify_ready=$ready generation=$gattServicesGeneration")
        return ready
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
            bleParser.sendFwInfoRequestsWithRetry()

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
            main.showToast(main.getString(SharedRes.strings.parser_error.resourceId, "mBLEParser"))
        }
    }
    private fun parseReceivedDataV3(data: ByteArray?) {
        if (data == null) { return }
        runCatching {
            mBLEParserV3?.parseReceivedData(data)
            handleV3InitResponseProgress(data)
        }.onFailure { t ->
            main.showToast(main.getString(SharedRes.strings.parser_error.resourceId, "mBLEParserV3"))
        }
    }
    private fun parseReceivedSensorsDataV3(data: ByteArray?) {
        if (data == null) { return }
        runCatching {
            mBLEParserV3?.parseReceivedSensorsData(data)
        }.onFailure { t ->
            main.showToast(main.getString(SharedRes.strings.parser_error.resourceId, "mBLEParserV3"))
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
        if (reconnectJob?.isActive == true) {
            Log.i(
                DFU_TRACE_TAG,
                "controller reconnect_worker reuse active=true flag=$reconnectThreadFlag connected=$mConnected " +
                    "generation=$gattServicesGeneration"
            )
            return
        }

        Log.i(
            DFU_TRACE_TAG,
            "controller reconnect_worker start flag=$reconnectThreadFlag connected=$mConnected generation=$gattServicesGeneration"
        )
        reconnectJob = bleScope.launch {
            var j = 1
            try {
                while (reconnectThreadFlag) {
                    Log.d(
                        DFU_TRACE_TAG,
                        "controller reconnect_attempt=$j via=${if (j % 5 == 0) "scan" else "direct"} " +
                            "connected=$mConnected generation=$gattServicesGeneration"
                    )
                    if (j % 5 == 0) {
                        scanLeDevice(true)
                    } else {
                        reconnect()
                    }
                    j++
                    delay(RECONNECT_BLE_PERIOD.toLong())
                }
            } finally {
                Log.i(
                    DFU_TRACE_TAG,
                    "controller reconnect_worker finish connected=$mConnected generation=$gattServicesGeneration flag=$reconnectThreadFlag"
                )
                Log.d("BLE_RECON", "reconnectThread() finished, connected=$mConnected")
            }
        }
    }
    private suspend fun reconnect() {
        val targetAddress = connectedDeviceAddress
        if (targetAddress.isBlank() || targetAddress == "null") {
            Log.w("BLEController", "reconnect skipped: empty target address")
            return
        }

        // Сначала пробуем обычный reconnect без перевязывания сервиса — так стабильнее на части стеков.
        val connectedViaExistingService = withContext(Dispatchers.Main) {
            mBluetoothLeService?.connect(targetAddress) ?: false
        }
        Log.d(
            DFU_TRACE_TAG,
            "controller direct_connect dispatched=$connectedViaExistingService address=$targetAddress generation=$gattServicesGeneration"
        )
        if (connectedViaExistingService) return

        // Fallback: поднимаем сервис заново, если предыдущая попытка не стартовала.
        withContext(Dispatchers.Main) {
            runCatching { mContext.unbindService(mServiceConnection) }
                .onFailure { Log.w("BLEController", "Не удалось отцепить сервис: ${it.message}") }
            mBluetoothLeService = null
            val gattServiceIntent = Intent(mContext, BluetoothLeService::class.java)
            mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
            registerGattReceiverIfNeeded()
        }
    }
    fun disconnect() {
        if (mDisconnected) return
        reconnectThreadFlag = false
        reconnectJob?.cancel()
        if (mScanning) scanLeDevice(false)
        ControllerBleStatusConnection.UiBridges.bleStatusController?.stopReconnecting()
        SettingsProfileUploadWorkScheduler.enqueueDisconnectUpload(
            context = mContext,
            reason = "manual_disconnect"
        )
        mDisconnected = true
        println("--> дисконнектим всё к хуям и анбайндим")
        bleScope.launch(Dispatchers.IO) {
            runCatching { mBluetoothLeService?.disconnect() }
            runCatching { mBluetoothLeService?.close() }
            runCatching { mContext.unbindService(mServiceConnection) }
            withContext(Dispatchers.Main) {
                mConnected = false
                listWidgets.clear()
                UiState.resetWidgetRequests()
                mainOrNull?.openScanActivity()
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
        val adapter = mBluetoothAdapter
        if (adapter == null) {
            if (enable) Log.w("BLEController", "scanLeDevice(true) skipped: adapter is null")
            mScanning = false
            return
        }

        if (enable) {
            if (mScanning) return
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) { return }
            runCatching {
                adapter.startLeScan(mLeScanCallback)
                mScanning = true
            }.onFailure {
                mScanning = false
                Log.w("BLEController", "startLeScan failed: ${it.message}")
            }
        } else {
            if (!mScanning) return
            runCatching {
                adapter.stopLeScan(mLeScanCallback)
            }.onFailure {
                Log.w("BLEController", "stopLeScan failed: ${it.message}")
            }
            mScanning = false
        }
    }
    @SuppressLint("MissingPermission")
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        val deviceName = device.name ?: return@LeScanCallback
        if (mDisconnected) return@LeScanCallback
        System.err.println("------->   ===============найден девайс: ${device.address} - $deviceName  ищем $connectedDeviceAddress ==============")
        if (device.address == connectedDeviceAddress) {
            System.err.println("------->   ==========это нужный нам девайс $device  $scanWithoutConnectFlag ==============")
            if (!scanWithoutConnectFlag) {
                Handler(Looper.getMainLooper()).post {
                    if (mDisconnected) return@post
                    scanLeDevice(false)
                    reconnectThreadFlag = true
                    reconnectThread()
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
                        val properties = mCharacteristic?.properties ?: 0
                        val supportsWrite = properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
                        val supportsWriteNoResponse =
                            properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0
                        if (supportsWrite || supportsWriteNoResponse) {
                            mCharacteristic?.writeType =
                                if (supportsWrite) {
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                } else {
                                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                                }
                            Log.d("bleCommand", "Отправка команды: ${byteArray?.let {
                                EncodeByteToHex.bytesToHexString(
                                    it
                                )
                            }} на UUID: $uuid properties=$properties writeType=${mCharacteristic?.writeType}")
                            System.err.println("BLE debug запись ${EncodeByteToHex.bytesToHexString(byteArray!!)}")
                            mCharacteristic?.value = byteArray
                            commandDispatched =
                                mBluetoothLeService?.writeCharacteristic(mCharacteristic) == true
                            if (!commandDispatched) {
                                Log.w(
                                    DFU_TRACE_TAG,
                                    "controller control_write rejected bytes=${byteArray?.size ?: -1} " +
                                        "connected=$mConnected generation=$gattServicesGeneration"
                                )
                            }
                        } else {
                            Log.w("bleCommand", "WRITE unsupported for UUID=$uuid properties=$properties")
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
        if (mScanning) scanLeDevice(false)
        bleJob.cancel()
        mDisconnected = true
        progressDialog?.dismiss()
        progressDialog = null
        onNeedFullInitListener = null
        onConnectedListener = null
        onDisconnectedListener = null
        runCatching { mBluetoothLeService?.disconnect() }
        runCatching { mBluetoothLeService?.close() }
        runCatching { mContext.unbindService(mServiceConnection) }
            .onFailure { Log.w("BLEController", "unbindService failed: ${it.message}") }
        mBluetoothLeService = null
        unregisterGattReceiverIfNeeded()
    }

    private suspend fun initRequestsV3() {
        while (true) {
            resetV3InitProgressTracking()

            val serialNotifyEnabled = enableNotifyAndAwaitResponse(SERIALPORTCHAR_UUID)
            if (!serialNotifyEnabled) {
                Log.w("BLEParserV3", "Не удалось подтвердить включение notify для SERIALPORTCHAR_UUID")
                main.showToast(main.getString(SharedRes.strings.notify_enable_failed.resourceId, "SERIALPORTCHAR_UUID"))
                continue
            }

            val gotDeviceDataResponse = requestDeviceDataAndAwaitResponse()
            if (!gotDeviceDataResponse) {
                Log.w("BLEParserV3", "Ответ на requestDeviceData() не получен до включения MAIN_CHANNEL notify")
                main.showToast(main.getString(SharedRes.strings.no_device_data_response_retry.resourceId))
                continue
            }

            sendPhoneDateTimeV3()

            val mainChannelNotifyEnabled = enableNotifyAndAwaitResponse(MAIN_CHANNEL_CHARACTERISTIC) { attempt, max ->
                main.showToast(main.getString(SharedRes.strings.main_channel_notify_attempt.resourceId, attempt, max))
                Log.w("BLEParserV3", "Не включилась notify MAIN_CHANNEL — попытка $attempt/$max")
            }
            if (!mainChannelNotifyEnabled) {
                Log.w("BLEParserV3", "Не удалось подтвердить включение notify для MAIN_CHANNEL_CHARACTERISTIC")
                main.showToast(main.getString(SharedRes.strings.notify_enable_failed.resourceId, "MAIN_CHANNEL_CHARACTERISTIC"))
                continue
            }

            val initRequests = buildV3InitRequests()
            startV3InitProgressTracking(initRequests)

            if (initRequests.isEmpty()) {
                UiState.widgetsLoadingFlow.tryEmit(Unit)
                onConnectedListener?.invoke()
                return
            }

            initRequests.forEach { request ->
                main.bleCommandWithQueue(
                    request.packet,
                    SERIALPORTCHAR_UUID,
                    WRITE
                ) {}
            }
            return
        }
    }

    fun refreshWidgetsV3BySwipe() {
        main.lifecycleScope.launch {
            initRequestsV3()
            UiState.fullInitInProgress.value = false
        }
    }

    fun requestTelemetryDataV3() {
        val packet = BLECommandsV3.requestTelemetryData()
        Log.d(
            "TelemetryV3",
            "TX PWCE_GET_TELEMETRY_DATA packet=${EncodeByteToHex.bytesToHexString(packet)}"
        )
        main.bleCommandWithQueue(
            packet,
            SERIALPORTCHAR_UUID,
            WRITE
        ) {}
    }

    fun requestSerialNumberV3() {
        val packet = WidgetCommandBridgeV3.buildReadRequest(
            DEVICE_INFORMATION.number.toInt(),
            SET_SERIAL_NUMBER.number
        ) ?: requestWithCommand(DEVICE_INFORMATION.number.toInt(), GET_SERIAL_NUMBER.number)
        Log.d(
            "DeviceSerialV3",
            "TX GET_SERIAL_NUMBER packet=${EncodeByteToHex.bytesToHexString(packet)}"
        )
        main.bleCommandWithQueue(
            packet,
            SERIALPORTCHAR_UUID,
            WRITE
        ) {}
    }

    private suspend fun sendPhoneDateTimeV3(timeoutMs: Long = 500L) {
        val calendar = Calendar.getInstance()
        val weekDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val packet = BLECommandsV3.sendDateTime(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            weekDay = weekDay,
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            second = calendar.get(Calendar.SECOND)
        )
        val sent = CompletableDeferred<Unit>()

        Log.d(
            "DateTimeV3",
            "TX GMCE_SET_DATE_TIME packet=${EncodeByteToHex.bytesToHexString(packet)}"
        )
        bleManager.sendBytesKmm(
            packet,
            SERIALPORTCHAR_UUID,
            WRITE
        ) {
            sent.complete(Unit)
        }

        val completed = withTimeoutOrNull(timeoutMs) { sent.await() } != null
        if (!completed) {
            Log.w("DateTimeV3", "GMCE_SET_DATE_TIME write callback timeout")
        }
    }

    fun setOnNeedFullInitListener(listener: () -> Unit) {
        onNeedFullInitListener = listener
    }

    fun setOnConnectedListener(listener: () -> Unit) {
        onConnectedListener = listener
    }

    internal fun setUploadingState(state: Boolean) { isUploading = state }
    internal fun isCurrentlyUploading(): Boolean { return isUploading }
    internal fun setFirmwareUpdateSessionActive(state: Boolean) {
        firmwareUpdateSessionActive = state
        Log.i(DFU_TRACE_TAG, "firmware_session active=$state generation=$gattServicesGeneration")
    }
    internal fun setProgressDialog(dialog: Dialog?) { progressDialog = dialog }
    internal fun getBluetoothLeService() : BluetoothLeService? { return mBluetoothLeService }
    internal fun getBluetoothAdapter() : BluetoothAdapter? { return mBluetoothAdapter }
    internal fun getStatusConnected() : Boolean { return mConnected }

    internal fun dfuMaximumWriteWithoutResponseSize(): Int {
        val size = mBluetoothLeService?.maximumWriteWithoutResponseSize() ?: 20
        Log.i(DFU_TRACE_TAG, "controller maximum_wwr_size=$size connected=$mConnected generation=$gattServicesGeneration")
        return size
    }

    internal fun dfuSupportsWriteWithoutResponse(): Boolean {
        return mBluetoothLeService?.supportsWriteWithoutResponse(SERIALPORTCHAR_UUID) == true
    }

    internal fun dfuSetHighPerformanceMode() {
        Log.i(DFU_TRACE_TAG, "controller high_performance request connected=$mConnected generation=$gattServicesGeneration")
        mBluetoothLeService?.requestDfuHighPerformanceMode()
    }

    internal fun dfuWriteWithoutResponse(packet: ByteArray): Boolean {
        val accepted = mBluetoothLeService?.writeDfuWithoutResponse(SERIALPORTCHAR_UUID, packet) == true
        if (!accepted) {
            Log.w(
                DFU_TRACE_TAG,
                "controller wwr rejected bytes=${packet.size} connected=$mConnected generation=$gattServicesGeneration"
            )
        }
        return accepted
    }

    internal suspend fun dfuWriteControlAndAwait(packet: ByteArray, timeoutMs: Long): Boolean {
        check(pendingFirmwareControlWrite == null) { "Another DFU control write is pending" }
        val completion = CompletableDeferred<Boolean>()
        pendingFirmwareControlWrite = completion
        return try {
            val accepted = bleCommand(packet, SERIALPORTCHAR_UUID, WRITE)
            Log.d(
                DFU_TRACE_TAG,
                "controller direct_control accepted=$accepted bytes=${packet.size} " +
                    "connected=$mConnected generation=$gattServicesGeneration"
            )
            accepted && withTimeoutOrNull(timeoutMs) { completion.await() } == true
        } finally {
            if (pendingFirmwareControlWrite === completion) {
                pendingFirmwareControlWrite = null
            }
        }
    }

    internal fun dfuWriteControlExpectDisconnect(packet: ByteArray): Boolean {
        val dispatched = bleCommand(packet, SERIALPORTCHAR_UUID, WRITE)
        Log.i(
            DFU_TRACE_TAG,
            "controller reset_write dispatched=$dispatched bytes=${packet.size} connected=$mConnected generation=$gattServicesGeneration"
        )
        return dispatched
    }

    /**
     * A normal V3 connection sends SET_DATE_TIME, which starts an asynchronous
     * DataTable save.  JUMP_TO_BOOTLOADER must not be layered over that save:
     * the FAM can remain in the reserve-copy retry stage and never reset.
     *
     * Reset main first, reconnect with dfuReconnectActive (so the normal init
     * writes are suppressed), then request a sparse connection interval before
     * JUMP.  The actual v1 JUMP packet and bootloader protocol stay unchanged.
     */
    internal suspend fun prepareFirmwareBootloaderJump(): Boolean {
        val startedAt = System.currentTimeMillis()
        val initialGeneration = gattServicesGeneration
        dfuReconnectActive = true

        val resetPacket = requestWithCommand(POWER_CONTROL.number.toInt(), PCCE_RESET_DEVICE)
        val resetDispatched = bleCommand(resetPacket, SERIALPORTCHAR_UUID, WRITE)
        Log.i(
            DFU_TRACE_TAG,
            "firmware_switch preflight_reset dispatched=$resetDispatched connected=$mConnected " +
                "generation=$initialGeneration"
        )
        check(resetDispatched) { "Android BLE preflight reset before bootloader jump was rejected" }

        // The reset command is consumed immediately, but STM32WB may reset
        // before returning the ATT write response.  Android otherwise reports
        // the dead link only after its ~5 s GATT timeout.  Release that stale
        // link after the command has had one connection interval to arrive.
        delay(FIRMWARE_SWITCH_PREFLIGHT_COMMAND_SETTLE_MS)
        if (mConnected) {
            Log.i(DFU_TRACE_TAG, "firmware_switch preflight releasing stale reset GATT")
            mBluetoothLeService?.disconnectForFirmwareProgramSwitch()
        }
        val disconnected = withTimeoutOrNull(FIRMWARE_SWITCH_PREFLIGHT_DISCONNECT_TIMEOUT_MS) {
            while (mConnected) delay(20L)
            true
        } == true
        Log.i(
            DFU_TRACE_TAG,
            "firmware_switch preflight_disconnect=$disconnected connected=$mConnected " +
                "elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        check(disconnected) { "FAM did not reset before clean bootloader handoff" }

        if (!reconnectThreadFlag) {
            reconnectThreadFlag = true
            reconnectThread()
        }
        val reconnected = withTimeoutOrNull(FIRMWARE_SWITCH_PREFLIGHT_RECONNECT_TIMEOUT_MS) {
            while (!mConnected || gattServicesGeneration <= initialGeneration) delay(20L)
            true
        } == true
        Log.i(
            DFU_TRACE_TAG,
            "firmware_switch preflight_reconnect=$reconnected connected=$mConnected " +
                "initial_generation=$initialGeneration current_generation=$gattServicesGeneration " +
                "elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        check(reconnected) { "Android BLE reconnect after FAM preflight reset timed out" }
        requireDfuSerialNotifications()

        val accepted = mBluetoothLeService?.requestFirmwareSwitchLowPowerMode() == true
        Log.i(
            DFU_TRACE_TAG,
            "firmware_switch prepare_low_power accepted=$accepted connected=$mConnected " +
                "generation=$gattServicesGeneration"
        )
        check(accepted) { "Android BLE low-power request before bootloader jump was rejected" }

        val resetObserved = withTimeoutOrNull(FIRMWARE_SWITCH_FLASH_QUIET_WINDOW_MS) {
            while (mConnected) delay(20L)
            true
        } == true
        Log.i(
            DFU_TRACE_TAG,
            "firmware_switch prepare_complete reset_observed=$resetObserved connected=$mConnected " +
                "elapsed_ms=${System.currentTimeMillis() - startedAt}"
        )
        return resetObserved
    }

    internal suspend fun firmwareReconnectAfterBootloaderJump() {
        val startedAt = System.currentTimeMillis()
        val initialGeneration = gattServicesGeneration
        dfuReconnectActive = true
        reconnectThreadFlag = true
        try {
            Log.i(
                DFU_TRACE_TAG,
                "firmware_switch start connected=$mConnected generation=$initialGeneration"
            )
            // Let the firmware command handler consume JUMP_TO_BOOTLOADER
            // after Android reports the ATT write callback.
            delay(FIRMWARE_SWITCH_COMMAND_SETTLE_MS)
            Log.i(DFU_TRACE_TAG, "firmware_switch command_settle_complete")

            // FAM persists BootloaderStart asynchronously and then resets
            // itself.  Do not poll or reconnect to the still-running main
            // program during that save window; the peripheral disconnect is
            // the authoritative completion signal.
            val peripheralDisconnected = withTimeoutOrNull(
                FIRMWARE_SWITCH_DEVICE_RESET_TIMEOUT_MS
            ) {
                while (mConnected) delay(20L)
                true
            } == true
            Log.i(
                DFU_TRACE_TAG,
                "firmware_switch peripheral_disconnect=$peripheralDisconnected connected=$mConnected " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )
            if (!peripheralDisconnected) {
                Log.w(
                    DFU_TRACE_TAG,
                    "firmware_switch peripheral reset timeout; releasing stale main GATT"
                )
                mBluetoothLeService?.disconnectForFirmwareProgramSwitch()
                val hostDisconnected = withTimeoutOrNull(2_000L) {
                    while (mConnected) delay(20L)
                    true
                } == true
                Log.i(
                    DFU_TRACE_TAG,
                    "firmware_switch host_disconnect=$hostDisconnected connected=$mConnected"
                )
                check(hostDisconnected) { "Firmware program-switch disconnect timeout" }
                delay(FIRMWARE_SWITCH_FALLBACK_QUIET_WINDOW_MS)
            } else {
                delay(FIRMWARE_SWITCH_POST_RESET_HANDOFF_MS)
            }

            reconnectThread()
            val ready = withTimeoutOrNull(15_000L) {
                while (!mConnected || gattServicesGeneration <= initialGeneration) delay(20L)
                true
            }
            Log.i(
                DFU_TRACE_TAG,
                "firmware_switch reconnect_ready=$ready connected=$mConnected " +
                    "initial_generation=$initialGeneration current_generation=$gattServicesGeneration " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )
            check(ready == true) { "Firmware bootloader reconnect timeout" }
            requireDfuSerialNotifications()
        } finally {
            dfuReconnectActive = false
            Log.i(
                DFU_TRACE_TAG,
                "firmware_switch finish connected=$mConnected generation=$gattServicesGeneration " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )
        }
    }

    internal suspend fun dfuReconnect() {
        val startedAt = System.currentTimeMillis()
        dfuReconnectActive = true
        reconnectThreadFlag = true
        try {
            val hadWwr = dfuSupportsWriteWithoutResponse()
            Log.i(
                DFU_TRACE_TAG,
                "controller reconnect start connected=$mConnected generation=$gattServicesGeneration wwr=$hadWwr"
            )
            if (!hadWwr) {
                Log.i("DFU_METRIC", "confirmed_v2_refreshing_stale_gatt_cache=true")
                val refreshed = mBluetoothLeService?.refreshGattCache()
                Log.i(DFU_TRACE_TAG, "controller reconnect cache_refresh=$refreshed")
            }
            mBluetoothLeService?.disconnect()
            val disconnected = withTimeoutOrNull(2_000L) {
                while (mConnected) delay(20L)
                true
            }
            Log.i(DFU_TRACE_TAG, "controller reconnect disconnect_wait=$disconnected connected=$mConnected")
            reconnectThread()
            val ready = withTimeoutOrNull(15_000L) {
                while (!mConnected || !dfuSupportsWriteWithoutResponse()) delay(20L)
                true
            }
            Log.i(
                DFU_TRACE_TAG,
                "controller reconnect ready=$ready connected=$mConnected generation=$gattServicesGeneration " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )
            check(ready == true) { "Android DFU reconnect timeout" }
            requireDfuSerialNotifications()
        } finally {
            Log.i(
                DFU_TRACE_TAG,
                "controller reconnect finish connected=$mConnected generation=$gattServicesGeneration " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )
            dfuReconnectActive = false
        }
    }

    internal suspend fun dfuAwaitReconnect() {
        val startedAt = System.currentTimeMillis()
        dfuReconnectActive = true
        val initialGeneration = gattServicesGeneration
        try {
            Log.i(
                "DFU_METRIC",
                "post_crc_reconnect_wait start connected=$mConnected generation=$initialGeneration"
            )
            if (!mConnected) {
                reconnectThreadFlag = true
                reconnectThread()
            }
            val automaticReconnectObserved = withTimeoutOrNull(6_000L) {
                while (!mConnected || gattServicesGeneration <= initialGeneration) delay(20L)
                true
            } == true
            Log.i(
                DFU_TRACE_TAG,
                "controller post_crc automatic_observed=$automaticReconnectObserved connected=$mConnected " +
                    "initial_generation=$initialGeneration current_generation=$gattServicesGeneration " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )

            if (!automaticReconnectObserved) {
                // COMPLITE_CRC may reset and reconnect before this coroutine is
                // entered. In that race the generation captured above already
                // belongs to the main application, so waiting for a newer one
                // can never succeed. Refresh the connection once in software;
                // this also re-enables notifications on the new GATT database.
                Log.i(
                    "DFU_METRIC",
                    "post_crc_reconnect_wait automatic_event_missed=true forcing_refresh=true"
                )
                val generationBeforeRefresh = gattServicesGeneration
                Log.i(
                    DFU_TRACE_TAG,
                    "controller post_crc forced_refresh start connected=$mConnected generation=$generationBeforeRefresh"
                )
                mBluetoothLeService?.disconnect()
                val disconnected = withTimeoutOrNull(2_000L) {
                    while (mConnected) delay(20L)
                    true
                }
                Log.i(DFU_TRACE_TAG, "controller post_crc forced_disconnect_wait=$disconnected connected=$mConnected")
                reconnectThreadFlag = true
                reconnectThread()
                val refreshedConnection = withTimeoutOrNull(15_000L) {
                    while (!mConnected || gattServicesGeneration <= generationBeforeRefresh) {
                        delay(20L)
                    }
                    true
                }
                Log.i(
                    DFU_TRACE_TAG,
                    "controller post_crc forced_refresh result=$refreshedConnection connected=$mConnected " +
                        "before_generation=$generationBeforeRefresh current_generation=$gattServicesGeneration " +
                        "elapsed_ms=${System.currentTimeMillis() - startedAt}"
                )
                check(refreshedConnection == true) { "Android DFU post-CRC reconnect timeout" }
            }
            requireDfuSerialNotifications()
            Log.i(
                "DFU_METRIC",
                "post_crc_reconnect_wait complete connected=$mConnected generation=$gattServicesGeneration"
            )
        } finally {
            Log.i(
                DFU_TRACE_TAG,
                "controller post_crc finish connected=$mConnected generation=$gattServicesGeneration " +
                    "elapsed_ms=${System.currentTimeMillis() - startedAt}"
            )
            dfuReconnectActive = false
        }
    }

    private suspend fun requireDfuSerialNotifications() {
        check(
            enableNotifyAndAwaitResponse(
                uuid = SERIALPORTCHAR_UUID,
                timeoutMs = 500L,
                attempts = 10,
                baseDelayMs = 20L
            )
        ) { "Android DFU SERIALPORT notification subscription failed" }
        Log.i("DFU_METRIC", "serial_notifications_ready=true")
        Log.i(
            DFU_TRACE_TAG,
            "controller serial_notifications_ready connected=$mConnected generation=$gattServicesGeneration"
        )
    }

    internal fun setReconnectThreadFlag(reconnectThreadFlag: Boolean) {
        this.reconnectThreadFlag = reconnectThreadFlag
    }

    private fun buildV3InitRequests(): List<V3InitRequest> {
        val getSerialNumberPacket = WidgetCommandBridgeV3.buildReadRequest(
            DEVICE_INFORMATION.number.toInt(),
            SET_SERIAL_NUMBER.number
        ) ?: requestWithCommand(DEVICE_INFORMATION.number.toInt(), GET_SERIAL_NUMBER.number)

        return listOf(
            V3InitRequest(
                packet = request(PWCE_GET_THRESHOLD_VALUE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_THRESHOLD_VALUE.number.toInt()
            ),
            V3InitRequest(
                packet = requestWithCommand(
                    EMG_MASTER_CONTROL.number.toInt(),
                    EMCE_GET_EMG_GAIN_VALUE.number.toInt()
                ),
                expectedResponseCommand = EMG_MASTER_CONTROL.number.toInt(),
                expectedResponseSubcommand = EMCE_GET_EMG_GAIN_VALUE.number.toInt()
            ),
            V3InitRequest(
                packet = requestWithCommand(
                    EMG_MASTER_CONTROL.number.toInt(),
                    EMCE_GET_EMG_MODE.number.toInt()
                ),
                expectedResponseCommand = EMG_MASTER_CONTROL.number.toInt(),
                expectedResponseSubcommand = EMCE_GET_EMG_MODE.number.toInt()
            ),
            V3InitRequest(
                packet = requestWithCommand(
                    EMG_MASTER_CONTROL.number.toInt(),
                    EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt()
                ),
                expectedResponseCommand = EMG_MASTER_CONTROL.number.toInt(),
                expectedResponseSubcommand = EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt()
            ),
            V3InitRequest(
                packet = request(PWCE_GET_EMG_CHANGE_GESTURE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_EMG_CHANGE_GESTURE.number.toInt()
            ),
            V3InitRequest(
                packet = requestWithCommand(
                    GUI_CONTROL.number.toInt(),
                    GMCE_GET_SCREEN_TIMEOUT.number.toInt()
                ),
                expectedResponseCommand = GUI_CONTROL.number.toInt(),
                expectedResponseSubcommand = GMCE_GET_SCREEN_TIMEOUT.number.toInt()
            ),
            V3InitRequest(
                packet = request(PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt()
            ),
            V3InitRequest(
                packet = requestWithCommand(
                    GUI_CONTROL.number.toInt(),
                    GMCE_GET_LEFT_RIGHT_HAND.number.toInt()
                ),
                expectedResponseCommand = GUI_CONTROL.number.toInt(),
                expectedResponseSubcommand = GMCE_GET_LEFT_RIGHT_HAND.number.toInt()
            ),
            V3InitRequest(
                packet = requestWithCommand(
                    GUI_CONTROL.number.toInt(),
                    GMCE_GET_BATTERY.number.toInt()
                ),
                expectedResponseCommand = GUI_CONTROL.number.toInt(),
                expectedResponseSubcommand = GMCE_GET_BATTERY.number.toInt()
            ),
            V3InitRequest(
                packet = request(PWCE_GET_HAND_CONTROL_MODE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_HAND_CONTROL_MODE.number.toInt()
            ),
            V3InitRequest(
                packet = request(PWCE_GET_GESTURE_CHANGE_MODE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_GESTURE_CHANGE_MODE.number.toInt()
            ),
            V3InitRequest(
                packet = request(PWCE_GET_SPEED_SETTINGS.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_SPEED_SETTINGS.number.toInt()
            ),
            V3InitRequest(
                packet = request(PWCE_GET_FORCE_SETTINGS.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_FORCE_SETTINGS.number.toInt()
            ),
            V3InitRequest(
                packet = getSerialNumberPacket,
                expectedResponseCommand = DEVICE_INFORMATION.number.toInt(),
                expectedResponseSubcommand = GET_SERIAL_NUMBER.number
            )
        )
    }

    private fun resetV3InitProgressTracking() {
        synchronized(v3InitProgressLock) {
            v3InitExpectedResponses.clear()
            v3InitProgressTotal = 0
            v3InitProgressCurrent = 0
            v3InitTrackingActive = false
            v3InitCompletionEmitted = false
        }
        UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(current = 0, total = 0)
    }

    private fun startV3InitProgressTracking(requests: List<V3InitRequest>) {
        synchronized(v3InitProgressLock) {
            v3InitExpectedResponses.clear()
            requests.forEach { request ->
                v3InitExpectedResponses.add(v3InitResponseKey(request.expectedResponseCommand, request.expectedResponseSubcommand))
            }
            v3InitProgressTotal = requests.size
            v3InitProgressCurrent = 0
            v3InitTrackingActive = requests.isNotEmpty()
            v3InitCompletionEmitted = false
        }
        UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(
            current = 0,
            total = requests.size
        )
    }

    private fun handleV3InitResponseProgress(data: ByteArray) {
        if (!UiState.isInterfaceV3Activated) return

        val parsed = parseCommandAndSubcommand(data) ?: return
        var progressSnapshot: WidgetsLoadingProgress? = null
        var shouldEmitCompletion = false

        synchronized(v3InitProgressLock) {
            if (!v3InitTrackingActive) return

            val key = v3InitResponseKey(parsed.first, parsed.second)
            if (!v3InitExpectedResponses.remove(key)) return

            v3InitProgressCurrent = (v3InitProgressCurrent + 1).coerceAtMost(v3InitProgressTotal)
            progressSnapshot = WidgetsLoadingProgress(
                current = v3InitProgressCurrent,
                total = v3InitProgressTotal
            )

            if (v3InitProgressCurrent >= v3InitProgressTotal && !v3InitCompletionEmitted) {
                v3InitCompletionEmitted = true
                v3InitTrackingActive = false
                shouldEmitCompletion = true
            }
        }

        progressSnapshot?.let { UiState.widgetsLoadingProgressFlow.value = it }

        if (shouldEmitCompletion) {
            UiState.widgetsLoadingFlow.tryEmit(Unit)
            onConnectedListener?.invoke()
            downloadSettingsProfilesAfterV3Init()
        }
    }

    private fun downloadSettingsProfilesAfterV3Init() {
        if (settingsProfileDownloadedForConnection) return
        val serial = SettingsProfileManager.serial().trim()
        if (serial.isBlank()) {
            platformLog(SETTINGS_PROFILE_DOWNLOAD_LOG_TAG, "skip: device serial is not loaded from GET_SERIAL_NUMBER")
            return
        }

        settingsProfileDownloadedForConnection = true
        val lang = main.locate.takeIf { it.isNotBlank() } ?: "en"
        platformLog(SETTINGS_PROFILE_DOWNLOAD_LOG_TAG, "start: serial=$serial lang=$lang")

        main.lifecycleScope.launch {
            runCatching {
                settingsProfileReceiver.downloadAndApplyForSerial(
                    serial = serial,
                    lang = lang
                )
            }.onSuccess { result ->
                platformLog(
                    SETTINGS_PROFILE_DOWNLOAD_LOG_TAG,
                    "success: deviceId=${result.deviceId} profileCount=${result.state.profileCount} activeProfile=${result.state.activeProfileId} applied=${result.applyValues.size}"
                )
            }.onFailure { error ->
                platformLog(
                    SETTINGS_PROFILE_DOWNLOAD_LOG_TAG,
                    "failed: ${error.message ?: error::class.simpleName}"
                )
            }
        }
    }

    private fun parseCommandAndSubcommand(data: ByteArray): Pair<Int, Int>? {
        if (data.size < 3) return null

        val isLongPacket = (data[0].toInt() and 0x80) != 0
        val command = data[1].toInt() and 0xFF
        val subcommandIndex = if (isLongPacket) 5 else 2
        if (data.size <= subcommandIndex) return null

        val subcommand = data[subcommandIndex].toInt() and 0xFF
        return command to subcommand
    }

    private fun v3InitResponseKey(command: Int, subcommand: Int): String {
        return "$command:$subcommand"
    }

    private companion object {
        private const val SETTINGS_PROFILE_DOWNLOAD_LOG_TAG = "SettingsProfileDownload"
        private const val DFU_TRACE_TAG = "DFU_V2_TRACE"
        private const val PCCE_RESET_DEVICE = 2
        private const val FIRMWARE_SWITCH_PREFLIGHT_COMMAND_SETTLE_MS = 500L
        private const val FIRMWARE_SWITCH_PREFLIGHT_DISCONNECT_TIMEOUT_MS = 2_000L
        private const val FIRMWARE_SWITCH_PREFLIGHT_RECONNECT_TIMEOUT_MS = 15_000L
        private const val FIRMWARE_SWITCH_FLASH_QUIET_WINDOW_MS = 1_500L
        private const val FIRMWARE_SWITCH_COMMAND_SETTLE_MS = 100L
        private const val FIRMWARE_SWITCH_DEVICE_RESET_TIMEOUT_MS = 7_000L
        private const val FIRMWARE_SWITCH_POST_RESET_HANDOFF_MS = 250L
        private const val FIRMWARE_SWITCH_FALLBACK_QUIET_WINDOW_MS = 1_500L
    }
}
