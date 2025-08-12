package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_SERVICE
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBPeripheralStateConnected
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.darwin.NSObject
import kotlin.native.internal.test.main


/** Информация об обнаруженном устройстве */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleDeviceKmm
    actual constructor(id: String, name: String?, rssi: Int) {
    actual val id: String get() = peripheral.identifier.UUIDString()
    actual val name: String? get() = peripheral.name
    actual val rssi: Int = rssi
    internal lateinit var peripheral: CBPeripheral
//    private var selectedDevice: CBPeripheral?

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
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleManagerKmm actual constructor() {
    private var connectedDevice: BleDeviceKmm? = null
    private var onDeviceCallback: ((BleDeviceKmm) -> Unit)? = null
    private val discovered = mutableMapOf<String, CBPeripheral>()

    private data class PendingWrite(
        val peripheral: CBPeripheral,
        val serviceUuid: String,
        val characteristicUuid: String,
        val data: ByteArray
    )
    private var pendingWrite: PendingWrite? = null

    @OptIn(ExperimentalForeignApi::class)
    private val delegate = object : NSObject(),
        CBCentralManagerDelegateProtocol,
        CBPeripheralDelegateProtocol {

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
            connectedDevice = device
            discovered[device.id] = didDiscoverPeripheral
            onDeviceCallback?.invoke(device)
        }

        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            print("BLE-CONNECT коннект состоялся!!!")
            print("BLE-CONNECT ${pendingWrite?.peripheral}")
            pendingWrite?.let {
                didConnectPeripheral.delegate = this
                print("BLE-CONNECT старт поиска сервисов ${it.serviceUuid}")
                didConnectPeripheral.discoverServices(listOf(CBUUID.UUIDWithString(it.serviceUuid)))
            }
        }

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


        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverServices: NSError?
        ) {
            //
            print("BLE-CONNECT начало процесса поиска сервисов")
            val write = pendingWrite ?: return
            val service = peripheral.services?.firstOrNull {
                (it as CBService).UUID.UUIDString() == write.serviceUuid
            } as? CBService ?: return
            peripheral.discoverCharacteristics(
                listOf(CBUUID.UUIDWithString(write.characteristicUuid)),
                service
            )
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?
        ) {
            //
            print("BLE-CONNECT начало процесса поиска характеристик")
            val write = pendingWrite ?: return
            val characteristic = didDiscoverCharacteristicsForService.characteristics?.firstOrNull {
                (it as CBCharacteristic).UUID.UUIDString() == write.characteristicUuid
            } as? CBCharacteristic ?: return
            val bytes = write.data
            val nsData = bytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
            print("BLE-CONNECT тестовая отправка команды")
            peripheral.writeValue(nsData, characteristic, CBCharacteristicWriteWithResponse)
            pendingWrite = null
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
        platformLog("sendBytesKmm", "dataString = $receiveDataString")
        val peripheral = connectedDevice?.peripheral
        try {
            pendingWrite = PendingWrite(peripheral!!, MAIN_CHANNEL_SERVICE, MAIN_CHANNEL_CHARACTERISTIC, data)
            if (peripheral.state != CBPeripheralStateConnected) {
                manager.connectPeripheral(peripheral, null)
            } else {
                peripheral.delegate = delegate
                peripheral.discoverServices(listOf(CBUUID.UUIDWithString(MAIN_CHANNEL_SERVICE)))
            }
        } catch (e: Exception) {
            print("peripheral оказался пустым в actual реализации функции sendBytesKmm для ios")
        }
    }
}