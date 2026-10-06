package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareUpdateResult

data class V3ServiceFirmwareUiState(val progressByRequest: Map<Long, Int> = emptyMap())

sealed interface V3ServiceFirmwareAction {
    data object ViewCreated : V3ServiceFirmwareAction
    data object ViewDestroyed : V3ServiceFirmwareAction
    data class InstallConfirmed(val requestId: Long, val boardAddress: Int, val file: V3ServiceFirmwareLocalFile) : V3ServiceFirmwareAction
    data class DebugInstallRequested(val file: V3ServiceFirmwareLocalFile) : V3ServiceFirmwareAction
    data class ResultShown(val requestId: Long) : V3ServiceFirmwareAction
}

sealed interface V3ServiceFirmwareEffect {
    val requestId: Long
    data class Stalled(override val requestId: Long, val phase: String, val progress: Int, val startedAt: Long) : V3ServiceFirmwareEffect
    data class Completed(override val requestId: Long, val result: V3ServiceFirmwareUpdateResult) : V3ServiceFirmwareEffect
    data class Failed(override val requestId: Long, val error: Exception) : V3ServiceFirmwareEffect
}
