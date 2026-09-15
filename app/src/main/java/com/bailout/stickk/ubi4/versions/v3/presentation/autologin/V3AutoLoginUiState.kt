package com.bailout.stickk.ubi4.versions.v3.presentation.autologin

data class V3AutoLoginUiState(
    val isChecked: Boolean = false,
    val isEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val readFailed: Boolean = false,
    val saveFailed: Boolean = false,
)
