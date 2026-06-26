package com.bailout.stickk.ubi4.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryMessagesRequest(
    val messages: List<TelemetryMessage>
)

@Serializable
data class TelemetryMessage(
    val deviceId: String,
    val occurred: Long,
    val data: TelemetryMessageData
)

@Serializable
data class TelemetryMessageData(
    val version: String,
    @SerialName("gesture_movement_count")
    val gestureMovementCount: Map<String, Long>,
    @SerialName("user_gesture_movement_count")
    val userGestureMovementCount: Map<String, Long>,
    val grips: Long
)
