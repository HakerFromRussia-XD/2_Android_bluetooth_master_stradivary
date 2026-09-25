package com.bailout.stickk.ubi4.versions.v3.presentation.dashboard

import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContent
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContentTarget

data class V3DashboardSlotContentUiState(
    val content: V3DashboardSlotContent = V3DashboardSlotContent(),
    val resetAllConfirmationId: Long? = null,
)

sealed interface V3DashboardSlotContentAction {
    data class ViewCreated(val slot: V3DashboardSlotContentTarget) : V3DashboardSlotContentAction
    data object Refresh : V3DashboardSlotContentAction
    data class ParameterChanged(val path: String, val value: String) : V3DashboardSlotContentAction
    data object Send : V3DashboardSlotContentAction
    data object Save : V3DashboardSlotContentAction
    data object Reset : V3DashboardSlotContentAction
    data object ResetAll : V3DashboardSlotContentAction
    data class ResetAllConfirmed(val requestId: Long) : V3DashboardSlotContentAction
    data class ResetAllDismissed(val requestId: Long) : V3DashboardSlotContentAction
    data object ViewDestroyed : V3DashboardSlotContentAction
}
