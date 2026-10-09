package com.bailout.stickk.ubi4.versions.v3.domain.gestures

import kotlinx.coroutines.flow.Flow

data class V3ActiveGesture(
    val deviceAddress: String,
    val gestureId: Int?,
    val isInteractionEnabled: Boolean,
)

interface V3GesturesRepository {
    val updates: Flow<Unit>
    val rotationGroupUpdates: Flow<Unit>
    fun getActiveGesture(): V3ActiveGesture
    /** Current gesture IDs; native snapshot observation retains raw events without an initial read. */
    fun observeActiveGesture(): Flow<Int?>
    /** Native rotation snapshots retain repeated raw events and empty groups without an initial read. */
    fun observeRotationGroup(): Flow<List<Int>?>
    fun getRotationGroupGestureIds(): List<Int>?
    fun requestActiveGesture(deviceAddress: String): Boolean
    fun requestRotationGroup(deviceAddress: String): Boolean
    fun setRotationGroup(deviceAddress: String, gestureIds: List<Int>): Boolean
    fun selectGesture(deviceAddress: String, gestureId: Int): Boolean
}
