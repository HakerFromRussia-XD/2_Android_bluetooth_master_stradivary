package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.models.network.TelemetryMessagesRequest
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock

class Ubi4TelemetryRepository(
    private val api: Ubi4RequestsApi = Ubi4RequestsApi()
) {
    suspend fun sendTelemetry(
        deviceId: String,
        counters: TelemetryGestureCounters,
        occurred: Long = Clock.System.now().epochSeconds
    ): TelemetryMessagesRequest {
        val request = Ubi4TelemetryPayloadFactory.build(
            deviceId = deviceId,
            occurred = occurred,
            counters = counters
        )

        return when (val result = api.postTelemetryMessages(request)) {
            is NetworkResult.Success -> request
            is NetworkResult.Error -> {
                throw IOException("Telemetry upload failed ${result.code}: ${result.message}")
            }
        }
    }
}
