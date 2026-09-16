package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

interface V3ProsthesisCalibrationRepository {
    val interactionEnabled: StateFlow<Boolean>
    fun startCalibration(deviceAddress: String): Boolean
    /** Release the accepted button press even when interaction becomes locked. */
    fun releaseCalibrationButton(deviceAddress: String)
}
