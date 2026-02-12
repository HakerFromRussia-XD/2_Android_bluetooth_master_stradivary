package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.OneButtonItem
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class OneButtonDelegateAdapter(
    val onButtonPressed: (addressDevice: Int, parameterID: Int, command: Int) -> Unit,
    val onButtonReleased: (addressDevice: Int, parameterID: Int, command: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit) :
    ViewBindingDelegateAdapter<OneButtonItem, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: OneButtonItem) {
        onDestroyParent{ onDestroy() }
        platformLog("[Ubi4Widget1ButtonBinding]","работает OneButtonDelegateAdapter для ${item.title}")
        btn1Tv.text = item.title
        var addressDevice = 0
        var parameterID = 0
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0

        when (val widget = item.widget) {
            is CommandParameterWidgetEStruct -> {
                addressDevice = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).parameterID
                clickCommand = widget.clickCommand
                pressedCommand = widget.pressedCommand
                releasedCommand = widget.releasedCommand
                widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    Log.d("TestButton", "$it")
                    Log.d("TestButton", "deviceId = $addressDevice")
                }
                Log.d(
                    "ONE_BUTTON",
                    "BIND ESTRUCT: addr=$addressDevice pid=$parameterID " +
                            "click=$clickCommand pressed=$pressedCommand released=$releasedCommand"
                )
            }
            is CommandParameterWidgetSStruct -> {
                addressDevice = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(0).parameterID
                clickCommand = widget.clickCommand
                pressedCommand = widget.pressedCommand
                releasedCommand = widget.releasedCommand

                Log.d(
                    "ONE_BUTTON",
                    "BIND SSTRUCT: addr=$addressDevice pid=$parameterID " +
                            "click=$clickCommand pressed=$pressedCommand released=$releasedCommand"
                )

            }
        }
        btn1Tv.setOnTouchListener(View.OnTouchListener { _, motionEvent ->
            if (clickCommand == 0) {
                when (motionEvent.action){
                    MotionEvent.ACTION_DOWN -> { onButtonPressed(addressDevice, parameterID, pressedCommand) }
                    MotionEvent.ACTION_UP -> { onButtonReleased(addressDevice, parameterID, releasedCommand) }
                }
            } else {
                when (motionEvent.action){
                    MotionEvent.ACTION_UP -> { onButtonReleased(addressDevice, parameterID, clickCommand) }
                }
            }


            return@OnTouchListener true
        })
    }

    override fun isForViewType(item: Any): Boolean = item is OneButtonItem
    override fun OneButtonItem.getItemId(): Any = title
    fun onDestroy() {
        Log.d("onDestroy" , "onDestroy button")
    }
}