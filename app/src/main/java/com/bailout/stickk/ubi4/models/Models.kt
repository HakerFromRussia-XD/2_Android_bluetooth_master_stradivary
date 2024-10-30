package com.bailout.stickk.ubi4.models


data class DialogGestureItem(val title: String, val check: Boolean)
data class GesturesItem(val title: String, val widget: Any)
data class OneButtonItem(val title: String, val description: String, val widget: Any)
data class PlotItem(val title: String, val widget: Any)

//
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

data class GestureWithAddress (
    val deviceAddress: Int, val parameterID: Int,
    val gesture: Gesture
)
data class Gesture (
    val gestureId: Int,
    val openPosition1: Int, val openPosition2: Int,
    val openPosition3: Int, val openPosition4: Int,
    val openPosition5: Int, val openPosition6: Int,
    val closePosition1: Int, val closePosition2: Int,
    val closePosition3: Int, val closePosition4: Int,
    val closePosition5: Int, val closePosition6: Int,
    val openToCloseTimeShift1: Int, val openToCloseTimeShift2: Int,
    val openToCloseTimeShift3: Int, val openToCloseTimeShift4: Int,
    val openToCloseTimeShift5: Int, val openToCloseTimeShift6: Int,
    val closeToOpenTimeShift1: Int, val closeToOpenTimeShift2: Int,
    val closeToOpenTimeShift3: Int, val closeToOpenTimeShift4: Int,
    val closeToOpenTimeShift5: Int, val closeToOpenTimeShift6: Int
)

