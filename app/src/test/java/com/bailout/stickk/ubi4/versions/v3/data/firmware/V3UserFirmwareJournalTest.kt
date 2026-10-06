package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.versions.v3.domain.firmware.UserFirmwareBackend
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.UserFirmwareCoordinator
import com.bailout.stickk.ubi4.firmware.user.*
import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareSession
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareStatus
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareBoard
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareTarget
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** Exercises common coordination through its existing backend, without a BLE transfer. */
@OptIn(ExperimentalCoroutinesApi::class)
class V3UserFirmwareJournalTest {
    @TempDir lateinit var directory: Path
    private var nextFile = 0
    private val repository by lazy {
        UserFirmwareRepository(directory.toString(), object : UserFirmwareArchiveReader {
            override fun read(path: String, callback: (UserFirmwareArchive?) -> Unit) {
                error("Journal operations must not read firmware archives")
            }
        }, mockk<YandexDiskFirmwareRepository>())
    }
    private val old = UserFirmwareVersion(0, 6, 9)
    private val latest = UserFirmwareVersion(0, 6, 10)
    private val targets = listOf(32, 0).map {
        UserFirmwareTarget(AssemblyModule(it, file = "$it.zip", sha256 = "a".repeat(64)), latest, "/cache/$it.zip")
    }

    @Test fun `missing blank and cleared journals offer an update without persisting consent`() = runTest {
        for (text in listOf(null, "", " \t\n", "null")) {
            val backend = Backend(text)
            val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
            coordinator.check()
            assertEquals("offered", coordinator.state.value.phase)
            assertTrue(coordinator.state.value.blocksInteraction)
            assertEquals(listOf("read", "boards", "targets"), backend.events)
            assertTrue(backend.writes.isEmpty())
        }
    }

    @Test fun `invalid JSON and another device journal stop before catalog or writes`() = runTest {
        for (text in listOf("{", " null ",
            Json.encodeToString(UserFirmwareJournal("another-device", targets, formatVersion = 2)))) {
            val backend = Backend(text)
            val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
            coordinator.check()
            assertEquals("unavailable", coordinator.state.value.phase)
            assertFalse(coordinator.state.value.blocksInteraction)
            assertTrue(coordinator.state.value.detail.isNotEmpty())
            assertEquals(listOf("read"), backend.events)
            assertTrue(backend.writes.isEmpty())
            assertFalse(coordinator.needsResume())
            assertEquals("USER_DFU", backend.messages.single().first)
            assertTrue(backend.messages.single().second.startsWith("check failed "))
            assertTrue(backend.messages.single().second.endsWith(": ${coordinator.state.value.detail}"))
        }
    }

    @Test fun `old journal formats including the default are cleared before rebuilding the offer`() = runTest {
        for (format in listOf(Int.MIN_VALUE, -1, 0, 1)) {
            val backend = Backend(Json.encodeToString(UserFirmwareJournal("00001", targets, formatVersion = format)))
            val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
            coordinator.check()
            assertEquals("offered", coordinator.state.value.phase)
            assertEquals(listOf("read", "clear", "boards", "targets"), backend.events)
            assertEquals(listOf<V3UserFirmwareSession<UserFirmwareTarget>?>(null), backend.writes)
            assertEquals("USER_DFU" to "discarded obsolete user-update journal", backend.messages.first())
        }
    }

    @Test fun `restored queue skips completed boards and preserves attempts until acknowledgement`() = runTest {
        // Existing check accepts formatVersion >= 2, including a higher version.
        for (format in listOf(2, 3, Int.MAX_VALUE)) {
            val saved = UserFirmwareJournal("00001", targets, setOf(32), setOf(32, 0), format)
            val backend = Backend(Json.encodeToString(saved))
            backend.currentBoards[0] = UserFirmwareBoard(0, old, false)
            val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
            coordinator.check()
            assertEquals("preparing", coordinator.state.value.phase)
            assertEquals(V3UserFirmwareStatus("preparing", 2, 2, blocksInteraction = true), coordinator.state.value)
            assertTrue(coordinator.needsResume())
            assertEquals(listOf("read"), backend.events)
            coordinator.start()
            assertEquals(listOf("read", "persist", "validate:0", "probe:0", "persist", "transfer:0", "persist"), backend.events)
            val restored = backend.writes.last()
            assertEquals(V3UserFirmwareSession("00001", targets, setOf(32, 0), setOf(32, 0), format), restored)
            assertEquals("complete", coordinator.state.value.phase)
            assertTrue(coordinator.state.value.blocksInteraction)
            coordinator.acknowledge()
            assertNull(backend.writes.last())
            assertEquals("idle", coordinator.state.value.phase)
            assertEquals(V3UserFirmwareStatus(), coordinator.state.value)
            assertFalse(coordinator.needsResume())
        }
    }

