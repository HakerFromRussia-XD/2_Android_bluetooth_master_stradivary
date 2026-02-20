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
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderEMGGainFlowV3
import com.bailout.stickk.ubi4.data.state.WidgetState.slidersFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
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
import java.util.Locale
import kotlin.math.roundToInt



class SliderDelegateAdapterV3(
    val onSetProgress: (addressDevice: Int, parameterID: Int, progress: ArrayList<Int>) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SliderItemV3, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetSliderInfo> = ArrayList()
    private var indexWidgetSlider = 0
    private var sliderInfoCounter = 0
    private var timer: CountDownTimer? = null
    private var isAttached = false

    private var collectJob: kotlinx.coroutines.Job? = null

    private val responseReceived = AtomicBoolean(false)

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


        var addressDevice = 0
        var parameterID = 0
        val dataOffsets: ArrayList<Int> = ArrayList()
        var minProgress = 0
        var maxProgress = 100
        var widgetPosition = 0
        var increment = 1.0f


        when (val widget = item.widget) {
            is SliderParameterWidgetSStruct -> {
                addressDevice = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).deviceAddress
                parameterID = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0).parameterID
                widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    dataOffsets.add(it.dataOffsets)
                }
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                increment = widget.increment
            }
        }

        // Количество параметров определяем количеством dataOffset
        val paramCount = dataOffsets.size
        val initialProgress = MutableList(paramCount) { 0 }
        val currentSliderInfo = WidgetSliderInfo(
                addressDevice = addressDevice,
                parameterID = parameterID,
                dataOffsets = dataOffsets,
                minProgress = minProgress,
                maxProgress = maxProgress,
                increment = increment,
                progress = ArrayList(initialProgress),
                widgetSlidersSb = arrayListOf(widgetSliderSb, widgetSlider2Sb),
                widgetSliderNumTv = arrayListOf(widgetSliderNumTv, widgetSliderNum2Tv),
                widgetSliderUnitTv = arrayListOf(widgetSliderUnitTv, widgetSliderUnit2Tv),
                widgetPosition = widgetPosition
            )
        currentSliderInfo.instanceId = sliderInfoCounter++
        widgetSlidersInfo.removeAll {
            it.addressDevice == addressDevice && it.parameterID == parameterID
        }
        widgetSlidersInfo.add(currentSliderInfo)
        // Cache-first draw to avoid showing 0 if the event already arrived earlier
        sliderCollect()

        // Получаем индекс текущего виджета по значению device и parameter
        indexWidgetSlider = getIndexWidgetSlider(addressDevice, parameterID)
        val range = if (maxProgress == minProgress) 100 else maxProgress - minProgress

        // Настраиваем слайдеры: если параметров больше одного, показываем второй слайдер
        widgetSliderSb.max = range
        if (paramCount > 1) {
            widgetSlider2Sb.max = range
            secondSliderCl.visibility = View.VISIBLE

            widgetSlider2Sb.progress = 0
            widgetSliderNum2Tv.text = formatSliderValue(minProgress, increment)
            currentSliderInfo.progress[1] = minProgress

        } else {
            secondSliderCl.visibility = View.GONE
        }

        widgetSliderSb.progress = 0
        widgetSliderNumTv.text = formatSliderValue(minProgress, increment)
        currentSliderInfo.progress[0] = minProgress
        widgetSliderTitleTv.text = item.title

        // Обработчик первого слайдера
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                widgetSliderNumTv.text = formatSliderValue(seekBar.progress + info.minProgress, info.increment)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSetProgress(addressDevice, parameterID,  widgetSlidersInfo[indexWidgetSlider].progress )
            }
        })

        // Кнопки инкремента и декремента для каждого слайдера
        minusBtnRipple.setOnClickListener {
            updateSliderProgressWithStep( sliderIndex = 0, step = -1, indexWidgetSlider = indexWidgetSlider)
        }
        plusBtnRipple.setOnClickListener {
            updateSliderProgressWithStep( sliderIndex = 0, step = +1, indexWidgetSlider = indexWidgetSlider)
        }
    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            when(widgetSlidersInfo[indexWidgetSlider].parameterID) {
                PDCE_EMG_CH_1_3_VAL.number -> {
                    sliderEMGGainFlowV3.collect { emgGainResult ->
//                        if (widgetSlidersInfo[indexWidgetSlider].dataOffsets.elementAt(0) == 0) updateSliderProgressWithStep(0, emgGainResult.openGain)
//                        if (widgetSlidersInfo[indexWidgetSlider].dataOffsets.elementAt(0) == 1) updateSliderProgressWithStep(0, emgGainResult.closeGain)
                    }
                }
            }
            slidersFlow.collect { parameterRef ->
                val idx = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
                val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)


                val info = widgetSlidersInfo.getOrNull(idx)
                Log.d(
                    "SliderProbe",
                    "flow addr=${parameterRef.addressDevice} pid=${parameterRef.parameterID} idx=$idx " +
                            "params=${info?.dataOffsets?.size ?: -1} offsets=${info?.dataOffsets}"
                )

                Log.d("SliderDebugTest!", "2 - addressDevice = ${parameterRef.addressDevice}, parameterID = ${parameterRef.parameterID} parameterData = ${parameter.data}")
                Log.d("SliderFlow", "addr=${parameterRef.addressDevice}, pid=${parameterRef.parameterID}, data=${parameter.data}, hasIdx=${idx != -1}")
                if (idx != -1) setUI(parameterRef) else Log.d("SliderFlow", "skip update: adapter doesn't have widget for addr=${parameterRef.addressDevice}, pid=${parameterRef.parameterID}")
            }
        }
    }
    private fun updateSliderProgressWithStep(sliderIndex: Int, step: Int, indexWidgetSlider: Int) {
        val sliderInfo = widgetSlidersInfo[indexWidgetSlider]
        if (sliderInfo == null) {
            Log.e("updateSliderProgress", "Не найден sliderInfo")
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
        sliderInfo.widgetSliderNumTv.getOrNull(sliderIndex)?.text = formatSliderValue(newValue, sliderInfo.increment)
        timer?.cancel()
        timer = object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                onSetProgress(sliderInfo.addressDevice, sliderInfo.parameterID, sliderInfo.progress)
            }
        }.start()

    }
    private fun updateSliderProgress(sliderIndex: Int, newProgress: Int) {
//        val sliderInfo = widgetSlidersInfo[indexWidgetSlider]
//
//        sliderInfo.progress[sliderIndex] = newValue
//        sliderInfo.widgetSlidersSb.getOrNull(sliderIndex)?.progress = newValue - minProgress
//        sliderInfo.widgetSliderNumTv.getOrNull(sliderIndex)?.text = formatSliderValue(newValue, sliderInfo.increment)
    }
    private fun setUI(parameterRef: ParameterRef) {
//        val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
//
//        val indexWidgetSlider = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
//        if (indexWidgetSlider != -1 && indexWidgetSlider < widgetSlidersInfo.size) {
//            try {
//                animateProgressBar(info.widgetSlidersSb[index], oldProgress, newValue - info.minProgress)
//            } catch (e: Exception) {
//                Log.e("SliderDebug", "Ошибка при обработке данных: ${e.message}", e)
//            } finally {
//                widgetSlidersInfo[indexWidgetSlider].responseReceived.set(true)
//                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.forEach { it?.cancel() }
//                widgetSlidersInfo[indexWidgetSlider].loadingAnimators.clear()
//                Log.d("SliderDebug", "Установлен флаг responseReceived=true для слайдера с индексом $indexWidgetSlider")
//            }
//        } else {
//            Log.d("parameter sliderCollect", "НЕТ слайдера для: addr=${parameterRef.addressDevice}, pid=${parameterRef.parameterID}. Всего=${widgetSlidersInfo.size}")
//            widgetSlidersInfo.forEachIndexed { i, it ->
//                Log.d("SliderMap", "MISS setUI for addr=${parameterRef.addressDevice} pid=${parameterRef.parameterID}")
//                Log.d("parameter sliderCollect", "[$i] addr=${it.addressDevice}, pid=${it.parameterID}, pos=${it.widgetPosition}, min=${it.minProgress}, max=${it.maxProgress}")
//            }
//        }
//        Log.d("SliderDebug", "Received parameter.data = '${parameter.data}', длина = ${parameter.data.length}")
    }

    private fun animateProgressBar(progressBar: ProgressBar, from: Int, to: Int) {
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
            it.addressDevice == addressDevice && it.parameterID == parameterID
        }
        if (idx == -1) {
            Log.d("SliderMap", "Not found: addr=$addressDevice pid=$parameterID; listSize=${widgetSlidersInfo.size}")
        }
        return idx
    }

//    override fun isForViewType(item: Any): Boolean = item is SliderItemV3
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
}

data class WidgetSliderInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var dataOffsets: ArrayList<Int> = ArrayList(),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var increment: Float = 1.0f,
    var progress: ArrayList<Int> = ArrayList(),
    var widgetSlidersSb: ArrayList<ProgressBar>,
    var widgetSliderNumTv: ArrayList<TextView>,
    var widgetSliderUnitTv: ArrayList<TextView?>,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ArrayList<ValueAnimator?> = ArrayList()
)
