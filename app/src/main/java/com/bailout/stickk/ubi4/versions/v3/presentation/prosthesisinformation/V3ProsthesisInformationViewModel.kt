package com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation

import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation.GetProsthesisInformationUseCaseV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class V3ProsthesisInformationViewModel(private val getInformation: GetProsthesisInformationUseCaseV3) : ViewModel() {
    private val state = MutableStateFlow(V3ProsthesisInformationUiState())
    val uiState = state.asStateFlow()
    private var attached = false
    private var cleared = false

    fun onAction(action: V3ProsthesisInformationAction) {
        if (cleared) return
        when (action) {
            V3ProsthesisInformationAction.ViewAttached -> if (!attached) {
                state.value = V3ProsthesisInformationUiState(getInformation())
                attached = true
            }
            V3ProsthesisInformationAction.ViewDetached -> attached = false
        }
    }

    override fun onCleared() {
        cleared = true
        attached = false
        super.onCleared()
    }
}
