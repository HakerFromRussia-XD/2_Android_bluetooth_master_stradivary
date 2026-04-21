package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.OpticStartLearningWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SwitchParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.models.widgets.GesturesItem
import com.bailout.stickk.ubi4.models.widgets.GesturesItemV3
import com.bailout.stickk.ubi4.models.widgets.OneButtonItem
import com.bailout.stickk.ubi4.models.widgets.PlotItem
import com.bailout.stickk.ubi4.models.widgets.PlotItemV3
import com.bailout.stickk.ubi4.models.widgets.SliderItem
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.models.widgets.SpinnerItem
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.models.widgets.SwitchItem
import com.bailout.stickk.ubi4.models.widgets.SwitchItemV3
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItem
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.models.widgets.TrainingGestureItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.parameterWidgetLabel
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.utility.logging.systemLang


class DataFactory {


    fun fakeData(): List<Any> = buildList {
//        add(OneButtonItem("COMMAND E", "description", CommandParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 3, widgetCode = ParameterWidgetCode.PWCE_BUTTON.number.toInt())))))
//        add(OneButtonItem("COMMAND S", "description", CommandParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 4, widgetCode = ParameterWidgetCode.PWCE_BUTTON.number.toInt())))))
//        add(SwitchItem("SWITCH E", SwitchParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 5, widgetCode = ParameterWidgetCode.PWCE_SWITCH.number.toInt())))))
        add(SwitchItem("SWITCH S", SwitchParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 6, widgetCode = ParameterWidgetCode.PWCE_SWITCH.number.toInt())))))
        add(SliderItem("SLIDER E", SliderParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 7, widgetCode = ParameterWidgetCode.PWCE_SLIDER.number.toInt())))))
        add(
            SpinnerItemV3(
                "Режим работы протеза",
                SpinnerParameterWidgetSStruct(
                    baseParameterWidgetSStruct = BaseParameterWidgetSStruct(
                        BaseParameterWidgetStruct(
                            widgetPosition = 8,
                            widgetCode = ParameterWidgetCode.PWCE_SPINBOX_V3.number.toInt()
                        ),
                        "Режим работы протеза"
                    ),
                    dataSpinnerParameterWidgetStruct = DataSpinnerParameterWidgetStruct(
                        spinnerItems = listOf(
                            "Нормальный",
                            "Спортивный",
                            "Плавное управление силой",
                            "Плавное управление скоростью",
                            "Плавное управление силой и скоростью"
                        ),
                        selectedIndex = 0
                    )
                )
            )
        )
//        add(SliderItem("SLIDER S", SliderParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 8, widgetCode = ParameterWidgetCode.PWCE_SLIDER.number.toInt())))))
//        add(PlotItem("PLOT E", PlotParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 9, widgetCode = ParameterWidgetCode.PWCE_PLOT.number.toInt())))))
//        add(PlotItem("PLOT S", PlotParameterWidgetSStruct(BaseParameterWidgetSStruct(BaseParameterWidgetStruct(widgetPosition = 10, widgetCode = ParameterWidgetCode.PWCE_PLOT.number.toInt())))))
//        add(TrainingGestureItem("OPTIC LEARNING", OpticStartLearningWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct(widgetPosition = 11, widgetCode = ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt())))))
        add(
            SpinnerItem(
                "Уровень доступа",
                SpinnerParameterWidgetEStruct(
                    BaseParameterWidgetEStruct(
                        BaseParameterWidgetStruct(
                            widgetPosition = 12,
                            widgetCode = ParameterWidgetCode.PWCE_SPINBOX.number.toInt()
                        )
                    )
                )
            )
        )
