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
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: ArrayList<Int>) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()

    private val responseReceived = AtomicBoolean(false)
    private var sliderInfoCounter = 0


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        Log.d("SliderAdapterTest", "onBind RUN")
        onDestroyParent{ onDestroy() }
        val addressDevice: ArrayList<Int> = ArrayList()
        val parameterID: ArrayList<Int> = ArrayList()
        val dataCode: ArrayList<Int> = ArrayList()
        val dataOffset: ArrayList<Int> = ArrayList()
        var minProgress = 0
        var maxProgress = 0
        var widgetPosition = 0


        when (item.widget) {
            is SliderParameterWidgetEStruct -> {
                item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    addressDevice.add(it.deviceAddress)
                    parameterID.add(it.parameterID)
                    dataOffset.add(it.dataOffset)
                    dataCode.add(it.dataCode)
                }
                minProgress = item.widget.minProgress
                maxProgress = item.widget.maxProgress
                widgetPosition =
                    item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition
                Log.d(
                    "addressDevice",
                    "E struct addressDevice = $addressDevice   ${item.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId}"
                )
            }

            is SliderParameterWidgetSStruct -> {
                item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    addressDevice.add(it.deviceAddress)
                    parameterID.add(it.parameterID)
                    dataOffset.add(it.dataOffset)
                    dataCode.add(it.dataCode)
                }
                widgetPosition =
                    item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                minProgress = item.widget.minProgress
                maxProgress = item.widget.maxProgress
                Log.d(
                    "addressDevice",
                    "S struct addressDevice = $addressDevice   ${item.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId}"
                )
            }


        }


        val widgetPos = widgetPosition
        val initialProgress = MutableList(addressDevice.size) { 0 }
        val currentSliderInfo = WidgetSliderInfo(
            addressDevice = addressDevice,
            parameterID = parameterID,
            dataOffset = dataOffset,
            minProgress = minProgress,
            maxProgress = maxProgress,
            progress = ArrayList(initialProgress),
            widgetSlidersSb = arrayListOf(widgetSliderSb, widgetSlider2Sb),
            widgetSliderNumTv = arrayListOf(widgetSliderNumTv, widgetSliderNum2Tv),
            widgetPosition = widgetPosition
        )

        currentSliderInfo.instanceId = sliderInfoCounter++
        widgetSlidersInfo.removeAll { it.widgetPosition == widgetPosition }
        widgetSlidersInfo.sortBy { it.widgetPosition } // сортировка важна для соответствия порядка виджетов на экране и в этом списке
        widgetSlidersInfo.forEachIndexed { index, info ->
            Log.d("SliderAdapterTest", "widgetSlidersInfo ${info}")
            Log.d("SliderAdapter", "widgetSlidersInfo[$index] -> widgetPosition=${info.widgetPosition}")
        }


        widgetSlidersInfo.add(currentSliderInfo)
//        addOrReplaceWidgetInfo(currentSliderInfo)
//        widgetSlidersInfo.add(WidgetSliderInfo(addressDevice, parameterID, dataOffset, minProgress, maxProgress, arrayListOf(0, 0), arrayListOf(widgetSliderSb, widgetSlider2Sb), arrayListOf(widgetSliderNumTv,widgetSliderNum2Tv), widgetPosition))

        sliderCollect()
//
        val indexWidgetSlider = getIndexWidgetSlider(addressDevice[0], parameterID[0])
