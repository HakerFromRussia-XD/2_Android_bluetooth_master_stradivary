package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

data class V3UserFirmwareUiState(
    val phase: String = "idle",
    val boardNumber: Int = 0,
    val boardCount: Int = 0,
    val progress: Int = 0,
    val isVisible: Boolean = false,
)
