package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

import kotlinx.cinterop.ExperimentalForeignApi
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


/** Информация об обнаруженном устройстве */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleDevice_fromTestProj
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
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class BleManager_fromTestProj actual constructor() {
    private var onDeviceCallback: ((BleDevice_fromTestProj) -> Unit)? = null
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
//            onDeviceCallback?.invoke(BleDevice(didDiscoverPeripheral))
            val device = BleDevice_fromTestProj(didDiscoverPeripheral, RSSI.intValue)
            discovered[device.id] = didDiscoverPeripheral
            onDeviceCallback?.invoke(device)
        }

        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            pendingWrite?.let {
                didConnectPeripheral.delegate = this
                didConnectPeripheral.discoverServices(listOf(CBUUID.UUIDWithString(it.serviceUuid)))
            }
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverServices: NSError?
        ) {
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
            val write = pendingWrite ?: return
            val characteristic = didDiscoverCharacteristicsForService.characteristics?.firstOrNull {
                (it as CBCharacteristic).UUID.UUIDString() == write.characteristicUuid
            } as? CBCharacteristic ?: return
            val bytes = write.data
            val nsData = bytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
            peripheral.writeValue(nsData, characteristic, CBCharacteristicWriteWithResponse)
            pendingWrite = null
        }
    }
    private val manager = CBCentralManager(delegate, queue = null)

    @Suppress("unused")
    actual fun startScan(onDeviceFound: (BleDevice_fromTestProj) -> Unit) {
        println("startScan from kmm 3")
        onDeviceCallback = onDeviceFound
        if (manager.state == CBManagerStatePoweredOn) {
            manager.scanForPeripheralsWithServices(null, null)
        }
    }

    @Suppress("unused")
    actual fun stopScan() {
        onDeviceCallback = null
        manager.stopScan()
    }

    @Suppress("unused")
    actual fun sendBytes(
        device: BleDevice_fromTestProj,
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    ) {
        val peripheral = device.peripheral
        pendingWrite = PendingWrite(peripheral, serviceUuid, characteristicUuid, data)
        if (peripheral.state != CBPeripheralStateConnected) {
            manager.connectPeripheral(peripheral, null)
        } else {
            peripheral.delegate = delegate
            peripheral.discoverServices(listOf(CBUUID.UUIDWithString(serviceUuid)))
        }
    }
}