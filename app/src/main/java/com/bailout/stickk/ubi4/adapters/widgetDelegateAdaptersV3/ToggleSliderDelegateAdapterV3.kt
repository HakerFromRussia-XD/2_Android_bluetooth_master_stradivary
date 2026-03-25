package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetToggleSliderBinding
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.sliderFlowV3
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.guiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
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
        private const val PENDING_WINDOW_MS = 300L
    }

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetInfoList: ArrayList<WidgetToggleSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0
    private var isAttached = false
    private var collectJob: kotlinx.coroutines.Job? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetToggleSliderBinding.onBind(item: ToggleSliderItemV3) {
        Log.d("ToggleSliderAdapter", "onBind RUN")
        onDestroyParent { onDestroy() }
        isAttached = true
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE
        toggleTurnOffBtnIv1.setColorFilter(getColor(root.context, R.color.ubi4_active))

        val parameterInfo: ParameterInfo<Int, Int, Int, Int>?
        val minProgress: Int
        val maxProgress: Int
        val widgetPosition: Int
        val increment: Float
        val unitLabel: String

        when (val widget = item.widget) {
            is ToggleSliderParameterWidgetSStruct -> {
                parameterInfo = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
                    .parameterInfoSet.elementAt(0)
                minProgress = widget.minProgress
                maxProgress = widget.maxProgress
                increment = widget.increment
                unitLabel = widget.unitLabel
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
            }

            else -> return
        }

        val currentSliderInfo = WidgetToggleSliderInfo(
            parameterInfo = parameterInfo,
            minProgress = minProgress,
            maxProgress = maxProgress,
            unitLabel = unitLabel,
            increment = increment,
            enabled = false,
            progress = 0,
            range = (if (maxProgress == minProgress) 100 else maxProgress - minProgress).coerceIn(0, 127),
            widgetSlidersSb = toggleSliderSb,
            toggleMinusBtnRipple1Btn = toggleMinusRipple1Btn,
            togglePlusBtnRipple1Btn = togglePlusRipple1Btn,
            togglePlusBtnTv1 = togglePlusBtnTv1,
            toggleMinusBtnTv1 = toggleMinusBtnTv1,
            toggleSliderNumTv = toggleSliderNumTv,
            toggleSliderUnitTv = toggleSliderUnitTv,
            widgetPosition = widgetPosition,
            turnOffBtnIv = arrayListOf(toggleTurnOffBtnIv1, toggleTurnOffBtnIv2),
        )
        currentSliderInfo.instanceId = sliderInfoCounter++
        widgetInfoList.add(currentSliderInfo)


//        indexWidgetSlidersArray = getIndexWidgetSlider(parameterInfo.dataCode)
        sliderCollect()


        // setup UI
        toggleSliderSb.max = currentSliderInfo.range
        toggleSliderTitleTv.text = item.title
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE

        // первичная синхронизация текста с текущим progress
        platformLog("indexWidgetSlidersArray", "==============================")
        platformLog("indexWidgetSlidersArray", "widgetInfoList $widgetInfoList")
        widgetInfoList.forEach { widgetInfo ->
//            val widgetInfo = widgetInfoList[indexWidgetSlider]
            val useInfinity0 = isInfinityLabel(widgetInfo)
            toggleSliderNumTv.text = formatValueForUi(
                toggleSliderSb.progress,
                widgetInfo.minProgress,
                currentSliderInfo.range,
                useInfinity0,
                widgetInfo.increment
            )
            toggleSliderUnitTv.text = if (widgetInfo.unitLabel.isEmpty()) "" else " "+ widgetInfo.unitLabel

            // seekbar 1
            toggleSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
//                    val widgetInfo = widgetInfoList[indexWidgetSlider]
                    val useInfinity = isInfinityLabel(widgetInfo)
                    toggleSliderNumTv.text = formatValueForUi(
                        progress,
                        widgetInfo.minProgress,
                        currentSliderInfo.range,
                        useInfinity,
                        widgetInfo.increment
                    )
                    toggleSliderUnitTv.text = if (widgetInfo.unitLabel.isEmpty()) "" else " "+ widgetInfo.unitLabel
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val progress = seekBar.progress.coerceIn(0, 127)
                    widgetInfo.progress = progress + widgetInfo.minProgress
                    setParameterData(widgetInfo)
                    debounceSend(widgetInfo)
                    setUI(widgetInfo.parameterInfo, false)
                }
            })

            // +/-
            toggleMinusRipple1Btn.setOnClickListener {
                widgetInfo.progress = (widgetInfo.progress - 1).coerceIn(0, 127)
                setParameterData(widgetInfo)
                debounceSend(widgetInfo)
                setUI(widgetInfo.parameterInfo, false)
            }
            togglePlusRipple1Btn.setOnClickListener {
                widgetInfo.progress = (widgetInfo.progress + 1).coerceIn(0, 127)
                setParameterData(widgetInfo)
                debounceSend(widgetInfo)
                setUI(widgetInfo.parameterInfo, false)
            }
            toggleTurnOffRipple1Btn.setOnClickListener {
                widgetInfo.enabled = !widgetInfo.enabled
                setParameterData(widgetInfo)
                debounceSend(widgetInfo)
                setUI(widgetInfo.parameterInfo)
            }
        }
    }
    private fun sliderCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            sliderFlowV3.collect { parameterInfo ->
                setUI(parameterInfo)
            }
        }
    }

    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>, withAnimation: Boolean = true) {
        widgetInfoList.forEach { infoWidget ->
            val parameter = ParameterProvider.getParameterV3(infoWidget.parameterInfo)
            val seekBar = infoWidget.widgetSlidersSb as? SeekBar ?: return@forEach
            var valueForChangeToggle = 0
            val subcommand = parameterInfo.dataCode
            when (subcommand) {
                PWCE_SET_EMG_CHANGE_GESTURE.number.toInt() -> {
                    valueForChangeToggle = parseToggleSafely(parameter.data)?.toggleValue ?: ToggleV3().toggleValue
                }
                PWCE_SET_EMG_MOVEMENT_LOCK.number.toInt() -> {
                    valueForChangeToggle = parseToggleSafely(parameter.data)?.toggleValue ?: ToggleV3().toggleValue
                    platformLog("PWCE_SET_EMG_MOVEMENT_LOCK", "valueForChangeToggle $valueForChangeToggle")
                }
                GMCE_SET_SCREEN_TIMEOUT.number.toInt() -> {
                    valueForChangeToggle = parseToggleSafely(parameter.data)?.toggleValue ?: ToggleV3().toggleValue
                }
            }
            try {
                infoWidget.responseReceived.set(true)

                val oldProgress = seekBar.progress

                val enabled = unpackEnabled(valueForChangeToggle)
                val progress = unpackValue(valueForChangeToggle).coerceIn(0, 127)
                val uiProgress = (progress - infoWidget.minProgress).coerceIn(0, infoWidget.range)

                infoWidget.enabled = enabled
                infoWidget.progress = progress

                if (withAnimation) { animateProgressBar(seekBar, oldProgress, uiProgress) }
                else { seekBar.progress = uiProgress }

                val useInfinity = isInfinityLabel(infoWidget)
                infoWidget.toggleSliderNumTv.text = formatValueForUi(
                    progress = uiProgress,
                    min = infoWidget.minProgress,
                    range = infoWidget.range,
                    useInfinity = useInfinity,
                    increment = infoWidget.increment
                )
                infoWidget.toggleSliderUnitTv.text = if (infoWidget.unitLabel.isEmpty()) "" else " "+ infoWidget.unitLabel

                applyToggleVisuals(infoWidget)
            } catch (e: Exception) {
                Log.e("ToggleSliderV3", "setUI error: ${e.message}", e)
            } finally {
                infoWidget.loadingAnimators?.cancel()
            }
        }
    }
    private fun applyToggleVisuals(info: WidgetToggleSliderInfo) {
        // в зависимости от enable деактивирует или активирует виджет (визуально)
        val sb = info.widgetSlidersSb as SeekBar
        val togglePlusBtnRipple1Btn = info.togglePlusBtnRipple1Btn
        val toggleMinusBtnRipple1Btn = info.toggleMinusBtnRipple1Btn
        val togglePlusBtnTv1 = info.togglePlusBtnTv1
        val toggleMinusBtnTv1 = info.toggleMinusBtnTv1
        val ctx = sb.context


        // SeekBar
        val trackRes = if (info.enabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled
        sb.progressDrawable = AppCompatResources.getDrawable(ctx, trackRes)?.mutate()
        sb.thumb = AppCompatResources.getDrawable(ctx, R.drawable.thumb_le)?.mutate()
        sb.isEnabled =  info.enabled
        //+ -
        togglePlusBtnRipple1Btn.isClickable = info.enabled
        toggleMinusBtnRipple1Btn.isClickable = info.enabled

        val colorResOnOffBtn =
            if ( info.enabled) R.color.ubi4_active
            else R.color.ubi4_gray_border
        val colorResPlusMinusBtn =
            if ( info.enabled) R.color.ubi4_white
            else R.color.ubi4_gray_border
        togglePlusBtnTv1.setTextColor(ctx.getColor(colorResPlusMinusBtn))
        toggleMinusBtnTv1.setTextColor(ctx.getColor(colorResPlusMinusBtn))
        info.turnOffBtnIv.getOrNull(0)?.setColorFilter(
            ContextCompat.getColor(ctx, colorResOnOffBtn),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun debounceSend(info: WidgetToggleSliderInfo) {
        info.timer?.cancel()
        info.timer = object : CountDownTimer(PENDING_WINDOW_MS, 1) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                if (!isAttached) return
                sendProgress(info.parameterInfo.parameterID, info.parameterInfo.dataCode, pack(info.progress, info.enabled))
            }
        }.start()
    }
    private fun sendProgress(command: Int, subcommand: Int, progress: Int){
        platformLog("sendProgress", "subcommand = $subcommand   progress = $progress")
        main.bleCommandWithQueue(BLECommandsV3.sendCommand(command, subcommand, progress), SERIALPORTCHAR_UUID, WRITE){}
    }

    fun onDestroy() {
        Log.d("ToggleSliderAdapter", "onDestroy")
        isAttached = false
//        info.timer?.cancel()
//        info.timer = null
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
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
    private fun isInfinityLabel(info: WidgetToggleSliderInfo): Boolean { return info.labelCodes == 9 }
    // ===== packed byte helpers: bit7=enabled, bits0..6=value(0..127) =====
    private fun unpackEnabled(packed: Int): Boolean = (packed and 0x80) != 0
    private fun unpackValue(packed: Int): Int = packed and 0x7F
    private fun pack(value0_127: Int, enabled: Boolean): Int {
        val v = value0_127.coerceIn(0, 127)
        return (if (enabled) 0x80 else 0x00) or (v and 0x7F)
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
    private fun setParameterData(info: WidgetToggleSliderInfo){
        val parameter = ParameterProvider.getParameterV3(info.parameterInfo)
        val toggleV3 = ToggleV3()
        toggleV3.toggleValue = pack(info.progress, info.enabled)
        parameter.data = json.encodeToString(toggleV3)
    }
    private fun parseToggleSafely(data: String): ToggleV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<ToggleV3>(data) }
            .onFailure { platformLog("ToggleSliderDelegateAdapterV3", "Failed to decode EMGGainResult: ${it.message}") }
            .getOrNull()
    }
}

data class WidgetToggleSliderInfo(
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0, 0, 0, 0),
    var minProgress: Int = 0,
    var maxProgress: Int = 0,
    var unitLabel: String = "",
    var increment: Float = 1.0f,
    var enabled: Boolean = false,
    var progress: Int = 0,
    var range: Int = 0,
    var widgetSlidersSb: ProgressBar,
    var toggleMinusBtnRipple1Btn: View,
    var togglePlusBtnRipple1Btn: View,
    var togglePlusBtnTv1: TextView,
    var toggleMinusBtnTv1: TextView,
    var toggleSliderNumTv: TextView,
    var toggleSliderUnitTv: TextView,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ValueAnimator? = null,
    var turnOffBtnIv: ArrayList<ImageView>,
    var labelCodes: Int = -1,
    var timer: CountDownTimer? = null
)
