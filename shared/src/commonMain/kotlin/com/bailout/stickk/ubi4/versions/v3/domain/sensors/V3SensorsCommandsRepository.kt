package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.StateFlow

enum class V3ProsthesisMovement { OPEN, CLOSE }

interface V3SensorsCommandsRepository {
    val interactionEnabled: StateFlow<Boolean>
    val refreshInProgress: StateFlow<Boolean>
    /** Returns whether the command was queued under the repository's device-context policy. */
    fun startMovement(deviceAddress: String, movement: V3ProsthesisMovement): Boolean
    /** STOP bypasses the interaction lock; the repository retains its device-context policy. */
    fun stopMovement(deviceAddress: String)
    fun refreshSensors(deviceAddress: String): Boolean
}
