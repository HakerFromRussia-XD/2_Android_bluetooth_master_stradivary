package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

import kotlinx.coroutines.flow.Flow

actual class BleClient {
    /**
     * Подключается к BLE устройству по его адресу.
     * @param address MAC-адрес или идентификатор устройства.
     */
    actual suspend fun connect(address: String) {
    }

    /**
     * Наблюдает за уведомлениями от BLE устройства по UUID характеристики.
     * @param uuid Идентификатор характеристики.
     * @return Flow, который эмиттит полученные данные в виде ByteArray.
     */
    actual fun observeNotifications(uuid: String): Flow<ByteArray> {
        TODO("Not yet implemented")
    }

}