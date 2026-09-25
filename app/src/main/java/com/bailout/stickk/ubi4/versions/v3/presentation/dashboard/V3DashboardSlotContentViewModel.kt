package com.bailout.stickk.ubi4.versions.v3.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.EditDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.LoadDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.ObserveDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.ResetAllDashboardSlotsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.ResetDashboardSlotUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.SaveDashboardSlotsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.SendDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContentTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3DashboardSlotContentViewModel(
    private val loadContent: LoadDashboardSlotContentUseCaseV3,
    private val observeContent: ObserveDashboardSlotContentUseCaseV3,
    private val editContent: EditDashboardSlotContentUseCaseV3,
    private val sendContent: SendDashboardSlotContentUseCaseV3,
    private val saveSlots: SaveDashboardSlotsUseCaseV3,
    private val resetSlot: ResetDashboardSlotUseCaseV3,
    private val resetAllSlots: ResetAllDashboardSlotsUseCaseV3,
) : ViewModel() {
    private val state = MutableStateFlow(V3DashboardSlotContentUiState())
    val uiState = state.asStateFlow()
    private var contentScope: CoroutineScope? = null
    private var slot: V3DashboardSlotContentTarget? = null
    private var nextConfirmationId = 0L

    fun onAction(action: V3DashboardSlotContentAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            is V3DashboardSlotContentAction.ViewCreated -> {
                contentScope?.cancel()
                state.value = state.value.copy(resetAllConfirmationId = null)
                slot = action.slot
                contentScope = CoroutineScope(viewModelScope.coroutineContext +
                    SupervisorJob(viewModelScope.coroutineContext[Job])).also { scope ->
                    scope.launch {
                        observeContent().collect { state.value = state.value.copy(content = it) }
                    }
                }
                refresh()
            }
            V3DashboardSlotContentAction.Refresh -> refresh()
            is V3DashboardSlotContentAction.ParameterChanged -> {
                if (slot != null) editContent(action.path, action.value)
            }
            V3DashboardSlotContentAction.Send -> slot?.let { sendContent(it) }
            V3DashboardSlotContentAction.Save -> slot?.let { saveSlots(it) }
            V3DashboardSlotContentAction.Reset -> slot?.let { resetSlot(it) }
            V3DashboardSlotContentAction.ResetAll -> {
                if (slot != null && state.value.resetAllConfirmationId == null) {
                    state.value = state.value.copy(resetAllConfirmationId = ++nextConfirmationId)
                }
            }
            is V3DashboardSlotContentAction.ResetAllConfirmed -> {
                val target = slot
                if (target != null && state.value.resetAllConfirmationId == action.requestId) {
                    state.value = state.value.copy(resetAllConfirmationId = null)
                    resetAllSlots(target)
                }
            }
            is V3DashboardSlotContentAction.ResetAllDismissed -> {
                if (state.value.resetAllConfirmationId == action.requestId) {
                    state.value = state.value.copy(resetAllConfirmationId = null)
                }
            }
            V3DashboardSlotContentAction.ViewDestroyed -> {
                contentScope?.cancel()
                contentScope = null
                slot = null
                state.value = state.value.copy(resetAllConfirmationId = null)
            }
        }
    }

    private fun refresh() {
        val target = slot ?: return
        // As before, each Refresh has its own timeout, all cancelled when the view is destroyed.
        contentScope?.launch { loadContent(target) }
    }
}
