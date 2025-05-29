package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridge

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class BLEParserBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    // Заглушка BleCommandExecutor для базового вызова
    private val bleCommandExecutor = object : BleCommandExecutor {
        override fun getQueueUBI4(): BlockingQueueUbi4 {
            return BlockingQueueUbi4() // если требует аргументы — добавь
        }

        override fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit) {
            // Assistant: Пустая реализация для вызова из iOS
        }

        override fun sendWidgetsArray() {
            // Assistant: Пустая реализация для вызова из iOS
        }

        override fun updateSerialNumber(deviceInfo: DeviceInfoStructs) {
            // Assistant: Пустая реализация для вызова из iOS
        }
    }

    private val parser = BLEParser(coroutineScope, bleCommandExecutor)

    fun parseData(data: ByteArray) {
        parser.parseReceivedData(data)
    }
}