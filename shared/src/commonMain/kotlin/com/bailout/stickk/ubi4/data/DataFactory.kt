package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureOpticParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.GestureParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.GesturesItem
import com.bailout.stickk.ubi4.models.widgets.OneButtonItem
import com.bailout.stickk.ubi4.models.widgets.PlotItem
import com.bailout.stickk.ubi4.models.widgets.SliderItem
import com.bailout.stickk.ubi4.models.widgets.SwitchItem
import com.bailout.stickk.ubi4.models.widgets.TrainingGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.parameterWidgetLabel
import com.bailout.stickk.ubi4.utility.logging.systemLang


class DataFactory {

    fun fakeData(): List<Any> = buildList {
        add(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 0)))
        add(PlotParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 1))))
        add(PlotParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 2))))
        add(CommandParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 3))))
        add(CommandParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 4))))
        add(SwitchParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 5))))
        add(SwitchParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 6))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 7))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 8))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 9))))
        add(SliderParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 10))))
        add(OpticStartLearningWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 11))))
        add(SpinnerParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 12))))

        add(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 0)))
        add(PlotParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 1))))
        add(PlotParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 2))))
        add(CommandParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 3))))
        add(CommandParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 4))))
        add(SwitchParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 5))))
        add(SwitchParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 6))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 7))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 8))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 9))))
        add(SliderParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 10))))
        add(OpticStartLearningWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 11))))
        add(SpinnerParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 12))))
    }
    fun fakeData2(): List<Any> = buildList {
//        add(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 1)))
        add(GesturesItem("test gestures widget", GestureParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 1)))))
        add(PlotParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 3))))
        add(SwitchParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 2))))
        add(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 4)))
        add(SwitchParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 5))))
        add(SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 7))))
    }

    fun fakeDataClear(): List<Any> = emptyList()

    fun mobileWidgets(): List<Any> {
        val widget = SwitchParameterWidgetSStruct(
            BaseParameterWidgetSStruct(
                BaseParameterWidgetStruct(
                    keyMobileSettings = MobileSettingsKey.AUTO_LOGIN.key,
                    deviceId = 2
                )
            )
        )
        return listOfNotNull(toWidgetItemS(ParameterWidgetCode.PWCE_SWITCH.number.toInt(), "auto_login", widget))
    }

    private val baseWidget = BaseParameterWidgetStruct().apply {
        parameterInfoSet = mutableSetOf(
            ParameterInfo(8, 2, 0, 2),
            ParameterInfo(6, 16, 1, 16)
        )
    }

    fun prepareData(display: Int): List<Any> {
        // Фильтруем виджеты по display
        val filteredWidgets = UiState.listWidgets.filter { widget ->
            when (widget) {
                is BaseParameterWidgetEStruct -> widget.baseParameterWidgetStruct.display == display
                is CommandParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is CommandParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is PlotParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is PlotParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is OpticStartLearningWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is SwitchParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is SwitchParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is SliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is SliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                else -> false
            }
        }
        // Сортируем по widgetPosition
        val sortedWidgets = filteredWidgets.sortedBy { widget ->
            when (widget) {
                is BaseParameterWidgetEStruct -> widget.baseParameterWidgetStruct.widgetPosition
                is CommandParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is CommandParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is PlotParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is PlotParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is OpticStartLearningWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is SwitchParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is SwitchParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is SliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is SliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                else -> 0
            }
        }
        // Преобразуем виджеты в UI-элементы
        return sortedWidgets.mapNotNull { widget ->
            when (widget) {
                is BaseParameterWidgetEStruct ->
                    toWidgetItemE(widget.baseParameterWidgetStruct.widgetCode, widget.labelCode, widget)
                is CommandParameterWidgetSStruct ->
                    toWidgetItemS(widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetSStruct.label, widget)
                is CommandParameterWidgetEStruct ->
                    toWidgetItemE(widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetEStruct.labelCode, widget)
                is PlotParameterWidgetSStruct ->
                    toWidgetItemS(widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetSStruct.label, widget)
                is PlotParameterWidgetEStruct ->
                    toWidgetItemE(widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetEStruct.labelCode, widget)
                is OpticStartLearningWidgetEStruct ->
                    toWidgetItemE(widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetEStruct.labelCode, widget)
                is SwitchParameterWidgetSStruct ->
                    toWidgetItemS(widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetSStruct.label, widget)
                is SwitchParameterWidgetEStruct ->
                    toWidgetItemE(widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetEStruct.labelCode, widget)
                is SliderParameterWidgetSStruct ->
                    toWidgetItemS(widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetSStruct.label, widget)
                is SliderParameterWidgetEStruct ->
                    toWidgetItemE(widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, widget.baseParameterWidgetEStruct.labelCode, widget)
                else -> null
            }
        }
    }

    private fun labelBy(code: String, lang: String = systemLang()): String {
        val langKey = when {
            lang.startsWith("ru", ignoreCase = true) -> "ru"
            else -> "en"
        }
        val langMap = parameterWidgetLabel[langKey] ?: parameterWidgetLabel["en"].orEmpty()
        return langMap[code] ?: "Unknown"
    }

    // Общие функции преобразования для виджетов (варианты с labelCode и label)
    private fun toWidgetItemE(widgetCode: Int, labelCode: Int, widget: Any): Any? {
        val label = labelBy(labelCode.toString())
        return toWidgetItemS(widgetCode, label, widget)
    }



    private fun toWidgetItemS(widgetCode: Int, label: String = "no name", widget: Any): Any? {
        val resolvedLabel = if (label.startsWith('%')) {
            labelBy( label.substring(1).trimEnd('\u0000'))
        } else {
            label
        }
        return when (widgetCode) {
            ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> null
            ParameterWidgetCode.PWCE_BUTTON.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_SWITCH.number.toInt() ->
                SwitchItem(resolvedLabel, widget)
            ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_SLIDER.number.toInt() ->
                SliderItem(resolvedLabel, widget)
            ParameterWidgetCode.PWCE_PLOT.number.toInt() ->
                PlotItem(resolvedLabel, widget)
            ParameterWidgetCode.PWCE_SPINBOX.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() ->
                OneButtonItem(resolvedLabel, "description", widget)
            ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() ->
                GesturesItem(resolvedLabel, widget)
            ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() ->
                TrainingGestureItem(resolvedLabel, widget)
            else -> OneButtonItem(resolvedLabel, "description", widget)
        }
    }
}















