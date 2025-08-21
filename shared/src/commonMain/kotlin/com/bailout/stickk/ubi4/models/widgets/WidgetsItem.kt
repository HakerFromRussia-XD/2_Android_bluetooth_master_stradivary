package com.bailout.stickk.ubi4.models.widgets

import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct

// Объявляем ожидаемый тип для представления файлов в KMM
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformFile(path: String) {
    val path: String
    fun delete(): Boolean
    val name: String
    fun readBytes(): ByteArray
}

// Widgets items
data class GesturesItem(
    val title: String,
    val widget: Any
)

data class OneButtonItem(
    val title: String,
    val description: String,
    val widget: Any
)

data class PlotItem(
    val title: String,
    val widget: Any
)

data class SliderItem(
    val title: String,
    val widget: Any
)

data class SwitchItem(
    val title: String,
    val widget: Any
)

data class TrainingGestureItem(
    val title: String,
    val widget: Any
)

data class SpinnerItem(
    val title: String,
    val widget: Any
)

// Файл (checkpoint) для оптических тренировок / сохранений
data class FileItem(
    val name: String,
    val file: PlatformFile,
    val number: Int,
    val timestamp: String = ""
)