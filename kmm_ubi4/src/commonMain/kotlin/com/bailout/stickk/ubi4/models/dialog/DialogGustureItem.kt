package com.bailout.stickk.ubi4.models.dialog

import com.bailout.stickk.ubi4.data.local.Gesture


data class DialogCollectionGestureItem(
    val gesture: Gesture,
    var check: Boolean = false
)

// Для оптических жестов
data class SprDialogCollectionGestureItem(
    val gesture: SprGestureItem,
    var check: Boolean = false
)

data class SprGestureItem(
    val sprGestureId: Int = 0,
    val title: String = "not set",
    val animationId: Int = 0,
    var check: Boolean = false,
    val keyNameGesture: String = "key"
)