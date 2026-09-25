package com.bailout.stickk.ubi4.versions.v3.presentation.blelog

import com.bailout.stickk.ubi4.versions.v3.domain.blelog.V3BleLogEntry

data class V3BleLogUiState(
    val hideGraphStream: Boolean = true,
    val entries: List<V3BleLogEntry> = emptyList(),
)

sealed interface V3BleLogAction {
    data object ViewCreated : V3BleLogAction
    data object ViewStarted : V3BleLogAction
    data object ViewStopped : V3BleLogAction
    data object ViewDestroyed : V3BleLogAction
    data class GraphStreamFilterChanged(val hidden: Boolean) : V3BleLogAction
}
