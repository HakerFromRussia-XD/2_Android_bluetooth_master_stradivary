package com.bailout.stickk.ubi4.versions.v3.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.LoadDashboardSlotsUseCaseV3
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3DashboardSlotsViewModel(
    private val loadSlots: LoadDashboardSlotsUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3DashboardSlotsUiState())
    val uiState = state.asStateFlow()
    private var loadingJob: Job? = null
    private var requestedAddress: Int? = null

    fun onAction(action: V3DashboardSlotsAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            is V3DashboardSlotsAction.ViewCreated -> {
                if (loadingJob?.isActive == true && requestedAddress == action.deviceAddress) return
                loadingJob?.cancel()
                requestedAddress = action.deviceAddress
                loadingJob = viewModelScope.launch {
                    loadSlots(action.deviceAddress).collect {
                        state.value = V3DashboardSlotsUiState(it.deviceAddress, it.isLoading, it.slots, it.errorMessage)
                    }
                }
            }
            V3DashboardSlotsAction.ViewDestroyed -> {
                loadingJob?.cancel()
                loadingJob = null
            }
        }
    }
}
