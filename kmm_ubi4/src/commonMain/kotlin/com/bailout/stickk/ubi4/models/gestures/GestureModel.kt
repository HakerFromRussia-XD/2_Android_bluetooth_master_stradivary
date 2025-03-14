package com.bailout.stickk.ubi4.models.gestures

import com.bailout.stickk.ubi4.data.local.Gesture


data class GestureInfo(
    val deviceAddress: Int,
    val parameterID: Int,
    val gestureID: Int
)

data class GestureWithAddress(
    val addressDevice: Int,
    val parameterID: Int,
    val gesture: Gesture,
    val gestureState: Int
)

// Конфигурация жеста: для анимированного 3D-конфигуратора
data class GestureConfig(
    val baselineDuration: Double,
    val preGestDuration: Double,
    val atGestDuration: Double,
    val postGestDuration: Double,
    val gestureSequence: List<String>,
    //val gesturesId: Map<String, Int> // если понадобится
)

// Детали фаз жеста
data class GesturePhase(
    var prePhase: Double = 0.0,
    var timeGesture: Double = 0.0,
    var postPhase: Double = 0.0,
    var animation: Int = 0,
    var headerText: String = "",
    var description: String = "",
    var gestureName: String = "",
    var gestureId: Int = 0
)
