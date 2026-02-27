package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.EMGGainResult
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import java.util.concurrent.atomic.AtomicBoolean
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterDataCodeEnum.*
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.roundToInt



class SliderDelegateAdapterV3(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SliderItemV3, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {
    private companion object {
        val requestedOnFirstShow = AtomicBoolean(false)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()
    private var indexWidgetSlider = 0
    private var sliderInfoCounter = 0
    private var timer: CountDownTimer? = null
    private var isAttached = false

    private var collectJob: kotlinx.coroutines.Job? = null


    private fun formatSliderValue(value: Int, increment: Float): String {
        val result = value * increment

        // Режим множителя (increment >= 1.0) -> 0 знаков
        if (increment >= 1.0f) {
            return result.toInt().toString()
        }
        // Режим делителя (increment < 1.0)
        val divisor = (1.0f / increment).roundToInt()
        val pattern = when (divisor) {
            2, 5, 10 -> "%.1f"
            else -> "%.2f"
        }

        return String.format(Locale.US, pattern, result)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItemV3) {
        Log.d("[Ubi4WidgetSliderBinding]", "работает SliderDelegateAdapterV3")
        onDestroyParent { onDestroy() }
        isAttached = true
        widgetSliderUnitTv?.text = ""
        widgetSliderUnitTv?.visibility = View.GONE
        widgetSliderUnit2Tv?.text = ""
        widgetSliderUnit2Tv?.visibility = View.GONE


        var parameterInfo: ParameterInfo<Int, Int, Int, Int>? = null
        var minProgress = 0
        var maxProgress = 100
        var widgetPosition = 0
        var increment = 1.0f


        when (val widget = item.widget) {
            is SliderParameterWidgetSStruct -> {
                parameterInfo = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0)
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                increment = widget.increment
            }
        }
        val currentParameterInfo = parameterInfo ?: return

        val currentSliderInfo = WidgetSliderInfo(
            parameterInfo = currentParameterInfo,
            minProgress = minProgress,
            maxProgress = maxProgress,
            increment = increment,
            progress = 0,
            widgetSlidersSb = widgetSliderSb,
            widgetSliderNumTv = widgetSliderNumTv,
            widgetSliderUnitTv = widgetSliderUnitTv,
            widgetPosition = widgetPosition
        )
        currentSliderInfo.instanceId = sliderInfoCounter++

        widgetSlidersInfo.removeAll {
            it.parameterInfo.deviceAddress == currentParameterInfo.deviceAddress && it.parameterInfo.parameterID == currentParameterInfo.parameterID
        }
        widgetSlidersInfo.add(currentSliderInfo)

        // Получаем индекс текущего виджета по значению device и parameter
        platformLog("SliderDelegateAdapterV3", "getIndexWidgetSlider(parameterInfo.deviceAddress, parameterInfo.parameterID) count = ${widgetSlidersInfo.size} ")
        platformLog("SliderDelegateAdapterV3", "getIndexWidgetSlider = ${getIndexWidgetSlider(parameterInfo.deviceAddress, parameterInfo.parameterID)}")

        indexWidgetSlider = getIndexWidgetSlider(currentParameterInfo.deviceAddress, currentParameterInfo.parameterID)
        if (indexWidgetSlider == -1) {
            Log.d("SliderMap", "Skip bind: slider index not found for $currentParameterInfo")
            return
        }
        sliderCollect()
        val range = if (maxProgress == minProgress) 100 else maxProgress - minProgress

        // Настраиваем слайдеры: если параметров больше одного, показываем второй слайдер
        widgetSliderSb.max = range
        secondSliderCl.visibility = View.GONE
        widgetSliderSb.progress = 0
        widgetSliderNumTv.text = formatSliderValue(minProgress, increment)
        currentSliderInfo.progress = minProgress
        widgetSliderTitleTv.text = item.title

        // Обработчик первого слайдера
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                widgetSliderNumTv.text = formatSliderValue(seekBar.progress + info.minProgress, info.increment)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSetProgress(
                    currentParameterInfo.deviceAddress,
                    currentParameterInfo.parameterID,
                    widgetSlidersInfo[indexWidgetSlider].progress
                )
//                onSetProgress(parameterInfo.deviceAddress, parameterInfo.parameterID,  widgetSlidersInfo[indexWidgetSlider].progress )
            }
        })

        // Кнопки инкремента и декремента для каждого слайдера
        minusBtnRipple.setOnClickListener {
            updateSliderProgressWithStep( step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple.setOnClickListener {
            updateSliderProgressWithStep( step = +1, indexWidgetSlider = indexWidgetSlider)
        }

        run {
            //достаётся параметр соответствующий определённому слайдеру
            val cachedNow = ParameterProvider.getParameterV3(parameterInfo)
            //если данные в параметре пустые, то отправляем запрос
            if (cachedNow.data.isEmpty()) {
                currentSliderInfo.responseReceived.set(false)
                RetryUtils.sendRequestWithRetry(
                    //отправляется этот реквест с периодичностью 400 мс
                    //пока флаг responseReceived не станет true в месте приёма
                    request = {
                        Log.d("SliderDebugTest!", "=============================================")
                        Log.d("SliderDebugTest!", "parameterInfo = $parameterInfo")
                        main.bleCommandWithQueue(
                            BLECommandsV3.request(parameterInfo.parameterID, parameterInfo.dataCode),
                            MAIN_CHANNEL_CHARACTERISTIC,
                            WRITE
                        ) {}
                    },
                    isResponseReceived = { currentSliderInfo.responseReceived.get() },
                    maxRetries = 5,
                    delayMillis = 400L,
                    scope = scope
                )
                Log.d("RequestUtils", "Запрос отправлен: кэш пуст. command=${parameterInfo.parameterID}, subcommand=${parameterInfo.dataCode}")
            } else {
                // Данные уже есть – убеждаемся, что не будем ретраить зря
                currentSliderInfo.responseReceived.set(true)
            }
        }
//        initRequest()
    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            sliderFlowV3.collect { parameterInfo ->
                setUI(parameterInfo)
            }
//            when(widgetSlidersInfo[indexWidgetSlider].parameterInfo.parameterID) {
//            when(widgetSlidersInfo[0].parameterInfo.parameterID) {
//                PDCE_EMG_CH_1_3_VAL.number -> {
//                    sliderFlowV3.collect { parameterInfo ->
//                        val parameter = ParameterProvider.getParameterV3(parameterInfo)
//                        platformLog("baseSubDevicesInfoStructSet", "parameter.data 2 = ${parameter.data}")
//                        Json.decodeFromString<EMGGainResult>(parameter.data)
//                        platformLog("baseSubDevicesInfoStructSet", "EMGGainResult = ${Json.decodeFromString<EMGGainResult>(parameter.data)}")
//                        setUI(parameterInfo)
//                    }
//                }
//            }
        }
    }
    private fun updateSliderProgressWithStep(step: Int, indexWidgetSlider: Int) {
        val sliderInfo = widgetSlidersInfo[indexWidgetSlider]
        val currentValue = sliderInfo.progress
        var newValue = currentValue + step
        val minProgress = sliderInfo.minProgress
        val effectiveMax = if (minProgress == sliderInfo.maxProgress) 100 else sliderInfo.maxProgress
        newValue = newValue.coerceIn(minProgress, effectiveMax)
        sliderInfo.progress = newValue
        sliderInfo.widgetSlidersSb.progress = newValue - minProgress
        sliderInfo.widgetSliderNumTv.text = formatSliderValue(newValue, sliderInfo.increment)
        timer?.cancel()
        timer = object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                onSetProgress(
                    sliderInfo.parameterInfo.deviceAddress,
                    sliderInfo.parameterInfo.parameterID,
                    sliderInfo.progress
                )
            }
        }.start()

    }
    private fun updateSliderProgress(newProgress: Int) {
        val sliderInfo = widgetSlidersInfo[indexWidgetSlider]

        sliderInfo.progress = newProgress
        sliderInfo.widgetSlidersSb.progress = newProgress
        sliderInfo.widgetSliderNumTv.text = formatSliderValue(newProgress, sliderInfo.increment)
    }
    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        if (indexWidgetSlider != -1 && indexWidgetSlider < widgetSlidersInfo.size) {
            val parameter = ParameterProvider.getParameterV3(parameterInfo)
            val indexWidgetSlider = getIndexWidgetSlider(parameterInfo.deviceAddress, parameterInfo.parameterID)
            platformLog("animateProgressBar", "deviceAddress: ${parameterInfo.deviceAddress}  parameterID: ${parameterInfo.parameterID}")
            platformLog("animateProgressBar", "indexWidgetSlider: ${indexWidgetSlider}")
            val oldProgress = widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb.progress
            val newValue = Json.decodeFromString<EMGGainResult>(parameter.data)

            try {
                animateProgressBar(widgetSlidersInfo[indexWidgetSlider].widgetSlidersSb, oldProgress, newValue.openGain - widgetSlidersInfo[indexWidgetSlider].minProgress)
            } catch (e: Exception) {
                platformLog("SliderDebug", "Ошибка при обработке данных: ${e.message}")
            } finally {
                widgetSlidersInfo[indexWidgetSlider].responseReceived.set(true)
                widgetSlidersInfo[indexWidgetSlider].loadingAnimators?.cancel()
//                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.clear()
            }
        }
    }

    private fun animateProgressBar(progressBar: ProgressBar, from: Int, to: Int) {
        platformLog("animateProgressBar", "animateProgressBar from: $from  to: $to")
        if (from == to) return

        if (WidgetState.dbSnapshotAppliedWithCrc) {
            progressBar.progress = to
            return
        }

        ValueAnimator.ofInt(from, to).apply {
            duration = DURATION_ANIMATION
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }
    private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
        val idx = widgetSlidersInfo.indexOfFirst {
            it.parameterInfo.deviceAddress == addressDevice && it.parameterInfo.parameterID == parameterID
        }
        if (idx == -1) {
            Log.d("SliderMap", "Not found: addr=$addressDevice pid=$parameterID; listSize=${widgetSlidersInfo.size}")
        }
        return idx
    }

    override fun isForViewType(item: Any): Boolean =
        item is SliderItemV3 && (
                item.widget is SliderParameterWidgetEStruct ||
                        item.widget is SliderParameterWidgetSStruct
                )
    override fun SliderItemV3.getItemId(): Any = when (val w = widget) {
        is SliderParameterWidgetEStruct -> {
            val s = w.baseParameterWidgetEStruct.baseParameterWidgetStruct
            val addr = s.parameterInfoSet.elementAt(0).deviceAddress
            val pid = s.parameterInfoSet.elementAt(0).parameterID
            val pos = s.widgetPosition
            "slider-$addr-$pid-$pos"
        }
        is SliderParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val addr = s.parameterInfoSet.elementAt(0).deviceAddress
            val pid = s.parameterInfoSet.elementAt(0).parameterID
            val pos = s.widgetPosition
            "slider-$addr-$pid-$pos"
        }
        else -> title
    }

    fun onDestroy() {
        Log.d("SliderAdapterTest", "onDestroy slider")
        isAttached = false
        timer?.cancel()
        timer = null
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
    }
    private fun initRequest() {
        if (requestedOnFirstShow.compareAndSet(false, true)) {
            platformLog("initRequest","initRequest")
        }
    }
}

data class WidgetSliderInfo (
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0, 0, 0, 0),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var increment: Float = 1.0f,
    var progress: Int = 0,
    var widgetSlidersSb: ProgressBar,
    var widgetSliderNumTv: TextView,
    var widgetSliderUnitTv: TextView?,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ValueAnimator? = null
)