    @Test fun `fresh board mode version and prior attempt keep the existing transfer decisions`() = runTest {
        val newer = UserFirmwareVersion(1, 0, 0)
        val transferCases = setOf(Triple(false, true, old), Triple(false, false, old),
            Triple(true, false, old), Triple(false, false, newer), Triple(true, false, newer))
        val completedCases = setOf(Triple(false, true, latest), Triple(true, true, latest))
        for (attempted in listOf(false, true)) {
            for (main in listOf(false, true)) {
                for (version in listOf(null, old, latest, newer)) {
                    val saved = UserFirmwareJournal("00001", listOf(targets.first()),
                        attempted = if (attempted) setOf(32) else emptySet(), formatVersion = 2)
                    val backend = Backend(Json.encodeToString(saved))
                    backend.currentBoards[32] = UserFirmwareBoard(32, version, main)
                    val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
                    coordinator.check()
                    val job = launch { coordinator.start() }
                    runCurrent()
                    val case = Triple(attempted, main, version)
                    val label = "attempted=$attempted main=$main version=$version"
                    assertEquals(if (case in transferCases) listOf("transfer:32") else emptyList<String>(),
                        backend.events.filter { it.startsWith("transfer:") }, label)
                    if (case in transferCases || case in completedCases) {
                        assertEquals("complete", coordinator.state.value.phase, label)
                        assertFalse("wait" in backend.events, label)
                    } else {
                        assertEquals("verifying", coordinator.state.value.phase, label)
                        assertEquals(V3UserFirmwareStatus("verifying", 1, 1, blocksInteraction = true,
                            detail = "Waiting for confirmed board state and firmware version"),
                            coordinator.state.value, label)
                        assertEquals("wait", backend.events.last(), label)
                        assertEquals(V3UserFirmwareSession("00001", listOf(targets.first()),
                            attempted = if (attempted) setOf(32) else emptySet(), formatVersion = 2),
                            backend.writes.single(), label)
                        assertEquals("USER_DFU" to "waiting address=32 firstAttempt=${!attempted} installed=$version target=$latest main=$main",
                            backend.messages.last(), label)
                    }
                    val beforeCancel = backend.messages.toList()
                    val stateBeforeCancel = coordinator.state.value
                    job.cancelAndJoin()
                    assertEquals(beforeCancel, backend.messages, label)
                    assertEquals(stateBeforeCancel, coordinator.state.value, label)
                }
            }
        }
    }

