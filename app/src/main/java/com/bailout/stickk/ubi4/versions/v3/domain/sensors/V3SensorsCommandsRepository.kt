package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.StateFlow

enum class V3ProsthesisMovement { OPEN, CLOSE }

interface V3SensorsCommandsRepository {
    val interactionEnabled: StateFlow<Boolean>
    val refreshInProgress: StateFlow<Boolean>
    /** Commands are accepted only for the device that owns the screen. */
    fun startMovement(deviceAddress: String, movement: V3ProsthesisMovement): Boolean
    /** STOP bypasses the interaction lock, but must never target a different device. */
    fun stopMovement(deviceAddress: String)
    fun refreshSensors(deviceAddress: String): Boolean
}
