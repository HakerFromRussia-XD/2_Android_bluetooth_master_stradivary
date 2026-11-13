package com.bailout.stickk.ubi4.versions.v3.data.phonesync

import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3PhoneSyncAlgorithmAuditTest {
    enum class Stage { Probing, Checking, Starting, WaitingForUser }
    private fun bytes(hex: String) = hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()

    private inner class Connection(val scope: TestScope) {
        val store = V3PhoneSyncStateStore()
        val sent = mutableListOf<ByteArray>()
        var rejected: V3PhoneSyncCommand? = null
        val repo = V3PhoneSyncRepositoryImpl(store, { packet ->
            sent += packet
            rejected?.let { !packet.contentEquals(V3PhoneSyncProtocol.packet(it)) } ?: true
        }, { scope.testScheduler.currentTime })
        val authorize = AuthorizePhoneUseCaseV3(repo)
        init { repo.beginSession() }
        fun receive(hex: String) { repo.onPacket(bytes(hex)) }
        fun begin(stage: Stage): Deferred<Boolean> {
            val result = scope.async { authorize() }
            scope.runCurrent()
            if (stage >= Stage.Checking) { scope.advanceTimeBy(300); scope.runCurrent() }
            if (stage >= Stage.Starting) { receive("00 04 01 00 5A"); scope.runCurrent() }
            if (stage >= Stage.WaitingForUser) { receive("00 04 02 02 B3"); scope.runCurrent() }
            assertEquals(stage.name, store.state.value.name)
            return result
        }
    }

    @ParameterizedTest @EnumSource(Stage::class)
    fun `disconnect is terminal in every pending phase`(stage: Stage) = runTest {
        val c = Connection(this)
        val result = c.begin(stage)
        c.repo.endSession(); runCurrent()
        assertFalse(result.await())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
        c.receive("00 04 02 01 51"); c.receive("00 05 00 00 35")
        assertFalse(c.repo.accessGranted)
    }

    @ParameterizedTest @EnumSource(Stage::class)
    fun `transport abort is terminal in every pending phase`(stage: Stage) = runTest {
        val c = Connection(this)
        val result = c.begin(stage)
        c.repo.cancel()
        // The controller's failure observer closes the physical connection.
        c.repo.endSession(); runCurrent()
        assertFalse(result.await())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
        assertFalse(c.repo.accessGranted)
    }

    @ParameterizedTest @EnumSource(V3PhoneSyncCommand::class)
    fun `send failure is terminal for each command`(command: V3PhoneSyncCommand) = runTest {
        val c = Connection(this).apply { rejected = command }
        val result = async { c.authorize() }
        runCurrent()
        if (command != V3PhoneSyncCommand.Echo) { advanceTimeBy(300); runCurrent() }
        if (command == V3PhoneSyncCommand.Start) { c.receive("00 04 01 00 5A"); runCurrent() }
        assertFalse(result.await())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
    }

    @Test fun `final failure does not require an earlier start ack`() = runTest {
        val c = Connection(this)
        val result = c.begin(Stage.Starting)
        c.receive("00 04 02 00 0F"); runCurrent()
        assertFalse(result.await())
        assertEquals(V3PhoneSyncState.Failed, c.store.state.value)
    }

    @Test fun `duplicate messages complete authorization only once`() = runTest {
        val c = Connection(this)
        val result = c.begin(Stage.Starting)
        c.receive("00 04 01 00 5A")
        c.receive("00 04 02 02 B3"); c.receive("00 04 02 02 B3"); runCurrent()
        assertFalse(result.isCompleted)
        c.receive("00 04 02 01 51"); c.receive("00 04 02 01 51"); runCurrent()
        assertTrue(result.await())
        assertTrue(c.authorize())
        c.receive("00 04 02 00 0F")
        assertEquals(V3PhoneSyncState.Authorized, c.store.state.value)
        assertEquals(3, c.sent.size)
    }

    @Test fun `a new authorized connection must be checked again on reconnect`() = runTest {
        val c = Connection(this)
        val first = c.begin(Stage.Checking)
        c.receive("00 04 01 01 04"); runCurrent()
        assertTrue(first.await())
        c.repo.endSession()
        assertFalse(c.repo.accessGranted)
        c.repo.beginSession()
        val second = async { c.authorize() }; runCurrent()
        assertFalse(second.isCompleted)
        advanceTimeBy(300); runCurrent()
        c.receive("00 04 01 01 04"); runCurrent()
        assertTrue(second.await())
        assertEquals(4, c.sent.size)
    }
}
