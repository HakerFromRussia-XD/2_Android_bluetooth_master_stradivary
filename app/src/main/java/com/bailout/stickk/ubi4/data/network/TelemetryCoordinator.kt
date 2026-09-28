package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.versions.v3.domain.telemetry.*
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class TelemetryCoordinator(
    private val scope: CoroutineScope,
    private val send: SendTelemetryUseCaseV3,
) {
    fun sendTelemetry() {
        scope.launch {
            try {
                when (send()) {
                    V3TelemetryResult.AlreadySending -> platformLog(LOG_TAG, "Telemetry upload skipped: upload already in progress")
                    V3TelemetryResult.SentRecently -> platformLog(LOG_TAG, "Telemetry upload skipped: last upload was less than 24h ago")
                    is V3TelemetryResult.Sent -> Unit
                }
            } catch (e: V3TelemetryException) {
                val log = when (e.reason) {
                    V3TelemetryFailure.UNAVAILABLE -> "Telemetry V3 unavailable: ${e.message}"
                    V3TelemetryFailure.TIMEOUT -> "Telemetry response timeout: ${e.message}"
                    V3TelemetryFailure.DEVICE_ID_MISSING -> "Device id missing: ${e.message}"
                }
                platformLog(LOG_TAG, log)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                platformLog(LOG_TAG, "Telemetry upload failed: ${t.message ?: "unknown"}")
            }
        }
    }

    private companion object {
        const val LOG_TAG = "TelemetryV3"
    }
}
