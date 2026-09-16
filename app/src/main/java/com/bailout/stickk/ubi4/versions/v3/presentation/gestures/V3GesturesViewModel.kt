package com.bailout.stickk.ubi4.versions.v3.presentation.gestures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.GetGesturesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.LoadRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.RemoveGestureFromRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.ObserveGesturesChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.RequestActiveGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SelectGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.GetRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.EditRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SaveRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3RotationGroupSelection
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.MoveGestureInRotationGroupUseCaseV3
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class V3GesturesViewModel(
    private val getGestures: GetGesturesUseCaseV3,
    private val observeGesturesChanges: ObserveGesturesChangesUseCaseV3,
    private val selectGesture: SelectGestureUseCaseV3,
    private val requestActiveGesture: RequestActiveGestureUseCaseV3,
    private val loadRotationGroup: LoadRotationGroupUseCaseV3,
    private val removeGestureFromRotationGroup: RemoveGestureFromRotationGroupUseCaseV3,
    private val getRotationGroupSelection: GetRotationGroupSelectionUseCaseV3,
    private val editRotationGroupSelection: EditRotationGroupSelectionUseCaseV3,
    private val saveRotationGroupSelection: SaveRotationGroupSelectionUseCaseV3,
    private val moveGestureInRotationGroup: MoveGestureInRotationGroupUseCaseV3,
) : ViewModel() {
    private var isViewAttached = false
    private var deviceAddress = getGestures().activeGesture.deviceAddress
    private var isRotationGroupVisible = false
    private var rotationGroupRequestJob: Job? = null
    private var requestedRotationGroupAddress: String? = null
    private var dialogSequence = 0L
    private var pendingRemoval: PendingRemoval? = null
    private data class PendingRemoval(
        val requestId: Long,
        val deviceAddress: String,
        val position: Int,
        val gestureIds: List<Int>,
    )
    private var selectionMessageSequence = 0L
    private var pendingSelection: PendingSelection? = null
    private data class PendingSelection(
        val requestId: Long,
        val deviceAddress: String,
        val selection: V3RotationGroupSelection,
        val limitMessageId: Long? = null,
    )
    private val _uiState = MutableStateFlow(V3GesturesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { observeGesturesChanges().collect { updateState() } }
    }

    fun onAction(action: V3GesturesAction) {
        if (!viewModelScope.isActive) return
        when (action) {
            V3GesturesAction.ViewAttached -> isViewAttached = true
            V3GesturesAction.ViewDetached -> isViewAttached = false
            is V3GesturesAction.RotationGroupVisibilityChanged -> isRotationGroupVisible = action.isVisible
            V3GesturesAction.RotationGroupRequested -> {
                if (!isViewAttached || !_uiState.value.isInteractionEnabled) return
                isRotationGroupVisible = true
                cancelRotationGroupRequest()
            }
            is V3GesturesAction.GestureSelected -> {
                if (!isViewAttached || !_uiState.value.isInteractionEnabled) return
                selectGesture(deviceAddress, action.gestureId)
            }
            is V3GesturesAction.RotationGestureMoved -> {
                if (isViewAttached && isRotationGroupVisible && _uiState.value.isInteractionEnabled) {
                    moveGestureInRotationGroup(deviceAddress, action.fromPosition, action.toPosition, action.gestureIdsBeforeDrag)
                }
            }
            is V3GesturesAction.RotationGestureRemovalRequested -> {
                val current = getGestures()
                if (isViewAttached && isRotationGroupVisible && current.activeGesture.isInteractionEnabled &&
                    current.activeGesture.deviceAddress == deviceAddress &&
                    current.rotationGroupGestureIds == action.gestureIds && action.position in action.gestureIds.indices
                ) {
                    pendingSelection = null
                    pendingRemoval = PendingRemoval(++dialogSequence, deviceAddress, action.position, action.gestureIds.toList())
                }
            }
            is V3GesturesAction.RotationGestureRemovalConfirmed -> {
                val pending = pendingRemoval
                if (pending != null && pending.requestId == action.requestId) {
                    pendingRemoval = null
                    if (isViewAttached && isRotationGroupVisible) {
                        removeGestureFromRotationGroup(pending.deviceAddress, pending.position, pending.gestureIds)
                    }
                }
            }
            is V3GesturesAction.RotationGestureRemovalCancelled -> {
                if (pendingRemoval?.requestId == action.requestId) pendingRemoval = null
            }
            V3GesturesAction.RotationGroupSelectionRequested -> {
                val current = getGestures().activeGesture
                if (isViewAttached && isRotationGroupVisible && current.isInteractionEnabled && current.deviceAddress == deviceAddress) {
                    getRotationGroupSelection()?.let {
                        pendingRemoval = null
                        pendingSelection = PendingSelection(++dialogSequence, deviceAddress, it)
                    }
                }
            }
            is V3GesturesAction.RotationGroupGestureToggled -> {
                pendingSelection?.takeIf { it.requestId == action.requestId }?.let { pending ->
                    val result = editRotationGroupSelection(pending.selection, action.gestureId)
                    pendingSelection = pending.copy(
                        selection = result.selection,
                        limitMessageId = if (result.isLimitReached) ++selectionMessageSequence else null,
                    )
                }
            }
            is V3GesturesAction.RotationGroupSelectionSaved -> {
                pendingSelection?.takeIf { it.requestId == action.requestId }?.let { pending ->
                    pendingSelection = null
                    if (isViewAttached && isRotationGroupVisible) {
                        saveRotationGroupSelection(pending.deviceAddress, pending.selection)
                    }
                }
            }
            is V3GesturesAction.RotationGroupSelectionCancelled -> {
                if (pendingSelection?.requestId == action.requestId) pendingSelection = null
            }
            is V3GesturesAction.RotationGroupLimitMessageShown -> {
                pendingSelection?.takeIf { it.requestId == action.requestId && it.limitMessageId == action.messageId }?.let {
                    pendingSelection = it.copy(limitMessageId = null)
                }
            }
        }
        updateState()
    }

    private fun updateState() {
        val gestures = getGestures()
        val current = gestures.activeGesture
        val enabled = isViewAttached && current.isInteractionEnabled
        val shouldRequest = enabled && (!_uiState.value.isInteractionEnabled || deviceAddress != current.deviceAddress)
        pendingRemoval = pendingRemoval?.takeIf {
            enabled && isRotationGroupVisible && it.deviceAddress == current.deviceAddress &&
                it.gestureIds == gestures.rotationGroupGestureIds
        }
        pendingSelection = pendingSelection?.takeIf {
            enabled && isRotationGroupVisible && gestures.isRotationGroupAvailable && it.deviceAddress == current.deviceAddress &&
                it.selection.originalGestureIds == gestures.rotationGroupGestureIds
        }
        deviceAddress = current.deviceAddress
        _uiState.value = V3GesturesUiState(
            activeGestureId = current.gestureId.takeIf { enabled },
            isInteractionEnabled = enabled,
            rotationGroupGestureIds = if (isViewAttached) gestures.rotationGroupGestureIds else emptyList(),
            rotationGestureRemoval = pendingRemoval?.let {
                V3RotationGestureRemovalUiState(it.requestId, it.gestureIds[it.position])
            },
            rotationGroupSelection = pendingSelection?.let {
                V3RotationGroupSelectionUiState(it.requestId, it.selection.availableGestureIds,
                    it.selection.selectedGestureIds, it.limitMessageId)
            },
        )
        if (shouldRequest) requestActiveGesture(deviceAddress)
        if (!enabled || !isRotationGroupVisible) {
            cancelRotationGroupRequest()
        } else if (requestedRotationGroupAddress != deviceAddress) {
            cancelRotationGroupRequest()
            val address = deviceAddress
            requestedRotationGroupAddress = address
            rotationGroupRequestJob = viewModelScope.launch { loadRotationGroup(address) }
        }
    }

    private fun cancelRotationGroupRequest() {
        rotationGroupRequestJob?.cancel()
        rotationGroupRequestJob = null
        requestedRotationGroupAddress = null
    }

    override fun onCleared() {
        isViewAttached = false
        pendingRemoval = null
        pendingSelection = null
        cancelRotationGroupRequest()
        _uiState.value = V3GesturesUiState()
        super.onCleared()
    }
}
