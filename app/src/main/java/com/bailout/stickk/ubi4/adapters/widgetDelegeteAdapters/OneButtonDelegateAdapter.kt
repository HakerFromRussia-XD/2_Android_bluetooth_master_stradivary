package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.adapters.models.OneButtonItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class OneButtonDelegateAdapter(
    val onButtonPressed: (parameterID: Int, command: Int) -> Unit,
    val onButtonReleased: (parameterID: Int, command: Int) -> Unit) :
    ViewBindingDelegateAdapter<OneButtonItem, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: OneButtonItem) {
        widget1Button.text = item.title
        var parameterID = 0
        var clickCommand = 0
        var pressedCommand = 0
        var releasedCommand = 0


        when (item.widget) {
            is CommandParameterWidgetEStruct -> {
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parentParameterID
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }
            is CommandParameterWidgetSStruct -> {
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parentParameterID
                clickCommand = item.widget.clickCommand
                pressedCommand = item.widget.pressedCommand
                releasedCommand = item.widget.releasedCommand
            }
        }
        widget1Button.setOnTouchListener(View.OnTouchListener { _, motionEvent ->
            if (clickCommand == 0) {
                when (motionEvent.action){
                    MotionEvent.ACTION_DOWN -> { onButtonPressed(parameterID, pressedCommand) }
                    MotionEvent.ACTION_UP -> { onButtonReleased(parameterID, releasedCommand) }
                }
            } else {
                when (motionEvent.action){
                    MotionEvent.ACTION_UP -> { onButtonReleased(parameterID, clickCommand) }
                }
            }

            return@OnTouchListener true
        })
    }

    override fun isForViewType(item: Any): Boolean = item is OneButtonItem

    override fun OneButtonItem.getItemId(): Any = title
}