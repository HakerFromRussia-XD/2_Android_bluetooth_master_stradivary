package com.bailout.stickk.ubi4.versions.v3.data.phonesync

import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3PhoneSyncConnectionTest {
    private fun bytes(hex: String) = hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()

    private inner class Connection(scope: TestScope, val store: V3PhoneSyncStateStore = V3PhoneSyncStateStore()) {
        val sent = mutableListOf<ByteArray>()
        var acceptWrites = true
        var onSend: ((ByteArray) -> Unit)? = null
        val repository = V3PhoneSyncRepositoryImpl(store, { packet ->
            sent += packet
            onSend?.invoke(packet)
            acceptWrites
        }, { scope.testScheduler.currentTime })
        val authorize = AuthorizePhoneUseCaseV3(repository)
        init { repository.beginSession() }
        fun receive(hex: String) = repository.onPacket(bytes(hex))
    }

    @Test fun `wire requests match the three agreed frames`() {
        assertArrayEquals(bytes("00 05 00 00 35"), V3PhoneSyncProtocol.packet(V3PhoneSyncCommand.Echo))
        assertArrayEquals(bytes("00 04 01 00 5A"), V3PhoneSyncProtocol.packet(V3PhoneSyncCommand.Check))
        assertArrayEquals(bytes("00 04 02 00 0F"), V3PhoneSyncProtocol.packet(V3PhoneSyncCommand.Start))
    }

    @Test fun `decoder accepts only complete addressed packets with valid crc and known results`() {
        mapOf(
            "00 05 00 00 35" to V3PhoneSyncReply.Echo,
            "00 04 01 00 5A" to V3PhoneSyncReply.NotBound,
            "00 04 01 01 04" to V3PhoneSyncReply.Bound,
            "00 04 02 00 0F" to V3PhoneSyncReply.Failure,
            "00 04 02 01 51" to V3PhoneSyncReply.Success,
            "00 04 02 02 B3" to V3PhoneSyncReply.Started,
        ).forEach { (packet, result) -> assertEquals(result, V3PhoneSyncProtocol.decode(bytes(packet))) }
        listOf("00 05 00 00 34", "00 04 01 02 E6", "00 04 02 03 ED", "00 05", "00 05 00 00 35 00", "01 05 00 00 35")
            .forEach { assertNull(V3PhoneSyncProtocol.decode(bytes(it)), it) }
    }

    @Test fun `echo at 299 ms admits legacy and refresh does not probe again`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent()
        advanceTimeBy(299)
        c.receive("00 05 00 00 35")
        runCurrent()
        assertTrue(result.await())
        assertEquals(V3PhoneSyncState.LegacyAllowed, c.store.state.value)
        assertTrue(c.authorize())
        assertEquals(1, c.sent.size)
        assertTrue(c.repository.accessGranted)
    }

    @Test fun `300 ms without echo selects check and late echo cannot select legacy`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent()
        advanceTimeBy(300)
        runCurrent()
        assertEquals(V3PhoneSyncState.Checking, c.store.state.value)
        assertArrayEquals(bytes("00 04 01 00 5A"), c.sent.last())
        c.receive("00 05 00 00 35")
        runCurrent()
        assertFalse(result.isCompleted)
        assertFalse(c.repository.accessGranted)
        c.receive("00 04 01 01 04")
        runCurrent()
        assertTrue(result.await())
        assertEquals(2, c.sent.size)
        assertEquals(V3PhoneSyncState.Authorized, c.store.state.value)
    }

    @Test fun `start ack is not success and human waiting has no timeout`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent(); advanceTimeBy(300); runCurrent()
        c.receive("00 04 01 00 5A"); runCurrent()
        assertArrayEquals(bytes("00 04 02 00 0F"), c.sent.last())
        c.receive("00 04 02 02 B3"); runCurrent()
        assertEquals(V3PhoneSyncState.WaitingForUser, c.store.state.value)
        advanceTimeBy(3_600_000); runCurrent()
        c.receive("00 04 02 02 B3"); runCurrent()
        assertFalse(result.isCompleted)
        assertFalse(c.repository.accessGranted)
        assertEquals(3, c.sent.size)
        c.receive("00 04 02 01 51"); runCurrent()
        assertTrue(result.await())
        assertTrue(c.authorize())
        assertEquals(3, c.sent.size)
    }

    @Test fun `buffered final success immediately following ack is not lost`() = runTest {
        val c = Connection(this)
        c.onSend = { packet ->
            if (packet.contentEquals(bytes("00 04 01 00 5A"))) c.receive("00 04 01 00 5A")
            if (packet.contentEquals(bytes("00 04 02 00 0F"))) {
                c.receive("00 04 02 02 B3")
                c.receive("00 04 02 01 51")
            }
        }
        val result = async { c.authorize() }
        runCurrent(); advanceTimeBy(300); runCurrent()
        assertTrue(result.await())
        assertEquals(V3PhoneSyncState.Authorized, c.store.state.value)
    }

    @Test fun `terminal result without an observed ack also completes the same attempt`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent(); advanceTimeBy(300); runCurrent()
        c.receive("00 04 01 00 5A"); runCurrent()
        c.receive("00 04 02 01 51"); runCurrent()
        assertTrue(result.await())
    }

    @Test fun `failure stays terminal within the same attempt`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent(); advanceTimeBy(300); runCurrent()
        c.receive("00 04 01 00 5A"); runCurrent()
        c.receive("00 04 02 00 0F"); runCurrent()
        assertFalse(result.await())
        c.repository.endSession()
        val replacement = Connection(this, c.store)
        assertFalse(replacement.authorize())
        assertEquals(V3PhoneSyncState.Failed, replacement.store.state.value)
        assertTrue(replacement.sent.isEmpty())
        replacement.receive("00 04 02 01 51")
        assertFalse(replacement.repository.accessGranted)
    }

    @Test fun `disconnect during echo is a failed attempt rather than a timeout classification`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent(); advanceTimeBy(100)
        c.repository.endSession(); runCurrent()
        assertFalse(result.await())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
        assertEquals(1, c.sent.size)
    }

    @Test fun `disconnect during human waiting cancels and ignores later success`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent(); advanceTimeBy(300); runCurrent()
        c.receive("00 04 01 00 5A"); runCurrent()
        c.receive("00 04 02 02 B3"); runCurrent()
        c.repository.endSession()
        c.receive("00 04 02 01 51"); runCurrent()
        assertFalse(result.await())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
    }

    @Test fun `closing a completed legacy connection permits a fresh probe on reconnect`() = runTest {
        val c = Connection(this)
        val first = async { c.authorize() }
        runCurrent(); c.receive("00 05 00 00 35"); runCurrent()
        assertTrue(first.await())
        c.repository.endSession(); c.repository.beginSession()
        val next = async { c.authorize() }
        runCurrent(); advanceTimeBy(300); runCurrent()
        assertEquals(3, c.sent.size)
        assertEquals(V3PhoneSyncState.Checking, c.store.state.value)
        c.receive("00 04 01 01 04"); runCurrent()
        assertTrue(next.await())
    }

    @Test fun `old awaiting exchange cannot authorize or fail a replacement session`() = runTest {
        val c = Connection(this)
        val first = async { c.authorize() }
        runCurrent()
        c.repository.beginSession(); runCurrent()
        assertFalse(first.await())
        assertEquals(V3PhoneSyncState.Probing, c.store.state.value)
        val next = async { c.authorize() }
        runCurrent(); c.receive("00 05 00 00 35"); runCurrent()
        assertTrue(next.await())
    }

    @Test fun `write rejection cannot be interpreted as a new device`() = runTest {
        val c = Connection(this).apply { acceptWrites = false }
        assertFalse(c.authorize())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
        assertEquals(1, c.sent.size)
    }

    @Test fun `malformed echo and unrelated frames do not authorize a connection`() = runTest {
        val c = Connection(this)
        val result = async { c.authorize() }
        runCurrent()
        c.receive("00 05 00 00 34")
        c.receive("00 04 01 01 04")
        assertFalse(c.repository.onPacket(bytes("00 06 01 00 15")))
        runCurrent(); advanceTimeBy(300); runCurrent()
        assertFalse(result.isCompleted)
        assertEquals(V3PhoneSyncState.Checking, c.store.state.value)
        c.store.cancel(); c.repository.endSession(); runCurrent()
        assertFalse(result.await())
    }
}
