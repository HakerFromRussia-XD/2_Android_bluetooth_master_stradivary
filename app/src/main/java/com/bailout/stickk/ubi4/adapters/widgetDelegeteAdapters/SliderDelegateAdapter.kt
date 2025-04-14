package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import com.bailout.stickk.ubi4.data.state.WidgetState.slidersFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.widgets.SliderItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class SliderDelegateAdapter(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: ArrayList<Int>) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItem) {
        Log.d("SliderAdapterTest", "onBind RUN")
        onDestroyParent { onDestroy() }

        var addressDevice = 0
        var parameterID = 0
        var dataCode = 0
        val dataOffset: ArrayList<Int> = ArrayList()
        var minProgress = 0
        var maxProgress = 0
        var widgetPosition = 0

        when (val widget = item.widget) {
            is SliderParameterWidgetEStruct -> {
                // Берем данные из первого элемента набора параметров
                addressDevice = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).parameterID

                dataCode = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).dataCode
                // Собираем данные для всех параметров (например, dataOffset и dataCode)
                widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    dataOffset.add(it.dataOffset)

                }
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                widgetPosition = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetPosition

                Log.d(
                    "addressDevice",
                    "E struct: addressDevice = $addressDevice   ${widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId}"
                )
            }
            is SliderParameterWidgetSStruct -> {
                addressDevice = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).parameterID
                dataCode = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).dataCode
                widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    dataOffset.add(it.dataOffset)
                }
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress

                Log.d(
                    "addressDevice",
                    "S struct: addressDevice = $addressDevice   ${widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId}"
                )
            }
        }

        // Количество параметров определяем количеством dataOffset
        val paramCount = dataOffset.size
        val initialProgress = MutableList(paramCount) { 0 }
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
        widgetSlidersInfo.sortBy { it.widgetPosition }
        widgetSlidersInfo.add(currentSliderInfo)

        sliderCollect()

        // Получаем индекс текущего виджета по значению device и parameter
        val indexWidgetSlider = getIndexWidgetSlider(addressDevice, parameterID)
        val range = if (maxProgress == minProgress) 100 else maxProgress - minProgress

        // Настраиваем слайдеры: если параметров больше одного, показываем второй слайдер
        widgetSliderSb.max = range
        if (paramCount > 1) {
            widgetSlider2Sb.max = range
            secondSliderCl.visibility = View.VISIBLE
            widgetSlider2Sb.progress = currentSliderInfo.progress[1]
            widgetSliderNum2Tv.text = currentSliderInfo.progress[1].toString()
        } else {
            secondSliderCl.visibility = View.GONE
        }

        widgetSliderSb.progress = currentSliderInfo.progress[0]
        widgetSliderNumTv.text = currentSliderInfo.progress[0].toString()
        widgetSliderTitleTv.text = item.title




        // Обработчик первого слайдера
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                widgetSlidersInfo[indexWidgetSlider].progress[0] = seekBar.progress
                onSetProgress(addressDevice, parameterID, widgetSlidersInfo[indexWidgetSlider].progress)
            }
        })

        // Обработчик второго слайдера (если доступен)
        widgetSlider2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNum2Tv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                widgetSlidersInfo[indexWidgetSlider].progress[1] = seekBar.progress
                onSetProgress(addressDevice, parameterID, widgetSlidersInfo[indexWidgetSlider].progress)
            }
        })

        // Кнопки инкремента и декремента для каждого слайдера
        minusBtnRipple.setOnClickListener {
            updateSliderProgress(widgetPosition, sliderIndex = 0, step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple.setOnClickListener {
            updateSliderProgress(widgetPosition, sliderIndex = 0, step = +1, indexWidgetSlider = indexWidgetSlider)
        }
        minusBtnRipple2.setOnClickListener {
            updateSliderProgress(widgetPosition, sliderIndex = 1, step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple2.setOnClickListener {
            updateSliderProgress(widgetPosition, sliderIndex = 1, step = +1, indexWidgetSlider = indexWidgetSlider)
        }

        currentSliderInfo.responseReceived.set(false)
        if (RetryUtils.canSendRequestWithFirstReceiveDataFlag(addressDevice, parameterID)){
            RetryUtils.sendRequestWithRetry(
                request = {
                    Log.d("SliderRequest", "addressDevice = $addressDevice, parameterID = $parameterID")
                    main.bleCommandWithQueue(
                        BLECommands.requestSlider(addressDevice, parameterID),
                        MAIN_CHANNEL,
                        SampleGattAttributes.WRITE
                    ) {}
                },
                isResponseReceived = { currentSliderInfo.responseReceived.get() },
                maxRetries = 5,
                delayMillis = 400L
            )
            Log.d("RequestUtils", "ВЕТКА IF Запрос не выполнен: firstReceiveDataFlag true parameterData = ${ParameterProvider.getParameter(addressDevice,parameterID).data} deviceAddress = $addressDevice, parameterId = $parameterID")

        }
        else {
            setUI(ParameterRef(addressDevice,parameterID, dataCode))
            Log.d("RequestUtils", "ВЕТКА ELSE Запрос не выполнен: firstReceiveDataFlag false! parameterData = ${ParameterProvider.getParameter(addressDevice,parameterID).data} deviceAddress = $addressDevice, parameterId = $parameterID")
        }

    }

    private fun updateSliderProgress(widgetPosition: Int, sliderIndex: Int, step: Int, indexWidgetSlider: Int) {
        val sliderInfo = widgetSlidersInfo.find { it.widgetPosition == widgetPosition }
        if (sliderInfo == null) {
            Log.e("updateSliderProgress", "Не найден sliderInfo для widgetPosition = $widgetPosition")
            return
        }
        val currentValue = sliderInfo.progress.getOrNull(sliderIndex)
        if (currentValue == null) {
            Log.e("updateSliderProgress", "Нет значения progress для sliderIndex = $sliderIndex")
            return
        }
        var newValue = currentValue + step
        val minProgress = sliderInfo.minProgress
        val effectiveMax = if (minProgress == sliderInfo.maxProgress) 100 else sliderInfo.maxProgress
        newValue = newValue.coerceIn(minProgress, effectiveMax)
        sliderInfo.progress[sliderIndex] = newValue
        sliderInfo.widgetSlidersSb.getOrNull(sliderIndex)?.progress = newValue - minProgress
        sliderInfo.widgetSliderNumTv.getOrNull(sliderIndex)?.text = newValue.toString()

        // Используем одни и те же значения для addressDevice и parameterID
        onSetProgress(sliderInfo.addressDevice, sliderInfo.parameterID, sliderInfo.progress)
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
        Log.d("setUITest", "ParameterRef = $parameterRef, parameter = $parameter")
        Log.d("parameter sliderCollect", "перед обновлением слайдера: addressDevice = ${parameterRef.addressDevice}, parameterID = ${parameterRef.parameterID}, data=${parameter.data}")

        val indexWidgetSlider = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
        if (indexWidgetSlider != -1 && indexWidgetSlider < widgetSlidersInfo.size) {
            try {
                val sizeOf = PreferenceKeysUBI4.ParameterTypeEnum.entries[parameter.type].sizeOf
                widgetSlidersInfo[indexWidgetSlider].dataOffset.forEachIndexed { index, it ->
                    Log.d("SliderDebug", "Слайдер[$index]: sizeOf=$sizeOf, data.length=${parameter.data.length}")
                    if (parameter.data.isNotEmpty()) {
                        val oldProgress = widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index].progress
                        val newValue = castUnsignedCharToInt(
                            parameter.data.substring((sizeOf * it) * 2, sizeOf * (it + 1) * 2).toInt(16).toByte()
                        )
                        widgetSlidersInfo[indexWidgetSlider].progress[index] = newValue
                        animateProgressBar(widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index], oldProgress, newValue)
                        widgetSlidersInfo[indexWidgetSlider].widgetSliderNumTv[index].text = newValue.toString()
                    }
                    // Обновляем отображение
                    widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb[index].progress =
                        widgetSlidersInfo[indexWidgetSlider].progress[index]
                    widgetSlidersInfo[indexWidgetSlider].widgetSliderNumTv[index].text =
                        widgetSlidersInfo[indexWidgetSlider].progress[index].toString()
                }
            } catch (e: Exception) {
                Log.e("SliderDebug", "Ошибка при обработке данных: ${e.message}", e)
            } finally {
                widgetSlidersInfo[indexWidgetSlider].responseReceived.set(true)
                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.forEach { it?.cancel() }
                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.clear()
                Log.d("SliderDebug", "Установлен флаг responseReceived=true для слайдера с индексом $indexWidgetSlider")
            }
        } else {
            Log.d("parameter sliderCollect", "НЕТ слайдера для параметров: addressDevice = ${parameterRef.addressDevice}, parameterID = ${parameterRef.parameterID}")
            widgetSlidersInfo.forEach {
                Log.d("parameter sliderCollect", "Существующий слайдер: addressDevice = ${it.addressDevice}, parameterID = ${it.parameterID}")
            }
        }
        Log.d("SliderDebug", "Received parameter.data = '${parameter.data}', длина = ${parameter.data.length}")
    }

    private fun animateProgressBar(progressBar: ProgressBar, from: Int, to: Int) {
        if (from == to) return
        ValueAnimator.ofInt(from, to).apply {
            duration = DURATION_ANIMATION
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }

    private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
        return widgetSlidersInfo.indexOfFirst { it.addressDevice == addressDevice && it.parameterID == parameterID }
    }

    override fun isForViewType(item: Any): Boolean = item is SliderItem
    override fun SliderItem.getItemId(): Any = title

    fun onDestroy() {
        Log.d("SliderAdapterTest", "onDestroy slider")
    }
}

data class WidgetSliderInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var dataOffset: ArrayList<Int> = ArrayList(),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var progress: ArrayList<Int> = ArrayList(),
    var widgetSlidersSb: ArrayList<ProgressBar>,
    var widgetSliderNumTv: ArrayList<TextView>,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ArrayList<ValueAnimator?> = ArrayList()
)