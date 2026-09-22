package com.bailout.stickk.ubi4.versions.v3.domain.telemetry

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class SendTelemetryUseCaseV3Test {
    private val day = 86_400_000L
    private val repository = FakeRepository()
    private val send = SendTelemetryUseCaseV3(repository)

    @Test fun `24 hours is inclusive and unset negative or future timestamps keep previous behavior`() = runTest {
        repository.now = 3 * day
        for (last in listOf(0L, -1L, repository.now - day, repository.now - day - 1)) {
            repository.last = last
            assertEquals(V3TelemetryResult.Sent(42), send())
        }
        for (last in listOf(repository.now - day + 1, repository.now, repository.now + 1)) {
            repository.last = last
            assertEquals(V3TelemetryResult.SentRecently, send())
        }
        assertEquals(4, repository.requests)
        assertEquals(List(4) { repository.now }, repository.timestamps)
    }

    @Test fun `concurrent request skips without waiting and timestamp records completion time`() = runTest {
        val response = CompletableDeferred<Long>()
        repository.sendBlock = { response.await() }
        val first = async { send() }
        runCurrent()
        assertEquals(V3TelemetryResult.AlreadySending, send())
        assertEquals(1, repository.requests)
        assertTrue(repository.timestamps.isEmpty())
        repository.now += 5000
        response.complete(123)
        assertEquals(V3TelemetryResult.Sent(123), first.await())
        assertEquals(listOf(repository.now), repository.timestamps)
        assertEquals(V3TelemetryResult.SentRecently, send())
    }

    @Test fun `send error and cancellation release guard without recording success`() = runTest {
        val failure = IllegalStateException("offline")
        repository.sendBlock = { throw failure }
        assertSame(failure, runCatching { send() }.exceptionOrNull())
        assertTrue(repository.timestamps.isEmpty())
        repository.sendBlock = { awaitCancellation() }
        val pending = launch { send() }
        runCurrent()
        assertEquals(2, repository.requests)
        pending.cancelAndJoin()
        assertTrue(repository.timestamps.isEmpty())
        repository.sendBlock = { 7 }
        assertEquals(V3TelemetryResult.Sent(7), send())
        assertEquals(3, repository.requests)
    }

    @Test fun `timestamp storage failure propagates and does not leave guard set`() = runTest {
        val failure = IllegalStateException("storage unavailable")
        repository.saveFailure = failure
        assertSame(failure, runCatching { send() }.exceptionOrNull())
        assertEquals(0L, repository.last)
        repository.saveFailure = null
        assertEquals(V3TelemetryResult.Sent(42), send())
        assertEquals(2, repository.requests)
    }

    private class FakeRepository : V3TelemetryRepository {
        var now = 200_000_000L
        var last = 0L
        var requests = 0
        val timestamps = mutableListOf<Long>()
        var sendBlock: suspend () -> Long = { 42 }
        var saveFailure: RuntimeException? = null
        override fun currentTimeMillis() = now
        override fun lastSendTimestamp() = last
        override fun saveLastSendTimestamp(timestamp: Long) {
            saveFailure?.let { throw it }
            timestamps += timestamp
            last = timestamp
        }
        override suspend fun sendTelemetry(): Long { requests++; return sendBlock() }
    }
}
