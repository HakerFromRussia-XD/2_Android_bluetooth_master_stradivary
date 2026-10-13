package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.ObserveServiceEngineerAccessUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.RestoreDeviceRoleUseCaseV3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class V3AccountProfileViewModel(
    private val getViewData: GetAccountProfileViewDataUseCaseV3,
    private val loadProfile: LoadAccountProfileUseCaseV3,
    private val cacheHeader: CacheAccountProfileHeaderUseCaseV3,
    private val observeSerialNumber: ObserveAccountSerialNumberUseCaseV3,
    private val restoreDeviceRole: RestoreDeviceRoleUseCaseV3,
    private val observeServiceEngineerAccess: ObserveServiceEngineerAccessUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3AccountProfileUiState())
    val uiState = state.asStateFlow()
    private var context = V3AccountProfileContext()
    private var currentHeader = V3AccountProfileHeader()
    private var session: V3AccountProfileLoadSession? = null
    private var viewScope: CoroutineScope? = null
    private var profileLoadJob: Job? = null
    private var serialObservationJob: Job? = null
    private var loadRequested = false
    private var serialObserved = false
    private var nextHeaderRevision = 0L
    private var nextMessageId = 0L
    private var cleared = false

    fun onAction(action: V3AccountProfileAction) {
        if (cleared) return
        when (action) {
            is V3AccountProfileAction.ViewAttached -> {
                if (viewScope != null) return
                restoreDeviceRole()
                val serviceEngineerAccess = observeServiceEngineerAccess()
                val data = getViewData(context)
                context = data.context
                session = null
                loadRequested = false
                serialObserved = false
                viewScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
                currentHeader = V3AccountProfileHeader()
                val header = currentHeader.takeIf { action.loadInBackground }
                state.value = V3AccountProfileUiState(
                    header = header,
                    headerRevision = ++nextHeaderRevision,
                    hasCachedProfile = false,
                    isContentVisible = action.loadInBackground || action.hasCachedBoards,
                    areBoardServiceActionsVisible = serviceEngineerAccess.value,
                )
                // The old adapter was initialized before the current device versions were read.
                currentHeader = currentHeader.copy(versions = data.versions)
                observeDeviceSerial()
                viewScope?.launch {
                    serviceEngineerAccess.collect { allowed ->
                        state.value = state.value.copy(areBoardServiceActionsVisible = allowed)
                    }
                }
            }
            V3AccountProfileAction.ViewDetached -> detachView()
            V3AccountProfileAction.LoadRequested -> {
                if (viewScope == null) return
                val retrySerialRead = loadRequested && session == null
                loadRequested = true
                if (retrySerialRead) observeDeviceSerial()
                else if (session != null) startProfileLoad()
                if (session == null) finishRefresh()
            }
            V3AccountProfileAction.HeaderRefreshRequested -> if (viewScope != null) updateHeader()
            is V3AccountProfileAction.HeaderRendered -> {
                if (viewScope != null && action.revision == state.value.headerRevision) state.value.header?.let { cacheHeader(it) }
            }
            is V3AccountProfileAction.MessageShown -> {
                if (viewScope == null) return
                state.value = state.value.copy(messages = state.value.messages.filterNot { it.id == action.id })
            }
        }
    }

    private fun observeDeviceSerial() {
        serialObservationJob?.cancel()
        serialObservationJob = viewScope?.launch(start = CoroutineStart.UNDISPATCHED) {
            observeSerialNumber().collect(::onSerialNumber)
        }
    }

    private fun onSerialNumber(serialNumber: String?) {
        val serial = serialNumber.orEmpty()
        if (serialObserved && serial == context.serialNumber) return
        val changedDevice = serialObserved
        serialObserved = true
        profileLoadJob?.cancel()
        profileLoadJob = null
        context = context.copy(serialNumber = serial)
        session = serial.takeIf(String::isNotEmpty)?.let { V3AccountProfileLoadSession(context) }
        if (changedDevice || serial.isEmpty()) {
            currentHeader = currentHeader.copy(firstName = "", lastName = "")
            state.value = state.value.copy(
                header = state.value.header?.let { currentHeader },
                headerRevision = ++nextHeaderRevision,
                hasCachedProfile = false,
                isTokenLoaded = false,
                messages = emptyList(),
            )
        }
        if (serial.isNotEmpty()) {
            getViewData(context).cachedHeader?.let { cached ->
                state.value = state.value.copy(
                    header = cached,
                    headerRevision = ++nextHeaderRevision,
                    hasCachedProfile = true,
                    isContentVisible = true,
                )
            }
        }
        if (loadRequested && session != null) startProfileLoad()
    }

    private fun startProfileLoad() {
        val activeSession = session ?: return
        profileLoadJob?.cancel()
        profileLoadJob = viewScope?.launch {
            loadProfile(activeSession).collect { event ->
                if (session === activeSession) handleResult(event)
            }
        }
    }

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
        session = null
        profileLoadJob?.cancel()
        profileLoadJob = null
        serialObservationJob?.cancel()
        serialObservationJob = null
        loadRequested = false
        serialObserved = false
        viewScope?.cancel()
        viewScope = null
        state.value = state.value.copy(isTokenLoaded = false, messages = emptyList())
    }
    override fun onCleared() {
        cleared = true
        detachView()
        super.onCleared()
    }
}
