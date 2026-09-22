package com.bailout.stickk.ubi4.versions.v3.data.telemetry

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.network.Ubi4TelemetrySendException
import com.bailout.stickk.ubi4.data.network.Ubi4TelemetrySender
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.versions.v3.domain.telemetry.*

class V3TelemetryRepositoryImpl(
    private val preferences: SharedPreferences,
    private val requestTelemetryData: () -> Unit,
    private val deviceIdentity: V3DeviceIdentityStore,
    private val sender: Ubi4TelemetrySender = Ubi4TelemetrySender(),
) : V3TelemetryRepository {
    override fun currentTimeMillis() = System.currentTimeMillis()

    override fun lastSendTimestamp() = preferences.getLong(PreferenceKeysUbi4.LAST_TELEMETRY_SEND_TIMESTAMP, 0L)

    override fun saveLastSendTimestamp(timestamp: Long) {
        preferences.edit().putLong(PreferenceKeysUbi4.LAST_TELEMETRY_SEND_TIMESTAMP, timestamp).apply()
    }

    override suspend fun sendTelemetry(): Long = try {
        sender.sendTelemetry(requestTelemetryData, ::fallbackDeviceIds).messages.firstOrNull()?.data?.grips ?: 0L
    } catch (e: Ubi4TelemetrySendException) {
        val reason = when (e) {
            is Ubi4TelemetrySendException.TelemetryV3Unavailable -> V3TelemetryFailure.UNAVAILABLE
            is Ubi4TelemetrySendException.TelemetryTimeout -> V3TelemetryFailure.TIMEOUT
            is Ubi4TelemetrySendException.DeviceIdMissing -> V3TelemetryFailure.DEVICE_ID_MISSING
        }
        throw V3TelemetryException(reason, e)
    }

    private fun fallbackDeviceIds(): List<String?> {
        // Sender evaluates this only after waiting for fresh BLE counters, if UUID is missing.
        val identity = deviceIdentity.identity.value
        return listOf(identity?.serial, identity?.deviceName, ConnectionState.connectedDeviceName,
            preferences.getString(PreferenceKeysUbi4.CONNECTED_DEVICE, "null").toString())
    }
}
