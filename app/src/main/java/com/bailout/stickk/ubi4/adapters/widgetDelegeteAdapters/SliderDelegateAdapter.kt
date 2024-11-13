package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
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
import com.bailout.stickk.ubi4.models.OneButtonItem
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SliderItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, command: Int) -> Unit
) :
    ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        widgetSliderTitleTv.text = item.title
        var addressDevice = 0
        var parameterID = 0
        var progress = 0


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


        widgetSliderNumTv.text = progress.toString()
        widgetSliderSb.progress = progress
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSetProgress(addressDevice, parameterID, seekBar.progress)
            }
        })
    }

    override fun isForViewType(item: Any): Boolean = item is SliderItem

    override fun SliderItem.getItemId(): Any = title
}