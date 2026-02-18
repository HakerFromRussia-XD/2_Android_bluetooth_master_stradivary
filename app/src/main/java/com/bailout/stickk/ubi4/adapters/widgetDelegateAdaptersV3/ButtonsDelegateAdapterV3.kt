package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class ButtonsDelegateAdapterV3(
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit) :
    ViewBindingDelegateAdapter<ButtonsItemV3, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: ButtonsItemV3) {
        onDestroyParent{ onDestroy() }
        platformLog("[Ubi4Widget1ButtonBinding]","работает ButtonsDelegateAdapterV3 для ${item.title}")
        widget1ButtonTv.text = item.title
        widget2ButtonTv.text = item.title2
        widget3ButtonTv.text = item.title3
        var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(ParameterInfo(0,0,0,0))
        var countOfButtons = 1


        when (val widget = item.widget) {
            is CommandParameterWidgetEStruct -> {
                parameterInfoSet = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet
                countOfButtons = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.size
            }
            is CommandParameterWidgetSStruct -> {
                parameterInfoSet = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                countOfButtons = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.size
            }
        }

        btn1Container.visibility = View.VISIBLE
        btn2Container.visibility = if (countOfButtons >= 2) View.VISIBLE else View.GONE
        btn3Container.visibility = if (countOfButtons >= 3) View.VISIBLE else View.GONE

        widget1Button.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            val moduleControlCommand = parameterInfoSet.firstOrNull { it.dataOffset == 0 }?.dataCode

            when (event.action){
                MotionEvent.ACTION_DOWN -> { main.bleCommandWithQueue( BLECommandsV3.sendCommand(moduleControlCommand!!), SERIALPORTCHAR_UUID, WRITE){} }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { main.bleCommandWithQueue( BLECommandsV3.sendCommand(0), SERIALPORTCHAR_UUID, WRITE){} }
            }
            true
        }
        widget2Button.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            val moduleControlCommand = parameterInfoSet.firstOrNull { it.dataOffset == 1 }?.dataCode

            when (event.action){
                MotionEvent.ACTION_DOWN -> { main.bleCommandWithQueue( BLECommandsV3.sendCommand(moduleControlCommand!!), SERIALPORTCHAR_UUID, WRITE){} }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { main.bleCommandWithQueue( BLECommandsV3.sendCommand(0), SERIALPORTCHAR_UUID, WRITE){} }
            }
            true
        }
        widget3Button.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            val moduleControlCommand = parameterInfoSet.firstOrNull { it.dataOffset == 2 }?.dataCode

            when (event.action){
                MotionEvent.ACTION_DOWN -> { main.bleCommandWithQueue( BLECommandsV3.sendCommand(moduleControlCommand!!), SERIALPORTCHAR_UUID, WRITE){} }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { main.bleCommandWithQueue( BLECommandsV3.sendCommand(0), SERIALPORTCHAR_UUID, WRITE){} }
            }
            true
        }

    }

    override fun isForViewType(item: Any): Boolean = item is ButtonsItemV3
    override fun ButtonsItemV3.getItemId():  Any = title
    fun onDestroy() { Log.d("onDestroy" , "onDestroy button") }
}