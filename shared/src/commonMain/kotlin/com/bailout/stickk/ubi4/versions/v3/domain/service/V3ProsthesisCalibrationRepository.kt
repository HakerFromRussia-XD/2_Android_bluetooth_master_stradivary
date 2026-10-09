package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

interface V3ProsthesisCalibrationRepository {
    val interactionEnabled: StateFlow<Boolean>
    fun startCalibration(deviceAddress: String): Boolean
    /** Queue the button release under the repository's device-context policy, without an interaction lock. */
    fun releaseCalibrationButton(deviceAddress: String)
}
