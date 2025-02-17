package com.bailout.stickk.ubi4.models

import com.bailout.stickk.ubi4.data.local.Gesture

//widgets items
data class GesturesItem(val title: String, val widget: Any)
data class OneButtonItem(val title: String, val description: String, val widget: Any)
data class PlotItem(val title: String, val widget: Any)
data class SliderItem(val title: String, val widget: Any)
data class SwitchItem(val title: String, val widget: Any)
data class TrainingGestureItem(val title: String, val widget: Any)

// dialogs
data class DialogCollectionGestureItem(val gesture: Gesture, var check: Boolean = false)
data class SprGestureItem(val sprGestureId:Int = 0, val title: String = "not set", val animationId: Int = 0, var check: Boolean = false, val keyNameGesture: String = "key")
data class SprDialogCollectionGestureItem(val gesture: SprGestureItem, var check: Boolean = false)
data class BindingGestureItem(
    val position: Int,
    var nameOfUserGesture: String,
    val sprGestureItem: SprGestureItem
)

// приём и передача данных в потоках ble
data class ParameterRef (
    val addressDevice: Int, val parameterID: Int
)
data class PlotParameterRef (
    val addressDevice: Int, val parameterID: Int, val dataPlots: ArrayList<Int>
)
// 3D конфигуратор и передача информации о жесте
data class GestureInfo (
    val deviceAddress: Int, val parameterID: Int, val gestureID: Int
)
data class GestureWithAddress (
    val addressDevice: Int, val parameterID: Int,
    val gesture: Gesture, val gestureState: Int
)

data class ParameterInfo<A, B, C, D>(
    val parameterID: A,
    val dataCode: B,
    val deviceAddress: C,
    val dataOffset: D
)
