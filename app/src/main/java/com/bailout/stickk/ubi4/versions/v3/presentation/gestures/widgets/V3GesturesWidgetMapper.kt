package com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.GesturesItemV3

/** UI compatibility boundary. Preserve order and metadata without sharing mutable widget structures. */
class V3GesturesWidgetMapper {
    fun fromItems(items: List<Any>): List<V3GesturesWidget> = items.map { item ->
        require(item is GesturesItemV3) { "Unsupported V3 Gestures item: ${item::class.simpleName}" }
        val (base, label) = when (val widget = item.widget) {
            is BaseParameterWidgetSStruct -> widget.baseParameterWidgetStruct to V3GesturesWidgetLabel.Text(widget.label)
            is BaseParameterWidgetEStruct -> widget.baseParameterWidgetStruct to V3GesturesWidgetLabel.Code(widget.labelCode)
            else -> error("Unsupported V3 Gestures widget: ${widget::class.simpleName}")
        }
        with(base) {
            V3GesturesWidget(item.title, label, widgetType, widgetLabelType, widgetCode, display, widgetPosition,
                deviceId, widgetId, dataOffset, dataSize, channelOffset, parameterInfoSet.toList(), keyMobileSettings)
        }
    }

    fun toItems(widgets: List<V3GesturesWidget>): List<GesturesItemV3> = widgets.map { widget ->
        with(widget) {
            val base = BaseParameterWidgetStruct(widgetType, widgetLabelType, widgetCode, display, widgetPosition,
                deviceId, widgetId, dataOffset, dataSize, channelOffset, parameters.toMutableSet(), keyMobileSettings)
            val description = when (label) {
                is V3GesturesWidgetLabel.Text -> BaseParameterWidgetSStruct(base, label.value)
                is V3GesturesWidgetLabel.Code -> BaseParameterWidgetEStruct(base, label.value)
            }
            GesturesItemV3(title, description)
        }
    }
}
