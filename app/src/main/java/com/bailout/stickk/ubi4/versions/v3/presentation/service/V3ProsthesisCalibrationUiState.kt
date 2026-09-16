package com.bailout.stickk.ubi4.versions.v3.presentation.service

/** State of the existing button, not the device's calibration progress. */
data class V3ProsthesisCalibrationUiState(
    val isEnabled: Boolean = false,
    val isPressed: Boolean = false,
)
