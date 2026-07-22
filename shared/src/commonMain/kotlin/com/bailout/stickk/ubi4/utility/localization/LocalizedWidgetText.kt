package com.bailout.stickk.ubi4.utility.localization

import com.bailout.stickk.ubi4.utility.logging.systemLang

object LocalizedWidgetText {
    private val isRussian: Boolean
        get() = systemLang().startsWith("ru", ignoreCase = true)

    private fun text(en: String, ru: String): String = if (isRussian) ru else en

    val graphs: String get() = text("Graphs", "Графики")
    val openingSensorSensitivity: String get() = text("Opening sensor sensitivity", "Чувствительность датчика открытия")
    val closingSensorSensitivity: String get() = text("Closing sensor sensitivity", "Чувствительность датчика закрытия")
    val open: String get() = text("Open", "Открыть")
    val close: String get() = text("Close", "Закрыть")
    val gestures: String get() = text("Gestures", "Жесты")
    val secondsShort: String get() = text("sec", "сек")
    val gestureSwitchingBySensors: String get() = text("Gesture switching by sensors", "Переключение жестов сенсорами")
    val emgMovementLock: String get() = text("EMG movement lock", "Блокировка движения с ЕМГ")
    val screenTimeout: String get() = text("Screen timeout", "Время работы экрана")
    val maximumSensorSensitivity: String get() = text("Maximum sensor sensitivity", "Максимальная чувствительность датчиков")
    val forceSetting: String get() = text("Force setting", "Настройка силы")
    val speedSetting: String get() = text("Speed setting", "Настройка скорости")
    val prosthesisOperatingMode: String get() = text("Prosthesis operating mode", "Режим работы протеза")
    val normalMode: String get() = text("Normal", "Нормальный")
    val sportMode: String get() = text("Sport", "Спортивный")
    val smoothForceControl: String get() = text("Smooth force control", "Плавное управление силой")
    val smoothSpeedControl: String get() = text("Smooth speed control", "Плавное управление скоростью")
    val smoothForceAndSpeedControl: String get() = text("Smooth force and speed control", "Плавное управление силой и скоростью")
    val noAction: String get() = text("No action", "Без действия")
    val goToOpenPosition: String get() = text("Go to open position", "Перейти в открытое положение")
    val gestureChangeAction: String get() = text("Action on gesture change", "Действие при смене жеста")
    val emgOperatingMode: String get() = text("EMG operating mode", "Режим работы ЕМГ")
    val firstStart: String get() = text("First start", "Первый старт")
    val left: String get() = text("Left", "Левая")
    val right: String get() = text("Right", "Правая")
    val handSide: String get() = text("Hand side", "Сторона руки")
    val prosthesisName: String get() = text("Prosthesis name", "Имя протеза")
    val write: String get() = text("Write", "Записать")
    val serialNumber: String get() = text("Serial number", "Серийный номер")
    val prosthesisCalibration: String get() = text("Prosthesis calibration", "Калибровка протеза")
    val send: String get() = text("Send", "Отправить")
    val autoLogin: String get() = text("Auto login", "Автоматический вход")

    fun handControlModes(): List<String> = listOf(
        normalMode,
        sportMode,
        smoothForceControl,
        smoothSpeedControl,
        smoothForceAndSpeedControl
    )

    fun gestureChangeActions(): List<String> = listOf(noAction, goToOpenPosition)
    fun emgControlModes(): List<String> = listOf("EMG 4.0", "EMG 3.0", firstStart)
    fun handSides(): List<String> = listOf(left, right)
}
