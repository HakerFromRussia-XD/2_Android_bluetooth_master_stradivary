package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.models.widgets.PlotItemV3
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry

/** Compatibility boundary for DataFactory and the existing V3 delegates. No device operations. */
class V3SensorsWidgetMapper {
    private val parameterKeys = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }

    fun fromItems(items: List<Any>): List<V3SensorsWidget> = items.map { item ->
        when (item) {
            is PlotItemV3 -> (item.widget as PlotParameterWidgetSStruct).let {
                V3SensorsWidget.Plot(
                    item.title, info(it.baseParameterWidgetSStruct), it.color, it.maxSize, it.minSize,
                    it.openThreshold, it.closeThreshold, it.openThresholdUpper, it.openThresholdLower,
                    it.closeThresholdUpper, it.closeThresholdLower,
                )
            }
            is SliderItemV3 -> (item.widget as SliderParameterWidgetSStruct).let {
                V3SensorsWidget.Slider(
                    item.title, info(it.baseParameterWidgetSStruct),
                    parameterKeys.getValue(it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.first()),
                    it.minProgress, it.maxProgress, it.increment,
                )
            }
            is ButtonsItemV3 -> (item.widget as CommandParameterWidgetSStruct).let {
                V3SensorsWidget.Buttons(
                    item.title, info(it.baseParameterWidgetSStruct), item.title2, item.title3, item.description,
                    it.clickCommand, it.pressedCommand, it.releasedCommand,
                    it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.mapNotNull { parameter ->
                        when {
                            parameter.dataOffsets == 0 && parameter.dataCode == ProsthesisModuleControlEnum.PMCE_OPEN_COMMAND.number.toInt() -> V3ProsthesisMovement.OPEN
                            parameter.dataOffsets == 1 && parameter.dataCode == ProsthesisModuleControlEnum.PMCE_CLOSE_COMMAND.number.toInt() -> V3ProsthesisMovement.CLOSE
                            else -> null
                        }
                    }.toSet(),
                )
            }
            else -> error("Unsupported V3 Sensors item: ${item::class.simpleName}")
        }
    }

    fun toItems(widgets: List<V3SensorsWidget>): List<Any> = widgets.map { widget ->
        when (widget) {
            is V3SensorsWidget.Plot -> PlotItemV3(widget.title, PlotParameterWidgetSStruct(
                base(widget.info), widget.color, widget.maxSize, widget.minSize,
                widget.openThreshold, widget.closeThreshold, widget.openThresholdUpper, widget.openThresholdLower,
                widget.closeThresholdUpper, widget.closeThresholdLower,
            ))
            is V3SensorsWidget.Slider -> SliderItemV3(widget.title, SliderParameterWidgetSStruct(
                base(widget.info), widget.minProgress, widget.maxProgress, widget.increment,
            ))
            is V3SensorsWidget.Buttons -> ButtonsItemV3(widget.title, widget.title2, widget.title3, widget.description,
                CommandParameterWidgetSStruct(base(widget.info), widget.clickCommand, widget.pressedCommand, widget.releasedCommand),
            )
        }
    }

    private fun info(widget: BaseParameterWidgetSStruct): V3SensorsWidgetInfo = with(widget.baseParameterWidgetStruct) {
        V3SensorsWidgetInfo(
            widget.label, widgetType, widgetLabelType, widgetCode, display, widgetPosition, deviceId, widgetId,
            dataOffset, dataSize, channelOffset, parameterInfoSet.toList(), keyMobileSettings,
        )
    }

    private fun base(info: V3SensorsWidgetInfo) = with(info) {
        BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            widgetType, widgetLabelType, widgetCode, display, widgetPosition, deviceId, widgetId,
            dataOffset, dataSize, channelOffset, parameters.toMutableSet(), keyMobileSettings,
        ), label)
    }
}
