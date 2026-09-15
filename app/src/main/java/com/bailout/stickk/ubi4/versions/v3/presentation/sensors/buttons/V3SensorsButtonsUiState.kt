package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement

data class V3SensorsButtonsUiState(
    val availableMovements: Set<V3ProsthesisMovement> = emptySet(),
    val pressedMovements: Set<V3ProsthesisMovement> = emptySet(),
    val isEnabled: Boolean = false,
)
