package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.StateFlow

interface V3ProsthesisCalibrationRepository {
    val interactionEnabled: StateFlow<Boolean>
    fun startCalibration(deviceAddress: String): Boolean
    /** Finish the accepted UI press without sending a device command, including while locked. */
    fun releaseCalibrationButton(deviceAddress: String)
}
