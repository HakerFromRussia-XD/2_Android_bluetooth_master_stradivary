package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareForDebugUseCaseV3
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3ServiceFirmwareViewModel(
    private val install: InstallServiceFirmwareUseCaseV3,
    private val installForDebug: InstallServiceFirmwareForDebugUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3ServiceFirmwareUiState())
    val uiState = state.asStateFlow()
    private val effectChannel = Channel<V3ServiceFirmwareEffect>(Channel.UNLIMITED)
    val effects = effectChannel.receiveAsFlow()
    private val attempts = mutableSetOf<Job>()
    private var attached = false
    private var generation = 0

    fun onAction(action: V3ServiceFirmwareAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3ServiceFirmwareAction.ViewCreated -> attached = true
            V3ServiceFirmwareAction.ViewDestroyed -> closeView()
            is V3ServiceFirmwareAction.ResultShown -> state.value = state.value.copy(
                progressByRequest = state.value.progressByRequest - action.requestId,
            )
            is V3ServiceFirmwareAction.InstallConfirmed -> if (attached) startAttempt(action)
            is V3ServiceFirmwareAction.DebugInstallRequested -> if (attached) {
                // Keep the probe-build rejection synchronous, before the operation is launched.
                installForDebug.requireAllowed()
                trackAttempt(viewModelScope.launch(start = CoroutineStart.LAZY) { installForDebug(action.file) })
            }
        }
    }

    private fun startAttempt(action: V3ServiceFirmwareAction.InstallConfirmed) {
        val session = generation
        fun isCurrent() = attached && generation == session && viewModelScope.isActive
        state.value = state.value.copy(progressByRequest = state.value.progressByRequest + (action.requestId to 0))
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            var phase = "prepare_notifications"
            var startedAt = 0L
            val timeoutJob = launch {
                var last = state.value.progressByRequest[action.requestId] ?: 0
                while (isActive) {
                    delay(30_000)
                    val progress = state.value.progressByRequest[action.requestId] ?: 0
                    if (progress == last && progress < 100) {
                        if (isCurrent()) effectChannel.trySend(V3ServiceFirmwareEffect.Stalled(action.requestId, phase, last, startedAt))
                        break
                    }
                    last = progress
                }
            }
            try {
                val result = install(action.boardAddress, action.file,
                    onProgress = { progress ->
                        // The old progress update was a sibling Main job, independent of the transfer.
                        viewModelScope.launch(Dispatchers.Main) {
                            if (isCurrent() && action.requestId in state.value.progressByRequest) state.value = state.value.copy(
                                progressByRequest = state.value.progressByRequest + (action.requestId to progress),
                            )
                        }
                    },
                    onPhaseChanged = { name, id -> phase = name; startedAt = id },
                )
                if (isCurrent()) effectChannel.trySend(V3ServiceFirmwareEffect.Completed(action.requestId, result))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (isCurrent()) effectChannel.trySend(V3ServiceFirmwareEffect.Failed(action.requestId, error))
            } finally {
                timeoutJob.cancel()
            }
        }
        trackAttempt(job)
    }

    private fun trackAttempt(job: Job) {
        attempts += job
        job.invokeOnCompletion { attempts -= job }
        job.start()
    }

    private fun closeView() {
        attached = false
        generation++
        attempts.toList().forEach { it.cancel() }
        state.value = V3ServiceFirmwareUiState()
        while (effectChannel.tryReceive().isSuccess) { /* Old Activity effects must not reach its replacement. */ }
    }

    override fun onCleared() {
        closeView()
        super.onCleared()
    }
}
