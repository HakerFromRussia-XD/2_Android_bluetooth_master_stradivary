package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleDevice_fromTestProj
import kotlinx.coroutines.flow.StateFlow

//data class BleDevice(val id: String, val name: String)

// Интерфейс сканера
interface BleScanner {
    val devices: StateFlow<List<BleDevice_fromTestProj>>
    fun startScan()
    fun stopScan()
}