package com.bailout.stickk.ubi4.versions.v3.presentation.vtosc

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.LoadVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.ObserveVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.ObserveVtoscLoadingUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.SaveVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscParameter
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.WriteVtoscConfigUseCaseV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3VtoscViewModel(
    private val observeConfig: ObserveVtoscConfigUseCaseV3,
    private val observeLoading: ObserveVtoscLoadingUseCaseV3,
    private val loadConfig: LoadVtoscConfigUseCaseV3,
    private val writeConfig: WriteVtoscConfigUseCaseV3,
    private val saveConfig: SaveVtoscConfigUseCaseV3,
) : ViewModel() {
    private var isViewAttached = false
    private var hasLocalDraft = false
    private val _uiState = MutableStateFlow(V3VtoscUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeConfig().collect { config ->
                if (!hasLocalDraft) {
                    _uiState.value = V3VtoscUiState.from(config, isLoading = false)
                }
            }
        }
        viewModelScope.launch {
            observeLoading().collect { loading ->
                // Pull-to-refresh often gets the same slot bytes; config flow may not re-emit.
                // Always drop the top loader when the READ finishes (success or failure).
                if (!loading) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    @MainThread
    fun onAction(action: V3VtoscAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3VtoscAction.ViewAttached -> {
                isViewAttached = true
                hasLocalDraft = false
                _uiState.update { it.copy(isLoading = true) }
                loadConfig()
            }
            V3VtoscAction.ViewDetached -> {
                isViewAttached = false
                hasLocalDraft = false
            }
            V3VtoscAction.RefreshRequested -> {
                if (!isViewAttached) return
                hasLocalDraft = false
                _uiState.update { it.copy(isLoading = true) }
                loadConfig()
            }
            is V3VtoscAction.SliderValueChanged -> updateSlider(action.parameter, action.value, commit = false)
            is V3VtoscAction.SliderChangeCommitted -> updateSlider(action.parameter, action.value, commit = true)
        }
    }

    private fun updateSlider(parameter: V3VtoscParameter, value: Int, commit: Boolean) {
        if (!isViewAttached) return
        val coerced = value.coerceIn(parameter.range)
        hasLocalDraft = true
        _uiState.update { state ->
            state.copy(
                sliders = state.sliders.map { slider ->
                    if (slider.parameter == parameter) slider.copy(value = coerced) else slider
                },
                isLoading = false,
            )
        }
        if (commit) {
            writeConfig(_uiState.value.config())
            saveConfig()
            hasLocalDraft = false
        }
    }
}
