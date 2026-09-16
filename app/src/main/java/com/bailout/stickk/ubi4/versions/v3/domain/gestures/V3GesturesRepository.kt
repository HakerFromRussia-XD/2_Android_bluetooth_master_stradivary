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
    fun getRotationGroupGestureIds(): List<Int>?
    fun requestActiveGesture(deviceAddress: String): Boolean
    fun requestRotationGroup(deviceAddress: String): Boolean
    fun setRotationGroup(deviceAddress: String, gestureIds: List<Int>): Boolean
    fun selectGesture(deviceAddress: String, gestureId: Int): Boolean
}
