package com.bailout.stickk.ubi4.versions.v3.presentation.gestures

sealed interface V3GesturesAction {
    data object ViewAttached : V3GesturesAction
    data object ViewDetached : V3GesturesAction
    data class GestureSelected(val gestureId: Int) : V3GesturesAction
    data class GestureSettingsRequested(val gestureId: Int) : V3GesturesAction
    data class GestureSettingsOpened(val requestId: Long) : V3GesturesAction
    data object CollectionSelected : V3GesturesAction
    data object RotationGroupSelected : V3GesturesAction
    data object FactoryCollectionToggled : V3GesturesAction
    data class RotationGestureMoved(
        val fromPosition: Int,
        val toPosition: Int,
        val gestureIdsBeforeDrag: List<Int>,
    ) : V3GesturesAction
    data class RotationGestureRemovalRequested(val position: Int, val gestureIds: List<Int>) : V3GesturesAction
    data class RotationGestureRemovalConfirmed(val requestId: Long) : V3GesturesAction
    data class RotationGestureRemovalCancelled(val requestId: Long) : V3GesturesAction
    data object RotationGroupSelectionRequested : V3GesturesAction
    data class RotationGroupGestureToggled(val requestId: Long, val gestureId: Int) : V3GesturesAction
    data class RotationGroupSelectionSaved(val requestId: Long) : V3GesturesAction
    data class RotationGroupSelectionCancelled(val requestId: Long) : V3GesturesAction
    data class RotationGroupLimitMessageShown(val requestId: Long, val messageId: Long) : V3GesturesAction
}
