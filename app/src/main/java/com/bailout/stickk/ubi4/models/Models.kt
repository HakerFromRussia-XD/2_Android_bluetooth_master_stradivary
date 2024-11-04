package com.bailout.stickk.ubi4.models

import com.bailout.stickk.ubi4.data.local.Gesture

//widgets items
data class GesturesItem(val title: String, val widget: Any)
data class OneButtonItem(val title: String, val description: String, val widget: Any)
data class PlotItem(val title: String, val widget: Any)

// dialogs
data class DialogGestureItem(val title: String, val check: Boolean)

// 3D конфигуратор и передача информации о группе ротации
data class RotationGroup (
    val gesture1Id: Int, val gesture1ImageId: Int,
    val gesture2Id: Int, val gesture2ImageId: Int,
    val gesture3Id: Int, val gesture3ImageId: Int,
    val gesture4Id: Int, val gesture4ImageId: Int,
    val gesture5Id: Int, val gesture5ImageId: Int,
    val gesture6Id: Int, val gesture6ImageId: Int,
    val gesture7Id: Int, val gesture7ImageId: Int,
    val gesture8Id: Int, val gesture8ImageId: Int,
)
data class GestureInfo (
    val deviceAddress: Int, val parameterID: Int, val gestureID: Int
)
data class GestureWithAddress (
    val deviceAddress: Int, val parameterID: Int,
    val gesture: Gesture, val gestureState: Int
)