    @Test fun `consent attempts and completion are persisted in order before the next transfer`() = runTest {
        val backend = Backend(null)
        val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
        val states = mutableListOf<V3UserFirmwareStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { coordinator.state.toList(states) }
        coordinator.check()
        coordinator.start()
        assertEquals(listOf("read", "boards", "targets", "persist", "validate:32", "validate:0",
            "probe:32", "persist", "transfer:32", "persist", "probe:0", "persist", "transfer:0", "persist"), backend.events)
        val initial = V3UserFirmwareSession("00001", targets, formatVersion = 2)
        assertEquals(initial, backend.writes.first())
        assertEquals(listOf(initial, initial.copy(attempted = setOf(32)),
            initial.copy(completed = setOf(32), attempted = setOf(32)),
            initial.copy(completed = setOf(32), attempted = setOf(32, 0)),
            initial.copy(completed = setOf(32, 0), attempted = setOf(32, 0))),
            backend.writes)
        assertEquals("complete", coordinator.state.value.phase)
        assertEquals(100, coordinator.state.value.progress)
        assertEquals(listOf(
            V3UserFirmwareStatus(),
            V3UserFirmwareStatus("checking"),
            V3UserFirmwareStatus("offered", 1, 2, blocksInteraction = true),
            V3UserFirmwareStatus("preparing", 1, 2, blocksInteraction = true),
            V3UserFirmwareStatus("verifying", 1, 2, blocksInteraction = true),
            V3UserFirmwareStatus("updating", 1, 2, blocksInteraction = true),
            V3UserFirmwareStatus("updating", 1, 2, 67, blocksInteraction = true),
            V3UserFirmwareStatus("verifying", 2, 2, blocksInteraction = true),
            V3UserFirmwareStatus("updating", 2, 2, blocksInteraction = true),
            V3UserFirmwareStatus("updating", 2, 2, 67, blocksInteraction = true),
            V3UserFirmwareStatus("complete", 2, 2, 100, blocksInteraction = true),
        ), states)
        assertEquals(listOf(
            "targets=32:0.6.10, 0:0.6.10 queue=32:0.6.10, 0:0.6.10",
            "probe address=32 target=0.6.10 installed=0.6.9 main=true attempted=false",
            "probe address=0 target=0.6.10 installed=0.6.9 main=true attempted=false",
        ).map { "USER_DFU" to it }, backend.messages)
    }

    @Test fun `postponing clears displayed progress without persisting or rebuilding the offer`() = runTest {
        val backend = Backend(null)
        val coordinator = UserFirmwareCoordinator("00001", backend, backend::log)
        coordinator.check()

        coordinator.postpone()
        coordinator.check()

        assertEquals(V3UserFirmwareStatus(), coordinator.state.value)
        assertEquals(listOf("read", "boards", "targets"), backend.events)
        assertTrue(backend.writes.isEmpty())
        assertFalse(coordinator.needsResume())
    }

    @Test fun `repository preserves journal JSON through replacement and clearing`() = runTest {
        val file = directory.resolve("nested/session.json").toFile()
        val saved = V3UserFirmwareSession("00001", listOf(targets.first()), setOf(32), setOf(32), 2)
        val expected = """{"deviceId":"00001","targets":[{"module":{"address":32,"file":"32.zip","sha256":"${"a".repeat(64)}"},"version":{"major":0,"minor":6,"patch":10},"path":"/cache/32.zip"}],"completed":[32],"attempted":[32],"formatVersion":2}"""
        assertNull(repository.readJournal(file.path))

        repository.writeJournal(file.path, saved)

        assertEquals(expected, file.readText())
        assertEquals(saved, repository.readJournal(file.path))
        val next = saved.copy(completed = emptySet(), attempted = emptySet(), formatVersion = 1)
        repository.writeJournal(file.path, next)
        assertEquals(next, repository.readJournal(file.path))
        assertEquals(Json.encodeToString(UserFirmwareJournal("00001", listOf(targets.first()))), file.readText())
        assertFalse(file.resolveSibling(file.name + ".tmp").exists())

        repository.writeJournal(file.path, null)

        assertEquals("null", file.readText())
        assertNull(repository.readJournal(file.path))
        assertFalse(file.resolveSibling(file.name + ".tmp").exists())
    }

    @Test fun `existing journal metadata and unknown progress addresses survive a session round trip`() = runTest {
        val file = directory.resolve("session.json").toFile()
        val archives = listOf(targets.last(), targets.first().copy(
            module = targets.first().module.copy(name = "Board 32", file = "BLDC_Driver/main.zip"),
            path = "/cache with space/прошивка.zip",
        ))
        for (format in listOf(1, 2, 3, Int.MAX_VALUE)) {
            val text = Json.encodeToString(UserFirmwareJournal("device/00001", archives,
                completed = setOf(99, 32), attempted = setOf(77, 0, 32), formatVersion = format))
            file.writeText(text)

            val session = repository.readJournal(file.path)

            assertEquals(V3UserFirmwareSession("device/00001", archives, setOf(99, 32), setOf(77, 0, 32), format), session)
            repository.writeJournal(file.path, session)
            assertEquals(text, file.readText())
        }
    }

