package com.bailout.stickk.ubi4.adapters.models


import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets


internal class DataFactory {

    fun fakeData(): List<Any> {
        val objects = ArrayList<Any>()
        addElement(8, objects, objects)
        //addElement(8, objects, objects)
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
                is CommandParameterWidgetEStruct -> {it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}
                is PlotParameterWidgetEStruct -> {it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}
                else -> {""}
            }
        }))
        System.err.println("DataFactory sorted listWidgets==============")
        listWidgets.forEach {
            System.err.println("DataFactory sorted listWidgets: $it")
        }

        // компановка элементов для отрисовки на выбраном экране
        val _listWidgets = ArrayList<Any>(listWidgets.size)
        listWidgets.forEach {
            when (it) {
                is CommandParameterWidgetEStruct -> {
                    if (it.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display) {
                        addElement(
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
                            _listWidgets,
                            it
                        )
                    }
                    System.err.println("prepareData CommandParameterWidgetEStruct widgetPosition: ${it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}")
                }
                is PlotParameterWidgetEStruct -> {
                    if (it.baseParameterWidgetEStruct.baseParameterWidgetStruct.display == display) {
                        addElement(
                            it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode,
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
        listWidgets = sortedList.listToArrayList()
    }

    private fun addElement(widgetCode: Int, widgets: ArrayList<Any>, widget: Any) {
        val item: Any = when (widgetCode) {
            ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { OneButtonItem("PWCE_UNKNOW", "Description", widget) }
            ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> {
                OneButtonItem("BUTTON", "description", widget)
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