package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField

/** Detached composition. Current values of screen-owned controls live in UiState. */
sealed interface V3ServiceWidget {
    data class Slider(
        val title: String,
        val info: V3ServiceWidgetInfo,
        val parameterKey: String,
        val minProgress: Int,
        val maxProgress: Int,
        val increment: Float,
    ) : V3ServiceWidget

    data class Spinner(
        val title: String,
        val info: V3ServiceWidgetInfo,
        val parameterKey: String,
        val options: List<String>,
        val initialSelectedIndex: Int,
    ) : V3ServiceWidget

    data class TextInput(
        val title: String,
        val buttonTitle: String,
        val info: V3ServiceWidgetInfo,
        val field: V3DeviceInfoField,
        val clickCommand: Int,
        val pressedCommand: Int,
        val releasedCommand: Int,
    ) : V3ServiceWidget

    data class Buttons(
        val title: String,
        val title2: String,
        val title3: String,
        val description: String,
        val info: V3ServiceWidgetInfo,
        val clickCommand: Int,
        val pressedCommand: Int,
        val releasedCommand: Int,
    ) : V3ServiceWidget

    data object BleLog : V3ServiceWidget
}

/** Preserve the shared metadata until its existing Android delegates are replaced. */
data class V3ServiceWidgetInfo(
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
