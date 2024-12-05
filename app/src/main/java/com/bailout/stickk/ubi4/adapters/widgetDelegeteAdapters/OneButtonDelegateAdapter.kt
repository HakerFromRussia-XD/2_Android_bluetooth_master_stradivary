package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.models.OneButtonItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import java.io.File
import kotlinx.coroutines.cancel

class OneButtonDelegateAdapter(
    val onButtonPressed: (addressDevice: Int, parameterID: Int, command: Int) -> Unit,
    val onButtonReleased: (addressDevice: Int, parameterID: Int, command: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit) :
    ViewBindingDelegateAdapter<OneButtonItem, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: OneButtonItem) {
        onDestroyParent{ onDestroy() }
        widget1ButtonTv.text = item.title
        var addressDevice = 0
        var parameterID = 0
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        when (item.widget) {
            is CommandParameterWidgetEStruct -> {
                addressDevice = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }
            is CommandParameterWidgetSStruct -> {
                addressDevice = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }
        }
        widget1Button.setOnTouchListener(View.OnTouchListener { _, motionEvent ->
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