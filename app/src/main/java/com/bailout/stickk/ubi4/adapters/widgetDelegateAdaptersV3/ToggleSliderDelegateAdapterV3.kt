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
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.V3ToggleSliderAction
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.forEach
import kotlin.math.roundToInt

class ToggleSliderDelegateAdapterV3(
    private val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
    private val parameterKeys: Set<String> = emptySet(),
    private val onAction: (V3ToggleSliderAction) -> Unit = {},
    private val animationsEnabled: () -> Boolean = { true },
) : ViewBindingDelegateAdapter<ToggleSliderItemV3, Ubi4WidgetToggleSliderBinding>(
    Ubi4WidgetToggleSliderBinding::inflate
) {

    private companion object {
        private const val PENDING_WINDOW_MS = 300L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var widgetInfoList: ArrayList<WidgetToggleSliderInfo> = ArrayList()
    private var sliderInfoCounter = 0
    private var isAttached = false
    private var collectJob: kotlinx.coroutines.Job? = null
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
    private val parameterKeysByInfo = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }
    private var toggleSliderStates: Map<String, ToggleSliderUiStateV3> = emptyMap()
    private val toggleSliderBindings = mutableMapOf<String, ToggleSliderBinding>()
    private val progressAnimators = mutableMapOf<ProgressBar, ValueAnimator>()

    private data class ToggleSliderBinding(
        val binding: Ubi4WidgetToggleSliderBinding,
        val widget: ToggleSliderParameterWidgetSStruct,
        var animator: ValueAnimator? = null,
    )

    fun renderToggleSliders(states: Map<String, ToggleSliderUiStateV3>) {
        val previous = toggleSliderStates
        toggleSliderStates = states
        toggleSliderBindings.forEach { (key, holder) ->
            val state = states[key]
            if (state != previous[key]) renderToggleSlider(holder, state, animate = state?.animateValueChange == true)
        }
    }

    private fun releaseToggleSliderBinding(root: View) {
        toggleSliderBindings.entries.removeAll { (_, holder) ->
            if (holder.binding.root !== root) return@removeAll false
            holder.animator?.cancel()
            holder.binding.toggleSliderSb.setOnSeekBarChangeListener(null)
            holder.binding.toggleMinusRipple1Btn.setOnClickListener(null)
            holder.binding.togglePlusRipple1Btn.setOnClickListener(null)
            holder.binding.toggleTurnOffRipple1Btn.setOnClickListener(null)
            true
        }
    }

    override fun Ubi4WidgetToggleSliderBinding.onRecycled() {
        releaseToggleSliderBinding(root)
    }

    private fun Ubi4WidgetToggleSliderBinding.bindToggleSlider(
        item: ToggleSliderItemV3,
        widget: ToggleSliderParameterWidgetSStruct,
        key: String,
    ) {
        onDestroyParent { onDestroy() }
        toggleSliderBindings.entries.removeAll { (oldKey, holder) ->
            (oldKey == key || holder.binding.root === root).also { if (it) holder.animator?.cancel() }
        }
        val holder = ToggleSliderBinding(this, widget)
        toggleSliderBindings[key] = holder
        toggleSliderSb.setOnSeekBarChangeListener(null)
        toggleSliderTitleTv.text = item.title
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE
        renderToggleSlider(holder, toggleSliderStates[key], animate = false)

        fun dispatch(action: V3ToggleSliderAction) {
            if (toggleSliderBindings[key] === holder) onAction(action)
        }
        toggleSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    holder.animator?.cancel()
                    val min = toggleSliderStates[key]?.allowedTimeRange?.first ?: widget.minProgress
                    dispatch(V3ToggleSliderAction.ToggleSliderValueChanged(key, progress + min))
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { holder.animator?.cancel() }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val min = toggleSliderStates[key]?.allowedTimeRange?.first ?: widget.minProgress
                dispatch(V3ToggleSliderAction.ToggleSliderChangeCommitted(key, seekBar.progress + min))
            }
        })
        toggleMinusRipple1Btn.setOnClickListener { dispatch(V3ToggleSliderAction.ToggleSliderStepClicked(key, -1)) }
        togglePlusRipple1Btn.setOnClickListener { dispatch(V3ToggleSliderAction.ToggleSliderStepClicked(key, 1)) }
        toggleTurnOffRipple1Btn.setOnClickListener {
            val state = toggleSliderStates[key] ?: return@setOnClickListener
            dispatch(V3ToggleSliderAction.ToggleSliderEnabledChanged(key, !state.value.isEnabled))
        }
    }

    private fun renderToggleSlider(holder: ToggleSliderBinding, state: ToggleSliderUiStateV3?, animate: Boolean) {
        val binding = holder.binding
        val min = state?.allowedTimeRange?.first ?: holder.widget.minProgress
        val max = state?.allowedTimeRange?.last ?: holder.widget.maxProgress
        val progress = ((state?.value?.timeTenths ?: 0) - min).coerceIn(0, max - min)
        holder.animator?.cancel()
        holder.animator = null
        binding.toggleSliderSb.max = max - min
        if (animate && animationsEnabled() && binding.toggleSliderSb.progress != progress) {
            holder.animator = ValueAnimator.ofInt(binding.toggleSliderSb.progress, progress).apply {
                duration = DURATION_ANIMATION
                addUpdateListener { binding.toggleSliderSb.progress = it.animatedValue as Int }
                start()
            }
        } else binding.toggleSliderSb.progress = progress
        binding.toggleSliderNumTv.text = formatValueForUi(progress, min, max - min, false, holder.widget.increment)
        binding.toggleSliderUnitTv.text = holder.widget.unitLabel.let { if (it.isEmpty()) "" else " $it" }

        val sliderEnabled = state?.isSliderEnabled == true
        val context = binding.root.context
        binding.toggleSliderSb.isEnabled = sliderEnabled
        binding.toggleSliderSb.progressDrawable = AppCompatResources.getDrawable(
            context, if (sliderEnabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled,
        )?.mutate()
        binding.toggleSliderSb.thumb = AppCompatResources.getDrawable(context, R.drawable.thumb_le)?.mutate()
        binding.toggleMinusRipple1Btn.isClickable = sliderEnabled
        binding.togglePlusRipple1Btn.isClickable = sliderEnabled
        binding.toggleTurnOffRipple1Btn.isClickable = state?.isInteractionEnabled == true
        val textColor = context.getColor(if (sliderEnabled) R.color.ubi4_white else R.color.ubi4_gray_border)
        binding.toggleMinusBtnTv1.setTextColor(textColor)
        binding.togglePlusBtnTv1.setTextColor(textColor)
        binding.toggleTurnOffBtnIv1.setColorFilter(
            context.getColor(if (sliderEnabled) R.color.ubi4_active else R.color.ubi4_gray_border),
            android.graphics.PorterDuff.Mode.SRC_IN,
        )
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetToggleSliderBinding.onBind(item: ToggleSliderItemV3) {
        releaseToggleSliderBinding(root)
        progressAnimators.remove(toggleSliderSb)?.cancel()
        val widget = item.widget as? ToggleSliderParameterWidgetSStruct
        val key = widget?.baseParameterWidgetSStruct?.baseParameterWidgetStruct
            ?.parameterInfoSet?.firstOrNull()?.let(parameterKeysByInfo::get)
        if (widget != null && key in parameterKeys) {
            bindToggleSlider(item, widget, requireNotNull(key))
            return
        }
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
        widgetInfoList.removeAll {
            it.parameterInfo.deviceAddress == currentSliderInfo.parameterInfo.deviceAddress &&
                    it.parameterInfo.parameterID == currentSliderInfo.parameterInfo.parameterID &&
                    it.parameterInfo.dataCode == currentSliderInfo.parameterInfo.dataCode &&
                    it.widgetPosition == currentSliderInfo.widgetPosition
        }
        widgetInfoList.add(currentSliderInfo)


//      indexWidgetSlidersArray = getIndexWidgetSlider(parameterInfo.dataCode)
        sliderCollect()


        // setup UI
        toggleSliderSb.max = currentSliderInfo.range
        toggleSliderTitleTv.text = item.title
        toggleSliderUnit2Tv.text = ""
        toggleSliderUnit2Tv.visibility = View.GONE

        // первичная синхронизация текста с текущим progress
        platformLog("indexWidgetSlidersArray", "==============================")
        platformLog("indexWidgetSlidersArray", "widgetInfoList $widgetInfoList")

        val widgetInfo = currentSliderInfo
        val useInfinity0 = isInfinityLabel(widgetInfo)
        toggleSliderNumTv.text = formatValueForUi(
            toggleSliderSb.progress,
            widgetInfo.minProgress,
            widgetInfo.range,
            useInfinity0,
            widgetInfo.increment
        )
        toggleSliderUnitTv.text = if (widgetInfo.unitLabel.isEmpty()) "" else " ${widgetInfo.unitLabel}"

        toggleSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                val useInfinity = isInfinityLabel(widgetInfo)
                toggleSliderNumTv.text = formatValueForUi(
                    progress,
                    widgetInfo.minProgress,
                    widgetInfo.range,
                    useInfinity,
                    widgetInfo.increment
                )
                toggleSliderUnitTv.text = if (widgetInfo.unitLabel.isEmpty()) "" else " ${widgetInfo.unitLabel}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!isInteractionEnabled || !widgetInfo.enabled) return
                val progress = seekBar.progress.coerceIn(0, widgetInfo.range)
                widgetInfo.progress = progress + widgetInfo.minProgress
                setParameterData(widgetInfo)
                debounceSend(widgetInfo)
                setUI(widgetInfo.parameterInfo, false)
            }
        })

        toggleMinusRipple1Btn.setOnClickListener {
            if (!isInteractionEnabled || !widgetInfo.enabled) return@setOnClickListener
            val uiProgress = (widgetInfo.progress - widgetInfo.minProgress - 1)
                .coerceIn(0, widgetInfo.range)
            widgetInfo.progress = uiProgress + widgetInfo.minProgress
            setParameterData(widgetInfo)
            debounceSend(widgetInfo)
            setUI(widgetInfo.parameterInfo, false)
        }

        togglePlusRipple1Btn.setOnClickListener {
            if (!isInteractionEnabled || !widgetInfo.enabled) return@setOnClickListener
            val uiProgress = (widgetInfo.progress - widgetInfo.minProgress + 1)
                .coerceIn(0, widgetInfo.range)
            widgetInfo.progress = uiProgress + widgetInfo.minProgress
            setParameterData(widgetInfo)
            debounceSend(widgetInfo)
            setUI(widgetInfo.parameterInfo, false)
        }

        toggleTurnOffRipple1Btn.setOnClickListener {
            if (!isInteractionEnabled) return@setOnClickListener
            widgetInfo.enabled = !widgetInfo.enabled
            setParameterData(widgetInfo)
            debounceSend(widgetInfo)
            setUI(widgetInfo.parameterInfo, false)
        }

        observeInteractionState()
        applyToggleSliderLockState(currentSliderInfo)
        setUI(currentSliderInfo.parameterInfo, true, currentSliderInfo.widgetPosition)
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

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return
        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                widgetInfoList.forEach { applyToggleSliderLockState(it) }
            }
        }
    }

    private fun setUI(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        withAnimation: Boolean = true,
        widgetPosition: Int? = null,
    ) {
        widgetInfoList.forEach { infoWidget ->
            val sameWidget =
                infoWidget.parameterInfo.deviceAddress == parameterInfo.deviceAddress &&
                    infoWidget.parameterInfo.parameterID == parameterInfo.parameterID &&
                    infoWidget.parameterInfo.dataCode == parameterInfo.dataCode &&
                    (widgetPosition == null || infoWidget.widgetPosition == widgetPosition)

            if (!sameWidget) return@forEach
            val seekBar = infoWidget.widgetSlidersSb as? SeekBar ?: return@forEach
            val parameterMeta = ParameterInfoRegistry.getMeta(infoWidget.parameterInfo) ?: return@forEach
            val typedValue = ParameterStoreV3.get(infoWidget.parameterInfo)
                ?: run {
                    val serialized = ParameterProvider.getParameterV3(infoWidget.parameterInfo).data
                    ParameterCodecRegistryV3.decodeFromSerialized(parameterMeta.codecId, serialized)
                }
            val valueForChangeToggle =
                (typedValue as? ParameterTypedValueV3.Toggle)?.value?.toggleValue
                    ?: ToggleV3().toggleValue
            try {
                infoWidget.responseReceived.set(true)

                val oldProgress = seekBar.progress

                val enabled = unpackEnabled(valueForChangeToggle)
                val progress = unpackValue(valueForChangeToggle).coerceIn(0, 127)
                val uiProgress = (progress - infoWidget.minProgress).coerceIn(0, infoWidget.range)

                infoWidget.enabled = enabled
                infoWidget.progress = progress

                // RecyclerView can reuse a view previously owned by another parameter.
                // Keep its pending value current, but let screen state own the reused view.
                if (toggleSliderBindings.values.any { it.binding.toggleSliderSb === seekBar }) return@forEach

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

                applyToggleSliderLockState(infoWidget)
            } catch (e: Exception) {
                Log.e("ToggleSliderV3", "setUI error: ${e.message}", e)
            } finally {
                infoWidget.loadingAnimators?.cancel()
            }
        }
    }
    private fun applyToggleSliderLockState(info: WidgetToggleSliderInfo) {
        if (toggleSliderBindings.values.any { it.binding.toggleSliderSb === info.widgetSlidersSb }) return
        // в зависимости от enable деактивирует или активирует виджет (визуально)
        val sb = info.widgetSlidersSb as SeekBar
        val togglePlusBtnRipple1Btn = info.togglePlusBtnRipple1Btn
        val toggleMinusBtnRipple1Btn = info.toggleMinusBtnRipple1Btn
        val togglePlusBtnTv1 = info.togglePlusBtnTv1
        val toggleMinusBtnTv1 = info.toggleMinusBtnTv1
        val ctx = sb.context
        val sliderEnabled = info.enabled && isInteractionEnabled


        // SeekBar
        val trackRes = if (sliderEnabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled
        sb.progressDrawable = AppCompatResources.getDrawable(ctx, trackRes)?.mutate()
        sb.thumb = AppCompatResources.getDrawable(ctx, R.drawable.thumb_le)?.mutate()
        sb.isEnabled =  sliderEnabled
        //+ -
        togglePlusBtnRipple1Btn.isClickable = sliderEnabled
        toggleMinusBtnRipple1Btn.isClickable = sliderEnabled
        info.toggleTurnOffRipple1Btn?.isClickable = isInteractionEnabled

        val colorResOnOffBtn =
            if ( sliderEnabled) R.color.ubi4_active
            else R.color.ubi4_gray_border
        val colorResPlusMinusBtn =
            if ( sliderEnabled) R.color.ubi4_white
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
        progressAnimators.values.forEach { it.cancel() }
        progressAnimators.clear()
        toggleSliderBindings.values.forEach { holder ->
            holder.animator?.cancel()
            holder.binding.toggleSliderSb.setOnSeekBarChangeListener(null)
            holder.binding.toggleMinusRipple1Btn.setOnClickListener(null)
            holder.binding.togglePlusRipple1Btn.setOnClickListener(null)
            holder.binding.toggleTurnOffRipple1Btn.setOnClickListener(null)
        }
        toggleSliderBindings.clear()
        toggleSliderStates = emptyMap()
        Log.d("ToggleSliderAdapter", "onDestroy")
        isAttached = false
        widgetInfoList.forEach { info ->
            info.timer?.cancel()
            info.timer = null
        }
        widgetInfoList.clear()
        sliderInfoCounter = 0
        scope.coroutineContext.cancelChildren()
        collectJob?.cancel()
        collectJob = null
        interactionJob?.cancel()
        interactionJob = null
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
        progressAnimators.remove(progressBar)?.cancel()
        if (from == to) return

        if (WidgetState.dbSnapshotAppliedWithCrc) {
            progressBar.progress = to
            return
        }

        progressAnimators[progressBar] = ValueAnimator.ofInt(from, to).apply {
            duration = DURATION_ANIMATION
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }
    private fun setParameterData(info: WidgetToggleSliderInfo){
        val toggleV3 = ToggleV3()
        toggleV3.toggleValue = pack(info.progress, info.enabled)
        val typedValue = ParameterTypedValueV3.Toggle(toggleV3)
        ParameterStoreV3.put(info.parameterInfo, typedValue)
        SettingsProfileManager.saveBleValue(info.parameterInfo, typedValue)

        val parameterMeta = ParameterInfoRegistry.getMeta(info.parameterInfo) ?: return
        ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
            ParameterProvider.getParameterV3(info.parameterInfo).data = encoded
        }
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
    var toggleTurnOffRipple1Btn: View? = null,
    var widgetPosition: Int = 0,
    var instanceId: Int = 0,
    var responseReceived: AtomicBoolean = AtomicBoolean(false),
    var loadingAnimators: ValueAnimator? = null,
    var turnOffBtnIv: ArrayList<ImageView>,
    var labelCodes: Int = -1,
    var timer: CountDownTimer? = null
)
