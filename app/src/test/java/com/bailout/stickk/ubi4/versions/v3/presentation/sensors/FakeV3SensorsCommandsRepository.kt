package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsCommandsRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeV3SensorsCommandsRepository : V3SensorsCommandsRepository {
    override val interactionEnabled = MutableStateFlow(true)
    override val refreshInProgress = MutableStateFlow(false)
    var currentAddress = "first-device"
    val events = mutableListOf<String>()
    override fun startMovement(deviceAddress: String, movement: V3ProsthesisMovement): Boolean {
        if (deviceAddress != currentAddress || !interactionEnabled.value) return false
        events += "$deviceAddress:$movement"
        return true
    }
    override fun stopMovement(deviceAddress: String) {
        if (deviceAddress == currentAddress) events += "$deviceAddress:STOP"
    }
    override fun refreshSensors(deviceAddress: String): Boolean {
        if (deviceAddress != currentAddress || refreshInProgress.value) return false
        refreshInProgress.value = true
        events += "$deviceAddress:REFRESH"
        return true
    }
}
