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
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.parser.ParameterCodecActionV3
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
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
import java.util.Locale
import kotlin.math.roundToInt
class SliderDelegateAdapterV3(
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<SliderItemV3, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {
    private companion object {
        val requestedOnFirstShow = AtomicBoolean(false)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetInfoList: ArrayList<WidgetSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0
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
            widgetPosition = widgetPosition
        )
        currentSliderInfo.instanceId = sliderInfoCounter++
        widgetInfoList.removeAll {
            it.parameterInfo.deviceAddress == currentSliderInfo.parameterInfo.deviceAddress &&
                it.parameterInfo.parameterID == currentSliderInfo.parameterInfo.parameterID &&
                it.parameterInfo.dataCode == currentSliderInfo.parameterInfo.dataCode &&
                it.parameterInfo.dataOffsets == currentSliderInfo.parameterInfo.dataOffsets &&
                it.widgetPosition == currentSliderInfo.widgetPosition
        }
        widgetInfoList.add(currentSliderInfo)

        val infoWidget = currentSliderInfo
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widgetSliderNumTv.text = formatSliderValue(
                    seekBar.progress + infoWidget.minProgress,
                    infoWidget.increment
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val uiProgress = seekBar.progress.coerceIn(0, infoWidget.range)
                val absoluteProgress = uiProgress + infoWidget.minProgress
                infoWidget.progress = absoluteProgress
                sendProgress(infoWidget.parameterInfo, absoluteProgress)
            }
        })

        minusBtnRipple.setOnClickListener {
            updateSliderProgressWithStep(step = -1, infoWidget = infoWidget)
        }
        plusBtnRipple.setOnClickListener {
            updateSliderProgressWithStep(step = +1, infoWidget = infoWidget)
        }

        // Настраиваем слайдеры: если параметров больше одного, показываем второй слайдер
        widgetSliderSb.max = currentSliderInfo.range
        secondSliderCl.visibility = View.GONE
        widgetSliderSb.progress = 0
        widgetSliderNumTv.text = formatSliderValue(minProgress, increment)
        currentSliderInfo.progress = minProgress
        widgetSliderTitleTv.text = item.title
        sliderCollect()
        setUI(currentParameterInfo, withAnimation = false, widgetPosition = currentSliderInfo.widgetPosition)
    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            ParameterStoreV3.updates.collect { key ->
                widgetInfoList.forEach { infoWidget ->
                    if (ParameterStoreV3.toKey(infoWidget.parameterInfo) == key) {
                        setUI(infoWidget.parameterInfo)
                    }
                }
            }
        }
    }
    private fun updateSliderProgressWithStep(step: Int, infoWidget: WidgetSliderInfo) {
//        val sliderInfo = widgetInfoList[indexWidgetSlider]
        val currentValue = infoWidget.progress
        var newValue = currentValue + step
        val minProgress = infoWidget.minProgress
        val effectiveMax = if (minProgress == infoWidget.maxProgress) 100 else infoWidget.maxProgress
        newValue = newValue.coerceIn(minProgress, effectiveMax)
        infoWidget.progress = newValue
        infoWidget.widgetSlidersSb.progress = newValue - minProgress
        infoWidget.widgetSliderNumTv.text = formatSliderValue(newValue, infoWidget.increment)
        infoWidget.timer?.cancel()
        infoWidget.timer = object : CountDownTimer(300, 300) {
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
    private fun setUI(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>? = null,
        withAnimation: Boolean = true,
        widgetPosition: Int? = null
    ) {
        widgetInfoList.forEach { infoWidget ->
            val sameWidget = parameterInfo == null ||
                (
                    infoWidget.parameterInfo.deviceAddress == parameterInfo.deviceAddress &&
                        infoWidget.parameterInfo.parameterID == parameterInfo.parameterID &&
                        infoWidget.parameterInfo.dataCode == parameterInfo.dataCode &&
                        (widgetPosition == null || infoWidget.widgetPosition == widgetPosition)
                    )
            if (!sameWidget) return@forEach

            val parameterMeta = ParameterInfoRegistry.getMeta(infoWidget.parameterInfo) ?: return@forEach
            val typedValue = ParameterStoreV3.get(infoWidget.parameterInfo)
                ?: run {
                    val serialized = ParameterProvider.getParameterV3(infoWidget.parameterInfo).data
                    ParameterCodecRegistryV3.decodeFromSerialized(parameterMeta.codecId, serialized)
                }
            platformLog(
                "SliderDelegateAdapterV3",
                "setUI parameterInfo: ${infoWidget.parameterInfo}"
            )
            when (val value = typedValue) {
                is ParameterTypedValueV3.EmgGains -> {
                    val emgGainResult = value.value
                    if (infoWidget.parameterInfo.dataOffsets == 0) {
                        infoWidget.progress = emgGainResult.openGain
                        setProgressBar(
                            infoWidget,
                            emgGainResult.openGain - infoWidget.minProgress,
                            withAnimation
                        )
                    }
                    if (infoWidget.parameterInfo.dataOffsets == 1) {
                        infoWidget.progress = emgGainResult.closeGain
                        setProgressBar(
                            infoWidget,
                            emgGainResult.closeGain - infoWidget.minProgress,
                            withAnimation
                        )
                    }
                }
                is ParameterTypedValueV3.Slider -> {
                    val sliderValue = value.value.sliderValue
                    infoWidget.progress = sliderValue
                    setProgressBar(
                        infoWidget,
                        (sliderValue - infoWidget.minProgress).coerceIn(0, infoWidget.range),
                        withAnimation
                    )
                }
                else -> return@forEach
            }
        }
    }
    private fun setProgressBar(infoWidget: WidgetSliderInfo, to: Int, withAnimation: Boolean) {
        try {
            if (withAnimation) {
                animateProgressBar(infoWidget, to)
            } else {
                infoWidget.pendingUiProgress = null
                infoWidget.loadingAnimators?.cancel()
                infoWidget.loadingAnimators = null
                infoWidget.widgetSlidersSb.progress = to
            }
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
        val parameterMeta = ParameterInfoRegistry.getMeta(parameterInfo)
        if (parameterMeta == null) {
            main.showToast("В SliderDelegateAdapterV3 нет метаданных параметра")
            platformLog("SliderDelegateAdapterV3", "Нет метаданных для $parameterInfo")
            return
        }
        val currentTyped = ParameterStoreV3.get(parameterInfo)
            ?: run {
                val serialized = ParameterProvider.getParameterV3(parameterInfo).data
                ParameterCodecRegistryV3.decodeFromSerialized(parameterMeta.codecId, serialized)
            }
        val encodedAction = ParameterCodecRegistryV3.encodeAction(
            codecId = parameterMeta.codecId,
            currentValue = currentTyped,
            action = ParameterCodecActionV3.SetInt(
                value = progress,
                dataOffset = parameterInfo.dataOffsets
            )
        ) ?: return

        when (encodedAction) {
            is com.bailout.stickk.ubi4.data.parser.ParameterEncodedActionV3.EmgGainsValue -> {
                val emgGainResult = EMGGainsV3(
                    openGain = encodedAction.openGain,
                    closeGain = encodedAction.closeGain
                )
                val newTyped = ParameterTypedValueV3.EmgGains(emgGainResult)
                ParameterStoreV3.put(parameterInfo, newTyped)
                ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, newTyped)?.let { encoded ->
                    ParameterProvider.getParameterV3(parameterInfo).data = encoded
                }
                main.bleCommandWithQueue(
                    BLECommandsV3.sendGaines(emgGainResult.openGain, emgGainResult.closeGain),
                    SERIALPORTCHAR_UUID, WRITE){}
                platformLog("sendProgress", "emgGainResult: $emgGainResult")
            }
            is com.bailout.stickk.ubi4.data.parser.ParameterEncodedActionV3.IntValue -> {
                val sliderValue = encodedAction.value
                val newTyped = ParameterTypedValueV3.Slider(SliderV3(sliderValue = sliderValue))
                ParameterStoreV3.put(parameterInfo, newTyped)
                ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, newTyped)?.let { encoded ->
                    ParameterProvider.getParameterV3(parameterInfo).data = encoded
                }
                main.bleCommandWithQueue(
                    BLECommandsV3.sendCommand(
                        command = parameterInfo.parameterID,
                        subcommand = parameterInfo.dataCode,
                        parameter = sliderValue
                    ),
                    SERIALPORTCHAR_UUID, WRITE){}
                platformLog("sendProgress", "sliderValue: $sliderValue")
            }
            else -> return
        }
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
        widgetInfoList.forEach { info ->
            info.timer?.cancel()
            info.timer = null
            info.loadingAnimators?.cancel()
            info.loadingAnimators = null
            info.pendingUiProgress = null
        }
        widgetInfoList.clear()
        sliderInfoCounter = 0
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
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
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ValueAnimator? = null,
    var pendingUiProgress: Int? = null,
    var timer: CountDownTimer? = null
)
