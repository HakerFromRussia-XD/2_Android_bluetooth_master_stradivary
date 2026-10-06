package com.bailout.stickk.ubi4.versions.v3.domain.main

import kotlinx.coroutines.flow.Flow
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceIdentity

interface V3MainRepository {
    fun rememberLastConnection()
    fun restoreSavedConnection()
    fun resetLastConnection()
    fun initializeWidgetStorage()
    fun cancelAppCloseProfileUpload()
    fun enqueueAppCloseProfileUpload(language: String)
    fun getVisibleDisplays(): Set<Int>
    fun observeUpdates(): Flow<V3MainUpdate>
    fun loadDeviceIdentity(): V3DeviceIdentity
    fun applySerialNumber(serial: String): V3DeviceIdentity
    fun observeDeviceIdentity(): Flow<V3DeviceIdentity?>
}

sealed interface V3MainUpdate {
    data object WidgetsChanged : V3MainUpdate
    data class BatteryChanged(val percent: Int) : V3MainUpdate
}
