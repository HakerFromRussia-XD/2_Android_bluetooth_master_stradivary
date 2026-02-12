package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.ButtonsItem
import com.bailout.stickk.ubi4.models.widgets.OneButtonItem
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class ButtonsDelegateAdapterV3(
    val onButtonPressedNewV3: (parameter: Byte) -> Unit,
    val onButtonReleasedNewV3: (parameter: Byte) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit) :
    ViewBindingDelegateAdapter<ButtonsItem, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: ButtonsItem) {
        onDestroyParent{ onDestroy() }
        platformLog("[Ubi4Widget1ButtonBinding]","работает ButtonsDelegateAdapterV3 для ${item.title}")
        btn1Tv.text = item.title
        btn2Tv.text = item.title2
        btn3Tv.text = item.title3
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
                    MotionEvent.ACTION_DOWN -> { onButtonPressedNewV3 (0) }
                    MotionEvent.ACTION_UP -> { onButtonReleasedNewV3 (0) }
                }
            } else {
                when (motionEvent.action){
                    MotionEvent.ACTION_UP -> { onButtonReleasedNewV3 (0) }
                }
            }


            return@OnTouchListener true
        })
        btn2Tv.setOnTouchListener(View.OnTouchListener { _, motionEvent ->
            if (clickCommand == 0) {
                when (motionEvent.action){
                    MotionEvent.ACTION_DOWN -> onButtonPressedNewV3(1)
                    MotionEvent.ACTION_UP -> onButtonReleasedNewV3(0)
                }
            } else {
                when (motionEvent.action){
                    MotionEvent.ACTION_UP -> onButtonReleasedNewV3(0)
                }
            }

            return@OnTouchListener true
        })
        btn3Tv.setOnTouchListener(View.OnTouchListener { _,  motionEvent ->
            if (clickCommand == 0) {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> onButtonPressedNewV3(2)
                    MotionEvent.ACTION_UP -> onButtonReleasedNewV3(0)
                }
            } else {
                when (motionEvent.action){
                    MotionEvent.ACTION_UP -> onButtonReleasedNewV3(0)
                }
            }
            return@OnTouchListener true
        })

    }

    override fun isForViewType(item: Any): Boolean = item is OneButtonItem
    override fun ButtonsItem.getItemId():  Any = title
    fun onDestroy() { Log.d("onDestroy" , "onDestroy button") }
}