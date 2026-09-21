package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

import kotlinx.coroutines.flow.Flow

/** Positions and delays are in device order: index, middle, ring, little, thumb axes. */
data class V3GestureSettings(
    val gestureId: Int,
    val openPositions: List<Int>,
    val closePositions: List<Int>,
    val openToCloseDelays: List<Int>,
    val closeToOpenDelays: List<Int>,
)

interface V3GestureEditorRepository {
    // Null preserves the existing malformed-response notification.
    fun observeSettings(): Flow<V3GestureSettings?>
    suspend fun awaitReady()
    fun requestSettings(gestureId: Int)
}
