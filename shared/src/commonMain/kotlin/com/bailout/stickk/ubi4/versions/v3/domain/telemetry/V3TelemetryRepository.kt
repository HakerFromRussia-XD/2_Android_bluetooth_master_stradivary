package com.bailout.stickk.ubi4.versions.v3.domain.telemetry

interface V3TelemetryRepository {
    fun currentTimeMillis(): Long
    fun lastSendTimestamp(): Long
    fun saveLastSendTimestamp(timestamp: Long)
    /** Sends fresh device counters and returns the grip count in the sent message. */
    suspend fun sendTelemetry(): Long
}

sealed interface V3TelemetryResult {
    data object AlreadySending : V3TelemetryResult
    data object SentRecently : V3TelemetryResult
    data class Sent(val grips: Long) : V3TelemetryResult
}

enum class V3TelemetryFailure { UNAVAILABLE, TIMEOUT, DEVICE_ID_MISSING }

class V3TelemetryException(val reason: V3TelemetryFailure, cause: Throwable) :
    Exception(cause.message, cause)
