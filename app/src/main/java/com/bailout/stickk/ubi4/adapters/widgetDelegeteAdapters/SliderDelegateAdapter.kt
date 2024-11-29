package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.util.Log
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.SliderItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        onDestroyParent{ onDestroy() }
        var addressDevice = 0
        var parameterID = 0
        var progress = 0
        widgetSliderTitleTv.text = item.title

        when (item.widget) {
            is SliderParameterWidgetEStruct -> {
                addressDevice = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                progress = item.widget.progress
                Log.d("addressDevice" , "E struct addressDevice = $addressDevice   ${item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId}")
            }
            is SliderParameterWidgetSStruct -> {
                addressDevice = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
                parameterID = item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                progress = item.widget.progress
                Log.d("addressDevice" , "S struct addressDevice = $addressDevice   ${item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId}")
            }
        }
        widgetSlidersInfo.add(WidgetSliderInfo(addressDevice, parameterID, progress, widgetSliderSb, widgetSliderNumTv))
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
                MainActivityUBI4.slidersFlow.collect { parameterRef ->
                    val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
                    // в parameterRef прилетают addressDevice и parameterID для того слайдера, который нужно обновить
                    // а в Array widgetSlidersInfo хранится список всех сочетаний адресов девайсов
                    // и айдишников параметров вместе с их вьюхами для изменения
                    Log.d ("parameter sliderCollect", "перед попыткой добавить данные в слайдер  addressDevice = ${parameterRef.addressDevice}  parameterID = ${parameterRef.parameterID}  parameter.data = ${parameter.data}}")
                    val indexWidgetSlider = getWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
                    if (indexWidgetSlider != -1 && indexWidgetSlider < widgetSlidersInfo.size) {
                        if (parameter.data=="") Log.d ("parameter sliderCollect", "не успешная попытка обновления")
                        if (parameter.data!="") Log.d ("parameter sliderCollect", "успешная попытка обновления")
                        if (parameter.data=="") "" else widgetSlidersInfo[indexWidgetSlider].progress = castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte())
                        widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb.progress = widgetSlidersInfo[indexWidgetSlider].progress
                        widgetSlidersInfo[indexWidgetSlider].widgetSliderNumTv.text = widgetSlidersInfo[indexWidgetSlider].progress.toString()
                    } else {
                        Log.d ("parameter sliderCollect", "НЕТ слайдера, которому передназначаются данные")
                        Log.d ("parameter sliderCollect", "данные для addressDevice = ${parameterRef.addressDevice}  parameterID = ${parameterRef.parameterID}")
                        widgetSlidersInfo.forEach {
                            Log.d ("parameter sliderCollect", "есть только такие addressDevice = ${it.addressDevice}  parameterID = ${it.parameterID}")
                        }
                    }
                }
            }
        }
    }
    fun onDestroy() {
        scope.cancel()
    }

    private fun getWidgetSlider(addressDevice: Int, parameterID: Int): Int {
        widgetSlidersInfo.forEachIndexed { index, widgetSliderInfo ->
            if (widgetSliderInfo.addressDevice == addressDevice && widgetSliderInfo.parameterID == parameterID) {
                return index
            }
        }
        return -1
    }
    override fun isForViewType(item: Any): Boolean = item is SliderItem
    override fun SliderItem.getItemId(): Any = title
}

data class WidgetSliderInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var progress: Int = 0,
    var widgetSlidersSb: ProgressBar,
    var widgetSliderNumTv: TextView,
)
