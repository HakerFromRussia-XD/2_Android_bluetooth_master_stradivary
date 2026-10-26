package com.bailout.stickk.ubi4.versions.v3.domain.main

import kotlinx.coroutines.flow.Flow
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceIdentity

interface V3MainDisplaysRepository {
    fun getVisibleDisplays(): Set<Int>
}

interface V3WidgetUpdatesRepository {
    fun observeWidgetUpdates(): Flow<Int>
}

interface V3MainStatusReader {
    fun currentConnectionReady(): Boolean
    fun observeConnectionReady(): Flow<Boolean>
    fun observeBatteryPercent(): Flow<Int>
}

interface V3DeviceDisconnectionRepository {
    fun disconnectDevice()
}

interface V3DeviceConnectionRepository {
    fun prepareConnection(deviceName: String, deviceAddress: String): Boolean
    fun connectDevice(deviceAddress: String)
}

interface V3MainRepository : V3MainDisplaysRepository {
    fun rememberLastConnection()
    fun restoreSavedConnection()
    fun resetLastConnection()
    fun initializeWidgetStorage()
    fun cancelAppCloseProfileUpload()
    fun enqueueAppCloseProfileUpload(language: String)
    fun observeUpdates(): Flow<V3MainUpdate>
    fun loadDeviceIdentity(): V3DeviceIdentity
    fun applySerialNumber(serial: String): V3DeviceIdentity
    fun observeDeviceIdentity(): Flow<V3DeviceIdentity?>
}

sealed interface V3MainUpdate {
    data object WidgetsChanged : V3MainUpdate
    data class BatteryChanged(val percent: Int) : V3MainUpdate
}
