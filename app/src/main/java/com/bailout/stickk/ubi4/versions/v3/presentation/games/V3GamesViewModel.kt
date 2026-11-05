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
    private var games = loadManifest.localGames.associateBy { it.packageName }
    private var viewScope: CoroutineScope? = null
    private var manifestJob: Job? = null
    private val storeJobs = mutableMapOf<String, Job>()
    private var nextEffectId = 0L
    private var cleared = false

    fun onAction(action: V3GamesAction) {
        if (cleared) return
        when (action) {
            V3GamesAction.ViewAttached -> {
                if (viewScope != null) return
                viewScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
                renderIdleState()
                refreshManifest()
            }
            V3GamesAction.ViewDetached -> detachView()
            else -> {
                if (viewScope == null) return
                when (action) {
                    V3GamesAction.ViewResumed -> { renderIdleState(); refreshManifest() }
                    is V3GamesAction.PrimaryClicked -> {
                        // Recheck the OS at tap time: installation can change while this screen is open.
                        val game = games[action.packageName] ?: return
                        if (storeJobs[game.packageName]?.isActive == true) return
                        val availability = getAvailability(game)
                        renderIdleState()
                        when (availability.action) {
                            V3GameAction.PLAY -> emit(V3GamesCommand.LaunchGame(game))
                            V3GameAction.UPDATE -> emit(V3GamesCommand.OpenStore(game.packageName))
                            V3GameAction.INSTALL -> checkAvailabilityAndOpenStore(game)
                        }
                    }
                    is V3GamesAction.DeleteClicked -> {
                        val game = games[action.packageName] ?: return
                        if (getAvailability(game).isInstalled) emit(V3GamesCommand.UninstallGame(game.packageName))
                        renderIdleState()
                    }
                    is V3GamesAction.EffectHandled -> state.value = state.value.copy(effects = state.value.effects.filterNot { it.id == action.id })
                    is V3GamesAction.PlatformActionFailed -> {
                        renderIdleState()
                        message(action.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun renderIdleState() {
        state.value = state.value.copy(cards = games.values.map { game ->
            V3GameCardState(getAvailability(game), storeJobs[game.packageName]?.isActive != true)
        })
    }

    private fun refreshManifest() {
        val scope = viewScope ?: return
        if (!loadManifest.hasManifestUrl || manifestJob?.isActive == true) return
        manifestJob = scope.launch {
            try {
                val remote = loadManifest()
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                games = (loadManifest.localGames + remote).associateBy { it.packageName }
                renderIdleState()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                // Keep known catalog metadata; it never decides whether a game is installed.
                renderIdleState()
            }
        }
    }

    private fun checkAvailabilityAndOpenStore(game: V3Game) {
        val scope = viewScope ?: return
        if (storeJobs[game.packageName]?.isActive == true) return
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                val published = checkStore(game.packageName)
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                if (published) emit(V3GamesCommand.OpenStore(game.packageName)) else message(V3GamesMessage.NOT_PUBLISHED)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                if (viewScope !== scope) return@launch
                message(V3GamesMessage.STORE_CHECK_FAILED)
            } finally {
                if (viewScope === scope) {
                    storeJobs.remove(game.packageName)
                    renderIdleState()
                }
            }
        }
        storeJobs[game.packageName] = job
        job.start()
        renderIdleState()
    }

    private fun emit(command: V3GamesCommand) {
        state.value = state.value.copy(effects = state.value.effects + V3GamesEffect(++nextEffectId, command))
    }
    private fun message(message: V3GamesMessage, detail: String? = null) = emit(V3GamesCommand.ShowMessage(message, detail))
    private fun detachView() {
        viewScope?.cancel()
        viewScope = null
        manifestJob = null
        storeJobs.clear()
        state.value = state.value.copy(effects = emptyList())
    }
    override fun onCleared() { cleared = true; detachView(); super.onCleared() }
}
