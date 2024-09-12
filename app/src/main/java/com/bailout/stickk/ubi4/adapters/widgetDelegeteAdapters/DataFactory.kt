package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import com.bailout.stickk.ubi4.data.OneButtonWidget
import com.bailout.stickk.ubi4.data.TestSpinnerButtonWidget
import com.bailout.stickk.ubi4.data.TestTwoButtonWidget
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterWidgetCode
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets


internal object DataFactory {

    fun prepareData(): List<Any> {
        System.err.println("DataFactory prepareData  size: ${listWidgets.size}")
        val objects = ArrayList<Any>(listWidgets.size)
        listWidgets.forEach {
            when (it::class.simpleName) {
                CommandParameterWidgetEStruct::class.simpleName -> {
                    addElement((it as CommandParameterWidgetEStruct).baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, objects)
                    System.err.println("prepareData CommandParameterWidgetEStruct widgetPosition: ${it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}")
                }
                PlotParameterWidgetEStruct::class.simpleName -> {
                    addElement((it as PlotParameterWidgetEStruct).baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode, objects)
                    System.err.println("prepareData PlotParameterWidgetEStruct widgetPosition: ${it.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition}")
                }
                OneButtonWidget::class.simpleName -> {
                    (it as OneButtonWidget)
                    System.err.println("prepareData OneButtonWidget")
                }
                TestTwoButtonWidget::class.simpleName -> {
                    (it as TestTwoButtonWidget)
                    System.err.println("prepareData TestTwoButtonWidget")
                }
                TestSpinnerButtonWidget::class.simpleName -> {
                    (it as TestSpinnerButtonWidget)
                    addElement(11, objects)
                    System.err.println("prepareData TestSpinnerButtonWidget")
                }
            }
        }
        System.err.println("prepareData objects $objects")
        return objects
    }

    private fun addElement(widgetCode: Int, objects: ArrayList<Any>) {
        val item: Any = when (widgetCode) {
            ParameterWidgetCode.PWCE_UNKNOW.number.toInt() -> { OneButtonItem("PWCE_UNKNOW", "Description") }
            ParameterWidgetCode.PWCE_BUTTON.number.toInt() -> { OneButtonItem("BUTTON", "description") }
            ParameterWidgetCode.PWCE_SWITCH.number.toInt() -> { OneButtonItem("SWITCH", "description") }
            ParameterWidgetCode.PWCE_COMBOBOX.number.toInt() -> { OneButtonItem("COMBOBOX", "description") }
            ParameterWidgetCode.PWCE_SLIDER.number.toInt() -> { OneButtonItem("SLIDER", "description") }
            ParameterWidgetCode.PWCE_PLOT.number.toInt() -> { OneButtonItem("PLOT", "description")  }
            ParameterWidgetCode.PWCE_SPINBOX.number.toInt() -> { OneButtonItem("SPINBOX", "description")  }
            ParameterWidgetCode.PWCE_EMG_GESTURE_CHANGE_SETTINGS.number.toInt() -> { OneButtonItem("EMG_GESTURE_CHANGE_SETTINGS", "description")  }
            ParameterWidgetCode.PWCE_GESTURE_SETTINGS.number.toInt() -> { OneButtonItem("GESTURE_SETTINGS", "description")  }
            ParameterWidgetCode.PWCE_CALIB_STATUS.number.toInt() -> { OneButtonItem("CALIB_STATUS", "description")  }
            ParameterWidgetCode.PWCE_CONTROL_MODE.number.toInt() -> { OneButtonItem("CONTROL_MODE", "description")  }
            ParameterWidgetCode.PWCE_OPEN_CLOSE_THRESHOLD.number.toInt() -> { OneButtonItem("OPEN_CLOSE_THRESHOLD", "description")  }
            ParameterWidgetCode.PWCE_PLOT_AND_1_THRESHOLD.number.toInt() -> { OneButtonItem("PLOT_AND_1_THRESHOLD", "description")  }
            ParameterWidgetCode.PWCE_PLOT_AND_2_THRESHOLD.number.toInt() -> { OneButtonItem("PLOT_AND_2_THRESHOLD", "description")  }
            else -> { OneButtonItem("Open", "description") }
        }
        objects.add(item)
    }

    fun prepareDataOld(): List<Any> {
        val objects = ArrayList<Any>(listWidgets.size)
        for (i in 0 until listWidgets.size) {
            val item: Any = when (3) {
                else -> { OneButtonItem("Open $i", "description") }
            }
            objects.add(item)
        }
        System.err.println("prepareData objects $objects")
        return objects
    }
}