package com.bailout.stickk.ubi4.versions.v3.presentation.gestures

import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets.V3GesturesWidget
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3CustomGestureNames

data class V3GesturesUiState(
    val activeGestureId: Int? = null,
    val isInteractionEnabled: Boolean = false,
    val rotationGroupGestureIds: List<Int> = emptyList(),
    val rotationGestureRemoval: V3RotationGestureRemovalUiState? = null,
    val rotationGroupSelection: V3RotationGroupSelectionUiState? = null,
    val selectedSection: Int = 1,
    val isFactoryCollectionExpanded: Boolean = true,
    val gestureSettings: V3GestureSettingsUiState? = null,
    val widgets: List<V3GesturesWidget> = emptyList(),
    val widgetsUpdateId: Long = 0,
    val customGestureNames: V3CustomGestureNames = V3CustomGestureNames(),
)

data class V3GestureSettingsUiState(val requestId: Long, val gestureId: Int)

data class V3RotationGestureRemovalUiState(val requestId: Long, val gestureId: Int)

data class V3RotationGroupSelectionUiState(
    val requestId: Long,
    val availableGestureIds: List<Int>,
    val selectedGestureIds: Set<Int>,
    val limitMessageId: Long? = null,
)
