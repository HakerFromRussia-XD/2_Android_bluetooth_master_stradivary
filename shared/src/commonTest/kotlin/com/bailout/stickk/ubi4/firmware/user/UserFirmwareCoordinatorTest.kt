package com.bailout.stickk.ubi4.firmware.user

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserFirmwareCoordinatorTest {
    private val old = UserFirmwareVersion(0, 6, 9)
    private val latest = UserFirmwareVersion(0, 6, 10)
    private fun target(address: Int) = UserFirmwareTarget(AssemblyModule(address, "Board", "$address.zip", "a".repeat(64)), latest, "/cache/$address.zip")

    @Test fun skipsAbsentEqualAndNewerAndKeepsFamLast() {
        val boards = listOf(UserFirmwareBoard(0, old, true), UserFirmwareBoard(9, latest, true),
            UserFirmwareBoard(32, UserFirmwareVersion(1, 0, 0), true), UserFirmwareBoard(33, old, true))
        assertEquals(listOf(33, 0), UserFirmwarePolicy.queue(boards, listOf(target(0), target(9), target(32), target(33), target(34))).map { it.module.address })
    }

    @Test fun unknownVersionDoesNotBecomeZero() {
        assertNull(UserFirmwareVersion.parse("—"))
        assertNull(UserFirmwareVersion.parse("0.x.10"))
        assertNull(UserFirmwareVersion.parse("0.6.10.20"))
        assertFailsWith<IllegalArgumentException> {
            UserFirmwarePolicy.queue(listOf(UserFirmwareBoard(9, null, true)), listOf(target(9)))
        }
    }

    @Test fun networkErrorDoesNotOfferOrStartAnUpdate() = runTest {
        val backend = FakeBackend().apply { catalogError = true }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        assertEquals("unavailable", coordinator.state.value.phase)
        assertFalse(coordinator.state.value.blocksInteraction)
        assertTrue(backend.transfers.isEmpty())
    }

    @Test fun corruptedArchiveStopsBeforeFirstTransfer() = runTest {
        val backend = FakeBackend()
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        backend.invalidArchive = true
        val job = launch { coordinator.start() }
        runCurrent()
        assertEquals("waiting", coordinator.state.value.phase)
        assertTrue(coordinator.state.value.blocksInteraction)
        assertTrue(backend.transfers.isEmpty())
        job.cancelAndJoin()
    }

    @Test fun restartKeepsCompletedStepsAndChecksInFlightBoardBeforeWriting() = runTest {
        val backend = FakeBackend().apply { interruptAt = 0 }
        val first = UserFirmwareCoordinator("device", backend)
        first.check()
        val job = launch { first.start() }
        runCurrent()
        assertEquals(listOf(32, 0), backend.transfers)
        job.cancelAndJoin()
        // Flash succeeded before process loss; journal still describes an in-flight board.
        backend.boards[0] = UserFirmwareBoard(0, latest, true)
        backend.interruptAt = null
        val resumed = UserFirmwareCoordinator("device", backend)
        resumed.check()
        assertTrue(resumed.needsResume())
        resumed.start()
        assertEquals(listOf(32, 0), backend.transfers)
        assertEquals("complete", resumed.state.value.phase)
        assertTrue(resumed.state.value.blocksInteraction)
        resumed.acknowledge()
        assertFalse(resumed.state.value.blocksInteraction)
    }

    @Test fun failedTransferChecksStateAndRetriesWithoutAdvancingTime() = runTest {
        val backend = FakeBackend().apply { failures = 3 }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        coordinator.start()
        assertEquals(listOf(32, 32, 32, 32, 0), backend.transfers)
        assertEquals(0L, testScheduler.currentTime)
        assertEquals("complete", coordinator.state.value.phase)
    }

    @Test fun doesNotDeclareBootloaderWithTargetVersionSuccessful() {
        assertFalse(UserFirmwarePolicy.completed(UserFirmwareBoard(32, latest, false), target(32)))
        assertFalse(UserFirmwarePolicy.retry(UserFirmwareBoard(32, latest, false), target(32)))
        assertFalse(UserFirmwarePolicy.retry(UserFirmwareBoard(32, old, true), target(32)))
    }

    @Test fun manifestContainsReferencesAndRejectsDamagedChecksumAndDuplicateAddresses() {
        val modules = "[{\"address\":32,\"file\":\"BLDC_Driver/main.zip\",\"sha256\":\"${"a".repeat(64)}\"}]"
        val raw = """{"format":1,"kind":"assembly","created_at":"2026-09-16T12:00:00+03:00","created_by":"test","product":"V3","config_version":1,"modules":$modules}"""
        val obj = Json.parseToJsonElement(raw).jsonObject
        val checksum = firmwareSha256(FirmwareAssembly.canonical(obj).encodeToByteArray())
        val valid = JsonObject(obj + ("checksum" to JsonPrimitive(checksum))).toString()
        val manifest = FirmwareAssembly.parse(valid)
        assertEquals("BLDC_Driver/main.zip", manifest.modules.single().file)
        assertFailsWith<IllegalArgumentException> { FirmwareAssembly.parse(valid.replace("main.zip", "bad.zip")) }
        val duplicated = JsonObject(obj + ("modules" to JsonArray(listOf(obj["modules"]!!.jsonArray[0], obj["modules"]!!.jsonArray[0]))))
        val signedDuplicate = JsonObject(duplicated + ("checksum" to JsonPrimitive(firmwareSha256(FirmwareAssembly.canonical(duplicated).encodeToByteArray()))))
        assertFailsWith<IllegalArgumentException> { FirmwareAssembly.parse(signedDuplicate.toString()) }
        val slot = Json.decodeFromString<FirmwareModuleSlotVersion>("""{"version":1,"subversion":8,"quickfiks":2}""")
        assertEquals(2, slot.quickfiks)
    }

    @Test fun everyInteractiveUpdatePhaseBlocksTheApp() {
        listOf("offered", "preparing", "updating", "verifying", "waiting", "complete").forEach {
            assertTrue(UserFirmwareUiState(phase = it).blocksInteraction, it)
        }
        listOf("idle", "checking", "unavailable").forEach {
            assertFalse(UserFirmwareUiState(phase = it).blocksInteraction, it)
        }
    }

    private inner class FakeBackend : UserFirmwareBackend {
        val boards = mutableMapOf(0 to UserFirmwareBoard(0, old, true), 32 to UserFirmwareBoard(32, old, true))
        val transfers = mutableListOf<Int>()
        var saved: String? = null
        var catalogError = false
        var invalidArchive = false
        var interruptAt: Int? = null
        var failures = 0
        override suspend fun boards() = boards.values.toList()
        override suspend fun targets(boards: List<UserFirmwareBoard>): List<UserFirmwareTarget> {
            if (catalogError) error("Network unavailable")
            return listOf(target(0), target(32))
        }
        override suspend fun validate(target: UserFirmwareTarget) { if (invalidArchive) error("SHA-256 mismatch") }
        override suspend fun probe(address: Int) = boards.getValue(address)
        override suspend fun transfer(target: UserFirmwareTarget, progress: (Int) -> Unit) {
            val address = target.module.address
            transfers += address
            if (address == interruptAt) awaitCancellation()
            if (failures-- > 0) {
                boards[address] = UserFirmwareBoard(address, old, false)
                error("Transfer failed")
            }
            boards[address] = UserFirmwareBoard(address, latest, true)
            progress(100)
        }
        override suspend fun readJournal() = saved
        override suspend fun writeJournal(text: String) { saved = text }
        override suspend fun awaitChange() { awaitCancellation() }
        override fun isSameDevice() = true
    }
}