//        val progress: ArrayList<Int> = ArrayList(List(addressDevice.size) { 0 })
        val range = if (maxProgress == minProgress) 100 else maxProgress - minProgress
        widgetSliderSb.max = range
        if (addressDevice.size > 1) {
            widgetSlider2Sb.max = range
            secondSliderCl.visibility = View.VISIBLE
        } else {
            secondSliderCl.visibility = View.GONE
        }

        widgetSliderSb.progress = currentSliderInfo.progress[0]
        widgetSliderNumTv.text = currentSliderInfo.progress[0].toString()
        widgetSliderTitleTv.text = item.title

        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }
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

        minusBtnRipple.setOnClickListener {
            updateSliderProgress(widgetPos, sliderIndex = 0, step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple.setOnClickListener {
          widgetSlidersInfo[indexWidgetSlider].progress[0]

            updateSliderProgress(widgetPos, sliderIndex = 0, step = +1, indexWidgetSlider = indexWidgetSlider)

        }

        minusBtnRipple2.setOnClickListener {
            updateSliderProgress(widgetPos, sliderIndex = 1, step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple2.setOnClickListener {
          widgetSlidersInfo[indexWidgetSlider].progress[1]
            updateSliderProgress(widgetPos, sliderIndex = 1, step = +1, indexWidgetSlider = indexWidgetSlider)
        }

        currentSliderInfo.responseReceived.set(false)

        RetryUtils.sendRequestWithRetry(
            request = {
                Log.d("SliderRequest", "addressDevice = $addressDevice parameterID = $parameterID")
                main.bleCommandWithQueue(
                    BLECommands.requestSlider(addressDevice[0], parameterID[0]),
                    MAIN_CHANNEL,
                    SampleGattAttributes.WRITE
                ){}
            },
            isResponseReceived = {
                // Просто возвращаем значение нашего флага
                currentSliderInfo.responseReceived.get()
            },
            maxRetries = 5,
            delayMillis = 400L
        )

//                Handler().postDelayed({
//            main.bleCommandWithQueue(
//                BLECommands.requestSlider(addressDevice[0], parameterID[0]),
//                MAIN_CHANNEL,
//                SampleGattAttributes.WRITE){}
//        }, 500)
    }
    private fun updateSliderProgress(widgetPosition: Int, sliderIndex: Int, step: Int, indexWidgetSlider: Int) {
        val sliderInfo = widgetSlidersInfo.find { it?.widgetPosition == widgetPosition }
        if (sliderInfo == null) {
            Log.e("updateSliderProgress", "Не найден sliderInfo для widgetPosition = $widgetPosition")
            return
        }
        val currentValue = sliderInfo.progress.getOrNull(sliderIndex)
        if (currentValue == null) {
            Log.e("updateSliderProgress", "Нет значения progress для sliderIndex = $sliderIndex")
            return
        }
        var newValue = currentValue + step//.coerceIn(sliderInfo.minProgress, sliderInfo.maxProgress)

        val minProgress = sliderInfo.minProgress
        val effectiveMax = if (minProgress == sliderInfo.maxProgress) 100 else sliderInfo.maxProgress
        newValue = newValue.coerceIn(minProgress, effectiveMax)


        //обновляем слайдер
        sliderInfo.progress[sliderIndex] = newValue
        sliderInfo.widgetSlidersSb.getOrNull(sliderIndex)?.progress = newValue - minProgress
        sliderInfo.widgetSliderNumTv.getOrNull(sliderIndex)?.text = newValue.toString()
        val address = sliderInfo.addressDevice.getOrNull(sliderIndex)
        val paramId = sliderInfo.parameterID.getOrNull(sliderIndex)
        if (address == null || paramId == null) {
            Log.e("updateSliderProgress", "Нет данных для передачи: address или parameterID отсутствуют")
            return
        }
        // Сообщаем о новом значении BLE-устройству
        onSetProgress(address, paramId, sliderInfo.progress)
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

                ////ДЕБААЖИМ////
                val substringValue = parameter.data.substring((sizeOf*it)*2, sizeOf*(it+1)*2)
                Log.d("SliderDebug", "Parsed substring = $substringValue")
                val progressValue = castUnsignedCharToInt(substringValue.toInt(16).toByte())
                Log.d("SliderDebug", "Calculated progressValue = $progressValue")
            }
            widgetSlidersInfo[indexWidgetSlider].responseReceived.set(true)
        } else {
            Log.d ("parameter sliderCollect", "НЕТ слайдера, которому передназначаются данные")
            Log.d ("parameter sliderCollect", "данные для addressDevice = ${parameterRef.addressDevice}  parameterID = ${parameterRef.parameterID}")
            widgetSlidersInfo.forEach {
                Log.d ("parameter sliderCollect", "есть только такие addressDevice = ${it.addressDevice}  parameterID = ${it.parameterID}")
            }

        }

        Log.d("SliderDebug", "Received parameter.data = '${parameter.data}', длина = ${parameter.data.length}")
    }
//    private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
//        //TODO не корректно работает фугкция поиска виджета по адресам и айдишникам
//        widgetSlidersInfo.forEachIndexed { index, widgetSliderInfo ->
//            if (widgetSliderInfo.addressDevice[0] == addressDevice && widgetSliderInfo.parameterID[0] == parameterID) {
//                return index
//            }
//        }
//        return -1
//    }

    private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
        Log.d("SliderAdapterTest", "getIndexWidgetSlider run")
        widgetSlidersInfo.forEachIndexed { index, sliderInfo ->
            sliderInfo.addressDevice.forEachIndexed { i, dev ->
                if (dev == addressDevice && sliderInfo.parameterID[i] == parameterID) {
                    return index
                }
            }
        }
        return -1
    }

//    private fun addOrReplaceWidgetInfo(newWidgetInfo: WidgetSliderInfo) {
//        // Ищем запись с тем же набором адресов и параметров
//        val indexOfExisting = widgetSlidersInfo.indexOfFirst {
//            it.addressDevice == newWidgetInfo.addressDevice && it.parameterID == newWidgetInfo.parameterID
//        }
//        if (indexOfExisting != -1) {
//            // Если нашли – заменяем её
//            widgetSlidersInfo[indexOfExisting] = newWidgetInfo
//        } else {
//            // Если не нашли – добавляем новую
//            widgetSlidersInfo.add(newWidgetInfo)
//        }
//        // Если порядок нужен для отрисовки (сортировка по widgetPosition)
//        widgetSlidersInfo.sortBy { it.widgetPosition }
//    }


    override fun isForViewType(item: Any): Boolean = item is SliderItem
    override fun SliderItem.getItemId(): Any = title

    fun onDestroy() {
        scope.cancel()
        Log.d("SliderAdapterTest" , "onDestroy slider")
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
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false)
)
