package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

/** Информация об обнаруженном устройстве */
actual class BleDevice_fromTestProj actual constructor (
    actual val id: String,
    actual val name: String?
)

/** Менеджер для работы с Bluetooth LE */
actual class BleManager_fromTestProj actual constructor() {
    /** Начать сканирование. Каждый найденный девайс передаётся в [onDeviceFound]. */
    actual fun startScan(onDeviceFound: (BleDevice_fromTestProj) -> Unit) {}

    /** Остановить сканирование. */
    actual fun stopScan() {}

    /**
     * Отправить [data] в характеристику [characteristicUuid]
     * устройства [device] (или по его id).
     */
    actual fun sendBytes(
        device: BleDevice_fromTestProj,
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray,
    ) {}

}