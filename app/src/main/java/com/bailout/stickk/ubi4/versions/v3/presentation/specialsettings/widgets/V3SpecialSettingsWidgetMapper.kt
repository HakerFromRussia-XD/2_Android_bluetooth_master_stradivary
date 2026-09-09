package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.models.widgets.SwitchItem
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE

/** Compatibility boundary for DataFactory and the existing delegate adapters. */
class V3SpecialSettingsWidgetMapper {
    private val parameterKeys = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }

    fun fromItems(items: List<Any>): List<V3SpecialSettingsWidget> = items.map { item ->
        when (item) {
            is SliderItemV3 -> (item.widget as SliderParameterWidgetSStruct).let {
                V3SpecialSettingsWidget.Slider(info(item.title, it.baseParameterWidgetSStruct), it.minProgress, it.maxProgress, it.increment)
            }
            is ToggleSliderItemV3 -> (item.widget as ToggleSliderParameterWidgetSStruct).let {
                V3SpecialSettingsWidget.ToggleSlider(info(item.title, it.baseParameterWidgetSStruct), it.minProgress, it.maxProgress, it.increment, it.unitLabel)
            }
            is SpinnerItemV3 -> (item.widget as SpinnerParameterWidgetSStruct).let {
                val info = info(item.title, it.baseParameterWidgetSStruct)
                val data = it.dataSpinnerParameterWidgetStruct
                if (info.key == P_KEY_SETTINGS_PROFILE) {
                    V3SpecialSettingsWidget.SettingsProfile(info, data.spinnerItems.toList(), data.selectedIndex)
                } else {
                    V3SpecialSettingsWidget.Spinner(info, data.spinnerItems.toList(), data.selectedIndex)
                }
            }
            is SwitchItem -> (item.widget as SwitchParameterWidgetSStruct).let {
                val base = it.baseParameterWidgetSStruct.baseParameterWidgetStruct
                V3SpecialSettingsWidget.Switch(
                    V3SpecialSettingsWidgetInfo(base.keyMobileSettings, item.title, base.widgetPosition), it.switchChecked,
                )
            }
            else -> error("Unsupported V3 SpecialSettings item: ${item::class.simpleName}")
        }
    }

    fun toItems(widgets: List<V3SpecialSettingsWidget>): List<Any> = widgets.map { widget ->
        val info = widget.info
        when (widget) {
            is V3SpecialSettingsWidget.Slider -> SliderItemV3(info.title, SliderParameterWidgetSStruct(
                base(info, ParameterWidgetCode.PWCE_SLIDER_V3), widget.minProgress, widget.maxProgress, widget.increment,
            ))
            is V3SpecialSettingsWidget.ToggleSlider -> ToggleSliderItemV3(info.title, ToggleSliderParameterWidgetSStruct(
                base(info, ParameterWidgetCode.PWCE_TOGGLE_SLIDER_V3), widget.minProgress, widget.maxProgress, widget.increment, widget.unitLabel,
            ))
            is V3SpecialSettingsWidget.Spinner -> spinner(info, widget.options, widget.initialSelectedIndex)
            is V3SpecialSettingsWidget.SettingsProfile -> spinner(info, widget.options, widget.initialSelectedIndex)
            is V3SpecialSettingsWidget.Switch -> SwitchItem(info.title, SwitchParameterWidgetSStruct(
                BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                    keyMobileSettings = info.key, deviceId = 2, widgetPosition = info.position,
                )), widget.initialChecked,
            ))
        }
    }

    private fun info(title: String, widget: BaseParameterWidgetSStruct): V3SpecialSettingsWidgetInfo {
        val base = widget.baseParameterWidgetStruct
        return V3SpecialSettingsWidgetInfo(
            key = parameterKeys.getValue(base.parameterInfoSet.first()), title = title, position = base.widgetPosition,
        )
    }

    private fun base(info: V3SpecialSettingsWidgetInfo, code: ParameterWidgetCode) = BaseParameterWidgetSStruct(
        BaseParameterWidgetStruct(
            display = 2, widgetCode = code.number.toInt(), widgetPosition = info.position,
            parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(info.key)),
        ), info.title,
    )

    private fun spinner(info: V3SpecialSettingsWidgetInfo, options: List<String>, selectedIndex: Int) = SpinnerItemV3(
        info.title, SpinnerParameterWidgetSStruct(
            base(info, ParameterWidgetCode.PWCE_SPINBOX_V3),
            // The current adapter expects its own mutable list; never expose the state's list to it.
            DataSpinnerParameterWidgetStruct(options.toMutableList(), selectedIndex),
        ),
    )
}
