package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ObserveServiceEngineerAccessUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.RestoreDeviceRoleUseCaseV3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class V3AccountProfileViewModel(
    private val getViewData: GetAccountProfileViewDataUseCaseV3,
    private val loadProfile: LoadAccountProfileUseCaseV3,
    private val cacheHeader: CacheAccountProfileHeaderUseCaseV3,
    private val restoreDeviceRole: RestoreDeviceRoleUseCaseV3,
    private val observeServiceEngineerAccess: ObserveServiceEngineerAccessUseCaseV3,
    private val loadFirmwareCatalog: LoadServiceFirmwareCatalogUseCaseV3,
    private val getFirmwareForBoard: GetServiceFirmwareForBoardUseCaseV3,
    private val downloadFirmwareFiles: DownloadServiceFirmwareFilesUseCaseV3,
    private val getBoards: GetAccountBoardsUseCaseV3,
    private val cacheBoards: CacheAccountBoardsUseCaseV3,
    private val observeBoards: ObserveAccountBoardsUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3AccountProfileUiState())
    val uiState = state.asStateFlow()
    private var context = V3AccountProfileContext()
    private var currentHeader = V3AccountProfileHeader()
    private var session: V3AccountProfileLoadSession? = null
    private var viewScope: CoroutineScope? = null
    private var nextHeaderRevision = 0L
    private var nextMessageId = 0L
    private var cleared = false
    private var firmwareCatalog: List<V3ServiceFirmwareFile>? = null
    private var firmwareCatalogJob: Job? = null
    private var firmwareCatalogRequest = 0L
    private var installedFirmwareVersions = emptyMap<Int, String>()
    private var nextFirmwareMessageId = 0L
    private var firmwareDownloadJob: Job? = null
    private var boardsUpdatesJob: Job? = null
    private var nextBoardSubmissionId = 0L
    private var enableCachedBoardUpdates = false

    fun onAction(action: V3AccountProfileAction) {
        if (cleared) return
        when (action) {
            is V3AccountProfileAction.ViewAttached -> {
                if (viewScope != null) return
                restoreDeviceRole()
                val serviceEngineerAccess = observeServiceEngineerAccess()
                val data = getViewData(context)
                val cached = getBoards.cached()
                val hasCachedBoards = !cached.isNullOrEmpty()
                val initialBoards = getBoards.restore(cached.orEmpty())
                enableCachedBoardUpdates = !action.loadInBackground && (data.cachedHeader != null || hasCachedBoards)
                context = data.context
                session = V3AccountProfileLoadSession(context)
                viewScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
                val header = data.cachedHeader ?: currentHeader.takeIf { action.loadInBackground }
                state.value = V3AccountProfileUiState(
                    header = header,
                    headerRevision = ++nextHeaderRevision,
                    hasCachedProfile = data.cachedHeader != null,
                    isContentVisible = action.loadInBackground || data.cachedHeader != null || hasCachedBoards,
                    areBoardServiceActionsVisible = serviceEngineerAccess.value,
                    boards = initialBoards,
                )
                if (hasCachedBoards && !action.loadInBackground) submitBoards(initialBoards, immediately = false)
                // The old adapter was initialized before the current device versions were read.
                currentHeader = currentHeader.copy(versions = data.versions)
                viewScope?.launch {
                    serviceEngineerAccess.collect { allowed ->
                        state.value = state.value.copy(areBoardServiceActionsVisible = allowed)
                    }
                }
                val scope = requireNotNull(viewScope)
                scope.launch {
                    observeBoards.bootloaderChanges().collect { (address, inBootloader) ->
                        coroutineContext.ensureActive()
                        if (viewScope !== scope || !state.value.isBoardRenderingReady || state.value.boards.none { it.deviceAddress == address }) return@collect
                        val boards = state.value.boards.map {
                            it.copy(isInBootloader = if (it.deviceAddress == address) inBootloader else it.isInBootloader)
                        }
                        submitBoards(boards, immediately = true)
                    }
                }
            }
            V3AccountProfileAction.ViewDetached -> detachView()
            V3AccountProfileAction.LoadRequested -> {
                val activeSession = session ?: return
                viewScope?.launch {
                    loadProfile(activeSession).collect { event ->
                        if (session === activeSession) handleResult(event)
                    }
                }
            }
            V3AccountProfileAction.HeaderRefreshRequested -> if (viewScope != null) updateHeader()
            is V3AccountProfileAction.HeaderRendered -> {
                if (viewScope != null && action.revision == state.value.headerRevision) state.value.header?.let { cacheHeader(it) }
            }
            is V3AccountProfileAction.MessageShown -> {
                if (viewScope == null) return
                state.value = state.value.copy(messages = state.value.messages.filterNot { it.id == action.id })
            }
            V3AccountProfileAction.FirmwareCatalogRefreshRequested -> refreshFirmwareCatalog()
            V3AccountProfileAction.CachedBoardsApplied -> if (viewScope != null) {
                state.value = state.value.copy(isBoardRenderingReady = enableCachedBoardUpdates)
            }
            V3AccountProfileAction.BoardRenderingReady -> if (viewScope != null) {
                state.value = state.value.copy(isBoardRenderingReady = true)
                refreshBoards()
            }
            V3AccountProfileAction.BoardUpdatesStarted -> startBoardUpdates()
            V3AccountProfileAction.BoardUpdatesStopped -> {
                boardsUpdatesJob?.cancel()
                boardsUpdatesJob = null
            }
            is V3AccountProfileAction.BoardSubmissionRendered -> {
                state.value = state.value.copy(boardSubmissions = state.value.boardSubmissions.filterNot { it.id == action.id })
            }
            is V3AccountProfileAction.FirmwareFilesRequested -> {
                requestFirmwareFiles(action)
            }
            is V3AccountProfileAction.FirmwareMessageShown -> {
                state.value = state.value.copy(firmwareMessages = state.value.firmwareMessages.filterNot { it.id == action.id })
            }
        }
    }

    private fun startBoardUpdates() {
        val scope = viewScope ?: return
        if (boardsUpdatesJob?.isActive == true) return
        boardsUpdatesJob = scope.launch {
            observeBoards.changes().collect {
                coroutineContext.ensureActive()
                if (viewScope === scope) refreshBoards()
            }
        }
    }

    private fun refreshBoards() {
        if (!state.value.isBoardRenderingReady) return
        val snapshot = getBoards(state.value.boards)
        installedFirmwareVersions = snapshot.installedVersions
        val updates = firmwareUpdates()
        val boards = snapshot.boards?.map { it.copy(isUpdateAvailable = updates[it.deviceAddress] ?: false) }
        state.value = state.value.copy(firmwareUpdates = updates)
        if (boards == null) return
        submitBoards(boards, immediately = false)
    }

    private fun submitBoards(boards: List<V3AccountBoard>, immediately: Boolean) {
        cacheBoards(boards)
        state.value = state.value.copy(boards = boards, boardSubmissions = state.value.boardSubmissions +
            V3AccountBoardsSubmission(++nextBoardSubmissionId, boards, immediately))
    }

    private fun requestFirmwareFiles(action: V3AccountProfileAction.FirmwareFilesRequested) {
        val scope = viewScope ?: return
        val files = firmwareCatalog?.let { getFirmwareForBoard(it, action.deviceAddress) }
        when {
            files == null -> addFirmwareMessage(V3ServiceFirmwareMessage.CatalogUnavailable(++nextFirmwareMessageId))
            files.isEmpty() -> addFirmwareMessage(V3ServiceFirmwareMessage.NotFound(++nextFirmwareMessageId))
            firmwareDownloadJob?.isActive == true -> return
            else -> {
                state.value = state.value.copy(firmwareDownloadRequestId = action.requestId)
                addFirmwareMessage(V3ServiceFirmwareMessage.DownloadStarted(++nextFirmwareMessageId, action.requestId))
                firmwareDownloadJob = scope.launch {
                    try {
                        val downloaded = downloadFirmwareFiles(files)
                        coroutineContext.ensureActive()
                        if (viewScope !== scope) return@launch
                        addFirmwareMessage(V3ServiceFirmwareMessage.FilesDownloaded(++nextFirmwareMessageId, action.requestId, downloaded))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        coroutineContext.ensureActive()
                        if (viewScope !== scope) return@launch
                        addFirmwareMessage(V3ServiceFirmwareMessage.DownloadFailed(++nextFirmwareMessageId, action.requestId, error))
                    }
                }
            }
        }
    }

    private fun addFirmwareMessage(message: V3ServiceFirmwareMessage) {
        state.value = state.value.copy(firmwareMessages = state.value.firmwareMessages + message)
    }

    private fun refreshFirmwareCatalog() {
        val scope = viewScope ?: return
        firmwareCatalogJob?.cancel()
        val request = ++firmwareCatalogRequest
        firmwareCatalog = null
        state.value = state.value.copy(
            firmwareCatalogSize = null, firmwareCatalogError = null, firmwareUpdates = emptyMap(),
            firmwareCatalogRevision = state.value.firmwareCatalogRevision + 1,
        )
        refreshBoards()
        firmwareCatalogJob = scope.launch {
            try {
                val catalog = loadFirmwareCatalog()
                coroutineContext.ensureActive()
                if (viewScope !== scope || firmwareCatalogRequest != request) return@launch
                firmwareCatalog = catalog
                state.value = state.value.copy(
                    firmwareCatalogSize = catalog.size, firmwareCatalogError = null,
                    firmwareUpdates = firmwareUpdates(),
                    firmwareCatalogRevision = state.value.firmwareCatalogRevision + 1,
                )
                refreshBoards()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                if (viewScope !== scope || firmwareCatalogRequest != request) return@launch
                state.value = state.value.copy(
                    firmwareCatalogError = error,
                    firmwareCatalogRevision = state.value.firmwareCatalogRevision + 1,
                )
                refreshBoards()
            }
        }
    }

    private fun firmwareUpdates(): Map<Int, Boolean> = firmwareCatalog?.let { catalog ->
        installedFirmwareVersions.mapValues { (address, version) ->
            getFirmwareForBoard.hasUpdate(catalog, address, version)
        }
    }.orEmpty()

    private fun handleResult(event: V3AccountProfileLoadEvent) {
        when (event) {
            V3AccountProfileLoadEvent.Authorized -> state.value = state.value.copy(isTokenLoaded = true, isContentVisible = true)
            is V3AccountProfileLoadEvent.ProfileLoaded -> {
                currentHeader = currentHeader.copy(firstName = event.profile.firstName, lastName = event.profile.lastName)
                updateHeader()
                finishRefresh()
            }
            V3AccountProfileLoadEvent.RefreshFinished -> finishRefresh()
            V3AccountProfileLoadEvent.ProfileUnavailable -> {
                updateHeader()
                state.value = state.value.copy(isContentVisible = true)
            }
            is V3AccountProfileLoadEvent.ServerError -> addMessage(event.message)
            V3AccountProfileLoadEvent.NoUserData -> addMessage(null)
        }
    }
    private fun updateHeader() {
        state.value = state.value.copy(header = currentHeader, headerRevision = ++nextHeaderRevision)
    }
    private fun finishRefresh() { state.value = state.value.copy(refreshCompletionId = state.value.refreshCompletionId + 1) }
    private fun addMessage(message: String?) {
        state.value = state.value.copy(messages = state.value.messages + V3AccountProfileMessage(++nextMessageId, message))
    }
    private fun detachView() {
        ++firmwareCatalogRequest
        firmwareCatalogJob?.cancel()
        firmwareCatalogJob = null
        firmwareDownloadJob?.cancel()
        firmwareDownloadJob = null
        boardsUpdatesJob?.cancel()
        boardsUpdatesJob = null
        enableCachedBoardUpdates = false
        firmwareCatalog = null
        installedFirmwareVersions = emptyMap()
        session = null
        viewScope?.cancel()
        viewScope = null
        state.value = state.value.copy(isTokenLoaded = false, messages = emptyList(),
            firmwareDownloadRequestId = null, firmwareMessages = emptyList(), isBoardRenderingReady = false, boardSubmissions = emptyList())
    }
    override fun onCleared() {
        cleared = true
        detachView()
        super.onCleared()
    }
}
