package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class ButtonsDelegateAdapterV3(
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit) :
    ViewBindingDelegateAdapter<ButtonsItemV3, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
    private val widgetInfoList = arrayListOf<WidgetButtonsInfoV3>()

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
            is CommandParameterWidgetSStruct -> {
                parameterInfoSet = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                countOfButtons = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.size
            }
        }

        btn1Container.visibility = View.VISIBLE
        btn2Container.visibility = if (countOfButtons >= 2) View.VISIBLE else View.GONE
        btn3Container.visibility = if (countOfButtons >= 3) View.VISIBLE else View.GONE
        val widgetInfo = WidgetButtonsInfoV3(
            btn1 = widget1Button,
            btn2 = widget2Button,
            btn3 = widget3Button,
            countOfButtons = countOfButtons
        )
        widgetInfoList.add(widgetInfo)
        applyButtonsLockState(widgetInfo)
        observeInteractionState()

        widget1Button.setOnTouchListener { v, event ->
            if (!isInteractionEnabled) return@setOnTouchListener true
            v.onTouchEvent(event)
            val subcommand = parameterInfoSet.firstOrNull { it.dataOffsets == 0 }?.dataCode

            when (event.action){
                MotionEvent.ACTION_DOWN -> {
                    main.bleCommandWithQueue( BLECommandsV3.sendSubcommand(subcommand!!, 0), SERIALPORTCHAR_UUID, WRITE){}
                    platformLog("ButtonsDelegateAdapterV3", "subcommand: $subcommand")
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { main.bleCommandWithQueue( BLECommandsV3.sendSubcommand(0, 0), SERIALPORTCHAR_UUID, WRITE){} }
            }
            true
        }
        widget2Button.setOnTouchListener { v, event ->
            if (!isInteractionEnabled) return@setOnTouchListener true
            v.onTouchEvent(event)
            val subcommand = parameterInfoSet.firstOrNull { it.dataOffsets == 1 }?.dataCode

            when (event.action){
                MotionEvent.ACTION_DOWN -> {
                    main.bleCommandWithQueue( BLECommandsV3.sendSubcommand(subcommand!!, 0), SERIALPORTCHAR_UUID, WRITE){}
                    platformLog("ButtonsDelegateAdapterV3", "subcommand: $subcommand")
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { main.bleCommandWithQueue( BLECommandsV3.sendSubcommand(0, 0), SERIALPORTCHAR_UUID, WRITE){} }
            }
            true
        }
        widget3Button.setOnTouchListener { v, event ->
            if (!isInteractionEnabled) return@setOnTouchListener true
            v.onTouchEvent(event)
            val subcommand = parameterInfoSet.firstOrNull { it.dataOffsets == 2 }?.dataCode

            when (event.action){
                MotionEvent.ACTION_DOWN -> {
                    main.bleCommandWithQueue( BLECommandsV3.sendSubcommand(subcommand!!, 0), SERIALPORTCHAR_UUID, WRITE){}
                    platformLog("ButtonsDelegateAdapterV3", "subcommand: $subcommand")}
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> { main.bleCommandWithQueue( BLECommandsV3.sendSubcommand(0, 0), SERIALPORTCHAR_UUID, WRITE){} }
            }
            true
        }

    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return
        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                widgetInfoList.forEach { applyButtonsLockState(it) }
            }
        }
    }

    private fun applyButtonsLockState(info: WidgetButtonsInfoV3) {
        info.btn1.isEnabled = isInteractionEnabled
        info.btn1.isClickable = isInteractionEnabled

        info.btn2.isEnabled = isInteractionEnabled
        info.btn2.isClickable = isInteractionEnabled

        info.btn3.isEnabled = isInteractionEnabled
        info.btn3.isClickable = isInteractionEnabled
    }

    override fun isForViewType(item: Any): Boolean = item is ButtonsItemV3
    override fun ButtonsItemV3.getItemId():  Any = title
    fun onDestroy() {
        widgetInfoList.clear()
        scope.coroutineContext.cancelChildren()
        interactionJob?.cancel()
        interactionJob = null
        Log.d("onDestroy" , "onDestroy button")
    }
}

data class WidgetButtonsInfoV3(
    val btn1: View,
    val btn2: View,
    val btn3: View,
    val countOfButtons: Int = 1
)
