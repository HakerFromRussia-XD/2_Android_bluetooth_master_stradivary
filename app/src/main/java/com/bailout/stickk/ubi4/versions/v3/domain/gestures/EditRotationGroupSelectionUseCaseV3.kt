package com.bailout.stickk.ubi4.versions.v3.domain.gestures

data class V3RotationGroupSelectionEditResult(
    val selection: V3RotationGroupSelection,
    val isLimitReached: Boolean = false,
)

class EditRotationGroupSelectionUseCaseV3 {
    operator fun invoke(selection: V3RotationGroupSelection, gestureId: Int): V3RotationGroupSelectionEditResult {
        if (gestureId !in selection.availableGestureIds) return V3RotationGroupSelectionEditResult(selection)
        val selected = if (gestureId in selection.selectedGestureIds) selection.selectedGestureIds - gestureId
            else selection.selectedGestureIds + gestureId
        val updated = selection.copy(selectedGestureIds = selected)
        return if (updated.isValid()) V3RotationGroupSelectionEditResult(updated)
            else V3RotationGroupSelectionEditResult(selection, isLimitReached = true)
    }
}
