package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.blelog.BleLogButtonItem
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys.P_KEY_SET_SERIAL_NUMBER

/** UI compatibility boundary; preserves widget types, labels, order and command metadata. */
class V3ServiceWidgetMapper {
    private val parameterKeys = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }

    fun fromItems(items: List<Any>): List<V3ServiceWidget> = items.map { item ->
        when (item) {
            is SliderItemV3 -> (item.widget as SliderParameterWidgetSStruct).let {
                V3ServiceWidget.Slider(item.title, info(it.baseParameterWidgetSStruct),
                    parameterKeys.getValue(it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.first()),
                    it.minProgress, it.maxProgress, it.increment)
            }
            is SpinnerItemV3 -> (item.widget as SpinnerParameterWidgetSStruct).let {
                V3ServiceWidget.Spinner(item.title, info(it.baseParameterWidgetSStruct),
                    parameterKeys.getValue(it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.first()),
                    it.dataSpinnerParameterWidgetStruct.spinnerItems.toList(), it.dataSpinnerParameterWidgetStruct.selectedIndex)
            }
            is TextInputItemV3 -> (item.widget as CommandParameterWidgetSStruct).let {
                V3ServiceWidget.TextInput(item.title, item.buttonTitle, info(it.baseParameterWidgetSStruct),
                    when (parameterKeys.getValue(it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.first())) {
                        P_KEY_SET_DEVICE_NAME -> V3DeviceInfoField.DEVICE_NAME
                        P_KEY_SET_SERIAL_NUMBER -> V3DeviceInfoField.SERIAL_NUMBER
                        else -> error("Unsupported Service text field")
                    },
                    it.clickCommand, it.pressedCommand, it.releasedCommand)
            }
            is ButtonsItemV3 -> (item.widget as CommandParameterWidgetSStruct).let {
                V3ServiceWidget.Buttons(item.title, item.title2, item.title3, item.description, info(it.baseParameterWidgetSStruct),
                    parameterKeys.getValue(it.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.first()),
                    it.clickCommand, it.pressedCommand, it.releasedCommand)
            }
            BleLogButtonItem -> V3ServiceWidget.BleLog
            else -> error("Unsupported V3 Service item: ${item::class.simpleName}")
        }
    }

    fun toItems(widgets: List<V3ServiceWidget>, roleOptions: List<String>? = null): List<Any> = widgets.map { widget ->
        when (widget) {
            is V3ServiceWidget.Slider -> SliderItemV3(widget.title, SliderParameterWidgetSStruct(
                base(widget.info), widget.minProgress, widget.maxProgress, widget.increment))
            is V3ServiceWidget.Spinner -> SpinnerItemV3(widget.title, SpinnerParameterWidgetSStruct(
                base(widget.info), DataSpinnerParameterWidgetStruct(
                    (roleOptions?.takeIf { widget.parameterKey == P_KEY_DEVICE_ROLE } ?: widget.options).toMutableList(),
                    widget.initialSelectedIndex)))
            is V3ServiceWidget.TextInput -> TextInputItemV3(widget.title, widget.buttonTitle, CommandParameterWidgetSStruct(
                base(widget.info), widget.clickCommand, widget.pressedCommand, widget.releasedCommand))
            is V3ServiceWidget.Buttons -> ButtonsItemV3(widget.title, widget.title2, widget.title3, widget.description,
                CommandParameterWidgetSStruct(base(widget.info), widget.clickCommand, widget.pressedCommand, widget.releasedCommand))
            V3ServiceWidget.BleLog -> BleLogButtonItem
        }
    }

    private fun info(widget: BaseParameterWidgetSStruct) = with(widget.baseParameterWidgetStruct) {
        V3ServiceWidgetInfo(widget.label, widgetType, widgetLabelType, widgetCode, display, widgetPosition,
            deviceId, widgetId, dataOffset, dataSize, channelOffset, parameterInfoSet.toList(), keyMobileSettings)
    }

    private fun base(info: V3ServiceWidgetInfo) = with(info) {
        BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetType, widgetLabelType, widgetCode, display,
            widgetPosition, deviceId, widgetId, dataOffset, dataSize, channelOffset, parameters.toMutableSet(), keyMobileSettings), label)
    }
}
