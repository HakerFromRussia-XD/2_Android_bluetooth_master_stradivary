package com.bailout.stickk.ubi4.data.network

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.models.network.TelemetryMessagesRequest
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.versions.v3.data.telemetry.V3TelemetryRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.telemetry.SendTelemetryUseCaseV3
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryCoordinatorTest {
    private val previousName = runCatching { ConnectionState.connectedDeviceName }.getOrDefault("")
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val sender = mockk<Ubi4TelemetrySender>()
    private val identity = V3DeviceIdentityStore()
    private var lastTimestamp = 0L
    private var savedName: String? = "saved-device"
    private var requests = 0
    private val timestamps = mutableListOf<Long>()
    private val toasts = mutableListOf<String>()
    private val result = TelemetryMessagesRequest(emptyList())

    @BeforeEach fun setup() {
        ConnectionState.connectedDeviceName = "connected-device"
        every { preferences.getLong(PreferenceKeysUbi4.LAST_TELEMETRY_SEND_TIMESTAMP, 0L) } answers { lastTimestamp }
        every { preferences.getString(PreferenceKeysUbi4.CONNECTED_DEVICE, "null") } answers { savedName }
        every { preferences.edit() } returns editor
        every { editor.putLong(PreferenceKeysUbi4.LAST_TELEMETRY_SEND_TIMESTAMP, any()) } answers {
            lastTimestamp = secondArg(); timestamps += lastTimestamp; editor
        }
        every { editor.apply() } just Runs
    }

    @AfterEach fun restore() { ConnectionState.connectedDeviceName = previousName }

    private fun coordinator(scope: CoroutineScope) = TelemetryCoordinator(scope,
        SendTelemetryUseCaseV3(V3TelemetryRepositoryImpl(preferences, { requests++ }, identity, sender)),
        { toasts += it })

    @Test fun `manual result messages stay unchanged while skips and cancellation remain silent`() = runTest {
        val coordinator = coordinator(backgroundScope)
        for ((failure, message) in listOf(
            Ubi4TelemetrySendException.TelemetryV3Unavailable() to "Telemetry V3 недоступна",
            Ubi4TelemetrySendException.TelemetryTimeout(Exception()) to "Не дождались telemetry",
            Ubi4TelemetrySendException.DeviceIdMissing() to "Не найден серийный номер",
            IllegalStateException("offline") to "Ошибка отправки telemetry: offline",
            IllegalStateException() to "Ошибка отправки telemetry: unknown",
        )) {
            coEvery { sender.sendTelemetry(any(), any()) } throws failure
            coordinator.sendTelemetry(); runCurrent()
            assertEquals(listOf(message), toasts)
            assertTrue(timestamps.isEmpty())
            toasts.clear()
        }
        coEvery { sender.sendTelemetry(any(), any()) } throws CancellationException()
        coordinator.sendTelemetry(); runCurrent()
        assertTrue(toasts.isEmpty())
        coEvery { sender.sendTelemetry(any(), any()) } returns result
        coordinator.sendTelemetry(); runCurrent()
        assertEquals(listOf("Telemetry отправлена: grips=0"), toasts)
        toasts.clear()
        coordinator.sendTelemetry(); runCurrent()
        assertTrue(toasts.isEmpty())
    }

    @Test fun `owner scope cancellation stops pending BLE wait and prevents later sends`() = runTest {
        val ownerJob = SupervisorJob()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + ownerJob)
        var cancelled = false
        coEvery { sender.sendTelemetry(any(), any()) } coAnswers {
            try { awaitCancellation() } finally { cancelled = true }
        }
        val coordinator = coordinator(scope)
        try {
            coordinator.sendTelemetry(false); runCurrent()
            ownerJob.cancel(); runCurrent()
            assertTrue(cancelled)
            assertTrue(timestamps.isEmpty())
            coordinator.sendTelemetry(false); runCurrent()
            coVerify(exactly = 1) { sender.sendTelemetry(any(), any()) }
            assertTrue(toasts.isEmpty())
        } finally { ownerJob.cancel() }
    }

    @Test fun `fallback reads latest identity connection and preferences after BLE wait in original order`() = runTest {
        val release = CompletableDeferred<Unit>()
        var ids: List<String?>? = null
        coEvery { sender.sendTelemetry(any(), any()) } coAnswers {
            firstArg<() -> Unit>().invoke()
            release.await()
            ids = secondArg<() -> List<String?>>().invoke()
            result
        }
        identity.update("old-serial", "old-name")
        coordinator(backgroundScope).sendTelemetry(showResultToast = false)
        runCurrent()
        assertEquals(1, requests)
        assertTrue(timestamps.isEmpty())
        identity.update(" new-serial ", "INDY3-new-name")
        ConnectionState.connectedDeviceName = "FTHS3-new-connection"
        savedName = "new-saved-name"
        release.complete(Unit)
        runCurrent()
        assertEquals(listOf(" new-serial ", "INDY3-new-name", "FTHS3-new-connection", "new-saved-name"), ids)
        assertEquals(1, timestamps.size)
        assertTrue(timestamps.single() > 0)
        assertTrue(toasts.isEmpty())
    }

    @Test fun `uninitialized and blank identity keep nulls blanks and preferences null sentinel`() = runTest {
        val lists = mutableListOf<List<String?>>()
        coEvery { sender.sendTelemetry(any(), any()) } coAnswers {
            lists += secondArg<() -> List<String?>>().invoke(); result
        }
        val coordinator = coordinator(backgroundScope)
        savedName = null
        coordinator.sendTelemetry(false); runCurrent()
        assertEquals(listOf(null, null, "connected-device", "null"), lists.last())
        lastTimestamp = 0L
        identity.update(" ", null)
        ConnectionState.connectedDeviceName = ""
        savedName = "NOT SET!"
        coordinator.sendTelemetry(false); runCurrent()
        assertEquals(listOf(" ", null, "", "NOT SET!"), lists.last())
    }

    @Test fun `24 hour gate and in progress gate prevent extra sends while successful send records timestamp`() = runTest {
        val release = CompletableDeferred<Unit>()
        coEvery { sender.sendTelemetry(any(), any()) } coAnswers { release.await(); result }
        val coordinator = coordinator(backgroundScope)
        lastTimestamp = System.currentTimeMillis()
        coordinator.sendTelemetry(false); runCurrent()
        coVerify(exactly = 0) { sender.sendTelemetry(any(), any()) }
        lastTimestamp = System.currentTimeMillis() - 25 * 60 * 60 * 1000L
        coordinator.sendTelemetry(false); runCurrent()
        coordinator.sendTelemetry(false); runCurrent()
        coVerify(exactly = 1) { sender.sendTelemetry(any(), any()) }
        assertTrue(timestamps.isEmpty())
        release.complete(Unit); runCurrent()
        coordinator.sendTelemetry(false); runCurrent()
        coVerify(exactly = 1) { sender.sendTelemetry(any(), any()) }
        assertEquals(1, timestamps.size)
        assertTrue(toasts.isEmpty())
    }

    @Test fun `failure and cancellation never save timestamp and release send guard for retry`() = runTest {
        val coordinator = coordinator(backgroundScope)
        for (failure in listOf(Ubi4TelemetrySendException.DeviceIdMissing(),
            Ubi4TelemetrySendException.TelemetryTimeout(Exception("timeout")),
            Ubi4TelemetrySendException.TelemetryV3Unavailable(), IllegalStateException("offline"),
            CancellationException("cancelled"))) {
            lastTimestamp = 0L
            timestamps.clear()
            coEvery { sender.sendTelemetry(any(), any()) } throws failure
            coordinator.sendTelemetry(false); runCurrent()
            assertTrue(timestamps.isEmpty())
            coEvery { sender.sendTelemetry(any(), any()) } returns result
            coordinator.sendTelemetry(false); runCurrent()
            assertEquals(1, timestamps.size)
        }
        assertTrue(toasts.isEmpty())
    }
}
