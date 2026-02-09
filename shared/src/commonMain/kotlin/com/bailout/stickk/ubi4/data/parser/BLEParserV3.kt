package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BleManagerKmm
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope

class BLEParserV3(
    private val coroutineScope: CoroutineScope,
    private val bleCommandExecutor: BleCommandExecutor,
    private val bleManager: BleManagerKmm
) {
    private var mConnected = false
    private var countErrors = 0
    private val DEVICE_SIZE = 7
    data class SubDeviceInfo(
        val address: Int,        // 0..255
        val deviceType: Int,     // 0..255
        val deviceCode: Int,     // 0..255
        val dfu: Int,            // 0..255  // 0 - нельзя прошить, 1 - можно шить
        val fwVersion: String    // "major.minor.quickfix"
    )
    private fun Byte.toHex(): String =
        (this.toInt() and 0xFF)
            .toString(16)
            .uppercase()
            .padStart(2, '0')

    fun parseReceivedData(data: ByteArray) {
        val receiveDataString: String = EncodeByteToHex.bytesToHexString(data)
//        platformLog("BLEParserV3", "data.size=${data.size}")
//        platformLog("BLEParserV3", "dataString=$receiveDataString")
        parseSubDeviceManagerGetAllSubDevice(data.copyOfRange(5, data.size)).forEach { item ->
            platformLog("BLEParserV3", "item=$item")
        }
    }

    private fun parseSubDeviceManagerGetAllSubDevice(payload: ByteArray?): List<SubDeviceInfo> {
        val devices = mutableListOf<SubDeviceInfo>()

        if (payload == null || payload.isEmpty()) {
            // logger.debug("Ответ SUB_DEVICE_MANAGER: payload пуст")
            return devices
        }

        // Первый байт — подкоманда
        val subcommand = payload[0].toInt() and 0xFF

        if (payload.size <= 1) {
            // logger.debug("Ответ SUB_DEVICE_MANAGER: список устройств пуст (подкоманда=$subcommand)")
            return devices
        }

        // logger.debug("Парсим SUB_DEVICE_MANAGER ответ (подкоманда=$subcommand, ${payload.size - 1} байт данных)")

        var i = 1 // начинаем после подкоманды
        while (i + DEVICE_SIZE <= payload.size) {
            val device = parseDevice(payload, i)
            if (device != null) {
                devices += device
                // logger.debug("Найдено устройство: адрес=${device.address}, тип=${device.deviceType}, версия=${device.fwVersion}")
            }
            i += DEVICE_SIZE
        }

        return devices
    }
    private fun parseDevice(bytes: ByteArray, offset: Int): SubDeviceInfo? {
        if (offset < 0 || offset + DEVICE_SIZE > bytes.size) return null

        val address = bytes[offset + 0].toInt() and 0xFF
        val deviceType = bytes[offset + 1].toInt() and 0xFF
        val deviceCode = bytes[offset + 2].toInt() and 0xFF
        val dfu = bytes[offset + 3].toInt() and 0xFF

        val major = bytes[offset + 4].toInt() and 0xFF
        val minor = bytes[offset + 5].toInt() and 0xFF
        val quickfix = bytes[offset + 6].toInt() and 0xFF

        val fwVersion = "$major.$minor.$quickfix"

        return SubDeviceInfo(
            address = address,
            deviceType = deviceType,
            deviceCode = deviceCode,
            dfu = dfu,
            fwVersion = fwVersion
        )
    }
}