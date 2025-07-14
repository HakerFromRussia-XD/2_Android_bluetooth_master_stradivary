package com.bailout.stickk.ubi4.ble

import kotlinx.coroutines.flow.StateFlow

data class BleDevice(val id: String, val name: String)

// Интерфейс сканера
interface BleScanner {
    val devices: StateFlow<List<BleDevice>>
    fun startScan()
    fun stopScan()
}