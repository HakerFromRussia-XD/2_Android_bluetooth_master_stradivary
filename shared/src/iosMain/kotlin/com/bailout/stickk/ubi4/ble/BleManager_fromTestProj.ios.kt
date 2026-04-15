package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SENSORS_STREAM_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.models.other.WidgetsLoadingProgress
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.darwin.NSObject
import platform.posix.memcpy


/** Информация об обнаруженном устройстве */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleDeviceKmm
    actual constructor(id: String, name: String?, rssi: Int) {
    actual val id: String = id
    actual val name: String? = name
    actual val rssi: Int = rssi
    internal lateinit var peripheral: CBPeripheral

    internal constructor(
        peripheral: CBPeripheral,
        rssi: Int,
        discoveredName: String? = peripheral.name
    ) :
            this(
                id = peripheral.identifier.UUIDString(),
                name = discoveredName,
                rssi = rssi
            ) {
        this.peripheral = peripheral
    }
}

/** Менеджер для работы с Bluetooth LE */
@ExperimentalForeignApi
@OptIn(ExperimentalForeignApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleManagerKmm actual constructor() {
    private data class InitRequestV3(
        val packet: ByteArray,
        val expectedResponseCommand: Int,
        val expectedResponseSubcommand: Int
    )

    private var connectedDevice: BleDeviceKmm? = null
    private var onDeviceCallback: ((BleDeviceKmm) -> Unit)? = null
    private val discovered = mutableMapOf<String, CBPeripheral>()
    private val servicesMass = mutableListOf<CBService>()
    private val characteristicsMass = mutableListOf<CBCharacteristic>()
    private var selectedDevice: CBPeripheral? = null

    private val chunkCallbackLock = Any()
    private val onChunkSentQueue = ArrayDeque<() -> Unit>()
    private var onCharacteristicsReady: (() -> Unit)? = null
    private var didNotifyCharacteristicsReady = false
    private val connectionScope = MainScope()
    private val pendingNotifyAcks = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private var pendingDeviceDataResponseAck: CompletableDeferred<Boolean>? = null
    private val v3InitProgressLock = Any()
    private val v3InitExpectedResponses = mutableSetOf<String>()
    private var v3InitProgressTotal: Int = 0
    private var v3InitProgressCurrent: Int = 0
    private var v3InitTrackingActive: Boolean = false
    private var v3InitCompletionEmitted: Boolean = false
    private var expectedServicesCount = 0
    private var discoveredServicesWithCharacteristics = 0
    private var reconnectTargetUuid: String? = null
    private var autoReconnectEnabled = false
    private var reconnectScanActive = false

    @OptIn(ExperimentalForeignApi::class)
    private val delegate = object : NSObject(),
        CBCentralManagerDelegateProtocol,
        CBPeripheralDelegateProtocol {
        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            platformLog("[BLE-CONNECT]","подключение не удалось!!!")
            BLEState.publishError()
            startAutoReconnect(didFailToConnectPeripheral)
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            platformLog("[BLE-CONNECT]","устройство отключено!!!")
            BLEState.publishDisconnect()
            startAutoReconnect(didDisconnectPeripheral)
        }

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            // здесь можно отследить включение Bluetooth
            if (central.state == CBManagerStatePoweredOn) {
                if (onDeviceCallback != null) {
                    central.scanForPeripheralsWithServices(null, null)
                }
                if (autoReconnectEnabled && reconnectTargetUuid != null) {
                    startReconnectScan()
                }
            } else {
                reconnectScanActive = false
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            // вызывается каждый раз, когда находится новое устройство
            val advertisedName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
            val resolvedName = advertisedName?.takeIf { it.isNotBlank() } ?: didDiscoverPeripheral.name
            val device = BleDeviceKmm(
                peripheral = didDiscoverPeripheral,
                rssi = RSSI.intValue,
                discoveredName = resolvedName
            )
            discovered[device.id] = didDiscoverPeripheral

            val targetUuid = reconnectTargetUuid
            if (autoReconnectEnabled && targetUuid != null && device.id.equals(targetUuid, ignoreCase = true)) {
                platformLog("[BLE-RECONNECT]", "target device found in scan, reconnecting: ${device.id}")
                if (reconnectScanActive) {
                    central.stopScan()
                    reconnectScanActive = false
                }
                BLEState.publishConnecting()
                central.connectPeripheral(didDiscoverPeripheral, options = null)
                return
            }

            onDeviceCallback?.invoke(device)
        }

        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            platformLog("[BLE-CONNECT]","коннект состоялся!!!")
            if (reconnectScanActive) {
                central.stopScan()
                reconnectScanActive = false
            }
            BLEState.publishConnecting()
            connectedDevice = BleDeviceKmm(didConnectPeripheral, 0)
            selectedDevice = didConnectPeripheral
            reconnectTargetUuid = didConnectPeripheral.identifier.UUIDString()
            autoReconnectEnabled = true
            didConnectPeripheral.delegate = this
            didNotifyCharacteristicsReady = false
            servicesMass.clear()
            characteristicsMass.clear()
            expectedServicesCount = 0
            discoveredServicesWithCharacteristics = 0
            pendingNotifyAcks.clear()
            pendingDeviceDataResponseAck = null
            resetV3InitProgressTracking()
            didConnectPeripheral.discoverServices(null)
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverServices: NSError?
        ) {
            platformLog("[BLE-CONNECT]","начало процесса поиска сервисов")
            val services = (peripheral.services as? List<*>) ?: emptyList<Any?>()
            expectedServicesCount = services.size
            if (expectedServicesCount == 0) {
                notifyCharacteristicsReadyOnce()
                return
            }
            services.forEach { any ->
                val service = any as CBService
                servicesMass.add(service)
                peripheral.discoverCharacteristics(characteristicUUIDs = null, forService = service)
            }
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?
        ) {
            platformLog("[BLE-CONNECT]","начало процесса поиска характеристик")
            (didDiscoverCharacteristicsForService.characteristics as? List<*>)?.forEach {
                val c = it as CBCharacteristic
                characteristicsMass.add(c)
                if (!UiState.isInterfaceV3Activated) {
                    peripheral.setNotifyValue(true, forCharacteristic = c)
                }
            }

            discoveredServicesWithCharacteristics += 1
            if (discoveredServicesWithCharacteristics >= expectedServicesCount) {
                notifyCharacteristicsReadyOnce()
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateNotificationStateForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            val key = didUpdateNotificationStateForCharacteristic.UUID.UUIDString().lowercase()
            val enabled = error == null
            pendingNotifyAcks.remove(key)?.complete(enabled)
        }

        // Метод для обработки успешной записи или ошибки
        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didWriteValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            if (error != null) {
                platformLog("[V3-SLIDER][BLE-WRITE]", "write error=${error.localizedDescription}")
            } else {
                didWriteValueForCharacteristic.value?.let { data: NSData ->
                    platformLog("sendBytesKmm", "Тут запись завершена успешно: ${EncodeByteToHex.bytesToHexString(data.toByteArray())}")
                }
                platformLog("[V3-SLIDER][BLE-WRITE]", "write success characteristic=${didWriteValueForCharacteristic.UUID.UUIDString()}")
//                onChunkSent?.invoke()
                val callback = synchronized(chunkCallbackLock) {
                    if (onChunkSentQueue.isNotEmpty()) onChunkSentQueue.removeFirst() else null
                }
                callback?.invoke()
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            var dataCount = 0
            didUpdateValueForCharacteristic.value?.let { data: NSData ->
                dataCount = data.length.toInt()
                platformLog("[BLE-CONNECT]","приём dataCount = $dataCount")
                platformLog("sendBytesKmm", "А тут мы обрабатываем принятые данные: ${EncodeByteToHex.bytesToHexString(data.toByteArray())}")
                val characteristicUuid = didUpdateValueForCharacteristic.UUID.UUIDString().lowercase()
                if (characteristicUuid == SERIALPORTCHAR_UUID.lowercase()) {
                    pendingDeviceDataResponseAck?.complete(true)
                }
                val bytes = data.toByteArray()
                handleV3InitResponseProgress(bytes, characteristicUuid)
                if (UiState.isInterfaceV3Activated) {
                    when (characteristicUuid) {
                        SERIALPORTCHAR_UUID.lowercase() -> BLEState.bleParserV3.parseReceivedData(bytes)
                        MAIN_CHANNEL_CHARACTERISTIC.lowercase(),
                        SENSORS_STREAM_UUID.lowercase() -> BLEState.bleParserV3.parseReceivedSensorsData(bytes)
                        else -> platformLog("[BLE-PARSER-ROUTER]", "route=V3 skip uuid=$characteristicUuid")
                    }
                } else {
                    BlePacketParserRouterV3.parseIncoming(bytes)
                }
            }
        }
    }
    private val manager = CBCentralManager(delegate, queue = null)

    @Suppress("unused")
    actual fun startScanKmm(onDeviceFound: (BleDeviceKmm) -> Unit) {
        println("startScan from kmm 3")
        onDeviceCallback = onDeviceFound
        if (manager.state == CBManagerStatePoweredOn) {
            manager.scanForPeripheralsWithServices(null, null)
        }
    }

    actual fun connectToDevice(uuid: String) {
        reconnectTargetUuid = uuid
        autoReconnectEnabled = true

        val discoveredPeripheral = discovered.entries
            .firstOrNull { it.key.equals(uuid, ignoreCase = true) }
            ?.value

        if (discoveredPeripheral != null) {
            platformLog("[BLE-CONNECT]","from kmm ALL DEVICES reconnect target found, uuid=$uuid")
            BLEState.publishConnecting()
            manager.connectPeripheral(discoveredPeripheral, options = null)
            return
        }

        platformLog("[BLE-RECONNECT]", "connect target not in discovered cache, starting scan for uuid=$uuid")
        startReconnectScan()
    }

    actual fun setOnCharacteristicsReadyListener(onReady: () -> Unit) {
        onCharacteristicsReady = onReady
        if (didNotifyCharacteristicsReady) {
            onReady()
        }
    }

    @Suppress("unused")
    actual fun stopScanKmm() {
        onDeviceCallback = null
        if (!reconnectScanActive) {
            manager.stopScan()
        }
    }


    internal val bleCommandExecutor = BleCommandExecutorIos { byteArray, command, type, onChunkSent ->
        dispatchSendBytesKmm(byteArray, command, type, onChunkSent)
    }

    @Suppress("unused")
    actual fun sendBytesKmm(
        data: ByteArray,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    ) {
//        this.onChunkSent = onChunkSent
        synchronized(chunkCallbackLock) {
            onChunkSentQueue.addLast(onChunkSent)
        }
        platformLog(
            "[V3-SLIDER][BLE-QUEUE]",
            "enqueue command=$command type=$typeCommand payload=${EncodeByteToHex.bytesToHexString(data)} queueSize=${onChunkSentQueue.size}"
        )
        bleCommandExecutor.bleCommandWithQueue(data, command, typeCommand, onChunkSent)
    }

    actual fun restartV3Synchronization() {
        if (UiState.isInterfaceV3Activated) {
            launchV3SynchronizationPipeline()
            return
        }

        sendBytesKmm(
            data = BLECommands.requestInicializeInformation(),
            command = MAIN_CHANNEL_CHARACTERISTIC,
            typeCommand = WRITE,
            onChunkSent = {}
        )
    }

    internal fun dispatchSendBytesKmm(
        data: ByteArray,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    ) {
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
        val normalizedCommand = command.lowercase()
        platformLog(
            "[V3-SLIDER][BLE-DISPATCH]",
            "dispatch command=$command type=$typeCommand payload=$receiveDataString charsCount=${characteristicsMass.size}"
        )

        var foundCharacteristic = false
        characteristicsMass.forEach { c ->
            val characteristicUuid = c.UUID.UUIDString()
            val normalizedCharacteristicUuid = characteristicUuid.lowercase()
            if (normalizedCharacteristicUuid == normalizedCommand) {
                foundCharacteristic = true
                platformLog(
                    "[V3-SLIDER][BLE-DISPATCH]",
                    "matched characteristic=$characteristicUuid for command=$command"
                )
                when (typeCommand) {
                    READ -> {
                        platformLog("sendBytesKmm", "читаем данные: $receiveDataString")
                    }

                    WRITE -> {
                        selectedDevice?.writeValue(data = data.toNSData(), forCharacteristic = c, type = CBCharacteristicWriteWithResponse)
                        platformLog("sendBytesKmm", "отправляем данные: $receiveDataString")
                    }

                    NOTIFY -> {
                        selectedDevice?.setNotifyValue(true, forCharacteristic = c)
                        platformLog("sendBytesKmm", "запускаем нотификацию: $receiveDataString")
                    }
                }
            }
        }

        if (!foundCharacteristic) {
            val available = characteristicsMass.joinToString(separator = ",") { it.UUID.UUIDString() }
            platformLog(
                "[V3-SLIDER][BLE-DISPATCH]",
                "NO_MATCH command=$command type=$typeCommand available=[$available] payload=$receiveDataString"
            )
        }
    }

    fun ByteArray.toNSData(): NSData {
        // Используем usePinned для создания указателя на массив байтов
        return this.usePinned { pinned ->
            // Печатаем CPointer<Byte> в качестве указателя на данные
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }


    @OptIn(ExperimentalForeignApi::class)
    fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        val bytes = this.bytes ?: return ByteArray(0)
        return ByteArray(length).also { array ->
            array.usePinned { pinned ->
                memcpy(pinned.addressOf(0), bytes, length.convert())
            }
        }
    }

    private fun notifyCharacteristicsReadyOnce() {
        if (didNotifyCharacteristicsReady) return
        didNotifyCharacteristicsReady = true
        BLEState.publishReady()

        if (UiState.isInterfaceV3Activated) {
            launchV3SynchronizationPipeline()
        }

        onCharacteristicsReady?.invoke()
    }

    private fun launchV3SynchronizationPipeline() {
        connectionScope.launch {
            UiState.startupInProgress.value = false
            BLEState.bleParserV3.generatedHardcodeWidgets()
            initRequestsV3()
        }
    }

    private fun findCharacteristic(uuid: String): CBCharacteristic? {
        val normalized = uuid.lowercase()
        return characteristicsMass.firstOrNull { it.UUID.UUIDString().lowercase() == normalized }
    }

    private suspend fun enableNotifyAndAwaitResponse(
        uuid: String,
        timeoutMs: Long = 250L,
        attempts: Int = 10,
        baseDelayMs: Long = 10L
    ): Boolean {
        val key = uuid.lowercase()
        repeat(attempts) { index ->
            val characteristic = findCharacteristic(uuid)
            if (characteristic == null || selectedDevice == null) {
                if (index < attempts - 1) {
                    delay(baseDelayMs.toLong() * (index + 1))
                }
                return@repeat
            }

            val ack = CompletableDeferred<Boolean>()
            pendingNotifyAcks[key] = ack
            selectedDevice?.setNotifyValue(true, forCharacteristic = characteristic)
            val success = withTimeoutOrNull(timeoutMs) { ack.await() } ?: false
            if (pendingNotifyAcks[key] === ack) {
                pendingNotifyAcks.remove(key)
            }

            if (success) return true
            if (index < attempts - 1) {
                delay(baseDelayMs.toLong() * (index + 1))
            }
        }
        return false
    }

    private suspend fun requestDeviceDataAndAwaitResponse(timeoutMs: Long = 250L): Boolean {
        val responseAck = CompletableDeferred<Boolean>()
        pendingDeviceDataResponseAck = responseAck
        sendBytesKmm(
            data = BLECommandsV3.requestDeviceData(),
            command = SERIALPORTCHAR_UUID,
            typeCommand = WRITE,
            onChunkSent = {}
        )
        val responseReceived = withTimeoutOrNull(timeoutMs) { responseAck.await() } ?: false
        if (pendingDeviceDataResponseAck === responseAck) {
            pendingDeviceDataResponseAck = null
        }
        return responseReceived
    }

    private suspend fun initRequestsV3() {
        while (true) {
            resetV3InitProgressTracking()

            val serialNotifyEnabled = enableNotifyAndAwaitResponse(SERIALPORTCHAR_UUID)
            if (!serialNotifyEnabled) {
                platformLog("BLEParserV3", "Не удалось подтвердить включение notify для SERIALPORTCHAR_UUID")
                continue
            }

            val gotDeviceDataResponse = requestDeviceDataAndAwaitResponse()
            if (!gotDeviceDataResponse) {
                platformLog("BLEParserV3", "Ответ на requestDeviceData() не получен до включения MAIN_CHANNEL notify")
                continue
            }

            val mainChannelNotifyEnabled = enableNotifyAndAwaitResponse(MAIN_CHANNEL_CHARACTERISTIC)
            if (!mainChannelNotifyEnabled) {
                platformLog("BLEParserV3", "Не удалось подтвердить включение notify для MAIN_CHANNEL_CHARACTERISTIC")
                continue
            }

            val initRequests = buildV3InitRequests()
            startV3InitProgressTracking(initRequests)

            if (initRequests.isEmpty()) {
                UiState.widgetsLoadingFlow.emit(Unit)
                return
            }

            initRequests.forEach { request ->
                sendBytesKmm(
                    data = request.packet,
                    command = SERIALPORTCHAR_UUID,
                    typeCommand = WRITE
                ) {}
            }
            return
        }
    }

    private fun buildV3InitRequests(): List<InitRequestV3> {
        return listOf(
            InitRequestV3(
                packet = BLECommandsV3.request(PWCE_GET_THRESHOLD_VALUE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_THRESHOLD_VALUE.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.requestWithCommand(
                    EMG_MASTER_CONTROL.number.toInt(),
                    EMCE_GET_EMG_GAIN_VALUE.number.toInt()
                ),
                expectedResponseCommand = EMG_MASTER_CONTROL.number.toInt(),
                expectedResponseSubcommand = EMCE_GET_EMG_GAIN_VALUE.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.requestWithCommand(
                    EMG_MASTER_CONTROL.number.toInt(),
                    EMCE_GET_EMG_MODE.number.toInt()
                ),
                expectedResponseCommand = EMG_MASTER_CONTROL.number.toInt(),
                expectedResponseSubcommand = EMCE_GET_EMG_MODE.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.requestWithCommand(
                    EMG_MASTER_CONTROL.number.toInt(),
                    EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt()
                ),
                expectedResponseCommand = EMG_MASTER_CONTROL.number.toInt(),
                expectedResponseSubcommand = EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.request(PWCE_GET_EMG_CHANGE_GESTURE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_EMG_CHANGE_GESTURE.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.requestWithCommand(
                    GUI_CONTROL.number.toInt(),
                    GMCE_GET_SCREEN_TIMEOUT.number.toInt()
                ),
                expectedResponseCommand = GUI_CONTROL.number.toInt(),
                expectedResponseSubcommand = GMCE_GET_SCREEN_TIMEOUT.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.request(PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.requestWithCommand(
                    GUI_CONTROL.number.toInt(),
                    GMCE_GET_LEFT_RIGHT_HAND.number.toInt()
                ),
                expectedResponseCommand = GUI_CONTROL.number.toInt(),
                expectedResponseSubcommand = GMCE_GET_LEFT_RIGHT_HAND.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.requestWithCommand(
                    GUI_CONTROL.number.toInt(),
                    com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_BATTERY.number.toInt()
                ),
                expectedResponseCommand = GUI_CONTROL.number.toInt(),
                expectedResponseSubcommand = com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_BATTERY.number.toInt()
            ),
            InitRequestV3(
                packet = BLECommandsV3.request(PWCE_GET_HAND_CONTROL_MODE.number.toInt()),
                expectedResponseCommand = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                expectedResponseSubcommand = PWCE_GET_HAND_CONTROL_MODE.number.toInt()
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
        UiState.widgetsLoadingProgressFlow.value = WidgetsLoadingProgress(
            current = 0,
            total = 0
        )
    }

    private fun startV3InitProgressTracking(requests: List<InitRequestV3>) {
        synchronized(v3InitProgressLock) {
            v3InitExpectedResponses.clear()
            requests.forEach { request ->
                v3InitExpectedResponses.add(
                    v3InitResponseKey(
                        command = request.expectedResponseCommand,
                        subcommand = request.expectedResponseSubcommand
                    )
                )
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

    private fun handleV3InitResponseProgress(data: ByteArray, characteristicUuid: String) {
        if (!UiState.isInterfaceV3Activated) return
        if (characteristicUuid != SERIALPORTCHAR_UUID.lowercase()) return

        val parsed = parseCommandAndSubcommand(data) ?: return
        var progressSnapshot: WidgetsLoadingProgress? = null
        var shouldEmitCompletion = false

        synchronized(v3InitProgressLock) {
            if (!v3InitTrackingActive) return

            val key = v3InitResponseKey(parsed.first, parsed.second)
            if (!v3InitExpectedResponses.remove(key)) return

            v3InitProgressCurrent = minOf(v3InitProgressCurrent + 1, v3InitProgressTotal)
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
            connectionScope.launch {
                UiState.widgetsLoadingFlow.emit(Unit)
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

    private fun startAutoReconnect(peripheral: CBPeripheral) {
        if (!autoReconnectEnabled) {
            return
        }

        reconnectTargetUuid = peripheral.identifier.UUIDString()
        selectedDevice = null
        connectedDevice = null
        didNotifyCharacteristicsReady = false

        platformLog("[BLE-RECONNECT]", "start auto reconnect flow for ${peripheral.identifier.UUIDString()}")

        if (manager.state != CBManagerStatePoweredOn) {
            platformLog("[BLE-RECONNECT]", "bluetooth is not powered on, waiting for state update")
            return
        }

        BLEState.publishConnecting()
        manager.connectPeripheral(peripheral, options = null)
        startReconnectScan()
    }

    private fun startReconnectScan() {
        if (manager.state != CBManagerStatePoweredOn) {
            reconnectScanActive = false
            return
        }
        if (reconnectScanActive) return

        manager.scanForPeripheralsWithServices(null, null)
        reconnectScanActive = true
        platformLog("[BLE-RECONNECT]", "scan started for auto reconnect target=$reconnectTargetUuid")
    }
}
