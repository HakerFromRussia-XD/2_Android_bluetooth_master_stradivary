package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.ObserveAccountStatisticsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.RequestAccountStatisticsUseCaseV3
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class V3AccountStatisticsViewModel(
    private val observeStatistics: ObserveAccountStatisticsUseCaseV3,
    private val requestStatistics: RequestAccountStatisticsUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3AccountStatisticsUiState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    private var viewAttached = false
    private var cleared = false

    fun onAction(action: V3AccountStatisticsAction) {
        if (cleared) return
        when (action) {
            V3AccountStatisticsAction.ViewAttached -> {
                if (viewAttached) return
                viewAttached = true
                observation = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    observeStatistics().collect { gestures ->
                        if (viewAttached) state.value = V3AccountStatisticsUiState(gestures)
                    }
                }
                requestStatistics()
            }
            V3AccountStatisticsAction.ViewDetached -> detachView()
        }
    }

    private fun detachView() {
        viewAttached = false
        observation?.cancel()
        observation = null
    }

    override fun onCleared() {
        cleared = true
        detachView()
        super.onCleared()
    }
}
