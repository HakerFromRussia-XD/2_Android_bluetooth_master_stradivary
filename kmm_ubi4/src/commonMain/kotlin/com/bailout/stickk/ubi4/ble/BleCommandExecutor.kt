package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4

interface BleCommandExecutor {
    fun getQueueUBI4(): BlockingQueueUbi4
    fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit)
    fun sendWidgetsArray()
    fun updateSerialNumber(deviceInfo: DeviceInfoStructs)
//    fun updateFirmwareInfo(info: FirmwareInfoStruct)
}