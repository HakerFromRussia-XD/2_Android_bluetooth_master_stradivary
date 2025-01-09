package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.annotation.SuppressLint
import android.os.Handler
import android.util.Log
import android.view.View
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
import com.bailout.stickk.ubi4.models.ParameterRef
import com.bailout.stickk.ubi4.models.SliderItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.slidersFlow
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: ArrayList<Int>) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        onDestroyParent{ onDestroy() }
        val addressDevice: ArrayList<Int> = ArrayList()
        val parameterID: ArrayList<Int> = ArrayList()
        val dataCode: ArrayList<Int> = ArrayList()
        val dataOffset: ArrayList<Int> = ArrayList()
        var minProgress = 0
        var maxProgress = 0
        widgetSliderTitleTv.text = item.title

        when (item.widget) {
            is SliderParameterWidgetEStruct -> {
                item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.forEach {
                    addressDevice.add(it.deviceAddress)
                    parameterID.add(it.parameterID)
                    dataOffset.add(it.dataOffset)
                    dataCode.add(it.dataCode)
                }
                minProgress = item.widget.minProgress
                maxProgress = item.widget.maxProgress
                Log.d("addressDevice" , "E struct addressDevice = $addressDevice   ${item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId}")
            }
            is SliderParameterWidgetSStruct -> {
                item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.forEach {
                    addressDevice.add(it.deviceAddress)
                    parameterID.add(it.parameterID)
                    dataOffset.add(it.dataOffset)
                    dataCode.add(it.dataCode)

                }
                minProgress = item.widget.minProgress
                maxProgress = item.widget.maxProgress
                Log.d("addressDevice" , "S struct addressDevice = $addressDevice   ${item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId}")
            }
        }
        widgetSlidersInfo.add(WidgetSliderInfo(addressDevice, parameterID, dataOffset, minProgress, maxProgress, arrayListOf(0, 0), arrayListOf(widgetSliderSb, widgetSlider2Sb), arrayListOf(widgetSliderNumTv,widgetSliderNum2Tv)))
        sliderCollect()
        setUI(ParameterRef(addressDevice[0], parameterID[0], dataCode[0]))
        val indexWidgetSlider = getIndexWidgetSlider(addressDevice[0], parameterID[0])
//        val progress: ArrayList<Int> = ArrayList(List(addressDevice.size) { 0 })
        if (addressDevice.size > 1) {
            secondSliderCl.visibility = View.VISIBLE
        } else {
            secondSliderCl.visibility = View.GONE
        }


        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                widgetSlidersInfo[indexWidgetSlider].progress[0] = seekBar.progress
                onSetProgress(addressDevice[0], parameterID[0],  widgetSlidersInfo[indexWidgetSlider].progress)
            }
        })
        widgetSlider2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNum2Tv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                widgetSlidersInfo[indexWidgetSlider].progress[1] = seekBar.progress
                onSetProgress(addressDevice[1], parameterID[1],  widgetSlidersInfo[indexWidgetSlider].progress)
            }
        })


        Handler().postDelayed({
            Log.d("SliderRequest", "addressDevice = $addressDevice parameterID = $parameterID")
            main.bleCommandWithQueue(BLECommands.requestSlider(addressDevice[0], parameterID[0]), MAIN_CHANNEL, SampleGattAttributes.WRITE){}
        }, 500)
    }
    private fun sliderCollect() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                slidersFlow.collect { parameterRef ->
                    setUI(parameterRef)
                }
            }
        }
    }


    private fun setUI(parameterRef: ParameterRef) {
        val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
        // в parameterRef прилетают addressDevice и parameterID для того слайдера, который нужно обновить
        // а в Array widgetSlidersInfo хранится список всех сочетаний адресов девайсов
        // и айдишников параметров вместе с их вьюхами для изменения
        Log.d ("parameter sliderCollect", "перед попыткой добавить данные в слайдер  addressDevice = ${parameterRef.addressDevice}  parameterID = ${parameterRef.parameterID}  parameter.data = ${parameter.data}}")
        val indexWidgetSlider = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
        if (indexWidgetSlider != -1 && indexWidgetSlider < widgetSlidersInfo.size) {
            if (parameter.data=="") Log.d ("parameter sliderCollect", "не успешная попытка обновления")
            if (parameter.data!="") Log.d ("parameter sliderCollect", "успешная попытка обновления")

            val sizeOf = PreferenceKeysUBI4.ParameterTypeEnum.entries[parameter.type].sizeOf
            widgetSlidersInfo[indexWidgetSlider].dataOffset.forEachIndexed { index, it ->
                if (parameter.data=="") "" else widgetSlidersInfo[indexWidgetSlider].progress[index] = castUnsignedCharToInt(parameter.data.substring((sizeOf*it)*2, sizeOf*(it+1)*2).toInt(16).toByte())
                widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index].progress = widgetSlidersInfo[indexWidgetSlider].progress[index]
                widgetSlidersInfo[indexWidgetSlider].widgetSliderNumTv[index].text = widgetSlidersInfo[indexWidgetSlider].progress[index].toString()
            }
        } else {
            Log.d ("parameter sliderCollect", "НЕТ слайдера, которому передназначаются данные")
            Log.d ("parameter sliderCollect", "данные для addressDevice = ${parameterRef.addressDevice}  parameterID = ${parameterRef.parameterID}")
            widgetSlidersInfo.forEach {
                Log.d ("parameter sliderCollect", "есть только такие addressDevice = ${it.addressDevice}  parameterID = ${it.parameterID}")
            }
        }
    }
    private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
        //TODO не корректно работает фугкция поиска виджета по адресам и айдишникам
        widgetSlidersInfo.forEachIndexed { index, widgetSliderInfo ->
            if (widgetSliderInfo.addressDevice[0] == addressDevice && widgetSliderInfo.parameterID[0] == parameterID) {
                return index
            }
        }
        return -1
    }
    override fun isForViewType(item: Any): Boolean = item is SliderItem
    override fun SliderItem.getItemId(): Any = title
    fun onDestroy() {
        scope.cancel()
        Log.d("onDestroy" , "onDestroy slider")
    }
}

data class WidgetSliderInfo (
    var addressDevice: ArrayList<Int> = ArrayList(),
    var parameterID: ArrayList<Int> = ArrayList(),
    var dataOffset: ArrayList<Int> = ArrayList(),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var progress: ArrayList<Int> = ArrayList(),
    var widgetSlidersSb: ArrayList<ProgressBar>,
    var widgetSliderNumTv: ArrayList<TextView>,
)
