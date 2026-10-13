package com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation.GetProsthesisInformationUseCaseV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class V3ProsthesisInformationViewModel(private val getInformation: GetProsthesisInformationUseCaseV3) : ViewModel() {
    private val state = MutableStateFlow(V3ProsthesisInformationUiState())
    val uiState = state.asStateFlow()
    private var attached = false
    private var cleared = false
    private var informationJob: Job? = null

    fun onAction(action: V3ProsthesisInformationAction) {
        if (cleared) return
        when (action) {
            V3ProsthesisInformationAction.ViewAttached -> if (!attached) {
                attached = true
                informationJob = viewModelScope.launch {
                    getInformation.observe().collect {
                        state.value = V3ProsthesisInformationUiState(it)
                    }
                }
            }
            V3ProsthesisInformationAction.ViewDetached -> {
                attached = false
                informationJob?.cancel()
                informationJob = null
            }
        }
    }

    override fun onCleared() {
        cleared = true
        attached = false
        informationJob?.cancel()
        super.onCleared()
    }
}
