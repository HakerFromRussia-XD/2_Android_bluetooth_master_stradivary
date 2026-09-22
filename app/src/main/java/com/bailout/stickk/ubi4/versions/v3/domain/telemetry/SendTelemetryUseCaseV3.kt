package com.bailout.stickk.ubi4.versions.v3.domain.telemetry

/** One instance per connection listener; invoked on the existing main-thread scope. */
class SendTelemetryUseCaseV3(private val repository: V3TelemetryRepository) {
    private var sendInProgress = false

    suspend operator fun invoke(): V3TelemetryResult {
        val now = repository.currentTimeMillis()
        if (sendInProgress) return V3TelemetryResult.AlreadySending
        val lastSend = repository.lastSendTimestamp()
        if (lastSend > 0L && now - lastSend < SEND_INTERVAL_MS) return V3TelemetryResult.SentRecently

        sendInProgress = true
        return try {
            val grips = repository.sendTelemetry()
            repository.saveLastSendTimestamp(repository.currentTimeMillis())
            V3TelemetryResult.Sent(grips)
        } finally {
            sendInProgress = false
        }
    }

    private companion object { const val SEND_INTERVAL_MS = 24 * 60 * 60 * 1_000L }
}
