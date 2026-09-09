package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.WidgetKindV3
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import java.util.Locale
import kotlin.math.roundToInt

class SliderDelegateAdapterV3(
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val onAction: (V3SliderAction) -> Unit,
    private val animationsEnabled: () -> Boolean,
) : ViewBindingDelegateAdapter<SliderItemV3, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {
    private companion object {
        val parameterKeys = ParameterInfoRegistry.parameterMetaMapV3
            .filterValues { it.widgetKind == WidgetKindV3.SLIDER }
            .entries.associate { (key, meta) -> meta.parameterInfo to key }
    }

    private val widgetInfoList = mutableListOf<WidgetSliderInfo>()
    private var isAttached = false
    private var sliderStates: Map<String, SliderUiStateV3> = emptyMap()

    private fun formatSliderValue(value: Int, increment: Float): String {
        val result = value * increment
        if (increment >= 1.0f) return result.toInt().toString()
        val divisor = (1.0f / increment).roundToInt()
        val pattern = when (divisor) {
            2, 5, 10 -> "%.1f"
            else -> "%.2f"
        }
        return String.format(Locale.US, pattern, result)
    }

    override fun Ubi4WidgetSliderBinding.onBind(item: SliderItemV3) {
        val widget = item.widget as? SliderParameterWidgetSStruct ?: return
        val baseWidget = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
        val parameterKey = parameterKeys[baseWidget.parameterInfoSet.firstOrNull()] ?: return
        val allowedRange = sliderStates[parameterKey]?.allowedRange ?: 0..0
        if (!isAttached) {
            onDestroyParent { onDestroy() }
            isAttached = true
        }

        // Release a previous binding before replacing its views or reusing the same SeekBar.
        widgetInfoList.removeAll { info ->
            val replaced = info.widgetSlidersSb === widgetSliderSb ||
                (info.parameterKey == parameterKey && info.widgetPosition == baseWidget.widgetPosition)
            if (replaced) releaseSliderViews(info)
            replaced
        }
        val infoWidget = WidgetSliderInfo(
            parameterKey = parameterKey,
            minProgress = allowedRange.first,
            increment = widget.increment,
            progress = allowedRange.first,
            range = allowedRange.last - allowedRange.first,
            widgetSlidersSb = widgetSliderSb,
            widgetSliderNumTv = widgetSliderNumTv,
            minusBtnRipple = minusBtnRipple,
            plusBtnRipple = plusBtnRipple,
            minusBtnTv = minusBtnTv,
            plusBtnTv = plusBtnTv,
            widgetPosition = baseWidget.widgetPosition,
        )
        widgetInfoList.add(infoWidget)

        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onAction(V3SliderAction.SliderValueChanged(
                        parameterKey, progress + infoWidget.minProgress
                    ))
                }
                widgetSliderNumTv.text = formatSliderValue(
                    seekBar.progress + infoWidget.minProgress, infoWidget.increment
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onAction(V3SliderAction.SliderChangeCommitted(
                    parameterKey, seekBar.progress + infoWidget.minProgress
                ))
            }
        })
        minusBtnRipple.setOnClickListener {
            onAction(V3SliderAction.SliderStepClicked(parameterKey, -1))
        }
        plusBtnRipple.setOnClickListener {
            onAction(V3SliderAction.SliderStepClicked(parameterKey, 1))
        }

        widgetSliderSb.max = infoWidget.range
        secondSliderCl.visibility = View.GONE
        widgetSliderSb.progress = 0
        widgetSliderNumTv.text = formatSliderValue(infoWidget.minProgress, infoWidget.increment)
        widgetSliderTitleTv.text = item.title
        renderSliderUiState(withAnimation = false)
    }

    fun renderSliders(states: Map<String, SliderUiStateV3>) {
        sliderStates = states
        renderSliderUiState()
    }

    private fun renderSliderUiState(withAnimation: Boolean = true) {
        widgetInfoList.forEach { info ->
            val slider = sliderStates[info.parameterKey]
            applySliderLockState(info, slider?.isEnabled == true)
            if (slider == null) return@forEach
            info.minProgress = slider.allowedRange.first
            info.range = slider.allowedRange.last - slider.allowedRange.first
            info.widgetSlidersSb.max = info.range
            val value = slider.value ?: info.minProgress
            val animate = withAnimation && slider.animateValueChange && animationsEnabled()
            if (info.progress != value || !animate) {
                info.progress = value
                setProgressBar(info, (value - info.minProgress).coerceIn(0, info.range), animate)
            }
        }
    }

    private fun applySliderLockState(infoWidget: WidgetSliderInfo, enabled: Boolean) {
        val seekBar = infoWidget.widgetSlidersSb
        val context = seekBar.context
        val trackRes = if (enabled) R.drawable.ubi4_track else R.drawable.ubi4_track_disabled
        seekBar.progressDrawable = AppCompatResources.getDrawable(context, trackRes)?.mutate()
        seekBar.thumb = AppCompatResources.getDrawable(context, R.drawable.thumb_le)?.mutate()
        seekBar.isEnabled = enabled
        infoWidget.minusBtnRipple.isClickable = enabled
        infoWidget.plusBtnRipple.isClickable = enabled
        val colorRes = if (enabled) R.color.ubi4_white else R.color.ubi4_gray_border
        infoWidget.minusBtnTv.setTextColor(context.getColor(colorRes))
        infoWidget.plusBtnTv.setTextColor(context.getColor(colorRes))
    }

    private fun setProgressBar(infoWidget: WidgetSliderInfo, to: Int, withAnimation: Boolean) {
        if (withAnimation) {
            if (infoWidget.loadingAnimators?.isRunning == true) {
                infoWidget.pendingUiProgress = to
            } else {
                startProgressAnimation(infoWidget, infoWidget.widgetSlidersSb.progress, to)
            }
        } else {
            cancelSliderAnimation(infoWidget)
            infoWidget.widgetSlidersSb.progress = to
        }
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
        isAttached = false
        widgetInfoList.forEach(::releaseSliderViews)
        widgetInfoList.clear()
        sliderStates = emptyMap()
    }

    private fun releaseSliderViews(info: WidgetSliderInfo) {
        cancelSliderAnimation(info)
        info.widgetSlidersSb.setOnSeekBarChangeListener(null)
        info.minusBtnRipple.setOnClickListener(null)
        info.plusBtnRipple.setOnClickListener(null)
    }

    private fun cancelSliderAnimation(info: WidgetSliderInfo) {
        info.pendingUiProgress = null
        info.loadingAnimators?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }
        info.loadingAnimators = null
    }

    private data class WidgetSliderInfo(
        val parameterKey: String,
        var minProgress: Int,
        val increment: Float,
        var progress: Int,
        var range: Int,
        val widgetSlidersSb: SeekBar,
        val widgetSliderNumTv: TextView,
        val minusBtnRipple: View,
        val plusBtnRipple: View,
        val minusBtnTv: TextView,
        val plusBtnTv: TextView,
        val widgetPosition: Int,
        var loadingAnimators: ValueAnimator? = null,
        var pendingUiProgress: Int? = null,
    )
}
