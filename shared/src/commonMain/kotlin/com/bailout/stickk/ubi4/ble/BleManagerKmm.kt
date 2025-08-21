package com.bailout.stickk.ubi4.ble

/** Информация об обнаруженном устройстве */
expect class BleDeviceKmm(id: String, name: String?, rssi: Int) {
    val id: String           // идентификатор устройства
    val name: String? // имя, может быть null
    val rssi: Int     // уровень сигнала
}

/** Менеджер для работы с Bluetooth LE */
expect class BleManagerKmm() {
    /** Начать сканирование. Каждый найденный девайс передаётся в [onDeviceFound]. */
    fun startScanKmm(onDeviceFound: (BleDeviceKmm) -> Unit)

    /** Остановить сканирование. */
    fun stopScanKmm()

    fun connectToDevice(uuid: String)

    /**
     * Отправить [data] в xарактеристику [characteristicUuid]
     * устройства [device] (или по его id).
     */
    fun sendBytesKmm(
        data: ByteArray,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    )
}
