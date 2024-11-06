package com.bailout.stickk.ubi4.data


import com.bailout.stickk.ubi4.models.GesturesItem
import com.bailout.stickk.ubi4.models.OneButtonItem
import com.bailout.stickk.ubi4.models.PlotItem
import com.bailout.stickk.ubi4.models.TrainingGestureItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetLabel.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets


internal class DataFactory {

    fun fakeData(): List<Any> {
        val objects = ArrayList<Any>()
        //addElement(8, objects, objects)
        addElementS(14, label = "Start training", objects,objects)
        return objects
    }
    fun fakeDataClear(): List<Any> {
        val objects = ArrayList<Any>()
        return objects
    }

    fun prepareData(display: Int): List<Any> {
        // сортировка всех виджетов по возрастанию
        sortWidgets(listWidgets.sortedWith ( compareBy {
            when (it) {
                is BaseParameterWidgetEStruct -> {it.baseParameterWidgetStruct.widgetPosition}
                is CommandParameterWidgetSStruct -> {it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition}
                is CommandParameterWidgetEStruct -> {it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}
                is PlotParameterWidgetSStruct -> {it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition}
                is PlotParameterWidgetEStruct -> {it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}
                else -> {""}
            }
        }))
        System.err.println("DataFactory sorted listWidgets==============")
        listWidgets.forEach {
            System.err.println("DataFactory sorted listWidgets: $it")
        }

        // компановка элементов для отрисовки на выбраном экране
        val setWidgets: Set<Any> = listWidgets.toSet()
        val _listWidgets = ArrayList<Any>(setWidgets.size)
        setWidgets.forEach {
            when (it) {
                is BaseParameterWidgetEStruct -> {
                    if (it.baseParameterWidgetStruct.display == display) {
                        addElement(
                            it.baseParameterWidgetStruct.widgetCode,
                            it.labelCode,
                            _listWidgets,
                            it
                        )
                    }
                }
                is CommandParameterWidgetSStruct -> {
                    if (it.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display) {
                        addElementS(
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                            it.baseParameterWidgetSStruct.label,
                            _listWidgets,
                            it
                        )
                    }
                }
                is CommandParameterWidgetEStruct -> {
                    if (it.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display) {
                        addElement(
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                            it.baseParameterWidgetEStruct.labelCode,
                            _listWidgets,
                            it
                        )
                    }
                    System.err.println("prepareData CommandParameterWidgetEStruct widgetPosition: ${it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}")
                }
                is PlotParameterWidgetSStruct -> {
                    if (it.baseParameterWidgetSStruct.baseParameterWidgetStruct.display == display) {
                        addElementS(
                            it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetCode,
                            it.baseParameterWidgetSStruct.label,
                            _listWidgets,
                            it
                        )
                    }
                    System.err.println("prepareData PlotParameterWidgetEStruct widgetPosition: ${it.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition}")
                    System.err.println("prepareData PlotParameterWidgetEStruct dataSize: ${it.baseParameterWidgetSStruct.baseParameterWidgetStruct.dataSize}")
                }
                is PlotParameterWidgetEStruct -> {
                    if (it.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display) {
                        addElement(
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                            it.baseParameterWidgetEStruct.labelCode,
                            _listWidgets,
                            it
                        )
                    }
                    System.err.println("prepareData PlotParameterWidgetEStruct widgetPosition: ${it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}")
                }
            }
        }
        return _listWidgets
    }
    private fun sortWidgets(sortedList: List<Any>) {
        listWidgets.clear()
        listWidgets = sortedList.toMutableSet()
    }

