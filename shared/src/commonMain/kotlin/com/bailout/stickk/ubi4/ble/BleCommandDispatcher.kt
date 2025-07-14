package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble

/**
 * Абстракция-мост для отправки BLE-команд из общего кода.
 * Платформенные реализации находятся в androidMain и iosMain.
 */
//expect object BleCommandDispatcher {
//    fun bleCommandWithQueue(
//        dataForWrite: ByteArray,
//        characteristic: String,
//        type: String
//    )
//}