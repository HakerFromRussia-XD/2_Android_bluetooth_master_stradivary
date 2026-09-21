package com.bailout.stickk.ubi4.versions.v3.presentation.gestureeditor

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.ObserveGestureSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.RequestGestureSettingsUseCaseV3
import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetGestureEditorNamesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SaveGestureEditorNamesUseCaseV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class V3GestureEditorViewModel(
    private val getNames: GetGestureEditorNamesUseCaseV3,
    private val saveNames: SaveGestureEditorNamesUseCaseV3,
    private val observeSettings: ObserveGestureSettingsUseCaseV3,
    private val requestSettings: RequestGestureSettingsUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3GestureEditorUiState())
    val uiState = state.asStateFlow()
    private var names: List<String> = emptyList()
    private var nameIndex = 0
    private var attached = false
    private var cleared = false
    private var settingsJob: Job? = null
    private var requestJob: Job? = null
    private var requested = false
    private var rendererReady = false
    private var settingsApplied = false
    private var initialOpenIssued = false
    private var nextUpdateId = 0L

    fun onAction(action: V3GestureEditorAction) {
        if (cleared) return
        when (action) {
            V3GestureEditorAction.ViewCreated -> {
                stopLoading()
                requested = false
                rendererReady = false
                settingsApplied = false
                initialOpenIssued = false
                // A recreated Activity used to reload preferences and leave edit mode.
                val stored = getNames()
                names = stored.names.toList()
                nameIndex = stored.gestureNumber - 1
                state.value = V3GestureEditorUiState(name = names[nameIndex])
                attached = true
                settingsJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    observeSettings().collect { settings ->
                        ensureActive()
                        state.value = state.value.copy(
                            loadedSettings = settings ?: state.value.loadedSettings,
                            pendingSettings = state.value.pendingSettings + V3GestureSettingsUpdate(++nextUpdateId, settings),
                        )
                    }
                }
            }
            V3GestureEditorAction.ViewDestroyed -> { attached = false; stopLoading() }
            else -> if (attached) when (action) {
                is V3GestureEditorAction.ViewStarted -> if (!requested && requestJob?.isActive != true) {
                    requestJob = viewModelScope.launch {
                        requestSettings(action.gestureId)
                        requested = true
                    }
                }
                V3GestureEditorAction.ViewStopped -> { requestJob?.cancel(); requestJob = null }
                V3GestureEditorAction.RendererReady -> { rendererReady = true; requestInitialOpenIfReady() }
                is V3GestureEditorAction.SettingsApplied -> {
                    val update = state.value.pendingSettings.firstOrNull() ?: return
                    if (update.id != action.updateId) return
                    state.value = state.value.copy(pendingSettings = state.value.pendingSettings.drop(1))
                    if (update.settings != null) settingsApplied = true
                    requestInitialOpenIfReady()
                }
                V3GestureEditorAction.InitialOpenHandled -> state.value = state.value.copy(initialOpenRequested = false)
                V3GestureEditorAction.EditNameClicked -> {
                    if (state.value.isEditingName) saveName(closeInput = true)
                    else state.value = state.value.copy(isEditingName = true, nameInput = state.value.name)
                }
                is V3GestureEditorAction.NameChanged -> if (state.value.isEditingName) {
                    state.value = state.value.copy(nameInput = action.text)
                }
                V3GestureEditorAction.SaveNameRequested -> if (state.value.isEditingName) saveName(closeInput = false)
                else -> Unit
            }
        }
    }

    private fun saveName(closeInput: Boolean) {
        val name = state.value.nameInput
        names = names.toMutableList().also { it[nameIndex] = name }.toList()
        saveNames(names)
        // The screen save button finishes without changing the input UI first.
        if (closeInput) state.value = state.value.copy(name = name, isEditingName = false)
    }

    private fun requestInitialOpenIfReady() {
        if (!rendererReady || !settingsApplied || initialOpenIssued) return
        initialOpenIssued = true
        state.value = state.value.copy(initialOpenRequested = true)
    }

    private fun stopLoading() {
        settingsJob?.cancel()
        settingsJob = null
        requestJob?.cancel()
        requestJob = null
    }

    override fun onCleared() { cleared = true; attached = false; stopLoading(); super.onCleared() }
}