    private fun addElement(widgetCode: Int, labelCode: Int, widgets: ArrayList<Any>, widget: Any) {
        val item: Any = when (widgetCode) {
            ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { OneButtonItem("PWCE_UNKNOW", "Description", widget) }
            ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                when (labelCode) {
                    PWLE_UNKNOW.number -> {OneButtonItem(PWLE_UNKNOW.label, "description", widget)}
                    PWLE_OPEN.number -> {OneButtonItem(PWLE_OPEN.label, "description", widget)}
                    PWLE_CLOSE.number -> {OneButtonItem(PWLE_CLOSE.label, "description", widget)}
                    PWLE_CALIBRATE.number -> {OneButtonItem(PWLE_CALIBRATE.label, "description", widget)}
                    PWLE_RESET.number -> {OneButtonItem(PWLE_RESET.label, "description", widget)}
                    PWLE_CONTROL_SETTINGS.number -> {OneButtonItem(PWLE_CONTROL_SETTINGS.label, "description", widget)}
                    PWLE_OPEN_CLOSE_THRESHOLD.number -> {OneButtonItem(PWLE_OPEN_CLOSE_THRESHOLD.label, "description", widget)}
                    PWLE_SELECT_GESTURE.number -> {OneButtonItem(PWLE_SELECT_GESTURE.label, "description", widget)}
                    else -> { OneButtonItem("BUTTON", "description", widget) }
                }
            }
            ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> { OneButtonItem("SWITCH", "description", widget) }
            ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { OneButtonItem("COMBOBOX", "description", widget) }
            ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> { OneButtonItem("SLIDER", "description", widget) }
            ParameterWidgetCode.PWCE_PLOT.number.toInt() -> { PlotItem("PLOT", widget)  }
            ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { OneButtonItem("SPINBOX", "description", widget)  }
            ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { OneButtonItem("EMG_GESTURE_CHANGE_SETTINGS", "description", widget)  }
            ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> {
//                OneButtonItem("GESTURE_SETTINGS", "description", widget)
                GesturesItem("GESTURE_SETTINGS", widget)
            }
            ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { OneButtonItem("CALIB_STATUS", "description", widget)  }
            ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { OneButtonItem("CONTROL_MODE", "description", widget)  }
            ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> { OneButtonItem("OPEN_CLOSE_THRESHOLD", "description", widget)  }
            ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { OneButtonItem("PLOT_AND_1_THRESHOLD", "description", widget)  }
            ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { OneButtonItem("PLOT_AND_2_THRESHOLD", "description", widget)  }
            ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> { GesturesItem("GESTURE_SETTINGS", widget) }
            else -> { OneButtonItem("Open", "description", widget) }
        }
        widgets.add(item)
    }
    private fun addElementS(widgetCode: Int, label: String, widgets: ArrayList<Any>, widget: Any) {
        val item: Any = when (widgetCode) {
            ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { OneButtonItem("PWCE_UNKNOW", "Description", widget) }
            ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                OneButtonItem(label, "description", widget)
            }
            ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> { OneButtonItem("SWITCH", "description", widget) }
            ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { OneButtonItem("COMBOBOX", "description", widget) }
            ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> { OneButtonItem("SLIDER", "description", widget) }
            ParameterWidgetCode.PWCE_PLOT.number.toInt() -> { PlotItem(label, widget)  }
            ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { OneButtonItem("SPINBOX", "description", widget)  }
            ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { OneButtonItem("EMG_GESTURE_CHANGE_SETTINGS", "description", widget)  }
            ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> {
//                OneButtonItem("GESTURE_SETTINGS", "description", widget)
                GesturesItem("GESTURE_SETTINGS", widget)
            }
            ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { OneButtonItem("CALIB_STATUS", "description", widget)  }
            ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { OneButtonItem("CONTROL_MODE", "description", widget)  }
            ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> { OneButtonItem("OPEN_CLOSE_THRESHOLD", "description", widget)  }
            ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { OneButtonItem("PLOT_AND_1_THRESHOLD", "description", widget)  }
            ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { OneButtonItem("PLOT_AND_2_THRESHOLD", "description", widget)  }
            ParameterWidgetCode.PWCE_TRAINING.number.toInt() -> { TrainingGestureItem(label, widget)  }
            ParameterWidgetCode.PWCE_GESTURES_WINDOW.number.toInt() -> { GesturesItem("GESTURE_SETTINGS", widget) }
            else -> { OneButtonItem("Open", "description", widget) }
        }
        widgets.add(item)
    }
    private fun <T> List<T>.listToArrayList(): ArrayList<T> {
        val array: ArrayList<T> = ArrayList()
        for (index in this) array.add(index)
        return array
    }
}