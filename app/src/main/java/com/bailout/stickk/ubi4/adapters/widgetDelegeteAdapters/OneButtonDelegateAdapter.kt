package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.ButtonConfig
import com.bailout.stickk.ubi4.models.widgets.OneButtonItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class OneButtonDelegateAdapter(
    val onButtonPressed: (addressDevice: Int, parameterID: Int, command: Int) -> Unit,
    val onButtonReleased: (addressDevice: Int, parameterID: Int, command: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit
) : ViewBindingDelegateAdapter<OneButtonItem, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: OneButtonItem) {
        onDestroyParent { onDestroy() }

        // Если список кнопок пуст, создаем конфиг из полей самого item для обратной совместимости
        val configs = if (item.buttons.isNotEmpty()) {
            item.buttons
        } else {
            listOf(ButtonConfig(item.title, item.widget))
        }

        // Настраиваем каждую кнопку. Лишние скроются внутри setupButton.
        setupButton(btn1Container, btn1Tv, btn1Ripple, configs.getOrNull(0))
        setupButton(btn2Container, btn2Tv, btn2Ripple, configs.getOrNull(1))
        setupButton(btn3Container, btn3Tv, btn3Ripple, configs.getOrNull(2))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButton(
        container: View,
        textView: TextView,
        ripple: View,
        config: ButtonConfig?
    ) {
        if (config == null) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        textView.text = config.title

        val params = extractWidgetParams(config.widget) ?: return

        ripple.setOnTouchListener { v, event ->
            v.onTouchEvent(event) // Для работы визуального эффекта (ripple)
            
            val action = event.action
            if (params.clickCommand == 0) {
                when (action) {
                    MotionEvent.ACTION_DOWN -> onButtonPressed(params.address, params.pid, params.pressedCommand)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onButtonReleased(params.address, params.pid, params.releasedCommand)
                }
            } else {
                if (action == MotionEvent.ACTION_UP) {
                    onButtonReleased(params.address, params.pid, params.clickCommand)
                }
            }
            true
        }
    }

    private data class WidgetParams(
        val address: Int,
        val pid: Int,
        val clickCommand: Int,
        val pressedCommand: Int,
        val releasedCommand: Int
    )

    private fun extractWidgetParams(widget: Any): WidgetParams? {
        return when (widget) {
            is CommandParameterWidgetEStruct -> {
                val info = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
                WidgetParams(
                    address = info?.deviceAddress ?: 0,
                    pid = info?.parameterID ?: 0,
                    clickCommand = widget.clickCommand,
                    pressedCommand = widget.pressedCommand,
                    releasedCommand = widget.releasedCommand
                )
            }
            is CommandParameterWidgetSStruct -> {
                val info = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
                WidgetParams(
                    address = info?.deviceAddress ?: 0,
                    pid = info?.parameterID ?: 0,
                    clickCommand = widget.clickCommand,
                    pressedCommand = widget.pressedCommand,
                    releasedCommand = widget.releasedCommand
                )
            }
            else -> null
        }
    }

    override fun isForViewType(item: Any): Boolean = item is OneButtonItem
    override fun OneButtonItem.getItemId(): Any = title
    
    fun onDestroy() {
        Log.d("onDestroy", "onDestroy button")
    }
}
