package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFY
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.READ
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
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
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.darwin.NSObject


/** Информация об обнаруженном устройстве */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleDeviceKmm
    actual constructor(id: String, name: String?, rssi: Int) {
    actual val id: String get() = peripheral.identifier.UUIDString()
    actual val name: String? get() = peripheral.name
    actual val rssi: Int = rssi
    internal lateinit var peripheral: CBPeripheral

    internal constructor(peripheral: CBPeripheral, rssi: Int) :
            this(
                id = peripheral.identifier.UUIDString(),
                name = peripheral.name,
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

//    private data class PendingWrite(
//        val peripheral: CBPeripheral,
//        val serviceUuid: String,
//        val characteristicUuid: String,
//        val data: ByteArray
//    )
//    private var pendingWrite: PendingWrite? = null

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
            print("BLE-CONNECT подключение не удалось!!!")
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            print("BLE-CONNECT устройство отключено!!!")
        }

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            // здесь можно отследить включение Bluetooth
            if (central.state == CBManagerStatePoweredOn && onDeviceCallback != null) {
                central.scanForPeripheralsWithServices(null, null)
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            // вызывается каждый раз, когда находится новое устройство
            val device = BleDeviceKmm(didDiscoverPeripheral, RSSI.intValue)
            discovered[device.id] = didDiscoverPeripheral
            onDeviceCallback?.invoke(device)
        }

        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            print("BLE-CONNECT коннект состоялся!!!")
            connectedDevice = BleDeviceKmm(didConnectPeripheral, 0)
            selectedDevice = didConnectPeripheral
            didConnectPeripheral.delegate = this
            didConnectPeripheral.discoverServices(null)
        }


        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverServices: NSError?
        ) {
            print("BLE-CONNECT начало процесса поиска сервисов")
            (peripheral.services as? List<*>)?.forEach { any ->
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
            print("BLE-CONNECT начало процесса поиска характеристик")
            (didDiscoverCharacteristicsForService.characteristics as? List<*>)?.forEach {
                val c = it as CBCharacteristic
                characteristicsMass.add(c); peripheral.setNotifyValue(true, forCharacteristic = c)
            }
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            print("BLE-CONNECT приём по идее")
            var dataCount = 0
            didUpdateValueForCharacteristic.value?.let { data: NSData ->
                dataCount = data.length.toInt()
                print("BLE-CONNECT приём dataCount = $dataCount")
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
        var connectedDevice: CBPeripheral?
        discovered.forEach {
            if (it.value.identifier.UUIDString == uuid) {
                print("BLE-CONNECT from kmm ALL DEVICES $it сравниваем с ${uuid}")
                connectedDevice = it.value
                manager.connectPeripheral(connectedDevice!!, options = null)
            }
        }
    }

    @Suppress("unused")
    actual fun stopScanKmm() {
        onDeviceCallback = null
        manager.stopScan()
    }

    @Suppress("unused")
    actual fun sendBytesKmm(
        data: ByteArray,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    ) {
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
        val peripheral = selectedDevice

        characteristicsMass.forEach { c ->
            platformLog(
                "sendBytesKmm",
                "characteristicsMass = ${c.UUID.UUIDString()} сравниваем с $command"
            )
            if (c.UUID.UUIDString() == command) {
                when (typeCommand) {
                    READ -> {
                        platformLog("sendBytesKmm", "читаем данные: $receiveDataString")
                    }

                    WRITE -> {
                        selectedDevice?.writeValue(data = data.toNSData(), forCharacteristic = c, type = CBCharacteristicWriteWithResponse)
                        platformLog("sendBytesKmm", "отправляем данные: $receiveDataString")
                    }

                    NOTIFY -> {
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
}


