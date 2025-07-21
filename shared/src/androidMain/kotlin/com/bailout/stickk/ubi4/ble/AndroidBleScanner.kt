package com.bailout.stickk.ubi4.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleDevice_fromTestProj
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidBleScanner : BleScanner {

    // Храним список найденных устройств
    private val _devices = MutableStateFlow<List<BleDevice_fromTestProj>>(emptyList())
    override val devices: StateFlow<List<BleDevice_fromTestProj>> = _devices
    val devicesLiveData: LiveData<List<BleDevice_fromTestProj>> = devices.asLiveData()

    // Метод для вызова из нативного Android‑кода при обнаружении устройства
    @SuppressLint("MissingPermission")
    fun onDeviceFound(device: BluetoothDevice) {
        val bleDevice = BleDevice_fromTestProj(
            id = device.address, // адрес как уникальный id
            name = device.name ?: "Unknown"
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