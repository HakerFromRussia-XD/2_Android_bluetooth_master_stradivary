package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

import kotlinx.coroutines.flow.Flow

/** Positions and delays are in device order: index, middle, ring, little, thumb axes. */
data class V3GestureSettings(
    val gestureId: Int = 0,
    val openPositions: List<Int> = List(6) { 0 },
    val closePositions: List<Int> = List(6) { 0 },
    val openToCloseDelays: List<Int> = List(6) { 0 },
    val closeToOpenDelays: List<Int> = List(6) { 0 },
)

enum class V3GestureCommand(val code: Int) {
    OPEN(0), CLOSE(1), OPEN_WITH_DELAY(128), CLOSE_WITH_DELAY(129), SAVE(255)
}

interface V3GestureEditorRepository {
    fun getHandSide(): Int
    // Null preserves the existing malformed-response notification.
    fun observeSettings(): Flow<V3GestureSettings?>
    suspend fun awaitReady()
    fun requestSettings(gestureId: Int)
    fun writeSettings(settings: V3GestureSettings, command: V3GestureCommand, name: String)
}
