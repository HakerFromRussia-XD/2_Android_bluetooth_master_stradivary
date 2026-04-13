package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import java.util.concurrent.atomic.AtomicBoolean
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.roundToInt



class SliderDelegateAdapterV3(
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SliderItemV3, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {
    private companion object {
        val requestedOnFirstShow = AtomicBoolean(false)
    }

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetInfoList: ArrayList<WidgetSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0
    private var timer: CountDownTimer? = null
    private var isAttached = false

    private var collectJob: kotlinx.coroutines.Job? = null
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value


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
//        Log.d("[Ubi4WidgetSliderBinding]", "работает SliderDelegateAdapterV3")
        onDestroyParent { onDestroy() }
        isAttached = true

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
            range = if (maxProgress == minProgress) 100 else maxProgress - minProgress,
            widgetSlidersSb = widgetSliderSb,
            widgetSliderNumTv = widgetSliderNumTv,
            widgetSliderUnitTv = widgetSliderUnitTv,
            minusBtnRipple = minusBtnRipple,
            plusBtnRipple = plusBtnRipple,
            minusBtnTv = minusBtnTv,
            plusBtnTv = plusBtnTv,
            widgetPosition = widgetPosition
        )
        currentSliderInfo.instanceId = sliderInfoCounter++
        widgetInfoList.add(currentSliderInfo)

        // Обработчик всех слайдеров
        widgetInfoList.forEach { infoWidget ->
            widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    widgetSliderNumTv.text = formatSliderValue(seekBar.progress + infoWidget.minProgress, infoWidget.increment)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (!isInteractionEnabled) return
                    infoWidget.progress = seekBar.progress
                    sendProgress(
                        infoWidget.parameterInfo,
                        infoWidget.progress
                    )
                }
            })

            // Кнопки инкремента и декремента для каждого слайдера
            minusBtnRipple.setOnClickListener {
                updateSliderProgressWithStep(step = -1, infoWidget = infoWidget)
            }
            plusBtnRipple.setOnClickListener {
                updateSliderProgressWithStep(step = +1, infoWidget = infoWidget)
            }
        }

        // Настраиваем слайдеры: если параметров больше одного, показываем второй слайдер
        widgetSliderSb.max = currentSliderInfo.range
        secondSliderCl.visibility = View.GONE
        widgetSliderSb.progress = 0
        widgetSliderNumTv.text = formatSliderValue(minProgress, increment)
        currentSliderInfo.progress = minProgress
        widgetSliderTitleTv.text = item.title
        applySliderLockState(currentSliderInfo)
        observeInteractionState()
        sliderCollect()
    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            sliderFlowV3.collect { setUI() }
        }
    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return
        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                if (!enabled) {
                    timer?.cancel()
                    timer = null
                }
                widgetInfoList.forEach { applySliderLockState(it) }
            }
        }
    }

    private fun applySliderLockState(infoWidget: WidgetSliderInfo) {
        val seekBar = infoWidget.widgetSlidersSb as? SeekBar ?: return
        val context = seekBar.context
        val trackRes = if (isInteractionEnabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled
        seekBar.progressDrawable = AppCompatResources.getDrawable(context, trackRes)?.mutate()
        seekBar.thumb = AppCompatResources.getDrawable(context, R.drawable.thumb_le)?.mutate()
        seekBar.isEnabled = isInteractionEnabled

        infoWidget.minusBtnRipple?.isClickable = isInteractionEnabled
        infoWidget.plusBtnRipple?.isClickable = isInteractionEnabled

        val colorRes = if (isInteractionEnabled) R.color.ubi4_white else R.color.ubi4_gray_border
        infoWidget.minusBtnTv?.setTextColor(context.getColor(colorRes))
        infoWidget.plusBtnTv?.setTextColor(context.getColor(colorRes))
    }

    private fun updateSliderProgressWithStep(step: Int, infoWidget: WidgetSliderInfo) {
        if (!isInteractionEnabled) return
//        val sliderInfo = widgetInfoList[indexWidgetSlider]
        val currentValue = infoWidget.progress
        var newValue = currentValue + step
        val minProgress = infoWidget.minProgress
        val effectiveMax = if (minProgress == infoWidget.maxProgress) 100 else infoWidget.maxProgress
        newValue = newValue.coerceIn(minProgress, effectiveMax)
        infoWidget.progress = newValue
        infoWidget.widgetSlidersSb.progress = newValue - minProgress
        infoWidget.widgetSliderNumTv.text = formatSliderValue(newValue, infoWidget.increment)
        timer?.cancel()
        timer = object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                sendProgress(
                    infoWidget.parameterInfo,
                    infoWidget.progress
                )
            }
        }.start()

    }
    private fun setUI() {
        // [new widgets V3] тут добавляем ветку расфасовки пришедших данных SliderDelegateAdapterV3 1
        widgetInfoList.forEach { infoWidget ->
            applySliderLockState(infoWidget)
            val parameter = ParameterProvider.getParameterV3(infoWidget.parameterInfo)
            platformLog("SliderDelegateAdapterV3", "setUI parameter.data: ${parameter.data}   parameterInfo: ${infoWidget.parameterInfo}")
            when (val subcommand = infoWidget.parameterInfo.dataCode) {
                ProsthesisModuleControlEnum.PWCE_SET_EMG_GAIN_VALUE.number.toInt() -> {
                    val emgGainResult = parseEmgGainResultSafely(parameter.data) ?: return
                    if (infoWidget.parameterInfo.dataOffsets == 0) {
                        infoWidget.progress = emgGainResult.openGain - infoWidget.minProgress
                        setProgressBar(
                            infoWidget,
                            emgGainResult.openGain - infoWidget.minProgress
                        )
                    }
                    if (infoWidget.parameterInfo.dataOffsets == 1) {
                        infoWidget.progress = emgGainResult.closeGain - infoWidget.minProgress
                        setProgressBar(
                            infoWidget,
                            emgGainResult.closeGain - infoWidget.minProgress
                        )
                    }
                }
                else -> {
                    main.showToast("В SliderDelegateAdapterV3 парсим неправильную сабкоманду $subcommand")
                    platformLog("SliderDelegateAdapterV3", "В SliderDelegateAdapterV3 парсим неправильную сабкоманду ${infoWidget.parameterInfo}")
                }
            }
        }
    }
    private fun setProgressBar(infoWidget: WidgetSliderInfo, to: Int) {
        try {
            animateProgressBar(infoWidget, to)
        } catch (_: Exception) { } finally {
//            indexWidgetSlidersArray.forEach { indexWidgetSlider ->
//                widgetInfoList[indexWidgetSlider].responseReceived.set(true)
//                widgetInfoList[indexWidgetSlider].loadingAnimators?.cancel()
//            }
        }
    }
    private fun animateProgressBar(infoWidget: WidgetSliderInfo, to: Int) {
        val progressBar = infoWidget.widgetSlidersSb
        if (WidgetState.dbSnapshotAppliedWithCrc) {
            infoWidget.pendingUiProgress = null
            infoWidget.loadingAnimators?.cancel()
            infoWidget.loadingAnimators = null
            progressBar.progress = to
            return
        }

        if (infoWidget.loadingAnimators?.isRunning == true) {
            infoWidget.pendingUiProgress = to
            return
        }

        startProgressAnimation(
            infoWidget = infoWidget,
            from = progressBar.progress,
            to = to
        )
    }

    private fun startProgressAnimation(infoWidget: WidgetSliderInfo, from: Int, to: Int) {
        if (from == to) {
            val pending = infoWidget.pendingUiProgress
            infoWidget.pendingUiProgress = null
            if (pending != null && pending != to) {
                startProgressAnimation(
                    infoWidget = infoWidget,
                    from = to,
                    to = pending
                )
            }
            return
        }

        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = DURATION_ANIMATION
        animator.addUpdateListener { animation ->
            infoWidget.widgetSlidersSb.progress = animation.animatedValue as Int
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (infoWidget.loadingAnimators === animation) {
                    infoWidget.loadingAnimators = null
                }

                val pending = infoWidget.pendingUiProgress
                infoWidget.pendingUiProgress = null
                if (pending != null && pending != infoWidget.widgetSlidersSb.progress) {
                    startProgressAnimation(
                        infoWidget = infoWidget,
                        from = infoWidget.widgetSlidersSb.progress,
                        to = pending
                    )
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                if (infoWidget.loadingAnimators === animation) {
                    infoWidget.loadingAnimators = null
                }
            }
        })

        infoWidget.loadingAnimators = animator
        animator.start()
    }
    private fun sendProgress(parameterInfo: ParameterInfo<Int, Int, Int, Int>, progress: Int) {
        // [new widgets V3] тут добавляем ветку отправки новых команд SliderDelegateAdapterV3 2
        val subcommand = parameterInfo.dataCode
        platformLog("sendProgress", "subcommand: ${parameterInfo} сравниваем с ${ProsthesisModuleControlEnum.PWCE_GET_EMG_GAIN_VALUE.number.toInt()}")
        when (subcommand) {
            ProsthesisModuleControlEnum.PWCE_SET_EMG_GAIN_VALUE.number.toInt() -> {
                val parameter = ParameterProvider.getParameterV3(parameterInfo)
                val emgGainResult = parseEmgGainResultSafely(parameter.data) ?: EMGGainsV3()
                if (parameterInfo.dataOffsets == 0) { emgGainResult.openGain = progress }
                if (parameterInfo.dataOffsets == 1) { emgGainResult.closeGain = progress }
                parameter.data = json.encodeToString(emgGainResult)
                main.bleCommandWithQueue(
                    BLECommandsV3.sendGaines(emgGainResult.openGain, emgGainResult.closeGain),
                    SERIALPORTCHAR_UUID, WRITE){}
                platformLog("sendProgress", "parameter.data: ${parameter.data}")
            }
        }
    }

    private fun parseEmgGainResultSafely(data: String): EMGGainsV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<EMGGainsV3>(data) }
            .onFailure { platformLog("SliderDelegateAdapterV3", "Failed to decode EMGGainResult: ${it.message}") }
            .getOrNull()
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
        widgetInfoList.forEach { info ->
            info.loadingAnimators?.cancel()
            info.loadingAnimators = null
            info.pendingUiProgress = null
        }
        widgetInfoList.clear()
        sliderInfoCounter = 0
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
        interactionJob?.cancel()
        interactionJob = null
    }
}

data class WidgetSliderInfo (
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0, 0, 0, 0),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var increment: Float = 1.0f,
    var progress: Int = 0,
    var range: Int = 0,
    var widgetSlidersSb: ProgressBar,
    var widgetSliderNumTv: TextView,
    var widgetSliderUnitTv: TextView?,
    var minusBtnRipple: View? = null,
    var plusBtnRipple: View? = null,
    var minusBtnTv: TextView? = null,
    var plusBtnTv: TextView? = null,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ValueAnimator? = null,
    var pendingUiProgress: Int? = null
)
