package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SENSORS_STREAM_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.GUI_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.guiModuleControlEnum.*
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
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
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
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
import com.bailout.stickk.ubi4.utility.synchronized
import kotlin.collections.ArrayDeque


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
                println("Ошибка записи: ${error.localizedDescription}")
            } else {
                didWriteValueForCharacteristic.value?.let { data: NSData ->
                    platformLog("sendBytesKmm", "Тут запись завершена успешно: ${EncodeByteToHex.bytesToHexString(data.toByteArray())}")
                }
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
        bleCommandExecutor.bleCommandWithQueue(data, command, typeCommand, onChunkSent)
    }

    internal fun dispatchSendBytesKmm(
        data: ByteArray,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    ) {
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
        characteristicsMass.forEach { c ->
//            platformLog(
//                "sendBytesKmm",
//                "characteristicsMass = ${c.UUID.UUIDString()} сравниваем с ${command.uppercase()}"
//            )
            if (c.UUID.UUIDString() == command.uppercase()) {
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
            connectionScope.launch {
                UiState.startupInProgress.value = false
                BLEState.bleParserV3.generatedHardcodeWidgets()
                UiState.widgetsLoadingFlow.emit(Unit)
                initRequestsV3()
            }
        }

        onCharacteristicsReady?.invoke()
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

            sendBytesKmm(BLECommandsV3.request(PWCE_GET_THRESHOLD_VALUE.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            sendBytesKmm(BLECommandsV3.request(PWCE_GET_EMG_GAIN_VALUE.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            sendBytesKmm(BLECommandsV3.request(PWCE_GET_EMG_CHANGE_GESTURE.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            sendBytesKmm(BLECommandsV3.requestWithCommand(GUI_CONTROL.number.toInt(), GMCE_GET_SCREEN_TIMEOUT.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            sendBytesKmm(BLECommandsV3.request(PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            sendBytesKmm(BLECommandsV3.requestWithCommand(GUI_CONTROL.number.toInt(), GMCE_GET_LEFT_RIGHT_HAND.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            sendBytesKmm(BLECommandsV3.request(PWCE_GET_HAND_CONTROL_MODE.number.toInt()), SERIALPORTCHAR_UUID, WRITE) {}
            return
        }
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
