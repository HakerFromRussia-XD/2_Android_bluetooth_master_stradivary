package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

/** Screen composition only. Values and actions are owned by the Sensors screen state. */
sealed interface V3SensorsWidget {
    val title: String
    val info: V3SensorsWidgetInfo

    data class Plot(
        override val title: String,
        override val info: V3SensorsWidgetInfo,
        val color: Int,
        val maxSize: Int,
        val minSize: Int,
        val openThreshold: Int,
        val closeThreshold: Int,
        val openThresholdUpper: Int,
        val openThresholdLower: Int,
        val closeThresholdUpper: Int,
        val closeThresholdLower: Int,
    ) : V3SensorsWidget

    data class Slider(
        override val title: String,
        override val info: V3SensorsWidgetInfo,
        val parameterKey: String,
        val minProgress: Int,
        val maxProgress: Int,
        val increment: Float,
    ) : V3SensorsWidget

    data class Buttons(
        override val title: String,
        override val info: V3SensorsWidgetInfo,
        val title2: String,
        val title3: String,
        val description: String,
        val clickCommand: Int,
        val pressedCommand: Int,
        val releasedCommand: Int,
        val movements: Set<V3ProsthesisMovement>,
    ) : V3SensorsWidget
}

/** A detached value snapshot of the metadata needed by the existing adapters. */
data class V3SensorsWidgetInfo(
    val label: String,
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
