package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_H
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_X
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SHUTDOWN_CURRENT_NEW
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.models.OneButtonItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SliderItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupGestures
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var addressDevice = 0
    private var parameterID = 0
    private var progress = 0
    private lateinit var _widgetSliderSb: ProgressBar

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        onDestroyParent{ onDestroy() }
        widgetSliderTitleTv.text = item.title
        _widgetSliderSb = widgetSliderSb

        when (item.widget) {
            is SliderParameterWidgetEStruct -> {
                addressDevice = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                progress = item.widget.progress
            }
            is SliderParameterWidgetSStruct -> {
                addressDevice = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                progress = item.widget.progress
            }
        }
        sliderCollect()


        widgetSliderNumTv.text = progress.toString()
        widgetSliderSb.progress = progress
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSetProgress(addressDevice, parameterID,  seekBar.progress)
            }
        })

        main.bleCommand(
            BLECommands.requestSlider(addressDevice, parameterID), MAIN_CHANNEL,
            SampleGattAttributes.WRITE
        )
    }
    private fun sliderCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                MainActivityUBI4.slidersFlow.collect { _ ->
                    val parameter = ParameterProvider.getParameter(addressDevice, parameterID)
                    Log.d ("parameter sliderCollect", "addressDevice = $addressDevice  parameterID = $parameterID  parameter.data = ${parameter.data}}")
//                    widgetSliderSb.progress = progress
                    if (parameter.data=="") "" else _widgetSliderSb.progress = castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte())
                }
            }
        }
    }
    fun onDestroy() {
        scope.cancel()
    }

    override fun isForViewType(item: Any): Boolean = item is SliderItem

    override fun SliderItem.getItemId(): Any = title
}