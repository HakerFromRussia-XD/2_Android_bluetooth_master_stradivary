package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState.telemetryGestureCountersFlow
import com.bailout.stickk.ubi4.models.network.TelemetryMessagesRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock

class Ubi4TelemetrySender(
    private val repository: Ubi4TelemetryRepository = Ubi4TelemetryRepository()
) {
    suspend fun sendTelemetry(
        requestTelemetryData: () -> Unit,
        fallbackDeviceIds: () -> List<String?> = { emptyList() }
    ): TelemetryMessagesRequest {
        if (!UiState.isInterfaceV3Activated) {
            throw Ubi4TelemetrySendException.TelemetryV3Unavailable()
        }

        val requestStartedAt = Clock.System.now().toEpochMilliseconds()
        requestTelemetryData()
        val counters = waitFreshTelemetry(requestStartedAt)
        val deviceId = counters.deviceUuid.takeIfValidDeviceId()
            ?: fallbackDeviceIds().firstNotNullOfOrNull { it.takeIfValidDeviceId() }
            ?: throw Ubi4TelemetrySendException.DeviceIdMissing()

        return repository.sendTelemetry(
            deviceId = deviceId,
            counters = counters
        )
    }

    private suspend fun waitFreshTelemetry(requestStartedAt: Long): TelemetryGestureCounters =
        try {
            withTimeout(TELEMETRY_WAIT_TIMEOUT_MS) {
                telemetryGestureCountersFlow.first { counters ->
                    counters.receivedAtMillis >= requestStartedAt &&
                        (counters.baseGestureMovementCount.isNotEmpty() ||
                            counters.customGestureMovementCount.isNotEmpty())
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw Ubi4TelemetrySendException.TelemetryTimeout(e)
        }

    private fun String?.takeIfValidDeviceId(): String? =
        this
            ?.trim()
            ?.takeIf {
                it.isNotEmpty() &&
                    !it.equals("null", ignoreCase = true) &&
                    !it.equals("NOT SET!", ignoreCase = true) &&
                    !it.equals("UNKNOWN", ignoreCase = true)
            }

    private companion object {
        const val TELEMETRY_WAIT_TIMEOUT_MS = 5_000L
    }
}

sealed class Ubi4TelemetrySendException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class TelemetryV3Unavailable : Ubi4TelemetrySendException("Telemetry V3 is unavailable")
    class DeviceIdMissing : Ubi4TelemetrySendException("Device id is missing")
    class TelemetryTimeout(cause: Throwable) : Ubi4TelemetrySendException("Telemetry response timeout", cause)
}
