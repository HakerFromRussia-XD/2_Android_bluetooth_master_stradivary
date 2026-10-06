package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountProfileViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareCatalogRepository
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.presentation.service.FakeV3DeviceRoleRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val remote = FakeAccountProfileRemote()
    private val local = FakeAccountProfileLocal()
    private val role = FakeV3DeviceRoleRepository()
    private val boards = FakeAccountBoardsRepository()
    private var firmwareLoads = 0
    private var firmwareCatalogLoad: suspend () -> List<V3ServiceFirmwareFile> = { emptyList() }
    private val firmwareDownloads = mutableListOf<List<V3ServiceFirmwareFile>>()
    private var firmwareDownload: suspend (List<V3ServiceFirmwareFile>) -> List<V3ServiceFirmwareLocalFile> = { files ->
        files.map { V3ServiceFirmwareLocalFile(it.name, "/cache/${it.name}") }
    }
    private val firmware = object : V3ServiceFirmwareCatalogRepository {
        override suspend fun loadCatalog(): List<V3ServiceFirmwareFile> {
            firmwareLoads++
            return firmwareCatalogLoad()
        }
        override suspend fun downloadFiles(files: List<V3ServiceFirmwareFile>): List<V3ServiceFirmwareLocalFile> {
            firmwareDownloads += files
            return firmwareDownload(files)
        }
    }
    private lateinit var vm: V3AccountProfileViewModel
    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = createViewModel()
        store.put("account", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun createViewModel(repository: V3AccountBoardsRepository = boards) =
        V3AccountProfileViewModelFactory(remote, local, role, firmware, repository).create(V3AccountProfileViewModel::class.java)
    private fun attach(background: Boolean = true, boards: Boolean = false) {
        if (boards) this.boards.cachedBoards = listOf(board())
        vm.onAction(V3AccountProfileAction.ViewAttached(background))
    }
    private fun load() = vm.onAction(V3AccountProfileAction.LoadRequested)

    @Test fun `catalog is lazy and reload clears availability and board highlights before suspension`() = runTest(dispatcher) {
        attach(); runCurrent()
        assertEquals(0, firmwareLoads)
        boards.rawBoards = listOf(board())
        vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        firmwareCatalogLoad = { listOf(guiFirmware("2.0.0")) }
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        assertEquals(1, vm.uiState.value.firmwareCatalogSize)
        assertEquals(mapOf(0x09 to true), vm.uiState.value.firmwareUpdates)
        val catalogRevision = vm.uiState.value.firmwareCatalogRevision
        val response = CompletableDeferred<List<V3ServiceFirmwareFile>>()
        firmwareCatalogLoad = { response.await() }
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested)
        assertNull(vm.uiState.value.firmwareCatalogSize)
        assertTrue(vm.uiState.value.firmwareUpdates.isEmpty())
        assertEquals(false, vm.uiState.value.boards.single().isUpdateAvailable)
        assertEquals(catalogRevision + 1, vm.uiState.value.firmwareCatalogRevision)
        runCurrent(); response.complete(emptyList()); runCurrent()
        assertEquals(0, vm.uiState.value.firmwareCatalogSize)
        assertEquals(mapOf(0x09 to false), vm.uiState.value.firmwareUpdates)
        assertEquals(2, firmwareLoads)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `selection distinguishes unavailable empty and board files and acknowledgements never reload`() = runTest(dispatcher) {
        attach()
        var requestId = 0L
        fun select(address: Int = 0x09) = vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(address, ++requestId))
        select(); assertTrue(vm.uiState.value.firmwareMessages.last() is V3ServiceFirmwareMessage.CatalogUnavailable)
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        select(); assertTrue(vm.uiState.value.firmwareMessages.last() is V3ServiceFirmwareMessage.NotFound)
        assertTrue(firmwareDownloads.isEmpty())
        firmwareCatalogLoad = { listOf(guiFirmware("1.0.0"), guiFirmware("2.0.0")) }
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        select(0x70); assertTrue(vm.uiState.value.firmwareMessages.last() is V3ServiceFirmwareMessage.NotFound)
        select(); runCurrent()
        assertEquals(listOf(listOf(guiFirmware("2.0.0"), guiFirmware("1.0.0"))), firmwareDownloads)
        val messages = vm.uiState.value.firmwareMessages
        assertTrue(messages[messages.lastIndex - 1] is V3ServiceFirmwareMessage.DownloadStarted)
        val downloaded = messages.last() as V3ServiceFirmwareMessage.FilesDownloaded
        assertEquals(requestId, downloaded.requestId)
        assertEquals(listOf("GUI_v2.0.0.zip", "GUI_v1.0.0.zip"), downloaded.files.map { it.name })
        vm.onAction(V3AccountProfileAction.FirmwareMessageShown(-1))
        assertEquals(messages, vm.uiState.value.firmwareMessages)
        messages.forEach { vm.onAction(V3AccountProfileAction.FirmwareMessageShown(it.id)) }
        messages.forEach { vm.onAction(V3AccountProfileAction.FirmwareMessageShown(it.id)) }
        runCurrent()
        assertTrue(vm.uiState.value.firmwareMessages.isEmpty())
        assertEquals(1, firmwareDownloads.size)
        assertEquals(2, firmwareLoads)
    }

    @Test fun `active download rejects another valid request but still reports missing or unavailable catalog`() = runTest(dispatcher) {
        firmwareCatalogLoad = { listOf(guiFirmware("1.0.0")) }
        val response = CompletableDeferred<List<V3ServiceFirmwareLocalFile>>()
        firmwareDownload = { response.await() }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 10)); runCurrent()
        val started = vm.uiState.value.firmwareMessages.single()
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 11)); runCurrent()
        assertEquals(listOf(started), vm.uiState.value.firmwareMessages)
        assertEquals(10L, vm.uiState.value.firmwareDownloadRequestId)
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x70, 12))
        assertTrue(vm.uiState.value.firmwareMessages.last() is V3ServiceFirmwareMessage.NotFound)
        firmwareCatalogLoad = { listOf(guiFirmware("9.0.0")) }
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested)
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 13))
        assertTrue(vm.uiState.value.firmwareMessages.last() is V3ServiceFirmwareMessage.CatalogUnavailable)
        runCurrent()
        val localFile = V3ServiceFirmwareLocalFile("old.zip", "/cache/old.zip")
        response.complete(listOf(localFile)); runCurrent()
        val result = vm.uiState.value.firmwareMessages.last() as V3ServiceFirmwareMessage.FilesDownloaded
        assertEquals(10L, result.requestId)
        assertEquals(listOf(localFile), result.files)
        assertEquals(listOf(listOf(guiFirmware("1.0.0"))), firmwareDownloads)
    }

    @Test fun `download failure keeps original error without partial dialog and permits retry`() = runTest(dispatcher) {
        val failure = IllegalStateException("second archive failed")
        firmwareCatalogLoad = { listOf(guiFirmware("1.0.0")) }
        firmwareDownload = { throw failure }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 1)); runCurrent()
        val error = vm.uiState.value.firmwareMessages.last() as V3ServiceFirmwareMessage.DownloadFailed
        assertSame(failure, error.error)
        assertEquals(1L, error.requestId)
        assertTrue(vm.uiState.value.firmwareMessages.none { it is V3ServiceFirmwareMessage.FilesDownloaded })
        assertTrue(vm.uiState.value.messages.isEmpty())
        firmwareDownload = { listOf(V3ServiceFirmwareLocalFile("retry.zip", "/cache/retry.zip")) }
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 2)); runCurrent()
        assertEquals(2L, (vm.uiState.value.firmwareMessages.last() as V3ServiceFirmwareMessage.FilesDownloaded).requestId)
        assertEquals(2, firmwareDownloads.size)
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `detach or clearing cancels downloading without error effect or new work`(clear: Boolean) = runTest(dispatcher) {
        var cancelled = false
        firmwareCatalogLoad = { listOf(guiFirmware("1.0.0")) }
        firmwareDownload = { try { awaitCancellation() } finally { cancelled = true } }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 1)); runCurrent()
        if (clear) store.clear() else vm.onAction(V3AccountProfileAction.ViewDetached)
        runCurrent()
        assertTrue(cancelled)
        assertNull(vm.uiState.value.firmwareDownloadRequestId)
        assertTrue(vm.uiState.value.firmwareMessages.isEmpty())
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 2)); runCurrent()
        assertEquals(1, firmwareDownloads.size)
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `late download success or failure cannot publish to a recreated view`(fails: Boolean) = runTest(dispatcher) {
        val oldResponse = CompletableDeferred<List<V3ServiceFirmwareLocalFile>>()
        firmwareCatalogLoad = { listOf(guiFirmware("1.0.0")) }
        firmwareDownload = { withContext(NonCancellable) { oldResponse.await() } }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 1)); runCurrent()
        vm.onAction(V3AccountProfileAction.ViewDetached)
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        firmwareDownload = { listOf(V3ServiceFirmwareLocalFile("new.zip", "/cache/new.zip")) }
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 2)); runCurrent()
        val current = vm.uiState.value
        if (fails) oldResponse.completeExceptionally(IllegalStateException("late error"))
        else oldResponse.complete(listOf(V3ServiceFirmwareLocalFile("old.zip", "/cache/old.zip")))
        runCurrent()
        assertEquals(current, vm.uiState.value)
        assertEquals(2, firmwareDownloads.size)
    }

    @Test fun `catalog failure stays unavailable without profile messages or refresh completion`() = runTest(dispatcher) {
        val error = IllegalStateException("offline")
        firmwareCatalogLoad = { throw error }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        assertNull(vm.uiState.value.firmwareCatalogSize)
        assertSame(error, vm.uiState.value.firmwareCatalogError)
        assertTrue(vm.uiState.value.firmwareUpdates.isEmpty())
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertEquals(0L, vm.uiState.value.refreshCompletionId)
        assertEquals(1, firmwareLoads)
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `replaced request cannot publish a late success or error`(fails: Boolean) = runTest(dispatcher) {
        val oldResponse = CompletableDeferred<List<V3ServiceFirmwareFile>>()
        firmwareCatalogLoad = { withContext(NonCancellable) { oldResponse.await() } }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        firmwareCatalogLoad = { listOf(guiFirmware("3.0.0")) }
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        val current = vm.uiState.value
        if (fails) oldResponse.completeExceptionally(IllegalStateException("late"))
        else oldResponse.complete(listOf(guiFirmware("1.0.0")))
        runCurrent()
        assertEquals(current, vm.uiState.value)
        vm.onAction(V3AccountProfileAction.FirmwareFilesRequested(0x09, 1)); runCurrent()
        assertEquals(listOf(listOf(guiFirmware("3.0.0"))), firmwareDownloads)
        assertEquals(2, firmwareLoads)
    }

    @Test fun `destroying the view cancels its catalog and late response cannot affect a recreated view`() = runTest(dispatcher) {
        val oldResponse = CompletableDeferred<List<V3ServiceFirmwareFile>>()
        var cancelled = false
        firmwareCatalogLoad = {
            try { withContext(NonCancellable) { oldResponse.await() } }
            finally { cancelled = !currentCoroutineContext().isActive }
        }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        vm.onAction(V3AccountProfileAction.ViewDetached)
        firmwareCatalogLoad = { listOf(guiFirmware("4.0.0")) }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        val recreated = vm.uiState.value
        oldResponse.complete(listOf(guiFirmware("1.0.0"))); runCurrent()
        assertTrue(cancelled)
        assertEquals(recreated, vm.uiState.value)
        store.clear()
        val cleared = vm.uiState.value
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        assertEquals(cleared, vm.uiState.value)
        assertEquals(2, firmwareLoads)
    }

    private fun guiFirmware(version: String) = V3ServiceFirmwareFile("GUI", "GUI_v$version.zip", "/GUI/$version", 42)

    private fun board(
        address: Int = 0x09, version: String? = "1.0.0", name: String? = "GUI",
        bootloader: Boolean = false, updateAvailable: Boolean? = null,
    ) = V3AccountBoard(name, address, address, version, bootloader, updateAvailable)

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `attach restores cached rows and foreground alone submits and recaches them`(background: Boolean) = runTest(dispatcher) {
        val cached = board(bootloader = true, updateAvailable = true)
        boards.cachedBoards = listOf(cached, board(0x0A, "0.0.0"))
        attach(background = background); runCurrent()
        val restored = listOf(cached.copy(isUpdateAvailable = false))
        assertEquals(restored, vm.uiState.value.boards)
        assertTrue(vm.uiState.value.isContentVisible)
        assertFalse(vm.uiState.value.isBoardRenderingReady)
        assertEquals(if (background) 0 else 1, vm.uiState.value.boardSubmissions.size)
        if (!background) {
            val submission = vm.uiState.value.boardSubmissions.single()
            assertTrue(submission.id > 0)
            assertEquals(restored, submission.boards)
            assertFalse(submission.submitImmediately)
        }
        assertEquals(if (background) emptyList() else listOf(restored), boards.cacheWrites)
        assertEquals(0, boards.boardReads)
        assertEquals(0, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `raw cached zero rows still make foreground content visible after restoration filters every row`() = runTest(dispatcher) {
        boards.cachedBoards = listOf(board(version = "0.0.0"))
        attach(background = false)
        assertTrue(vm.uiState.value.isContentVisible)
        assertTrue(vm.uiState.value.boards.isEmpty())
        assertEquals(emptyList<V3AccountBoard>(), vm.uiState.value.boardSubmissions.single().boards)
        assertEquals(listOf(emptyList<V3AccountBoard>()), boards.cacheWrites)
        vm.onAction(V3AccountProfileAction.CachedBoardsApplied)
        assertTrue(vm.uiState.value.isBoardRenderingReady)
        boards.rawBoards = listOf(board())
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        boards.changes.emit(Unit); runCurrent()
        assertEquals(listOf(board(updateAvailable = false)), vm.uiState.value.boards)
    }

    @ParameterizedTest @ValueSource(strings = ["foreground-profile", "foreground-boards", "foreground-empty", "background-profile", "background-boards"])
    fun `cached acknowledgement enables updates only for a foreground view with cache`(mode: String) = runTest(dispatcher) {
        if (mode.endsWith("profile")) local.storedHeader = V3AccountProfileHeader("Cached", "Name")
        if (mode.endsWith("boards")) boards.cachedBoards = listOf(board())
        boards.rawBoards = listOf(board(version = "2.0.0"))
        attach(background = mode.startsWith("background"))
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        boards.changes.emit(Unit); runCurrent()
        assertEquals(0, boards.boardReads)
        vm.onAction(V3AccountProfileAction.CachedBoardsApplied)
        val enabled = mode.startsWith("foreground") && !mode.endsWith("empty")
        assertEquals(enabled, vm.uiState.value.isBoardRenderingReady)
        boards.changes.emit(Unit); runCurrent()
        assertEquals(if (enabled) 1 else 0, boards.boardReads)
        if (enabled) assertEquals("2.0.0", vm.uiState.value.boards.single().version)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `generic updates subscribe once during started and resume the latest stopped change`() = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        attach(); attach(); runCurrent()
        assertEquals(0, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        assertEquals(1, boards.changes.subscriptionCount.value)
        boards.changes.emit(Unit); runCurrent()
        assertEquals(0, boards.boardReads)
        vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        assertEquals(1, boards.boardReads)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStopped)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStopped); runCurrent()
        assertEquals(0, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
        val stopped = vm.uiState.value
        boards.rawBoards = listOf(board(version = "2.0.0"))
        boards.changes.emit(Unit); runCurrent()
        assertEquals(stopped, vm.uiState.value)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        assertEquals(1, boards.changes.subscriptionCount.value)
        assertEquals(2, boards.boardReads)
        assertEquals("2.0.0", vm.uiState.value.boards.single().version)
        val submissionId = vm.uiState.value.boardSubmissions.last().id
        boards.changes.emit(Unit); runCurrent()
        assertEquals(3, boards.boardReads)
        assertEquals(submissionId + 1, vm.uiState.value.boardSubmissions.last().id)
        assertFalse(vm.uiState.value.boardSubmissions.last().submitImmediately)
    }

    @Test fun `bootloader ignores pre-render and unknown addresses but immediately submits while stopped`() = runTest(dispatcher) {
        boards.cachedBoards = listOf(board())
        boards.rawBoards = listOf(board())
        firmwareCatalogLoad = { listOf(guiFirmware("2.0.0")) }
        attach(background = false); runCurrent()
        val beforeRendering = vm.uiState.value
        boards.bootloaderChanges.emit(0x09 to true); runCurrent()
        assertEquals(beforeRendering, vm.uiState.value)
        vm.onAction(V3AccountProfileAction.CachedBoardsApplied)
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        vm.onAction(V3AccountProfileAction.BoardUpdatesStopped); runCurrent()
        val beforeBootloader = vm.uiState.value
        val reads = boards.boardReads
        val cacheWrites = boards.cacheWrites.size
        boards.bootloaderChanges.emit(0x70 to true); runCurrent()
        assertEquals(beforeBootloader, vm.uiState.value)
        assertEquals(cacheWrites, boards.cacheWrites.size)
        boards.bootloaderChanges.emit(0x09 to true); runCurrent()
        val expectedRows = beforeBootloader.boards.map { it.copy(isInBootloader = true) }
        assertEquals(beforeBootloader.copy(
            boards = expectedRows, boardSubmissions = beforeBootloader.boardSubmissions + V3AccountBoardsSubmission(
                beforeBootloader.boardSubmissions.last().id + 1, expectedRows, submitImmediately = true,
            ),
        ), vm.uiState.value)
        assertEquals(expectedRows, boards.cacheWrites.last())
        assertEquals(reads, boards.boardReads)
        assertEquals(mapOf(0x09 to true), vm.uiState.value.firmwareUpdates)
        assertEquals(true, vm.uiState.value.boards.single().isUpdateAvailable)
        assertEquals(0, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
    }

    @Test fun `full refresh preserves bootloader flags and posts availability from the current catalog`() = runTest(dispatcher) {
        boards.cachedBoards = listOf(board(bootloader = true))
        boards.rawBoards = listOf(board(version = "1.1.0"), board(0x0A, null, "MAIN"))
        firmwareCatalogLoad = { listOf(guiFirmware("2.0.0")) }
        attach(); vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        assertEquals(listOf(
            board(version = "1.1.0", bootloader = true, updateAvailable = true),
            board(0x0A, "—", "MAIN", updateAvailable = false),
        ), vm.uiState.value.boards)
        assertFalse(vm.uiState.value.boardSubmissions.last().submitImmediately)
        assertEquals(vm.uiState.value.boards, boards.cacheWrites.last())
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        boards.bootloaderChanges.emit(0x09 to false); runCurrent()
        assertTrue(vm.uiState.value.boardSubmissions.last().submitImmediately)
        boards.rawBoards = listOf(board(version = "3.0.0"))
        boards.changes.emit(Unit); runCurrent()
        assertEquals(listOf(board(version = "3.0.0", updateAvailable = false)), vm.uiState.value.boards)
        assertFalse(vm.uiState.value.boardSubmissions.last().submitImmediately)
        assertEquals(mapOf(0x09 to false), vm.uiState.value.firmwareUpdates)
    }

    @Test fun `temporarily empty snapshot retains rows and their flags but clears installed availability before returning`() = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        firmwareCatalogLoad = { listOf(guiFirmware("2.0.0")) }
        attach(); vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        val before = vm.uiState.value
        val cacheWrites = boards.cacheWrites.size
        boards.rawBoards = emptyList()
        boards.changes.emit(Unit); runCurrent()
        assertEquals(before.copy(firmwareUpdates = emptyMap()), vm.uiState.value)
        assertEquals(cacheWrites, boards.cacheWrites.size)
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        assertTrue(vm.uiState.value.firmwareUpdates.isEmpty())
        assertEquals(before.boards, vm.uiState.value.boards)
        assertEquals(before.boardSubmissions, vm.uiState.value.boardSubmissions)
        assertEquals(cacheWrites, boards.cacheWrites.size)
        boards.rawBoards = listOf(board(version = "0.0.0"), board(0x70, name = "Unknown"))
        boards.changes.emit(Unit); runCurrent()
        assertTrue(vm.uiState.value.boards.isEmpty())
        assertEquals(before.boardSubmissions.last().id + 1, vm.uiState.value.boardSubmissions.last().id)
        assertEquals(setOf(0x09, 0x70), vm.uiState.value.firmwareUpdates.keys)
        assertEquals(emptyList<V3AccountBoard>(), boards.cacheWrites.last())
    }

    @Test fun `catalog reload refreshes boards internally only after rendering is enabled`() = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        firmwareCatalogLoad = { listOf(guiFirmware("2.0.0")) }
        attach(); vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        assertEquals(0, boards.boardReads)
        assertTrue(vm.uiState.value.boardSubmissions.isEmpty())
        vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        assertEquals(listOf(board(updateAvailable = true)), vm.uiState.value.boards)
        val before = vm.uiState.value
        val response = CompletableDeferred<List<V3ServiceFirmwareFile>>()
        firmwareCatalogLoad = { response.await() }
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested)
        assertEquals(before.boardSubmissions.last().id + 1, vm.uiState.value.boardSubmissions.last().id)
        assertEquals(listOf(board(updateAvailable = false)), vm.uiState.value.boards)
        assertFalse(vm.uiState.value.boardSubmissions.last().submitImmediately)
        runCurrent(); response.complete(listOf(guiFirmware("3.0.0"))); runCurrent()
        assertEquals(before.boardSubmissions.last().id + 2, vm.uiState.value.boardSubmissions.last().id)
        assertEquals(listOf(board(updateAvailable = true)), vm.uiState.value.boards)
        assertEquals(3, boards.boardReads)
        assertEquals(3, boards.cacheWrites.size)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `latest state retains posted refresh before immediate bootloader and acknowledgements only remove submissions`() = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        attach(); vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        val initialId = vm.uiState.value.boardSubmissions.single().id
        vm.onAction(V3AccountProfileAction.BoardSubmissionRendered(initialId))
        boards.rawBoards = listOf(board(version = "2.0.0"))
        vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        val posted = vm.uiState.value.boardSubmissions.single()
        assertTrue(boards.bootloaderChanges.tryEmit(0x09 to true)); runCurrent()
        val immediate = V3AccountBoardsSubmission(
            posted.id + 1, posted.boards.map { it.copy(isInBootloader = true) }, submitImmediately = true,
        )
        val pending = listOf(posted, immediate)
        assertFalse(posted.submitImmediately)
        assertEquals(pending, vm.uiState.value.boardSubmissions)
        val delivered = mutableListOf<List<V3AccountBoardsSubmission>>()
        launch { vm.uiState.take(1).collect { delivered += it.boardSubmissions } }; runCurrent()
        assertEquals(listOf(pending), delivered)
        val beforeAcknowledgement = vm.uiState.value
        val reads = boards.boardReads
        val cacheWrites = boards.cacheWrites.toList()
        vm.onAction(V3AccountProfileAction.BoardSubmissionRendered(initialId))
        assertEquals(beforeAcknowledgement, vm.uiState.value)
        vm.onAction(V3AccountProfileAction.BoardSubmissionRendered(posted.id))
        vm.onAction(V3AccountProfileAction.BoardSubmissionRendered(posted.id))
        assertEquals(beforeAcknowledgement.copy(boardSubmissions = listOf(immediate)), vm.uiState.value)
        vm.onAction(V3AccountProfileAction.BoardSubmissionRendered(immediate.id))
        assertEquals(beforeAcknowledgement.copy(boardSubmissions = emptyList()), vm.uiState.value)
        assertEquals(reads, boards.boardReads)
        assertEquals(cacheWrites, boards.cacheWrites)
        assertEquals(1, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
        assertEquals(0, remote.tokenCalls)
    }

    @ParameterizedTest @ValueSource(booleans = [false, true])
    fun `detach or clear cancels both board subscriptions and ignores late actions and events`(clear: Boolean) = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        attach(); vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        assertEquals(1, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
        if (clear) store.clear() else vm.onAction(V3AccountProfileAction.ViewDetached)
        runCurrent()
        val detached = vm.uiState.value
        val reads = boards.boardReads
        val cacheWrites = boards.cacheWrites.size
        assertFalse(detached.isBoardRenderingReady)
        assertTrue(detached.boardSubmissions.isEmpty())
        boards.rawBoards = listOf(board(version = "9.0.0"))
        boards.changes.emit(Unit)
        boards.bootloaderChanges.emit(0x09 to true)
        vm.onAction(V3AccountProfileAction.CachedBoardsApplied)
        vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted)
        if (clear) attach()
        runCurrent()
        assertEquals(detached, vm.uiState.value)
        assertEquals(reads, boards.boardReads)
        assertEquals(cacheWrites, boards.cacheWrites.size)
        assertEquals(0, boards.changes.subscriptionCount.value)
        assertEquals(0, boards.bootloaderChanges.subscriptionCount.value)
    }

    @Test fun `queued events from destroyed subscriptions cannot update a recreated view before acknowledgement`() = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        attach(); vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        val oldSubmissionId = vm.uiState.value.boardSubmissions.last().id
        assertTrue(boards.changes.tryEmit(Unit))
        assertTrue(boards.bootloaderChanges.tryEmit(0x09 to true))
        vm.onAction(V3AccountProfileAction.ViewDetached)
        attach(background = false); runCurrent()
        val restored = vm.uiState.value
        assertFalse(restored.boards.single().isInBootloader)
        assertFalse(restored.isBoardRenderingReady)
        assertEquals(0, boards.changes.subscriptionCount.value)
        assertEquals(1, boards.bootloaderChanges.subscriptionCount.value)
        vm.onAction(V3AccountProfileAction.BoardSubmissionRendered(oldSubmissionId))
        assertEquals(restored, vm.uiState.value)
        vm.onAction(V3AccountProfileAction.BoardUpdatesStarted); runCurrent()
        assertEquals(restored, vm.uiState.value)
        boards.bootloaderChanges.emit(0x09 to true); runCurrent()
        assertEquals(restored, vm.uiState.value)
        vm.onAction(V3AccountProfileAction.CachedBoardsApplied)
        boards.bootloaderChanges.emit(0x09 to true); runCurrent()
        assertTrue(vm.uiState.value.boards.single().isInBootloader)
        assertEquals(restored.boardSubmissions.last().id + 1, vm.uiState.value.boardSubmissions.last().id)
    }

    @Test fun `rendered board cache survives a separate repository and viewmodel instance`() = runTest(dispatcher) {
        boards.rawBoards = listOf(board())
        firmwareCatalogLoad = { listOf(guiFirmware("2.0.0")) }
        attach(); vm.onAction(V3AccountProfileAction.BoardRenderingReady)
        vm.onAction(V3AccountProfileAction.FirmwareCatalogRefreshRequested); runCurrent()
        boards.bootloaderChanges.emit(0x09 to true); runCurrent()
        vm.onAction(V3AccountProfileAction.ViewDetached); runCurrent()
        val recreatedBoards = FakeAccountBoardsRepository(boards.cache)
        val recreated = createViewModel(recreatedBoards)
        store.put("recreated", recreated)
        recreated.onAction(V3AccountProfileAction.ViewAttached(false)); runCurrent()
        assertEquals(listOf(board(bootloader = true, updateAvailable = false)), recreated.uiState.value.boards)
        assertTrue(recreated.uiState.value.isContentVisible)
        assertTrue(recreated.uiState.value.boardSubmissions.single().id > 0)
        assertFalse(recreated.uiState.value.isBoardRenderingReady)
        assertFalse(recreated.uiState.value.boardSubmissions.single().submitImmediately)
        assertEquals(0, recreatedBoards.boardReads)
        assertEquals(1, recreatedBoards.cacheWrites.size)
        assertEquals(1, recreatedBoards.bootloaderChanges.subscriptionCount.value)
        assertEquals(0, boards.bootloaderChanges.subscriptionCount.value)
        assertEquals(0, remote.tokenCalls)
    }

    @ParameterizedTest @EnumSource(V3DeviceRole::class)
    fun `stored role determines initial board actions before rendering without writes`(selected: V3DeviceRole) = runTest(dispatcher) {
        role.selected = selected
        val allowed = selected == V3DeviceRole.SERVICE_ENGINEER
        role.serviceEngineerAccess.value = !allowed
        role.interactionEnabled.value = false
        attach()
        assertEquals(allowed, vm.uiState.value.areBoardServiceActionsVisible)
        runCurrent()
        assertEquals(allowed, vm.uiState.value.areBoardServiceActionsVisible)
        assertEquals(selected, role.access)
        assertTrue(role.writes.isEmpty())
        assertTrue(local.writes.isEmpty())
        assertEquals(0, local.cacheWrites)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `access changes affect only board actions without reloading or recaching the profile`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        val before = vm.uiState.value
        vm.onAction(V3AccountProfileAction.HeaderRendered(before.headerRevision))
        val writes = local.writes.toList()
        role.updateRoleAccess(V3DeviceRole.SERVICE_ENGINEER); runCurrent()
        assertEquals(before.copy(areBoardServiceActionsVisible = true), vm.uiState.value)
        role.interactionEnabled.value = false; runCurrent()
        assertTrue(vm.uiState.value.areBoardServiceActionsVisible)
        role.updateRoleAccess(V3DeviceRole.USER); runCurrent()
        assertEquals(before, vm.uiState.value)
        assertEquals(1, remote.tokenCalls)
        assertEquals(1, local.cacheWrites)
        assertEquals(writes, local.writes)
        assertTrue(role.writes.isEmpty())
    }

    @Test fun `detaching cancels role observation and reattaching restores the current role once`() = runTest(dispatcher) {
        attach(); attach(); runCurrent()
        assertEquals(1, role.serviceEngineerAccess.subscriptionCount.value)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        val detached = vm.uiState.value
        role.setSelectedRole(V3DeviceRole.SERVICE_ENGINEER); runCurrent()
        assertEquals(0, role.serviceEngineerAccess.subscriptionCount.value)
        assertEquals(detached, vm.uiState.value)
        attach()
        assertTrue(vm.uiState.value.areBoardServiceActionsVisible)
        runCurrent()
        assertEquals(1, role.serviceEngineerAccess.subscriptionCount.value)
        assertEquals(listOf(V3DeviceRole.SERVICE_ENGINEER), role.writes)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `clearing the screen cancels role observation and late attach cannot restart it`() = runTest(dispatcher) {
        attach(); runCurrent()
        store.clear()
        val cleared = vm.uiState.value
        role.setSelectedRole(V3DeviceRole.SERVICE_ENGINEER)
        attach(); runCurrent()
        assertEquals(cleared, vm.uiState.value)
        assertEquals(0, role.serviceEngineerAccess.subscriptionCount.value)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `background attach shows initial header without requesting or caching until transition action`() = runTest(dispatcher) {
        attach(); attach(); runCurrent()
        assertTrue(vm.uiState.value.isContentVisible)
        assertEquals(V3AccountProfileHeader(), vm.uiState.value.header)
        assertEquals(0, remote.tokenCalls)
        assertEquals(0, local.cacheWrites)
        load(); runCurrent()
        val state = vm.uiState.value
        assertEquals("First", state.header?.firstName)
        assertEquals(V3AccountProfileVersions("1.23", "2.34", "3.45"), state.header?.versions)
        assertTrue(state.isTokenLoaded)
        assertEquals(1, state.refreshCompletionId)
        assertEquals(1, remote.tokenCalls)
        assertEquals(0, local.cacheWrites)
        vm.onAction(V3AccountProfileAction.HeaderRendered(state.headerRevision))
        assertEquals(state.header, local.storedHeader)
        assertEquals(1, local.cacheWrites)
    }

    @Test fun `foreground waits for authorization but either cache makes content visible`() = runTest(dispatcher) {
        attach(background = false)
        assertFalse(vm.uiState.value.isContentVisible)
        assertNull(vm.uiState.value.header)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        attach(background = false, boards = true)
        assertTrue(vm.uiState.value.isContentVisible)
        assertNull(vm.uiState.value.header)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        local.storedHeader = V3AccountProfileHeader("Cached", "Name")
        attach(background = false)
        assertTrue(vm.uiState.value.isContentVisible)
        assertTrue(vm.uiState.value.hasCachedProfile)
        assertEquals("Cached", vm.uiState.value.header?.firstName)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `authorized content appears before slow user response and later error keeps its existing header`() = runTest(dispatcher) {
        val response = CompletableDeferred<V3AccountProfileResult<V3AccountProfile>>()
        remote.user = { _, _ -> response.await() }
        attach(background = false); load(); runCurrent()
        assertTrue(vm.uiState.value.isContentVisible)
        assertTrue(vm.uiState.value.isTokenLoaded)
        assertNull(vm.uiState.value.header)
        response.complete(V3AccountProfileResult.Error(500, "user error")); runCurrent()
        assertEquals("user error", vm.uiState.value.messages.single().serverMessage)
        assertNull(vm.uiState.value.header)
        assertTrue(local.writes.isEmpty())
    }

    @Test fun `retry counter survives refresh actions and resets only for a new view`() = runTest(dispatcher) {
        remote.token = { V3AccountProfileResult.Error(500, "HTTP 500") }
        attach(); load(); runCurrent()
        assertEquals(4, remote.tokenCalls)
        assertEquals(4, vm.uiState.value.refreshCompletionId)
        val first = vm.uiState.value.messages.single()
        assertNull(first.serverMessage)
        vm.onAction(V3AccountProfileAction.MessageShown(first.id))
        load(); runCurrent()
        assertEquals(5, remote.tokenCalls)
        assertEquals(1, vm.uiState.value.messages.size)
        assertNotEquals(first.id, vm.uiState.value.messages.single().id)
        vm.onAction(V3AccountProfileAction.ViewDetached); attach(); load(); runCurrent()
        assertEquals(9, remote.tokenCalls)
        assertEquals(1, vm.uiState.value.messages.size)
    }

    @Test fun `render acknowledgements and local UI refresh do not send network requests or replay messages`() = runTest(dispatcher) {
        remote.token = { V3AccountProfileResult.Error(null, "offline") }
        attach(); load(); runCurrent()
        val failed = vm.uiState.value
        vm.onAction(V3AccountProfileAction.MessageShown(failed.messages.single().id))
        vm.onAction(V3AccountProfileAction.HeaderRefreshRequested)
        vm.onAction(V3AccountProfileAction.HeaderRendered(failed.headerRevision))
        assertEquals(0, local.cacheWrites)
        vm.onAction(V3AccountProfileAction.HeaderRendered(vm.uiState.value.headerRevision))
        assertEquals(1, local.cacheWrites)
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertEquals(1, remote.tokenCalls)
    }

    @Test fun `token failure clears details but displays current names rather than cached names`() = runTest(dispatcher) {
        local.storedHeader = V3AccountProfileHeader("Cached", "Different")
        remote.token = { V3AccountProfileResult.Error(401, "error") }
        attach(); load(); runCurrent()
        assertEquals("", vm.uiState.value.header?.firstName)
        assertTrue(vm.uiState.value.isContentVisible)
        assertEquals(V3AccountDetail.entries.toSet(), local.values.keys)
        assertTrue(local.values.values.all { it.isEmpty() })
    }

    @ParameterizedTest @ValueSource(strings = ["token", "user", "devices", "info"])
    fun `destroying view cancels every request stage and ignores late noncooperative results`(stage: String) = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        suspend fun waitForRelease() = withContext(NonCancellable) { release.await() }
        when (stage) {
            "token" -> remote.token = { waitForRelease(); V3AccountProfileResult.Success("late") }
            "user" -> remote.user = { _, _ -> waitForRelease(); V3AccountProfileResult.Success(V3AccountProfile("Late", "", 8, "", "")) }
            "devices" -> remote.devices = { _, _, _ -> waitForRelease(); V3AccountProfileResult.Success(listOf(V3AccountDevice(9, "FEST-test"))) }
            "info" -> remote.info = { _, _, _ -> waitForRelease(); V3AccountProfileResult.Success(V3AccountDeviceInfo("Late", "", "", "", "", "", emptyList())) }
        }
        attach(); load(); runCurrent()
        vm.onAction(V3AccountProfileAction.ViewDetached)
        val detached = vm.uiState.value
        val writeCount = local.writes.size
        release.complete(Unit); runCurrent()
        load(); vm.onAction(V3AccountProfileAction.HeaderRefreshRequested)
        vm.onAction(V3AccountProfileAction.HeaderRendered(detached.headerRevision)); runCurrent()
        assertEquals(detached, vm.uiState.value)
        assertEquals(writeCount, local.writes.size)
        assertEquals(0, local.cacheWrites)
        assertEquals(1, remote.tokenCalls)
    }

    @Test fun `viewmodel clearing prevents late actions from creating another request session`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        store.clear()
        val before = vm.uiState.value
        attach(); load(); vm.onAction(V3AccountProfileAction.HeaderRefreshRequested); runCurrent()
        assertEquals(1, remote.tokenCalls)
        assertEquals(before, vm.uiState.value)
    }

    @Test fun `foreground restoration of collectors and duplicate attach do not launch a new load`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        val first = launch { vm.uiState.collect {} }; runCurrent(); first.cancel()
        attach()
        val second = launch { vm.uiState.collect {} }; runCurrent(); second.cancel()
        assertEquals(1, remote.tokenCalls)
    }
}

private class FakeAccountBoardsCache(var boards: List<V3AccountBoard>? = null)

private class FakeAccountBoardsRepository(
    val cache: FakeAccountBoardsCache = FakeAccountBoardsCache(),
) : V3AccountBoardsRepository {
    var rawBoards = emptyList<V3AccountBoard>()
    @get:JvmName("getFakeCachedBoards")
    @set:JvmName("setFakeCachedBoards")
    var cachedBoards: List<V3AccountBoard>?
        get() = cache.boards
        set(value) { cache.boards = value }
    var boardReads = 0
    val cacheWrites = mutableListOf<List<V3AccountBoard>>()
    override val changes = MutableSharedFlow<Unit>(replay = 1)
    override val bootloaderChanges = MutableSharedFlow<Pair<Int, Boolean>>(replay = 0, extraBufferCapacity = 1)
    override fun getBoards(): List<V3AccountBoard> { boardReads++; return rawBoards }
    override fun getCachedBoards() = cachedBoards
    override fun cacheBoards(boards: List<V3AccountBoard>) {
        cachedBoards = boards.toList()
        cacheWrites += boards.toList()
    }
}
