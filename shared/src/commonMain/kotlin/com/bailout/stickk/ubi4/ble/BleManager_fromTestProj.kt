package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

/** Информация об обнаруженном устройстве */
expect class BleDevice_fromTestProj(id: String, name: String?) {
    val id: String        // идентификатор устройства
    val name: String?     // имя, может быть null
}

/** Менеджер для работы с Bluetooth LE */
expect class BleManager_fromTestProj() {
    /** Начать сканирование. Каждый найденный девайс передаётся в [onDeviceFound]. */
    fun startScan(onDeviceFound: (BleDevice_fromTestProj) -> Unit)

    /** Остановить сканирование. */
    fun stopScan()

    /**
     * Отправить [data] в характеристику [characteristicUuid]
     * устройства [device] (или по его id).
     */
    fun sendBytes(
        device: BleDevice_fromTestProj,
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    )
}
