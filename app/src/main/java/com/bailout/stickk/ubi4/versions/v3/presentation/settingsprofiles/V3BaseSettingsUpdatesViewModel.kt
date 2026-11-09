package com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3BaseSettingsUpdatesViewModel(
    private val observeUpdates: ObserveBaseSettingsUpdatesUseCaseV3,
    private val refreshUpdates: RefreshBaseSettingsUpdatesUseCaseV3,
    private val respondToUpdate: RespondToBaseSettingsUpdateUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3BaseSettingsUpdatesUiState())
    val uiState = state.asStateFlow()
    private var environment = V3BaseSettingsEnvironment()
    private var observation: Job? = null
    private var refresh: Job? = null
    private var response: Job? = null
    private var session = 0L
    private var responseRequest = 0L

    fun onAction(action: V3BaseSettingsUpdatesAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3BaseSettingsUpdatesAction.ViewCreated -> {
                if (observation != null) return
                val currentSession = ++session
                observation = viewModelScope.launch {
                    observeUpdates().collect { next ->
                        if (session != currentSession) return@collect
                        val changed = next.serial != environment.serial || next.isUser != environment.isUser
                        val becameReady = next.canApply && !environment.canApply
                        val becameOnline = next.online && !environment.online
                        if (changed) {
                            responseRequest++
                            refresh?.cancel(); response?.cancel()
                        }
                        environment = next
                        val offer = next.offer.takeIf { next.canApply }
                        state.value = state.value.copy(
                            offer = offer, isApplying = if (changed) false else state.value.isApplying,
                            failed = state.value.failed && state.value.offer == offer,
                        )
                        if (changed || becameReady || becameOnline) refreshIfReady()
                    }
                }
            }
            V3BaseSettingsUpdatesAction.ViewResumed -> refreshIfReady()
            V3BaseSettingsUpdatesAction.ViewDestroyed -> close()
            is V3BaseSettingsUpdatesAction.Respond -> respond(action)
        }
    }

    private fun refreshIfReady() {
        if (observation == null || !environment.isUser || !environment.canApply || !environment.online || environment.serial.isBlank() || refresh?.isActive == true) return
        val serial = environment.serial
        refresh = viewModelScope.launch {
            try { refreshUpdates(serial) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep cached settings and retry on the next resume or role change. */ }
        }
    }

    private fun respond(action: V3BaseSettingsUpdatesAction.Respond) {
        if (observation == null || state.value.isApplying || state.value.offer != action.offer) return
        val currentSession = session
        val request = ++responseRequest
        state.value = state.value.copy(isApplying = true, failed = false)
        response = viewModelScope.launch {
            try {
                val handled = respondToUpdate(action.offer, action.accept)
                if (session == currentSession && request == responseRequest && state.value.offer == action.offer) {
                    state.value = state.value.copy(offer = action.offer.takeUnless { handled }, isApplying = false, failed = !handled)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (session == currentSession && request == responseRequest && state.value.offer == action.offer)
                    state.value = state.value.copy(isApplying = false, failed = true)
            } finally {
                if (session == currentSession && request == responseRequest) state.value = state.value.copy(isApplying = false)
            }
        }
    }

    private fun close() {
        session++
        responseRequest++
        observation?.cancel(); observation = null
        refresh?.cancel(); refresh = null
        response?.cancel(); response = null
        environment = V3BaseSettingsEnvironment()
        state.value = V3BaseSettingsUpdatesUiState()
    }

    override fun onCleared() { close(); super.onCleared() }
}
