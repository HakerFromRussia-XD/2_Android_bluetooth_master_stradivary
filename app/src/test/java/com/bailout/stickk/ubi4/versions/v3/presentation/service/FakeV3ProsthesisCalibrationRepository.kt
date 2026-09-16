package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3ProsthesisCalibrationRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeV3ProsthesisCalibrationRepository : V3ProsthesisCalibrationRepository {
    override val interactionEnabled = MutableStateFlow(true)
    var currentAddress = "first-device"
    val commands = mutableListOf<String>()
    override fun startCalibration(deviceAddress: String): Boolean {
        if (deviceAddress != currentAddress) return false
        commands += "start:$deviceAddress"
        return true
    }
    override fun releaseCalibrationButton(deviceAddress: String) {
        if (deviceAddress == currentAddress) commands += "release:$deviceAddress"
    }
}
