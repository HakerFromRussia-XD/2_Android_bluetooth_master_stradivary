package com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommandPacket

data class V3InitializationCommandUiState(
    val step: Int,
    val packet: V3InitializationCommandPacket,
    val isEnabled: Boolean,
    val isLastQueued: Boolean = false,
)

data class V3InitializationCommandsUiState(val commands: List<V3InitializationCommandUiState> = emptyList())
