package com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders

import android.animation.ValueAnimator
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat.getColor
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetToggleSliderBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import java.util.Locale
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

    private val parameterKeysByInfo = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }
    private var toggleSliderStates: Map<String, ToggleSliderUiStateV3> = emptyMap()
    private val toggleSliderBindings = mutableMapOf<String, ToggleSliderBinding>()

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


    override fun Ubi4WidgetToggleSliderBinding.onBind(item: ToggleSliderItemV3) {
        releaseToggleSliderBinding(root)
        val widget = item.widget as? ToggleSliderParameterWidgetSStruct ?: return
        val key = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
            .parameterInfoSet.firstOrNull()?.let(parameterKeysByInfo::get) ?: return
        if (key in parameterKeys) bindToggleSlider(item, widget, key)
    }

    fun onDestroy() {
        toggleSliderBindings.values.toList().forEach { releaseToggleSliderBinding(it.binding.root) }
        toggleSliderStates = emptyMap()
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
}
