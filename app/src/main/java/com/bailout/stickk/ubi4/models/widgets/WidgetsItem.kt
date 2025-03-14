package com.bailout.stickk.ubi4.models.widgets

import java.io.File

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
    val file: File,
    val number: Int,
    val timestamp: String = ""
)