package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

/** Информация об обнаруженном устройстве */
actual class BleDeviceKmm actual constructor (
    actual val id: String,
    actual val name: String?,
    actual val rssi: Int
)

/** Менеджер для работы с Bluetooth LE */
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
actual class BleManagerKmm actual constructor() {
    private var bleCommandExecutor: BleCommandExecutor? = null

    fun setBleCommandExecutor(executor: BleCommandExecutor) {
        bleCommandExecutor = executor
    }
    /** Начать сканирование. Каждый найденный девайс передаётся в [onDeviceFound]. */
    actual fun startScanKmm(onDeviceFound: (BleDeviceKmm) -> Unit) {}

    /** Остановить сканирование. */
    actual fun stopScanKmm() {}

    /**
     * Отправить [data] в характеристику [characteristicUuid]
     * устройства [device] (или по его id).
     */
    actual fun sendBytesKmm(
        data: ByteArray,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit,
    ) {
        bleCommandExecutor?.bleCommandWithQueue(
            data,
            command,
            typeCommand,
            onChunkSent
        )
    }

}