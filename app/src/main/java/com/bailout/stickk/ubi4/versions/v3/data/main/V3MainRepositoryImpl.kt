package com.bailout.stickk.ubi4.versions.v3.data.main

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.network.SettingsProfileUploadWorkScheduler
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainRepository
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class V3MainRepositoryImpl(
    private val deviceIdentity: V3DeviceIdentityStore,
    context: Context,
    private val preferences: SharedPreferences,
) : V3MainRepository {
    private val appContext = context.applicationContext

    override fun rememberLastConnection() {
        preferences.edit().putString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, ConnectionState.connectedDeviceAddress).apply()
    }

    override fun restoreSavedConnection() {
        ConnectionState.connectedDeviceName = preferences.getString(PreferenceKeysUbi4.CONNECTED_DEVICE, "NOT SET!").toString()
        ConnectionState.connectedDeviceAddress = preferences.getString(PreferenceKeysUbi4.CONNECTED_DEVICE_ADDRESS, "NOT SET!").toString()
    }

    override fun resetLastConnection() {
        preferences.edit().putString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "null").apply()
    }

    override fun initializeWidgetStorage() = WidgetRepoProvider.setCurrentMac(ConnectionState.connectedDeviceAddress)

    override fun cancelAppCloseProfileUpload() = SettingsProfileUploadWorkScheduler.cancelAppCloseUpload(appContext)

    override fun enqueueAppCloseProfileUpload(language: String) =
        SettingsProfileUploadWorkScheduler.enqueueAppCloseUpload(appContext, language)

    override fun loadDeviceIdentity() = deviceIdentity.update(ConnectionState.connectedDeviceName, null)

    override fun applySerialNumber(serial: String) = run {
        SettingsProfileManager.setCurrentSerial(serial)
        deviceIdentity.update(serial, serial)
    }

    override fun observeDeviceIdentity() = deviceIdentity.identity

    override fun getVisibleDisplays(): Set<Int> {
        // Use the same renderable-widget filtering as the existing DataFactory UI.
        val factory = DataFactory()
        return (0..4).filter { factory.prepareData(it).isNotEmpty() }.toSet()
    }

    override fun observeUpdates() = merge(
        UiState.updateFlow.map { V3MainUpdate.WidgetsChanged },
        WidgetState.batteryPercentFlow.map { V3MainUpdate.BatteryChanged(it) },
    )
}
