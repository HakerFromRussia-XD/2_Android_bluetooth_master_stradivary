package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement

sealed interface V3SensorsButtonsAction {
    data class ButtonPressed(val movement: V3ProsthesisMovement, val pressId: Long) : V3SensorsButtonsAction
    /** Release, cancel and disposal of the same UI gesture share its identity. */
    data class ButtonReleased(val pressId: Long) : V3SensorsButtonsAction
}
