package com.bailout.stickk.ubi4.versions.v3.presentation.dashboard

import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlot

data class V3DashboardSlotsUiState(
    val deviceAddress: Int = 0,
    val isLoading: Boolean = false,
    val slots: List<V3DashboardSlot> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface V3DashboardSlotsAction {
    data class ViewCreated(val deviceAddress: Int) : V3DashboardSlotsAction
    data object ViewDestroyed : V3DashboardSlotsAction
}
