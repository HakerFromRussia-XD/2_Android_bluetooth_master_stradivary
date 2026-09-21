package com.bailout.stickk.ubi4.versions.v3.presentation.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.games.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class V3GamesViewModel(
    private val getAvailability: GetGameAvailabilityUseCaseV3,
    private val loadManifest: LoadGameManifestUseCaseV3,
    private val checkStore: CheckGameInRuStoreUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3GamesUiState())
    val uiState = state.asStateFlow()
    private var remoteGame: V3Game? = null
    private var viewScope: CoroutineScope? = null
    private var manifestJob: Job? = null
    private var storeJob: Job? = null
    private var nextEffectId = 0L
    private var cleared = false

    fun onAction(action: V3GamesAction) {
        if (cleared) return
        when (action) {
            V3GamesAction.ViewAttached -> {
                if (viewScope != null) return
                viewScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
                renderIdleState()
                refreshManifest(false)
            }
            V3GamesAction.ViewDetached -> detachView()
            else -> {
                if (viewScope == null) return
                when (action) {
                    V3GamesAction.ViewResumed -> { renderIdleState(); refreshManifest(false) }
                    V3GamesAction.PrimaryClicked -> when (state.value.availability?.action) {
                        V3GameAction.PLAY -> emit(V3GamesCommand.LaunchGame(state.value.availability!!.game))
                        V3GameAction.UPDATE -> emit(V3GamesCommand.OpenStore)
                        V3GameAction.INSTALL -> checkAvailabilityAndOpenStore()
                        V3GameAction.UNAVAILABLE -> refreshManifest(true)
                        null -> Unit
                    }
                    V3GamesAction.DeleteClicked -> {
                        if (getAvailability(remoteGame).isInstalled) emit(V3GamesCommand.UninstallGame)
                        else renderIdleState()
                    }
                    is V3GamesAction.EffectHandled -> state.value = state.value.copy(effects = state.value.effects.filterNot { it.id == action.id })
                    is V3GamesAction.PlatformActionFailed -> {
                        if (action.message == V3GamesMessage.LAUNCH_FAILED) renderIdleState()
                        message(action.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun renderIdleState() {
        val availability = getAvailability(remoteGame)
        state.value = state.value.copy(availability = availability, isActionEnabled = availability.action != V3GameAction.UNAVAILABLE)
    }

    private fun refreshManifest(showErrors: Boolean) {
        val scope = viewScope ?: return
        if (manifestJob?.isActive == true) return
        if (state.value.availability?.hasManifestUrl != true) {
            remoteGame = null
            renderIdleState()
            if (showErrors && !getAvailability(null).isInstalled) message(V3GamesMessage.MANIFEST_URL_MISSING)
            return
        }
        manifestJob = scope.launch {
            try {
                val game = loadManifest()
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                remoteGame = game
                renderIdleState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                remoteGame = null
                renderIdleState()
                if (showErrors || !getAvailability(null).isInstalled) message(V3GamesMessage.MANIFEST_LOAD_FAILED, error.message)
            }
        }
    }

    private fun checkAvailabilityAndOpenStore() {
        val game = remoteGame ?: run { refreshManifest(true); return }
        val scope = viewScope ?: return
        if (storeJob?.isActive == true) return
        state.value = state.value.copy(isActionEnabled = false)
        storeJob = scope.launch {
            try {
                val published = checkStore(game.packageName)
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                if (published) emit(V3GamesCommand.OpenStore) else message(V3GamesMessage.NOT_PUBLISHED)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                message(V3GamesMessage.STORE_CHECK_FAILED)
            }
            renderIdleState()
        }
    }

    private fun emit(command: V3GamesCommand) {
        state.value = state.value.copy(effects = state.value.effects + V3GamesEffect(++nextEffectId, command))
    }
    private fun message(message: V3GamesMessage, detail: String? = null) = emit(V3GamesCommand.ShowMessage(message, detail))
    private fun detachView() {
        viewScope?.cancel()
        viewScope = null
        manifestJob = null
        storeJob = null
        state.value = state.value.copy(effects = emptyList())
    }
    override fun onCleared() { cleared = true; detachView(); super.onCleared() }
}
