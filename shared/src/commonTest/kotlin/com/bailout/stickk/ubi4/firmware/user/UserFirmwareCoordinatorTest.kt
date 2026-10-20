package com.bailout.stickk.ubi4.firmware.user

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserFirmwareCoordinatorTest {
    private val old = UserFirmwareVersion(0, 6, 9)
    private val latest = UserFirmwareVersion(0, 6, 10)
    private fun target(address: Int) = UserFirmwareTarget(AssemblyModule(address, "Board", "$address.zip", "a".repeat(64)), latest, "/cache/$address.zip")
    private fun board(address: Int, version: UserFirmwareVersion?, main: Boolean = true, code: Int = 2) =
        UserFirmwareBoard(address, version, main, deviceCode = code)

    @Test fun skipsAbsentEqualAndNewerAndKeepsFamLast() {
        val boards = listOf(board(0, old), board(9, latest),
            board(32, UserFirmwareVersion(1, 0, 0)), board(33, old))
        assertEquals(listOf(33, 0), UserFirmwarePolicy.queue(boards, listOf(target(0), target(9), target(32), target(33), target(34))).map { it.module.address })
    }

    @Test fun unknownVersionDoesNotBecomeZero() {
        assertNull(UserFirmwareVersion.parse("—"))
        assertNull(UserFirmwareVersion.parse("0.x.10"))
        assertNull(UserFirmwareVersion.parse("0.6.10.20"))
        assertTrue(UserFirmwarePolicy.queue(listOf(board(9, null)), listOf(target(9))).isEmpty())
    }

    @Test fun placeholderFingerDoesNotJoinFamQueueOrBoardCount() = runTest {
        val backend = FakeBackend().apply {
            boards[32] = board(32, UserFirmwareVersion(0, 0, 0), code = 0)
        }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        assertEquals("offered", coordinator.state.value.phase)
        assertEquals(1, coordinator.state.value.boardCount)
        coordinator.start()
        assertEquals(listOf(0), backend.transfers)
        assertEquals(listOf(0), backend.probes)
        assertEquals("complete", coordinator.state.value.phase)
    }

    @Test fun bothIdentityAndNonzeroVersionAreRequired() {
        val boards = listOf(board(32, old, code = 0), board(33, UserFirmwareVersion(0, 0, 0)))
        assertTrue(UserFirmwarePolicy.queue(boards, listOf(target(32), target(33))).isEmpty())
    }

    @Test fun unstartedSavedQueueIsRebuiltAndRequiresConsentAgain() = runTest {
        val backend = FakeBackend().apply {
            boards[32] = board(32, UserFirmwareVersion(0, 0, 0), code = 0)
            saved = Json.encodeToString(UserFirmwareJournal("device", listOf(target(32), target(0)), formatVersion = 2))
        }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        assertEquals("offered", coordinator.state.value.phase)
        assertEquals(1, coordinator.state.value.boardCount)
        assertFalse(coordinator.needsResume())
        assertEquals("null", backend.saved)
        assertTrue(backend.transfers.isEmpty())
    }

    @Test fun networkErrorDoesNotOfferOrStartAnUpdate() = runTest {
        val backend = FakeBackend().apply { catalogError = true }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        assertEquals("unavailable", coordinator.state.value.phase)
        assertFalse(coordinator.state.value.blocksInteraction)
        assertTrue(backend.transfers.isEmpty())
    }

    @Test fun postponeDismissesOfferWithoutPersistingOrStartingUpdate() = runTest {
        val backend = FakeBackend()
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        assertEquals("offered", coordinator.state.value.phase)

        coordinator.postpone()

        assertEquals("idle", coordinator.state.value.phase)
        assertFalse(coordinator.state.value.blocksInteraction)
        assertNull(backend.saved)
        assertTrue(backend.transfers.isEmpty())

        // A foreground/network callback in the same session must not reopen it.
        coordinator.check()
        assertEquals("idle", coordinator.state.value.phase)
    }

    @Test fun corruptedArchiveStopsBeforeFirstTransfer() = runTest {
        val backend = FakeBackend()
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        backend.invalidArchive = true
        coordinator.start()
        assertEquals("unavailable", coordinator.state.value.phase)
        assertFalse(coordinator.state.value.blocksInteraction)
        assertTrue(backend.transfers.isEmpty())
        assertEquals("null", backend.saved)
        assertEquals(1, backend.validations)
    }

    @Test fun silentBoardStopsAfterOneBoundedProbe() = runTest {
        val backend = FakeBackend().apply { silentProbe = true }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        coordinator.start()
        assertEquals("unavailable", coordinator.state.value.phase)
        assertFalse(coordinator.state.value.blocksInteraction)
        assertEquals(listOf(32), backend.probes)
        assertTrue(backend.transfers.isEmpty())
        assertEquals("null", backend.saved)
        assertEquals(15_000L, testScheduler.currentTime)
    }

    @Test fun boardThatLosesItsIdentityCannotBeWritten() = runTest {
        val backend = FakeBackend()
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        backend.boards[32] = board(32, old, code = 0)
        coordinator.start()
        assertEquals("unavailable", coordinator.state.value.phase)
        assertTrue(backend.transfers.isEmpty())
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
        backend.boards[0] = board(0, latest)
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

    @Test fun failedTransferWaitsForReconnectAndResumesWithoutAnotherStart() = runTest {
        val backend = FakeBackend().apply { failures = 3 }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        val job = launch { coordinator.start() }
        runCurrent()
        assertEquals(listOf(32), backend.transfers)
        assertEquals("waiting", coordinator.state.value.phase)
        assertTrue(coordinator.state.value.blocksInteraction)
        val saved = Json.decodeFromString<UserFirmwareJournal>(backend.saved!!)
        assertEquals(setOf(32), saved.attempted)
        assertTrue(saved.completed.isEmpty())
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(listOf(32), backend.transfers)
        backend.failures = 0
        backend.reconnect()
        runCurrent()
        assertEquals(listOf(32, 32, 0), backend.transfers)
        assertEquals("complete", coordinator.state.value.phase)
        job.join()
    }

    @Test fun famResumesInBootWithoutAMainVersionOrBoardIdentity() = runTest {
        val backend = FakeBackend().apply {
            boards.remove(32)
            failures = 1
        }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        val job = launch { coordinator.start() }
        runCurrent()
        assertEquals("waiting", coordinator.state.value.phase)
        backend.boards[0] = board(0, null, main = false, code = 0)
        backend.reconnect()
        runCurrent()
        assertEquals(listOf(0, 0), backend.transfers)
        assertEquals(1, coordinator.state.value.boardCount)
        assertEquals("complete", coordinator.state.value.phase)
        job.join()
    }

    @Test fun reconnectBeforeTransferErrorIsNotLost() = runTest {
        val backend = FakeBackend().apply {
            failures = 1
            reconnectBeforeError = true
        }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        coordinator.start()
        assertEquals(listOf(32, 32, 0), backend.transfers)
        assertEquals("complete", coordinator.state.value.phase)
    }

    @Test fun interruptedTransferCannotLeaveAnUnboundedResponseWait() = runTest {
        val backend = FakeBackend().apply { silentTransfer = true }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        val job = launch { coordinator.start() }
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals("waiting", coordinator.state.value.phase)
        assertEquals(listOf(32), backend.transfers)
        backend.silentTransfer = false
        backend.reconnect()
        runCurrent()
        assertEquals(listOf(32, 32, 0), backend.transfers)
        assertEquals("complete", coordinator.state.value.phase)
        job.join()
    }

    @Test fun restartInBootRecoversThePersistedFamTargetWithoutOfferingAgain() = runTest {
        val backend = FakeBackend().apply {
            boards.clear()
            boards[0] = board(0, null, main = false, code = 0)
            saved = Json.encodeToString(UserFirmwareJournal(
                "device", listOf(target(0)), attempted = setOf(0), formatVersion = 2
            ))
        }
        val coordinator = UserFirmwareCoordinator("device", backend)
        coordinator.check()
        assertTrue(coordinator.needsResume())
        coordinator.start()
        assertEquals(listOf(0), backend.transfers)
        assertEquals("complete", coordinator.state.value.phase)
    }

    @Test fun doesNotDeclareBootloaderWithTargetVersionSuccessful() {
        assertFalse(UserFirmwarePolicy.completed(board(32, latest, false), target(32)))
        assertFalse(UserFirmwarePolicy.retry(board(32, latest, false), target(32)))
        assertFalse(UserFirmwarePolicy.retry(board(32, old), target(32)))
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
        val boards = mutableMapOf(0 to board(0, old), 32 to board(32, old))
        val transfers = mutableListOf<Int>()
        val probes = mutableListOf<Int>()
        var validations = 0
        var silentProbe = false
        var saved: String? = null
        var catalogError = false
        var invalidArchive = false
        var interruptAt: Int? = null
        var failures = 0
        var silentTransfer = false
        var reconnectBeforeError = false
        private val revisions = MutableStateFlow(0L)
        fun reconnect() { revisions.value += 1 }
        override suspend fun boards() = boards.values.toList()
        override suspend fun targets(boards: List<UserFirmwareBoard>): List<UserFirmwareTarget> {
            if (catalogError) error("Network unavailable")
            return listOf(target(0), target(32))
        }
        override suspend fun validate(target: UserFirmwareTarget) {
            validations++
            if (invalidArchive) error("SHA-256 mismatch")
        }
        override suspend fun probe(address: Int): UserFirmwareBoard {
            probes += address
            if (silentProbe) awaitCancellation()
            return boards.getValue(address)
        }
        override suspend fun transfer(target: UserFirmwareTarget, progress: (Int) -> Unit) {
            val address = target.module.address
            transfers += address
            if (silentTransfer) awaitCancellation()
            if (address == interruptAt) awaitCancellation()
            if (failures-- > 0) {
                boards[address] = board(address, old, false)
                if (reconnectBeforeError) reconnect()
                error("Transfer failed")
            }
            boards[address] = board(address, latest)
            progress(100)
        }
        override suspend fun readJournal() = saved
        override suspend fun writeJournal(text: String) { saved = text }
        override val changeRevision: Long get() = revisions.value
        override suspend fun awaitChange(afterRevision: Long) { revisions.first { it > afterRevision } }
        override fun isSameDevice() = true
    }
}
