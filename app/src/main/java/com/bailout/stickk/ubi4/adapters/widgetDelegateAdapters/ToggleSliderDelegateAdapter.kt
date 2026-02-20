package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

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
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.slidersFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.systemLang
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Locale
import kotlin.math.roundToInt

class ToggleSliderDelegateAdapter(
    private val onSetProgress: (addressDevice: Int, parameterID: Int, packedBytes: ArrayList<Int>) -> Unit,
    private val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : ViewBindingDelegateAdapter<ToggleSliderItem, Ubi4WidgetToggleSliderBinding>(
    Ubi4WidgetToggleSliderBinding::inflate
) {

    private companion object {
        private const val PENDING_WINDOW_MS = 800L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetSlidersInfo: ArrayList<WidgetToggleSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0
    private var timer: CountDownTimer? = null
    private var isAttached = false
    private var collectJob: kotlinx.coroutines.Job? = null

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

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetToggleSliderBinding.onBind(item: ToggleSliderItem) {
        Log.d("ToggleSliderAdapter", "onBind RUN")
        onDestroyParent { onDestroy() }
        isAttached = true
        toggleSliderUnitTv.text = ""
        toggleSliderUnitTv.visibility = View.GONE
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE

        var addressDevice = 0
        var parameterID = 0
        var dataCode = 0
        val dataOffset: ArrayList<Int> = ArrayList()
        var minProgress = 0
        var maxProgress = 0
        var widgetPosition = 0
        var increment = 1.0f

        when (val widget = item.widget) {
            is ToggleSliderParameterWidgetEStruct -> {
                val s = widget.baseParameterWidgetEStruct.baseParameterWidgetStruct
                addressDevice = s.parameterInfoSet.elementAt(0).deviceAddress
                parameterID = s.parameterInfoSet.elementAt(0).parameterID
                dataCode = s.parameterInfoSet.elementAt(0).dataCode
                s.parameterInfoSet.forEach { dataOffset.add(it.dataOffsets) }
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                increment = widget.increment
                widgetPosition = s.widgetPosition
            }

            is ToggleSliderParameterWidgetSStruct -> {
                val s = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                addressDevice = s.parameterInfoSet.elementAt(0).deviceAddress
                parameterID = s.parameterInfoSet.elementAt(0).parameterID
                dataCode = s.parameterInfoSet.elementAt(0).dataCode
                s.parameterInfoSet.forEach { dataOffset.add(it.dataOffsets) }
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                increment = widget.increment
                widgetPosition = s.widgetPosition
            }

            else -> return
        }

        val paramCount = dataOffset.size

        // packed value = 7 бит, range максимум 127
        val range = (maxProgress - minProgress).coerceAtLeast(0).coerceAtMost(127)

        val initialPacked = MutableList(paramCount) { pack(0, enabled = false) }

        val currentInfo = WidgetToggleSliderInfo(
            addressDevice = addressDevice,
            parameterID = parameterID,
            dataCode = dataCode,
            dataOffset = dataOffset,
            minProgress = minProgress,
            maxProgress = maxProgress,
            increment = increment,
            packedProgress = ArrayList(initialPacked),
            widgetSlidersSb = arrayListOf(toggleSliderSb, toggleSlider2Sb),
            widgetSliderNumTv = arrayListOf(toggleSliderNumTv, toggleSliderNum2Tv),
            widgetSliderUnitTv = arrayListOf(toggleSliderUnitTv, toggleSliderUnit2Tv),
            turnOffBtnIv = arrayListOf(toggleTurnOffBtnIv1, toggleTurnOffBtnIv2),
            widgetPosition = widgetPosition
        )

        currentInfo.instanceId = sliderInfoCounter++

        widgetSlidersInfo.removeAll { it.addressDevice == addressDevice && it.parameterID == parameterID }
        widgetSlidersInfo.add(currentInfo)


        val indexWidgetSlider = getIndexWidgetSlider(addressDevice, parameterID)
        if (indexWidgetSlider == -1) return

        // setup UI
        toggleSliderSb.max = range

        toggleSecondSliderCl.visibility = if (paramCount > 1) View.VISIBLE else View.GONE

        if (paramCount > 1) {
            toggleSlider2Sb.max = range
        } else {
            toggleSlider2Sb.setOnSeekBarChangeListener(null)
        }

        val sliderE = item.widget as? ToggleSliderParameterWidgetEStruct
        if (sliderE != null) {
            val b = sliderE.baseParameterWidgetEStruct.baseParameterWidgetStruct
            val labelKey = (b.deviceId shl 16) or (b.widgetId shl 8) or b.widgetCode
            val labelsByOffset = UiState.labelCodesByOffset[labelKey]

            val langKey = if (systemLang().startsWith("ru", true)) "ru" else "en"
            val dict = PreferenceKeysUbi4.parameterWidgetLabel[langKey]
                ?: PreferenceKeysUbi4.parameterWidgetLabel["en"].orEmpty()


            val titleViews = listOf(toggleSliderTitleTv, toggleSliderTitle2Tv)
            val unitViews  = listOf(toggleSliderUnitTv, toggleSliderUnit2Tv)

            titleViews.forEachIndexed { idx, tv ->
                val off = dataOffset.getOrNull(idx)

                val code = off?.let { labelsByOffset?.get(it) } ?: -1
                if (idx in 0..1) currentInfo.labelCodes[idx] = code

                val label = code.takeIf { it >= 0 }?.let { dict[it.toString()] }

                // title
                tv.text = label?.title ?: if (idx == 0) item.title else tv.text

                // unit
                val unitTv = unitViews[idx]
                val unit = label?.unit
                if (unit.isNullOrBlank() || (idx == 1 && paramCount <= 1)) {
                    unitTv.text = ""
                    unitTv.visibility = View.GONE
                } else {
                    unitTv.text = unit
                    unitTv.visibility = View.VISIBLE
                }
            }
        } else {
            toggleSliderTitleTv.text = item.title
            toggleSliderUnitTv.text = ""
            toggleSliderUnitTv.visibility = View.GONE
            toggleSliderUnit2Tv.text = ""
            toggleSliderUnit2Tv.visibility = View.GONE
        }

        // cache-first draw
        run {
            val ref = ParameterRef(addressDevice, parameterID, dataCode)
            val cached = ParameterProvider.getParameter(addressDevice, parameterID)
            if (cached.data.isNotEmpty()) setUI(ref)
        }

        sliderCollect()


        // первичная синхронизация текста с текущим progress
        run {
            val info = widgetSlidersInfo[indexWidgetSlider]

            val useInfinity0 = isInfinityLabel(info, 0)
            toggleSliderNumTv.text =
                formatValueForUi(toggleSliderSb.progress, info.minProgress, range, useInfinity0, info.increment)

            if (paramCount > 1) {
                val useInfinity1 = isInfinityLabel(info, 1)
                toggleSliderNum2Tv.text =
                    formatValueForUi(toggleSlider2Sb.progress, info.minProgress, range, useInfinity1, info.increment)
            }
        }

        // seekbar 1
        toggleSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val info = widgetSlidersInfo[indexWidgetSlider]
                val useInfinity = isInfinityLabel(info, 0)
                toggleSliderNumTv.text = formatValueForUi(progress, info.minProgress, range, useInfinity, info.increment)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val info = widgetSlidersInfo[indexWidgetSlider]
                val oldPacked = info.packedProgress[0]
                val enabled = unpackEnabled(oldPacked)
                if (!enabled) {
                    val deviceValue = unpackValue(oldPacked)
                    seekBar.progress = (deviceValue - info.minProgress).coerceIn(0, range)
                    return
                }

                val uiProgress = seekBar.progress.coerceIn(0, range)
                val deviceValue = (uiProgress + info.minProgress).coerceIn(0, 127)
                info.packedProgress[0] = pack(deviceValue, enabled = true)
                markPending(info, 0, info.packedProgress[0])

                applyToggleVisuals(indexWidgetSlider, 0)

                Log.d("ToggleSliderSend", "→ send packed=${info.packedProgress}")
                onSetProgress(addressDevice, parameterID, info.packedProgress)
            }
        })

        // seekbar 2
        if (paramCount > 1) {
            toggleSlider2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val info = widgetSlidersInfo[indexWidgetSlider]
                    val useInfinity = isInfinityLabel(info, 1)
                    toggleSliderNum2Tv.text = formatValueForUi(progress, info.minProgress, range, useInfinity, info.increment)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val info = widgetSlidersInfo[indexWidgetSlider]
                    val oldPacked = info.packedProgress[1]
                    val enabled = unpackEnabled(oldPacked)
                    if (!enabled) {
                        val deviceValue = unpackValue(oldPacked)
                        seekBar.progress = (deviceValue - info.minProgress).coerceIn(0, range)
                        return
                    }

                    val uiProgress = seekBar.progress.coerceIn(0, range)
                    val deviceValue = (uiProgress + info.minProgress).coerceIn(0, 127)
                    info.packedProgress[1] = pack(deviceValue, enabled = true)
                    markPending(info, 1, info.packedProgress[1])

                    applyToggleVisuals(indexWidgetSlider, 1)

                    Log.d("ToggleSliderSend", "→ send packed=${info.packedProgress}")
                    onSetProgress(addressDevice, parameterID, info.packedProgress)
                }
            })
        } else {
            toggleSlider2Sb.setOnSeekBarChangeListener(null)
        }

        // +/-
        toggleMinusBtnRipple1.setOnClickListener {
            updateSliderProgress(
                widgetPosition,
                sliderIndex = 0,
                step = -1,
                indexWidgetSlider = indexWidgetSlider
            )
        }
        togglePlusBtnRipple1.setOnClickListener {
            updateSliderProgress(
                widgetPosition,
                sliderIndex = 0,
                step = +1,
                indexWidgetSlider = indexWidgetSlider
            )
        }

        if (paramCount > 1) {
            toggleMinusBtnRipple2.setOnClickListener {
                updateSliderProgress(
                    widgetPosition,
                    sliderIndex = 1,
                    step = -1,
                    indexWidgetSlider = indexWidgetSlider
                )
            }
            togglePlusBtnRipple2.setOnClickListener {
                updateSliderProgress(
                    widgetPosition,
                    sliderIndex = 1,
                    step = +1,
                    indexWidgetSlider = indexWidgetSlider
                )
            }
        } else {
            toggleMinusBtnRipple2.setOnClickListener(null)
            togglePlusBtnRipple2.setOnClickListener(null)
        }

        // toggle buttons: меняем только enabled-bit
        toggleTurnOffBtnRipple1.setOnClickListener { toggleEnabled(addressDevice, parameterID, 0) }
        toggleTurnOffBtnRipple2.setOnClickListener {
            if (paramCount > 1) toggleEnabled(
                addressDevice,
                parameterID,
                1
            )
        }

        // request if cache empty
        run {
            val cachedNow = ParameterProvider.getParameter(addressDevice, parameterID)
            if (cachedNow.data.isEmpty()) {
                currentInfo.responseReceived.set(false)
                RetryUtils.sendRequestWithRetry(
                    request = {
                        main.bleCommandWithQueue(
                            BLECommands.requestSlider(addressDevice, parameterID),
                            MAIN_CHANNEL_CHARACTERISTIC,
                            SampleGattAttributes.WRITE
                        ) {}
                    },
                    isResponseReceived = { currentInfo.responseReceived.get() },
                    maxRetries = 5,
                    delayMillis = 400L,
                    scope = scope
                )
            } else {
                currentInfo.responseReceived.set(true)
            }
        }
        // первичный визуал
        applyToggleVisuals(indexWidgetSlider, 0)
        if (paramCount > 1) applyToggleVisuals(indexWidgetSlider, 1)
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

    private fun toggleEnabled(addressDevice: Int, parameterID: Int, sliderIndex: Int) {
        val idx = getIndexWidgetSlider(addressDevice, parameterID)
        val info = widgetSlidersInfo.getOrNull(idx) ?: return

        val packed = info.packedProgress.getOrNull(sliderIndex) ?: return
        val value = unpackValue(packed)
        val enabled = unpackEnabled(packed)

        info.packedProgress[sliderIndex] = pack(value, !enabled)
        markPending(info, sliderIndex, info.packedProgress[sliderIndex])

        applyToggleVisuals(idx, sliderIndex)
        debounceSend(info)
    }

    private fun updateSliderProgress(
        widgetPosition: Int,
        sliderIndex: Int,
        step: Int,
        indexWidgetSlider: Int
    ) {
        val info = widgetSlidersInfo.find { it.widgetPosition == widgetPosition }
        if (info == null) {
            Log.e("updateToggleSlider", "Не найден sliderInfo для widgetPosition=$widgetPosition")
            return
        }

        val range = (info.maxProgress - info.minProgress).coerceAtLeast(0).coerceAtMost(127)
        val packed = info.packedProgress.getOrNull(sliderIndex)
        if (packed == null) {
            Log.e("updateToggleSlider", "Нет packed для sliderIndex=$sliderIndex")
            return
        }

        if (!unpackEnabled(packed)) return // выключено — ничего не меняем

        val deviceCurrent = unpackValue(packed) // absolute 0..127
        val uiCurrent = (deviceCurrent - info.minProgress).coerceIn(0, range)

        val uiNext = (uiCurrent + step).coerceIn(0, range)
        val deviceNext = (uiNext + info.minProgress).coerceIn(0, 127)

        info.packedProgress[sliderIndex] = pack(deviceNext, enabled = true)
        markPending(info, sliderIndex, info.packedProgress[sliderIndex])

        info.widgetSlidersSb.getOrNull(sliderIndex)?.progress = uiNext
        val useInfinity = isInfinityLabel(info, sliderIndex)

        info.widgetSliderNumTv.getOrNull(sliderIndex)?.text =
            formatValueForUi(uiNext, info.minProgress, range, useInfinity, info.increment)

        applyToggleVisuals(indexWidgetSlider, sliderIndex)
        debounceSend(info)
    }

    private fun debounceSend(info: WidgetToggleSliderInfo) {
        timer?.cancel()
        timer = object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                onSetProgress(info.addressDevice, info.parameterID, info.packedProgress)
            }
        }.start()
    }

    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            slidersFlow.collect { parameterRef ->
                val idx = getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
                if (idx != -1) setUI(parameterRef)
            }
        }
    }

    private fun setUI(parameterRef: ParameterRef) {
        val parameter =
            ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
        val indexWidgetSlider =
            getIndexWidgetSlider(parameterRef.addressDevice, parameterRef.parameterID)
        if (indexWidgetSlider == -1 || indexWidgetSlider >= widgetSlidersInfo.size) return

        val info = widgetSlidersInfo[indexWidgetSlider]
        val range = (info.maxProgress - info.minProgress).coerceAtLeast(0).coerceAtMost(127)

        fun readByteFromHex(hex: String, byteIndex: Int): Int? {
            val start = byteIndex * 2
            val end = start + 2
            if (start < 0 || end > hex.length) return null
            return runCatching { hex.substring(start, end).toInt(16) and 0xFF }.getOrNull()
        }

        try {
            val hex = parameter.data
            if (hex.isNullOrEmpty()) return
            info.responseReceived.set(true)

            val sizeOf = PreferenceKeysUbi4.ParameterTypeEnum.entries
                .getOrNull(parameter.type)
                ?.sizeOf
                ?: return

            info.dataOffset.forEachIndexed { sliderIndex, off ->
                val sb =
                    info.widgetSlidersSb.getOrNull(sliderIndex) as? SeekBar ?: return@forEachIndexed
                val tv = info.widgetSliderNumTv.getOrNull(sliderIndex) ?: return@forEachIndexed

                val oldProgress = sb.progress

                // off — индекс элемента, элемент занимает sizeOf байт
                val elementStartByte = off * sizeOf

                // packed лежит в ПЕРВОМ байте элемента
                val packedByteIndex = elementStartByte
                val packedByte = readByteFromHex(hex, packedByteIndex) ?: return@forEachIndexed
                val packedFromDevice = packedByte and 0xFF

                // анти-мигание: игнорируем "чужие" значения пока ждём ACK своего действия
                if (!shouldApplyDevicePacked(
                        info,
                        sliderIndex,
                        packedFromDevice
                    )
                ) return@forEachIndexed

                val enabled = (packedFromDevice and 0x80) != 0
                val deviceValue = (packedFromDevice and 0x7F).coerceIn(0, 127)

                if (sliderIndex < info.packedProgress.size) {
                    info.packedProgress[sliderIndex] = pack(deviceValue, enabled)
                }

                val uiProgress = (deviceValue - info.minProgress).coerceIn(0, range)

                animateProgressBar(sb, oldProgress, uiProgress)
                val useInfinity = isInfinityLabel(info, sliderIndex)
                tv.text = formatValueForUi(uiProgress, info.minProgress, range, useInfinity, info.increment)

                applyToggleVisuals(indexWidgetSlider, sliderIndex)
            }
        } catch (e: Exception) {
            Log.e("ToggleSlider", "setUI error: ${e.message}", e)
        } finally {
            info.loadingAnimators.forEach { it?.cancel() }
            info.loadingAnimators.clear()
        }
    }

    private fun applyToggleVisuals(indexWidgetSlider: Int, sliderIndex: Int) {
        val info = widgetSlidersInfo.getOrNull(indexWidgetSlider) ?: return
        val sb = info.widgetSlidersSb.getOrNull(sliderIndex) as? SeekBar ?: return
        val ctx = sb.context

        val packed = info.packedProgress.getOrNull(sliderIndex) ?: return

        val range = (info.maxProgress - info.minProgress)
            .coerceAtLeast(0)
            .coerceAtMost(127)

        val enabled = info.responseReceived.get() && range > 0 && unpackEnabled(packed)

        // SeekBar
        val trackRes = if (enabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled
        sb.progressDrawable = AppCompatResources.getDrawable(ctx, trackRes)?.mutate()
        sb.thumb = AppCompatResources.getDrawable(ctx, R.drawable.thumb_le)?.mutate()
        sb.isEnabled = enabled


        val colorRes =
            if (enabled) R.color.ubi4_active
            else R.color.ubi4_gray_border

        info.turnOffBtnIv.getOrNull(sliderIndex)?.setColorFilter(
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

    private fun getIndexWidgetSlider(addressDevice: Int, parameterID: Int): Int {
        return widgetSlidersInfo.indexOfFirst { it.addressDevice == addressDevice && it.parameterID == parameterID }
    }

    override fun isForViewType(item: Any): Boolean =
        item is ToggleSliderItem &&
                (item.widget is ToggleSliderParameterWidgetEStruct || item.widget is ToggleSliderParameterWidgetSStruct)

    override fun ToggleSliderItem.getItemId(): Any = when (val w = widget) {
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
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var dataCode: Int = 0,
    var dataOffset: ArrayList<Int> = ArrayList(),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var increment: Float = 1.0f,
    var packedProgress: ArrayList<Int> = ArrayList(), // packed bytes
    var widgetSlidersSb: ArrayList<ProgressBar>,
    var widgetSliderNumTv: ArrayList<TextView>,
    var widgetSliderUnitTv: ArrayList<TextView>,
    var turnOffBtnIv: ArrayList<ImageView>,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ArrayList<ValueAnimator?> = ArrayList(),
    var pendingPacked: IntArray = IntArray(2) { -1 },
    var pendingUntilMs: LongArray = LongArray(2) { 0L },
    var labelCodes: IntArray = IntArray(2) { -1 }
)