//        add(SliderItem("TOGGLE SLIDER",
//            ToggleSliderParameterWidgetEStruct(
//                BaseParameterWidgetEStruct(
//                    BaseParameterWidgetStruct(
//                        widgetPosition = 6,
//                        widgetCode = ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt()
//                    )
//                )
//            )
//        ))
    }



    fun fakeData2(): List<Any> = buildList {
        add(BaseParameterWidgetEStruct(BaseParameterWidgetStruct()))
        add(PlotParameterWidgetEStruct(BaseParameterWidgetEStruct(BaseParameterWidgetStruct())))
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
        return listOfNotNull(
            toWidgetItemS(
                ParameterWidgetCode.PWCE_SWITCH.number.toInt(),
                "auto_login",
                widget
            )
        )
    }

    private val baseWidget = BaseParameterWidgetStruct().apply {
        parameterInfoSet = mutableSetOf(
            ParameterInfo(8, 2, 0, 2),
            ParameterInfo(6, 16, 1, 16)
        )
    }

    fun prepareData(display: Int): List<Any> {
        platformLog(
            "WIDGET_SOURCE",
            "prepareData: display=$display listWidgets.size=${UiState.listWidgets.size}"
        )
        // Фильтруем виджеты по display
        val filteredWidgets = UiState.listWidgets.filter { widget ->
            when (widget) {
                is BaseParameterWidgetEStruct -> widget.baseParameterWidgetStruct.display == display
                is BaseParameterWidgetSStruct -> widget.baseParameterWidgetStruct.display == display
                is CommandParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is CommandParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is PlotParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is PlotParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is OpticStartLearningWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is SwitchParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is SwitchParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is SliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is SliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is ToggleSliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                is ToggleSliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is SpinnerParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display
                is SpinnerParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display
                else -> false
            }
        }


        // Сортируем по widgetPosition
        val sortedWidgets = filteredWidgets.sortedBy { widget ->
            when (widget) {
                is BaseParameterWidgetEStruct -> widget.baseParameterWidgetStruct.widgetPosition
                is BaseParameterWidgetSStruct -> widget.baseParameterWidgetStruct.widgetPosition
                is CommandParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is CommandParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is PlotParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is PlotParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is OpticStartLearningWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is SwitchParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is SwitchParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is SliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is SliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is ToggleSliderParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                is ToggleSliderParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is SpinnerParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                is SpinnerParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition

                else -> 0
            }
        }
        // Преобразуем виджеты в UI-элементы
        return sortedWidgets.mapNotNull { widget ->
            when (widget) {
                is BaseParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetStruct.widgetCode,
                        widget.labelCode,
                        widget
                    )
                is BaseParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetStruct.widgetCode,
                        widget.label,
                        widget
                    )

                is CommandParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetSStruct.label,
                        widget
                    )

                is CommandParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )

                is PlotParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetSStruct.label,
                        widget
                    )

                is PlotParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )

                is OpticStartLearningWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )

                is SwitchParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetSStruct.label,
                        widget
                    )

                is SwitchParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )

                is SliderParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetSStruct.label,
                        widget
                    )

                is SliderParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )

                is ToggleSliderParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetSStruct.label,
                        widget
                    )

                is ToggleSliderParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )
                is SpinnerParameterWidgetSStruct ->
                    toWidgetItemS(
                        widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetSStruct.label,
                        widget
                    )

                is SpinnerParameterWidgetEStruct ->
                    toWidgetItemE(
                        widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                        widget.baseParameterWidgetEStruct.labelCode,
                        widget
                    )


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
        return langMap[code]?.title ?: "Unknown"
    }

    // Общие функции преобразования для виджетов (варианты с labelCode и label)
    private fun toWidgetItemE(widgetCode: Int, labelCode: Int, widget: Any): Any? {
        val label = labelBy(labelCode.toString())
        return toWidgetItemS(widgetCode, label, widget)
    }


    private fun toWidgetItemS(widgetCode: Int, label: String = "no name%no name%no name", widget: Any): Any? {
        val partsLabel = label.split("%").map { it.trim() }

        val resultLabel = if (partsLabel.size < 3) {
            partsLabel + List(3 - partsLabel.size) { "no name" }
        } else {
            partsLabel
        }
        return when (widgetCode) {
            ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> null

            ParameterWidgetCode.PWCE_BUTTON.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_SWITCH.number.toInt() ->
                SwitchItem(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_SLIDER.number.toInt() ->
                SliderItem(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_TOGGLE_SLIDER.number.toInt() ->
                ToggleSliderItem(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_PLOT.number.toInt() ->
                PlotItem(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_SPINBOX.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() ->
                OneButtonItem(resultLabel[0], "description", widget)

            ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() ->
                GesturesItem(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_OPTIC_LEARNING_WIDGET.number.toInt() ->
                TrainingGestureItem(resultLabel[0], widget)

            ////////////////////////////////////////////////////////////////////////////////////////
            ///////                                 V3                                     /////////
            ////////////////////////////////////////////////////////////////////////////////////////
            ParameterWidgetCode.PWCE_BUTTON_V3.number.toInt() ->
                ButtonsItemV3(resultLabel[0], resultLabel[1], resultLabel[2], "description", widget)

            ParameterWidgetCode.PWCE_PLOT_V3.number.toInt() ->
                PlotItemV3(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_SLIDER_V3.number.toInt() ->
                SliderItemV3(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_TOGGLE_SLIDER_V3.number.toInt() ->
                ToggleSliderItemV3(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_GESTURES_WINDOW_V3.number.toInt() -> {
                GesturesItemV3(resultLabel[0], widget)
            }

            ParameterWidgetCode.PWCE_SWITCH_V3.number.toInt() ->
                SwitchItemV3(resultLabel[0], widget)

            ParameterWidgetCode.PWCE_SPINBOX_V3.number.toInt() -> {
                SpinnerItemV3(resultLabel[0], widget)
            }

            ParameterWidgetCode.PWCE_TEXT_INPUT_V3.number.toInt() -> {
                val buttonTitle = resultLabel[1]
                    .takeUnless { it.isBlank() || it.equals("no name", ignoreCase = true) }
                    ?: "Отправить"
                TextInputItemV3(
                    title = resultLabel[0],
                    buttonTitle = buttonTitle,
                    widget = widget
                )
            }
            else -> OneButtonItem(resultLabel[0], "description", widget)
        }
    }

    fun Any.extractDisplayOrNull(): Int? = when (this) {
        is BaseParameterWidgetEStruct -> baseParameterWidgetStruct.display
        is BaseParameterWidgetSStruct -> baseParameterWidgetStruct.display

        is CommandParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display
        is CommandParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is PlotParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display
        is PlotParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is OpticStartLearningWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is SwitchParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display
        is SwitchParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is SliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display
        is SliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        is ToggleSliderParameterWidgetSStruct -> baseParameterWidgetSStruct.baseParameterWidgetStruct.display
        is ToggleSliderParameterWidgetEStruct -> baseParameterWidgetEStruct.baseParameterWidgetStruct.display

        else -> null
    }


}
