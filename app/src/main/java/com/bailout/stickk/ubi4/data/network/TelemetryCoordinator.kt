package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class TelemetryCoordinator(
    private val scope: CoroutineScope,
    private val requestTelemetryData: () -> Unit,
    private val fallbackDeviceIds: () -> List<String?>,
    private val showToast: (String) -> Unit,
    private val sender: Ubi4TelemetrySender = Ubi4TelemetrySender()
) {
    fun sendTelemetry() {
        scope.launch {
            try {
                val sentRequest = sender.sendTelemetry(
                    requestTelemetryData = requestTelemetryData,
                    fallbackDeviceIds = fallbackDeviceIds
                )
                val grips = sentRequest.messages.firstOrNull()?.data?.grips ?: 0L
                showToast("Telemetry отправлена: grips=$grips")
            } catch (e: Ubi4TelemetrySendException.TelemetryV3Unavailable) {
                platformLog(LOG_TAG, "Telemetry V3 unavailable: ${e.message}")
                showToast("Telemetry V3 недоступна")
            } catch (e: Ubi4TelemetrySendException.TelemetryTimeout) {
                platformLog(LOG_TAG, "Telemetry response timeout: ${e.message}")
                showToast("Не дождались telemetry")
            } catch (e: Ubi4TelemetrySendException.DeviceIdMissing) {
                platformLog(LOG_TAG, "Device id missing: ${e.message}")
                showToast("Не найден серийный номер")
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                platformLog(LOG_TAG, "Telemetry upload failed: ${t.message ?: "unknown"}")
                showToast("Ошибка отправки telemetry: ${t.message ?: "unknown"}")
            }
        }
    }

    private companion object {
        const val LOG_TAG = "TelemetryV3"
    }
}