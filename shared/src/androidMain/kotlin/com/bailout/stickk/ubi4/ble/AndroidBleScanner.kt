package com.bailout.stickk.ubi4.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidBleScanner : BleScanner {

    // Храним список найденных устройств
    private val _devices = MutableStateFlow<List<BleDeviceKmm>>(emptyList())
    override val devices: StateFlow<List<BleDeviceKmm>> = _devices
    val devicesLiveData: LiveData<List<BleDeviceKmm>> = devices.asLiveData()

    // Метод для вызова из нативного Android‑кода при обнаружении устройства
    @SuppressLint("MissingPermission")
    fun onDeviceFound(device: BluetoothDevice) {
        val bleDevice = BleDeviceKmm(
            id = device.address, // адрес как уникальный id
            name = device.name ?: "Unknown",
            rssi = 0//TODO разобраться откуда здесь взять rssi
        )
        val updated = _devices.value.toMutableList()
        if (updated.none { it.id == bleDevice.id }) {
            updated.add(bleDevice)
            _devices.value = updated
        }
    }

    // Здесь можно вызывать нативный код для сканирования:
    override fun startScan() {
        // Например, если у тебя есть BLEController:
        // bleController.scanLeDevice(true)
        // Также сбрасываем список
        _devices.value = emptyList()
    }

    override fun stopScan() {
        // Например, bleController.scanLeDevice(false)
    }
}