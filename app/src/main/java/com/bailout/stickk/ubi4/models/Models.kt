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
data class SprGestureItem(val title: String, val image: Int, var check: Boolean)
data class BindingGestureItem(
    val position: Int,
    var nameOfUserGesture: String,
    val sprGestureItem: SprGestureItem
)


// 3D конфигуратор и передача информации о жесте
data class ParameterRef (
    val deviceAddress: Int, val parameterID: Int
)
data class GestureInfo (
    val deviceAddress: Int, val parameterID: Int, val gestureID: Int
)
data class GestureWithAddress (
    val deviceAddress: Int, val parameterID: Int,
    val gesture: Gesture, val gestureState: Int
)
//Smarakov
// Кнопки калибровки пальцев
data class CalibrationButtonsItem(
    val title: String,
    val widget: Any,
    val buttons: List<CalibrationButton>
)
// Модель для отдельной кнопки калибровки
data class CalibrationButton(
    val id: Int,
    val buttonTitle: String,
    val calibrationStatus: CalibrationStatus,
    val connectionStatus: ConnectionStatus,
    val encoderSteps: Int,
    val currentValue: Int
)

enum class CalibrationStatus {
    CALIBRATED,
    NOT_CALIBRATED,
    ERROR
}

enum class ConnectionStatus {
    CONNECTED,
    NOT_CONNECTED,
    ERROR
}