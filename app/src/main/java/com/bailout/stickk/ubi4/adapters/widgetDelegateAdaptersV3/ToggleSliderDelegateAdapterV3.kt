package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetToggleSliderBinding
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.forEach
import kotlin.math.roundToInt

class ToggleSliderDelegateAdapterV3(
//    private val onSetProgress: (addressDevice: Int, parameterID: Int, packedBytes: ArrayList<Int>) -> Unit,
    private val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<ToggleSliderItemV3, Ubi4WidgetToggleSliderBinding>(
    Ubi4WidgetToggleSliderBinding::inflate
) {

    private companion object {
        private const val PENDING_WINDOW_MS = 800L
    }

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetToggleSliderInfo> = ArrayList()
    private var indexWidgetSlidersArray = intArrayOf()
    private var sliderInfoCounter = 0
    private var timer: CountDownTimer? = null
    private var isAttached = false
    private var collectJob: kotlinx.coroutines.Job? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetToggleSliderBinding.onBind(item: ToggleSliderItemV3) {
        Log.d("ToggleSliderAdapter", "onBind RUN")
        onDestroyParent { onDestroy() }
        isAttached = true
        toggleSliderUnitTv.text = ""
        toggleSliderUnitTv.visibility = View.GONE
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE

        var parameterInfo: ParameterInfo<Int, Int, Int, Int>? = null
        var minProgress = 0
        var maxProgress = 0
        var widgetPosition = 0
        var increment = 1.0f

        when (val widget = item.widget) {
            is ToggleSliderParameterWidgetSStruct -> {
                parameterInfo = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0)
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                increment = widget.increment
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
            }

            else -> return
        }


        val currentParameterInfo = parameterInfo ?: return


        val currentSliderInfo = WidgetToggleSliderInfo(
            parameterInfo = currentParameterInfo,
            minProgress = minProgress,
            maxProgress = maxProgress,
            increment = increment,
//            progress = 0,
            progress = pack(minProgress.coerceIn(0, 127), enabled = true),
            widgetSlidersSb = toggleSliderSb,
            widgetSliderNumTv = toggleSliderNumTv,
            widgetSliderUnitTv = toggleSliderUnitTv,
            widgetPosition = widgetPosition,
            turnOffBtnIv = arrayListOf(toggleTurnOffBtnIv1, toggleTurnOffBtnIv2),
        )

        currentSliderInfo.instanceId = sliderInfoCounter++

        widgetSlidersInfo.add(currentSliderInfo)


        indexWidgetSlidersArray = getIndexWidgetSlider(currentParameterInfo.dataCode)
        sliderCollect()


        // setup UI
        val range = if (maxProgress == minProgress) 100 else maxProgress - minProgress
        toggleSliderSb.max = range
        toggleSliderTitleTv.text = item.title
        toggleSliderUnitTv.text = ""
        toggleSliderUnitTv.visibility = View.GONE
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE

        // cache-first draw
        val cached = ParameterProvider.getParameterV3(currentParameterInfo)
        if (cached.data.isNotEmpty()) setUI(currentParameterInfo)
        // первичная синхронизация текста с текущим progress
        indexWidgetSlidersArray.forEach { indexWidgetSlider ->
            val info = widgetSlidersInfo[indexWidgetSlider]
            val useInfinity0 = isInfinityLabel(info, 0)
            toggleSliderNumTv.text = formatValueForUi(
                toggleSliderSb.progress,
                info.minProgress,
                range,
                useInfinity0,
                info.increment
            )

            // seekbar 1
            toggleSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val info = widgetSlidersInfo[indexWidgetSlider]
                    val useInfinity = isInfinityLabel(info, 0)
                    toggleSliderNumTv.text = formatValueForUi(
                        progress,
                        info.minProgress,
                        range,
                        useInfinity,
                        info.increment
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val info = widgetSlidersInfo[indexWidgetSlider]
                    val parameter = ParameterProvider.getParameterV3(info.parameterInfo)
                    val oldProgress = info.progress
                    val enabled = unpackEnabled(oldProgress)
                    //TODO вернуть!
//                    if (!enabled) {
//                        val deviceValue = unpackValue(oldProgress)
//                        seekBar.progress = (deviceValue - info.minProgress).coerceIn(0, range)
//                        return
//                    }

                    val uiProgress = seekBar.progress.coerceIn(0, range)
                    val deviceValue = (uiProgress + info.minProgress).coerceIn(0, 127)
                    info.progress = pack(deviceValue, enabled = true)
                    markPending(info, 0, info.progress)
                    applyToggleVisuals(indexWidgetSlider)
                    sendToggleSlider(parameter.data)
//                        onSetProgress(addressDevice, parameterID, info.packedProgress)
                }
            })

            // +/-
            toggleMinusBtnRipple1.setOnClickListener {
                updateSliderProgress(
                    widgetPosition,
                    step = -1,
                    indexWidgetSlider = indexWidgetSlider
                )
            }
            togglePlusBtnRipple1.setOnClickListener {
                updateSliderProgress(
                    widgetPosition,
                    step = +1,
                    indexWidgetSlider = indexWidgetSlider
                )
            }
            toggleTurnOffBtnRipple1.setOnClickListener { toggleEnabled(parameterInfo.dataCode) }
            applyToggleVisuals(indexWidgetSlider)
        }
    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            sliderFlowV3.collect { parameterInfo -> setUI(parameterInfo) }
        }
    }


    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        val parameter = ParameterProvider.getParameterV3(parameterInfo)
        val indexWidgetSlidersArray = getIndexWidgetSlider(parameterInfo.dataCode)

        indexWidgetSlidersArray.forEach { indexWidgetSlider ->
            Log.d(
                "ToggleSliderDelegateAdapterV3",
                "setUI widgetSlidersInfo: ${widgetSlidersInfo[indexWidgetSlider].parameterInfo}"
            )

            val subcommand = parameterInfo.dataCode
            when (subcommand) {

                ProsthesisModuleControlEnum.PWCE_TEST_SWITCHER.number.toInt() -> {
                    val info = widgetSlidersInfo.getOrNull(indexWidgetSlider) ?: return@forEach
                    val seekBar = info.widgetSlidersSb as? SeekBar ?: return@forEach
                    val textView = info.widgetSliderNumTv

                    try {
                        val hex = parameter.data
                        if (hex.isNullOrEmpty()) return@forEach

                        info.responseReceived.set(true)

                        val sizeOf = PreferenceKeysUbi4.ParameterTypeEnum.entries
                            .getOrNull(parameter.type)
                            ?.sizeOf
                            ?: return@forEach

                        val oldProgress = seekBar.progress
                        val range = (info.maxProgress - info.minProgress)
                            .coerceAtLeast(0)
                            .coerceAtMost(127)

                        fun readByteFromHex(hex: String, byteIndex: Int): Int? {
                            val start = byteIndex * 2
                            val end = start + 2
                            if (start < 0 || end > hex.length) return null
                            return runCatching {
                                hex.substring(start, end).toInt(16) and 0xFF
                            }.getOrNull()
                        }


                        val elementStartByte = parameterInfo.dataOffsets * sizeOf
                        val packedByteIndex = elementStartByte

                        val packedFromDevice = readByteFromHex(hex, packedByteIndex) ?: return@forEach

                        if (!shouldApplyDevicePacked(info, 0, packedFromDevice)) return@forEach

                        val enabled = unpackEnabled(packedFromDevice)
                        val deviceValue = unpackValue(packedFromDevice).coerceIn(0, 127)
                        val uiProgress = (deviceValue - info.minProgress).coerceIn(0, range)

                        info.progress = pack(deviceValue, enabled)

                        animateProgressBar(seekBar, oldProgress, uiProgress)
                        seekBar.progress = uiProgress

                        val useInfinity = isInfinityLabel(info, 0)
                        textView.text = formatValueForUi(
                            progress = uiProgress,
                            min = info.minProgress,
                            range = range,
                            useInfinity = useInfinity,
                            increment = info.increment
                        )

                        applyToggleVisuals(indexWidgetSlider)
                    } catch (e: Exception) {
                        Log.e("ToggleSliderV3", "setUI error: ${e.message}", e)
                    } finally {
                        info.loadingAnimators?.cancel()
                    }
                }
            }
        }
    }

    // ===== packed byte helpers: bit7=enabled, bits0..6=value(0..127) =====
    private fun unpackEnabled(packed: Int): Boolean = (packed and 0x80) != 0
    private fun unpackValue(packed: Int): Int = packed and 0x7F
    private fun pack(value0_127: Int, enabled: Boolean): Int {
        val v = value0_127.coerceIn(0, 127)
        return (if (enabled) 0x80 else 0x00) or (v and 0x7F)
    }

    private fun markPending(info: WidgetToggleSliderInfo, sliderIndex: Int, packed: Int) {
        if (sliderIndex !in 0..1) return
        val now = SystemClock.elapsedRealtime()
        info.pendingPacked[sliderIndex] = packed
        info.pendingUntilMs[sliderIndex] = now + PENDING_WINDOW_MS
    }

    private fun shouldApplyDevicePacked(
        info: WidgetToggleSliderInfo,
        sliderIndex: Int,
        packedFromDevice: Int
    ): Boolean {
        if (sliderIndex !in 0..1) return true

        val now = SystemClock.elapsedRealtime()
        val until = info.pendingUntilMs[sliderIndex]
        val pending = info.pendingPacked[sliderIndex]

        // Пока окно активно — не даём девайсу перезатереть UI любым другим значением
        if (now < until && pending != -1 && packedFromDevice != pending) return false

        // Пришло то же самое — считаем ACK и чистим pending
        if (pending != -1 && packedFromDevice == pending) {
            info.pendingPacked[sliderIndex] = -1
            info.pendingUntilMs[sliderIndex] = 0L
        }
        return true
    }

    private fun isInfinityLabel(info: WidgetToggleSliderInfo, sliderIndex: Int): Boolean {
        return sliderIndex in 0..1 && info.labelCodes[sliderIndex] == 9
    }

    private fun formatValueForUi(progress: Int, min: Int, range: Int, useInfinity: Boolean, increment: Float): String {
        if (useInfinity && range > 0 && progress >= range) return "∞"
        val result = (progress + min) * increment

        if (increment >= 1.0f) {
            return result.toInt().toString()
        }

        val divisor = (1.0f / increment).roundToInt()
        val pattern = when (divisor) {
            2, 5, 10 -> "%.1f"
            else -> "%.2f"
        }

        return String.format(Locale.US, pattern, result)
    }