    @Test fun `coordinator accepts pure board and target contracts without archive DTOs`() = runTest {
        val target = object : V3UserFirmwareTarget<Int> {
            override val address = 32
            override val version = 10
        }
        val board = object : V3UserFirmwareBoard<Int> {
            override val address = 32
            override val version: Int? = 9
            override val isMain = true
        }
        val events = mutableListOf<String>()
        val writes = mutableListOf<V3UserFirmwareSession<V3UserFirmwareTarget<Int>>?>()
        val backend = object : UserFirmwareBackend<Int, V3UserFirmwareTarget<Int>> {
            override suspend fun boards(): List<V3UserFirmwareBoard<Int>> { events += "boards"; return listOf(board) }
            override suspend fun targets(boards: List<V3UserFirmwareBoard<Int>>): List<V3UserFirmwareTarget<Int>> {
                events += "targets"
                assertSame(board, boards.single())
                return listOf(target)
            }
            override suspend fun validate(target: V3UserFirmwareTarget<Int>) {
                events += "validate"
                assertSame(writes.first()!!.targets.single(), target)
            }
            override suspend fun probe(address: Int): V3UserFirmwareBoard<Int> {
                events += "probe"
                assertEquals(32, address)
                return board
            }
            override suspend fun transfer(target: V3UserFirmwareTarget<Int>, progress: (Int) -> Unit) {
                events += "transfer"
                assertSame(writes.first()!!.targets.single(), target)
                progress(100)
            }
            override suspend fun readJournal(): V3UserFirmwareSession<V3UserFirmwareTarget<Int>>? { events += "read"; return null }
            override suspend fun writeJournal(session: V3UserFirmwareSession<V3UserFirmwareTarget<Int>>?) {
                events += if (session == null) "clear" else "persist"
                writes += session
            }
            override suspend fun awaitChange() { events += "wait"; awaitCancellation() }
            override fun isSameDevice() = true
        }
        val coordinator = UserFirmwareCoordinator("00001", backend) { _, _ -> }
        coordinator.check()
        coordinator.start()

        assertEquals(listOf("read", "boards", "targets", "persist", "validate", "probe", "persist", "transfer", "persist"), events)
        assertSame(target, writes.first()!!.targets.single())
        assertEquals(V3UserFirmwareSession("00001", listOf(target), setOf(32), setOf(32), 2), writes.last())
        assertEquals("complete", coordinator.state.value.phase)
        assertEquals(100, coordinator.state.value.progress)
        coordinator.acknowledge()
        assertNull(writes.last())
        assertEquals("idle", coordinator.state.value.phase)
    }

    private inner class Backend(private val journalText: String?) : UserFirmwareBackend<UserFirmwareVersion, UserFirmwareTarget> {
        val events = mutableListOf<String>()
        val messages = mutableListOf<Pair<String, String>>()
        fun log(tag: String, message: String) { messages += tag to message }
        val writes = mutableListOf<V3UserFirmwareSession<UserFirmwareTarget>?>()
        private val journalFile = directory.resolve("session-${nextFile++}.json").toFile().apply {
            if (journalText != null) writeText(journalText)
        }
        val currentBoards = targets.associate { it.module.address to UserFirmwareBoard(it.module.address, old, true) }.toMutableMap()
        override suspend fun readJournal(): V3UserFirmwareSession<UserFirmwareTarget>? {
            events += "read"
            return repository.readJournal(journalFile.path)
        }
        override suspend fun writeJournal(session: V3UserFirmwareSession<UserFirmwareTarget>?) {
            events += if (session == null) "clear" else "persist"
            writes += session
        }
        override suspend fun boards(): List<UserFirmwareBoard> { events += "boards"; return currentBoards.values.toList() }
        override suspend fun targets(boards: List<V3UserFirmwareBoard<UserFirmwareVersion>>): List<UserFirmwareTarget> { events += "targets"; return targets }
        override suspend fun validate(target: UserFirmwareTarget) { events += "validate:${target.module.address}" }
        override suspend fun probe(address: Int): UserFirmwareBoard { events += "probe:$address"; return currentBoards.getValue(address) }
        override suspend fun transfer(target: UserFirmwareTarget, progress: (Int) -> Unit) {
            events += "transfer:${target.module.address}"
            progress(67)
        }
        override suspend fun awaitChange(): Nothing { events += "wait"; awaitCancellation() }
        override fun isSameDevice(): Boolean = true
    }
}
