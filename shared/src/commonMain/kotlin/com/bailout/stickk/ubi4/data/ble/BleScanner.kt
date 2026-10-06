package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.BleDeviceKmm
import kotlinx.coroutines.flow.StateFlow

//data class BleDevice(val id: String, val name: String)

// Интерфейс сканера
interface BleScanner {
    val devices: StateFlow<List<BleDeviceKmm>>
    fun startScan()
    fun stopScan()
}