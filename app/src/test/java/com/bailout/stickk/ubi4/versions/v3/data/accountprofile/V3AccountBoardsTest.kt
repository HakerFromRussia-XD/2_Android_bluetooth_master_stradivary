package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.firmware.FirmwareVersionCatalog
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.RunProgramType
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountBridge
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountBridgeBoard
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountBoardsTest {
    private val repository = V3AccountBoardsRepositoryImpl()
    private val getBoards = GetAccountBoardsUseCaseV3(repository, FirmwareVersionCatalog::isZeroVersion)
    private lateinit var savedBoards: MutableSet<BaseSubDeviceInfoStruct>
    private lateinit var savedV3Boards: MutableSet<BaseSubDeviceInfoStruct>
    private lateinit var savedUpdates: MutableSharedFlow<Int>

    @BeforeEach fun saveGlobals() {
        savedBoards = GlobalParameters.baseSubDevicesInfoStructSet
        savedV3Boards = GlobalParameters.baseSubDevicesInfoStructSetV3
        savedUpdates = UiState.updateFlow
        GlobalParameters.baseSubDevicesInfoStructSet = linkedSetOf()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = linkedSetOf()
    }

    @AfterEach fun restoreGlobals() {
        GlobalParameters.baseSubDevicesInfoStructSet = savedBoards
        GlobalParameters.baseSubDevicesInfoStructSetV3 = savedV3Boards
        UiState.updateFlow = savedUpdates
    }

    @Test fun `original board source wins and known device code takes priority over address family`() {
        val source = linkedSetOf(
            sub(0x09, code = 5, version = " 2.3.4 ", isBoot = 1),
            sub(0x72, code = 4, version = "1.0.0"),
        )
        GlobalParameters.baseSubDevicesInfoStructSet = source
        GlobalParameters.baseSubDevicesInfoStructSetV3 = linkedSetOf(sub(0x70, code = 6, version = "9.0.0"))
        val rows = repository.getBoards()
        source.clear()

        assertEquals(listOf("Bms", "Emg sense"), rows.map { it.name })
        assertEquals(listOf(0x09, 0x72), rows.map { it.deviceAddress })
        assertEquals(listOf(5, 4), rows.map { it.deviceCode })
        assertEquals(" 2.3.4 ", rows.first().version)
        assertTrue(rows.all { it.canUpdate && !it.isInBootloader })
    }

    @Test fun `unknown codes fall back only to the existing supported address families`() {
        GlobalParameters.baseSubDevicesInfoStructSet = linkedSetOf(
            sub(0x00), sub(0x09), sub(0x11), sub(0x12), sub(0x20), sub(0x25), sub(0x26), sub(0x70),
        )
        assertEquals(listOf("FAM", "GUI", "EMG", "EMG", "BLDC", "BLDC", null, null),
            repository.getBoards().map { it.name })
        assertEquals(listOf(0x00, 0x09, 0x11, 0x12, 0x20, 0x25),
            getBoards(emptyList()).boards?.map { it.deviceAddress })
    }

    @Test fun `native raw snapshots match actual account bridge including unknown zero blank duplicates and empty`() {
        val nativeRepository = V3AccountBoardsRepositoryImpl(useAddressNameFallback = false)
        var policyCalls = 0
        val nativeGet = GetAccountBoardsUseCaseV3(nativeRepository) {
            policyCalls++
            FirmwareVersionCatalog.isZeroVersion(it)
        }
        val previousCache = nativeRepository.getCachedBoards()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = linkedSetOf(sub(0x70, code = 6, version = "9.0.0"))
        val stages = listOf(
            linkedSetOf<BaseSubDeviceInfoStruct>(),
            linkedSetOf(
                sub(0x72, version = ""),
                sub(0x20, code = 9, version = "0.0.0", isBoot = 1),
                sub(0x09, code = 6, version = "  \t", isBoot = 1),
                sub(0x20, code = 9, version = "8.0.0"),
                sub(0x00, version = "1.0.0"),
                sub(0x09, code = 5, version = "9.0.0"),
            ),
            linkedSetOf(sub(0x09, version = "0.0.0"), sub(0x70, version = "   ")),
            linkedSetOf(
                sub(1, code = 2, version = "\u00A0"),
                sub(2, code = 6, version = "\u2007"),
                sub(3, code = 4, version = "\u202F"),
                sub(4, code = 5, version = "\u200B"),
            ),
            linkedSetOf(),
        )
        for (source in stages) {
            GlobalParameters.baseSubDevicesInfoStructSet = source
            val raw = nativeGet.current(missingVersion = "-")
            val nativeRows = raw.map { board ->
                AccountBridgeBoard(
                    boardName = requireNotNull(board.name),
                    deviceCode = board.deviceCode,
                    deviceAddress = board.deviceAddress,
                    version = requireNotNull(board.version),
                    canUpdate = board.canUpdate,
                    isInBootloader = board.isInBootloader,
                )
            }
            assertEquals(AccountBridge.currentBoards(), nativeRows)
            assertTrue(raw.all { it.canUpdate && !it.isInBootloader && it.isUpdateAvailable == null })
            assertSame(source, GlobalParameters.baseSubDevicesInfoStructSet)
        }
        assertEquals(0, policyCalls)
        assertEquals(previousCache, nativeRepository.getCachedBoards())
        GlobalParameters.baseSubDevicesInfoStructSet = linkedSetOf(sub(0x09), sub(0x70))
        assertEquals(listOf("Unknown", "Unknown"), nativeGet.current(missingVersion = "-").map { it.name })
        assertEquals(listOf("GUI", null), repository.getBoards().map { it.name })
    }

    @Test fun `current formats only missing versions with explicit fallback and preserves names modes and independent snapshots`() {
        val source = mutableListOf(
            board(9, name = "Unknown", version = "0.0.0").copy(isInBootloader = true, canUpdate = false),
            board(2, name = null, version = null).copy(isUpdateAvailable = true),
            board(9, name = "replacement", version = "4.0.0"),
            board(3, name = " raw ", version = "  "),
        )
        val cached = listOf(board(99))
        val fake = FakeBoards(source, cached)
        var policyCalls = 0
        val useCase = GetAccountBoardsUseCaseV3(fake) { policyCalls++; FirmwareVersionCatalog.isZeroVersion(it) }
        val first = useCase.current(missingVersion = "missing")
        assertEquals(listOf(source[1].copy(version = "missing"), source[3].copy(version = "missing"), source[0]), first)
        assertNull(first.first().name)
        assertEquals("missing", first.first().version)
        assertTrue(first.last().isInBootloader)
        assertFalse(first.last().canUpdate)
        source.clear()
        assertTrue(useCase.current(missingVersion = "-").isEmpty())
        assertEquals(3, first.size)
        source += board(1, version = "")
        assertEquals(listOf(source.single().copy(version = "")), useCase.current(missingVersion = ""))
        assertEquals(cached, fake.getCachedBoards())
        assertEquals(0, policyCalls)
    }

    @Test fun `bootloader callbacks match bridge without initial replay retain repeats and cancel independently`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val source = FirmwareInfoState.runProgramTypeFlow
        val values = mutableListOf<Pair<Int, Boolean>>()
        val bridgeValues = mutableListOf<Pair<Int, Boolean>>()
        val jobs = mutableListOf<Job>()
        val previousCache = repository.getCachedBoards()
        val previousSource = GlobalParameters.baseSubDevicesInfoStructSet
        try {
            source.emit(0x09 to RunProgramType.BOOTLOADER)
            val observe = ObserveAccountBoardsUseCaseV3(repository)
            assertEquals(0, source.subscriptionCount.value)
            val current = observe.observeBootloaderChanges { address, bootloader -> values += address to bootloader }
            jobs += current
            jobs += AccountBridge.observeBoardMode { mode -> bridgeValues += mode.deviceAddress to mode.isInBootloader }
            assertTrue(values.isEmpty())
            runCurrent()
            assertTrue(values.isEmpty())
            assertTrue(bridgeValues.isEmpty())
            assertEquals(2, source.subscriptionCount.value)
            val events = listOf(
                -7 to RunProgramType.BOOTLOADER,
                0x72 to RunProgramType.BOOTLOADER_V2,
                0x72 to RunProgramType.BOOTLOADER_V2,
                0x72 to RunProgramType.MAIN_APP,
                Int.MIN_VALUE to RunProgramType.MAIN_APP,
                Int.MAX_VALUE to RunProgramType.BOOTLOADER,
            )
            for (event in events) { source.emit(event); runCurrent() }
            assertEquals(events.map { (address, type) -> address to type.isBootloader }, values)
            assertEquals(bridgeValues, values)
            current.cancelAndJoin()
            source.emit(0x09 to RunProgramType.MAIN_APP); runCurrent()
            assertEquals(events.size, values.size)
            assertEquals(1, source.subscriptionCount.value)
            val restarted = mutableListOf<Pair<Int, Boolean>>()
            jobs += observe.observeBootloaderChanges { address, bootloader -> restarted += address to bootloader }
            runCurrent()
            assertTrue(restarted.isEmpty())
            source.emit(0x11 to RunProgramType.BOOTLOADER_V2); runCurrent()
            assertEquals(listOf(0x11 to true), restarted)
            assertEquals(bridgeValues.last(), restarted.single())
            assertEquals(previousCache, repository.getCachedBoards())
            assertSame(previousSource, GlobalParameters.baseSubDevicesInfoStructSet)
        } finally {
            jobs.forEach { it.cancelAndJoin() }
            assertEquals(0, source.subscriptionCount.value)
            Dispatchers.resetMain()
        }
    }

    @Test fun `first duplicate is selected before hiding rows while all first versions reach the catalog`() {
        GlobalParameters.baseSubDevicesInfoStructSet = linkedSetOf(
            sub(0x20, code = 9, version = "0.0.0"),
            sub(0x20, code = 9, version = "8.0.0"),
            sub(0x72, version = "3.0.0"),
            sub(0x72, code = 6, version = "4.0.0"),
            sub(0x11, code = 4, version = "  "),
            sub(0x09, code = 6, version = "1.0.0"),
            sub(0x09, code = 6, version = "9.0.0"),
            sub(0x00, code = 2, version = "2.0.0"),
        )
        val snapshot = getBoards(emptyList())
        assertEquals(listOf(0x00, 0x09, 0x11), snapshot.boards?.map { it.deviceAddress })
        assertEquals(listOf("2.0.0", "1.0.0", "—"), snapshot.boards?.map { it.version })
        assertEquals(mapOf(0x20 to "0.0.0", 0x72 to "3.0.0", 0x11 to "—", 0x09 to "1.0.0", 0x00 to "2.0.0"),
            snapshot.installedVersions)
    }

    @Test fun `blank and malformed versions stay visible while the shared zero-version policy hides numeric zero`() {
        val versions = listOf(null, "", "   ", "0", "00.0", " 0.0.0 ", "0.", "0.a", "0.1", "unknown", "1.2.3.20")
        val source = versions.mapIndexed { address, version -> board(address, version = version) }
        val snapshot = GetAccountBoardsUseCaseV3(FakeBoards(source), FirmwareVersionCatalog::isZeroVersion)(emptyList())
        assertEquals(listOf(0, 1, 2, 6, 7, 8, 9, 10), snapshot.boards?.map { it.deviceAddress })
        assertEquals(listOf("—", "—", "—", "0.", "0.a", "0.1", "unknown", "1.2.3.20"),
            snapshot.boards?.map { it.version })
        assertEquals(listOf("—", "—", "—", "0", "00.0", " 0.0.0 ", "0.", "0.a", "0.1", "unknown", "1.2.3.20"),
            snapshot.installedVersions.values.toList())
    }

    @Test fun `empty source retains stale rows but a nonempty fully hidden source clears them`() {
        val previous = listOf(board(0x09).copy(isInBootloader = true, isUpdateAvailable = true))
        val retained = getBoards(previous)
        assertNull(retained.boards)
        assertTrue(retained.installedVersions.isEmpty())
        assertTrue(previous.single().isInBootloader)
        assertEquals(true, previous.single().isUpdateAvailable)
        assertEquals(emptyList<V3AccountBoard>(), getBoards(emptyList()).boards)

        GlobalParameters.baseSubDevicesInfoStructSet = linkedSetOf(sub(0x09, version = "0.0.0"), sub(0x70))
        val cleared = getBoards(previous)
        assertEquals(emptyList<V3AccountBoard>(), cleared.boards)
        assertEquals(mapOf(0x09 to "0.0.0", 0x70 to "1.0.0"), cleared.installedVersions)
    }

    @Test fun `refresh preserves the first previous mode by address and starts new boards in main app`() {
        val previous = listOf(
            board(0x09).copy(isInBootloader = true), board(0x09), board(0x70).copy(isInBootloader = true),
        )
        val source = listOf(board(0x11).copy(isInBootloader = true), board(0x09))
        val result = GetAccountBoardsUseCaseV3(FakeBoards(source), FirmwareVersionCatalog::isZeroVersion)(previous)
        assertEquals(listOf(0x09 to true, 0x11 to false), result.boards?.map { it.deviceAddress to it.isInBootloader })
        assertTrue(source.first().isInBootloader)
    }

    @Test fun `cached restore preserves unknown names duplicates and order while clearing update flags`() {
        val cached = listOf(
            board(8, name = "Unknown").copy(isInBootloader = true, isUpdateAvailable = true, canUpdate = false),
            board(3, name = null).copy(isUpdateAvailable = null),
            board(8, name = "GUI").copy(isUpdateAvailable = true),
            board(1, version = "0.0.0").copy(isUpdateAvailable = true),
        )
        val useCase = GetAccountBoardsUseCaseV3(FakeBoards(cached = cached), FirmwareVersionCatalog::isZeroVersion)
        assertEquals(cached, useCase.cached())
        val restored = useCase.restore(requireNotNull(useCase.cached()))
        assertEquals(listOf(8, 3, 8), restored.map { it.deviceAddress })
        assertEquals(listOf("Unknown", null, "GUI"), restored.map { it.name })
        assertTrue(restored.all { it.isUpdateAvailable == false })
        assertTrue(restored.first().isInBootloader)
        assertFalse(restored.first().canUpdate)
        assertEquals(listOf(true, null, true, true), cached.map { it.isUpdateAvailable })

        val zeros = listOf(board(0, version = "0"))
        val zeroCache = GetAccountBoardsUseCaseV3(FakeBoards(cached = zeros), FirmwareVersionCatalog::isZeroVersion)
        assertFalse(zeroCache.cached().isNullOrEmpty())
        assertTrue(zeroCache.restore(requireNotNull(zeroCache.cached())).isEmpty())
    }

    @Test fun `process cache survives repository recreation and detaches both write and read snapshots`() {
        // Only fixture cleanup uses reflection: the public cache API cannot restore its initial null value.
        val cacheField = V3AccountBoardsRepositoryImpl::class.java.getDeclaredField("cachedBoards").apply { isAccessible = true }
        val originalCache = cacheField.get(null)
        try {
            val rows = mutableListOf(board(9).copy(isInBootloader = true, isUpdateAvailable = true))
            val expected = rows.toList()
            CacheAccountBoardsUseCaseV3(repository)(rows)
            rows.clear()
            val nextRepository = V3AccountBoardsRepositoryImpl()
            val firstRead = requireNotNull(GetAccountBoardsUseCaseV3(nextRepository, FirmwareVersionCatalog::isZeroVersion).cached())
            val secondRead = requireNotNull(getBoards.cached())
            assertEquals(expected, firstRead)
            assertEquals(expected, secondRead)
            assertNotSame(expected.first(), firstRead.first())
            assertNotSame(firstRead, secondRead)
            assertNotSame(firstRead.first(), secondRead.first())
        } finally {
            cacheField.set(null, originalCache)
        }
    }

    @Test fun `board observation retains source replay and repeated events without starting its own subscription`() = runTest {
        val source = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 64)
        UiState.updateFlow = source
        source.emit(4)
        val observe = ObserveAccountBoardsUseCaseV3(repository)
        val changes = observe.changes()
        assertEquals(0, source.subscriptionCount.value)
        val values = mutableListOf<Unit>()
        val job = backgroundScope.launch { changes.collect(values::add) }
        runCurrent()
        assertEquals(listOf(Unit), values)
        source.emit(9)
        source.emit(9)
        runCurrent()
        assertEquals(listOf(Unit, Unit, Unit), values)
        job.cancel()
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)
    }

    @Test fun `mode observation maps both bootloader types and main app without replay or address filtering`() = runTest {
        val source = FirmwareInfoState.runProgramTypeFlow
        source.emit(0x09 to RunProgramType.BOOTLOADER)
        val values = mutableListOf<Pair<Int, Boolean>>()
        val job = backgroundScope.launch { ObserveAccountBoardsUseCaseV3(repository).bootloaderChanges().collect(values::add) }
        runCurrent()
        assertTrue(values.isEmpty())
        listOf(RunProgramType.BOOTLOADER, RunProgramType.BOOTLOADER_V2, RunProgramType.MAIN_APP).forEach { mode ->
            source.emit(0x72 to mode)
            runCurrent()
        }
        assertEquals(listOf(0x72 to true, 0x72 to true, 0x72 to false), values)
        job.cancel()
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)
    }

    @Test fun `building and cached restore both use the injected version policy`() {
        val rows = listOf(board(1, version = "0.0.0"), board(2, version = "policy-hidden"))
        val useCase = GetAccountBoardsUseCaseV3(FakeBoards(rows, rows)) { it == "policy-hidden" }
        assertEquals(listOf(1), useCase(emptyList()).boards?.map { it.deviceAddress })
        assertEquals(listOf(1), useCase.restore(rows).map { it.deviceAddress })
        assertEquals(rows, useCase.cached())
    }

    private fun sub(address: Int, code: Int = 99, version: String = "1.0.0", isBoot: Int = 0) =
        BaseSubDeviceInfoStruct(deviceAddress = address, deviceCode = code, fwVersion = version, isBoot = isBoot)

    private fun board(address: Int, name: String? = "GUI", version: String? = "1.0.0") =
        V3AccountBoard(name, deviceCode = 6, deviceAddress = address, version = version)

    private class FakeBoards(
        private val source: List<V3AccountBoard> = emptyList(),
        private var cached: List<V3AccountBoard>? = null,
    ) : V3AccountBoardsRepository {
        override fun getBoards() = source
        override fun getCachedBoards() = cached
        override fun cacheBoards(boards: List<V3AccountBoard>) { cached = boards }
        override val changes: Flow<Unit> = emptyFlow()
        override val bootloaderChanges: Flow<Pair<Int, Boolean>> = emptyFlow()
    }
}
