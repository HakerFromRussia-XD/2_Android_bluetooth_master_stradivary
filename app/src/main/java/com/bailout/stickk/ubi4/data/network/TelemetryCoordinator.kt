package com.bailout.stickk.ubi4.data.network

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class TelemetryCoordinator(
    private val scope: CoroutineScope,
    private val preferences: SharedPreferences,
    private val requestTelemetryData: () -> Unit,
    private val fallbackDeviceIds: () -> List<String?>,
    private val showToast: (String) -> Unit,
    private val sender: Ubi4TelemetrySender = Ubi4TelemetrySender()
) {
    private var sendInProgress = false

    fun sendTelemetry(showResultToast: Boolean = true) {
        scope.launch {
            val now = System.currentTimeMillis()
            if (sendInProgress) {
                platformLog(LOG_TAG, "Telemetry upload skipped: upload already in progress")
//                if (showResultToast) showToast("Telemetry уже отправляется")
                return@launch
            }
            if (!canSendTelemetry(now)) {
                platformLog(LOG_TAG, "Telemetry upload skipped: last upload was less than 24h ago")
//                if (showResultToast) showToast("Telemetry уже отправлялась меньше 24 часов назад")
                return@launch
            }

            sendInProgress = true
            try {
                val sentRequest = sender.sendTelemetry(
                    requestTelemetryData = requestTelemetryData,
                    fallbackDeviceIds = fallbackDeviceIds
                )
                saveLastSendTimestamp(System.currentTimeMillis())
                val grips = sentRequest.messages.firstOrNull()?.data?.grips ?: 0L
                if (showResultToast) showToast("Telemetry отправлена: grips=$grips")
            } catch (e: Ubi4TelemetrySendException.TelemetryV3Unavailable) {
                platformLog(LOG_TAG, "Telemetry V3 unavailable: ${e.message}")
                if (showResultToast) showToast("Telemetry V3 недоступна")
            } catch (e: Ubi4TelemetrySendException.TelemetryTimeout) {
                platformLog(LOG_TAG, "Telemetry response timeout: ${e.message}")
                if (showResultToast) showToast("Не дождались telemetry")
            } catch (e: Ubi4TelemetrySendException.DeviceIdMissing) {
                platformLog(LOG_TAG, "Device id missing: ${e.message}")
                if (showResultToast) showToast("Не найден серийный номер")
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                platformLog(LOG_TAG, "Telemetry upload failed: ${t.message ?: "unknown"}")
                if (showResultToast) showToast("Ошибка отправки telemetry: ${t.message ?: "unknown"}")
            } finally {
                sendInProgress = false
            }
        }
    }

    private fun canSendTelemetry(now: Long): Boolean {
        val lastSendTimestamp = preferences.getLong(PreferenceKeysUbi4.LAST_TELEMETRY_SEND_TIMESTAMP, 0L)
        return lastSendTimestamp <= 0L || now - lastSendTimestamp >= TELEMETRY_SEND_INTERVAL_MS
    }

    private fun saveLastSendTimestamp(timestamp: Long) {
        preferences.edit()
            .putLong(PreferenceKeysUbi4.LAST_TELEMETRY_SEND_TIMESTAMP, timestamp)
            .apply()
    }

    private companion object {
        const val LOG_TAG = "TelemetryV3"
        const val TELEMETRY_SEND_INTERVAL_MS = 24 * 60 * 60 * 1_000L
    }
}
