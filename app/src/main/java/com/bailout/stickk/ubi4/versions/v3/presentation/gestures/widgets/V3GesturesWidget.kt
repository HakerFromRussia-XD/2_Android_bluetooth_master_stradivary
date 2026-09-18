package com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets

import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

/** Detached description for the existing gesture delegate; current values remain in screen state. */
data class V3GesturesWidget(
    val title: String,
    val label: V3GesturesWidgetLabel,
    val widgetType: Int,
    val widgetLabelType: Int,
    val widgetCode: Int,
    val display: Int,
    val widgetPosition: Int,
    val deviceId: Int,
    val widgetId: Int,
    val dataOffset: Int,
    val dataSize: Int,
    val channelOffset: Int,
    val parameters: List<ParameterInfo<Int, Int, Int, Int>>,
    val keyMobileSettings: String,
)

sealed interface V3GesturesWidgetLabel {
    data class Text(val value: String) : V3GesturesWidgetLabel
    data class Code(val value: Int) : V3GesturesWidgetLabel
}
