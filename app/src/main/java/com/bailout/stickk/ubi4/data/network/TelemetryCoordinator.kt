package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.versions.v3.domain.telemetry.*
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class TelemetryCoordinator(
    private val scope: CoroutineScope,
    private val send: SendTelemetryUseCaseV3,
    private val showToast: (String) -> Unit,
) {
    fun sendTelemetry(showResultToast: Boolean = true) {
        scope.launch {
            try {
                when (val result = send()) {
                    V3TelemetryResult.AlreadySending -> platformLog(LOG_TAG, "Telemetry upload skipped: upload already in progress")
                    V3TelemetryResult.SentRecently -> platformLog(LOG_TAG, "Telemetry upload skipped: last upload was less than 24h ago")
                    is V3TelemetryResult.Sent -> if (showResultToast) showToast("Telemetry отправлена: grips=${result.grips}")
                }
            } catch (e: V3TelemetryException) {
                val (log, toast) = when (e.reason) {
                    V3TelemetryFailure.UNAVAILABLE -> "Telemetry V3 unavailable: ${e.message}" to "Telemetry V3 недоступна"
                    V3TelemetryFailure.TIMEOUT -> "Telemetry response timeout: ${e.message}" to "Не дождались telemetry"
                    V3TelemetryFailure.DEVICE_ID_MISSING -> "Device id missing: ${e.message}" to "Не найден серийный номер"
                }
                platformLog(LOG_TAG, log)
                if (showResultToast) showToast(toast)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                platformLog(LOG_TAG, "Telemetry upload failed: ${t.message ?: "unknown"}")
                if (showResultToast) showToast("Ошибка отправки telemetry: ${t.message ?: "unknown"}")
            }
        }
    }

    private companion object {
        const val LOG_TAG = "TelemetryV3"
    }
}