//    private fun toggleEnabled(subcommand: Int) {
//        val idx = getIndexWidgetSlider(subcommand)
//        val info = widgetSlidersInfo.getOrNull(idx) ?: return
//
//        val packed = info.packedProgress.getOrNull(sliderIndex) ?: return
//        val value = unpackValue(packed)
//        val enabled = unpackEnabled(packed)
//
//        info.packedProgress[sliderIndex] = pack(value, !enabled)
//        markPending(info, sliderIndex, info.packedProgress[sliderIndex])
//
//        applyToggleVisuals(idx, sliderIndex)
//        debounceSend(info)
//    }

    private fun toggleEnabled(subcommand: Int) {
        val indices = getIndexWidgetSlider(subcommand)

        indices.forEach { idx ->
            val info = widgetSlidersInfo.getOrNull(idx) ?: return@forEach

            val packed = info.progress
            val value = unpackValue(packed)
            val enabled = unpackEnabled(packed)

            info.progress = pack(value, !enabled)
            markPending(info, 0, info.progress)

            applyToggleVisuals(idx)
            debounceSend(info)
        }
    }


    private fun updateSliderProgress(
        widgetPosition: Int,
        step: Int,
        indexWidgetSlider: Int
    ) {
        val info = widgetSlidersInfo.find { it.widgetPosition == widgetPosition }
        if (info == null) {
            Log.e("updateToggleSlider", "Не найден sliderInfo для widgetPosition=$widgetPosition")
            return
        }

        val range = (info.maxProgress - info.minProgress)
            .coerceAtLeast(0)
            .coerceAtMost(127)

        val progress = info.progress

        if (!unpackEnabled(progress)) return

        val deviceCurrent = unpackValue(progress)
        val uiCurrent = (deviceCurrent - info.minProgress).coerceIn(0, range)

        val uiNext = (uiCurrent + step).coerceIn(0, range)
        val deviceNext = (uiNext + info.minProgress).coerceIn(0, 127)

        info.progress = pack(deviceNext, enabled = true)
        markPending(info, 0, info.progress)

        val seekBar = info.widgetSlidersSb as? SeekBar
        seekBar?.progress = uiNext

        val useInfinity = isInfinityLabel(info, 0)
        info.widgetSliderNumTv.text = formatValueForUi(
            progress = uiNext,
            min = info.minProgress,
            range = range,
            useInfinity = useInfinity,
            increment = info.increment
        )

        applyToggleVisuals(indexWidgetSlider)
        debounceSend(info)
    }

    private fun sendToggleSlider(parameterData: String){
        platformLog("sendToggleSlider","sendToggleSlider RUN $parameterData")
    }

    private fun debounceSend(info: WidgetToggleSliderInfo) {
        timer?.cancel()
        timer = object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                val parameter = ParameterProvider.getParameterV3(info.parameterInfo)
                sendToggleSlider(parameter.data)
//                onSetProgress(info.addressDevice, info.parameterID, info.packedProgress)
            }
        }.start()
    }

    private fun applyToggleVisuals(indexWidgetSlider: Int) {
        val info = widgetSlidersInfo.getOrNull(indexWidgetSlider) ?: return
        val sb = info.widgetSlidersSb as SeekBar
        val ctx = sb.context

        val packed = info.progress

        val range = (info.maxProgress - info.minProgress)
            .coerceAtLeast(0)
            .coerceAtMost(127)

//        val enabled = info.responseReceived.get() && range > 0 && unpackEnabled(packed)
        val enabled = true

        // SeekBar
        val trackRes = if (enabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled
        sb.progressDrawable = AppCompatResources.getDrawable(ctx, trackRes)?.mutate()
        sb.thumb = AppCompatResources.getDrawable(ctx, R.drawable.thumb_le)?.mutate()
        sb.isEnabled = enabled


        val colorRes =
            if (enabled) R.color.ubi4_active
            else R.color.ubi4_gray_border

        info.turnOffBtnIv.getOrNull(0)?.setColorFilter(
            ContextCompat.getColor(ctx, colorRes),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
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

    private fun getIndexWidgetSlider(subcommand: Int): IntArray  {
        val indices = widgetSlidersInfo.mapIndexedNotNull { index, item ->
            if (item.parameterInfo.dataCode == subcommand) { index }
            else { null }
        }.toIntArray()
        return indices
    }

    override fun isForViewType(item: Any): Boolean =
        item is ToggleSliderItemV3 &&
                (item.widget is ToggleSliderParameterWidgetEStruct || item.widget is ToggleSliderParameterWidgetSStruct)

    override fun ToggleSliderItemV3.getItemId(): Any = when (val w = widget) {
        is ToggleSliderParameterWidgetEStruct -> {
            val s = w.baseParameterWidgetEStruct.baseParameterWidgetStruct
            val p = s.parameterInfoSet.first()
            "toggle-slider-${p.deviceAddress}-${p.parameterID}-${s.widgetPosition}"
        }
        is ToggleSliderParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val p = s.parameterInfoSet.first()
            "toggle-slider-${p.deviceAddress}-${p.parameterID}-${s.widgetPosition}"
        }
        else -> "toggle-slider-$title"
    }


    fun onDestroy() {
        Log.d("ToggleSliderAdapter", "onDestroy")
        isAttached = false
        timer?.cancel()
        timer = null
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
    }
}

data class WidgetToggleSliderInfo(
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
    var loadingAnimators: ValueAnimator? = null,
    var turnOffBtnIv: ArrayList<ImageView>,
    var pendingPacked: IntArray = IntArray(2) { -1 },
    var pendingUntilMs: LongArray = LongArray(2) { 0L },
    var labelCodes: IntArray = IntArray(2) { -1 }
)
