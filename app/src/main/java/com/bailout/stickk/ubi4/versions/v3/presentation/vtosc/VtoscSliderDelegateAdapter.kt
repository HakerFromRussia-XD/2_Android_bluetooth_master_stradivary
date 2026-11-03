package com.bailout.stickk.ubi4.versions.v3.presentation.vtosc

import android.view.View
import android.widget.SeekBar
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscParameter
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

/** VTFB settings sliders. Values and writes belong to [V3VtoscViewModel]. */
class VtoscSliderDelegateAdapter(
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val onAction: (V3VtoscAction) -> Unit,
) : ViewBindingDelegateAdapter<VtoscSliderItem, Ubi4WidgetSliderBinding>(Ubi4WidgetSliderBinding::inflate) {
    private class Row(
        val binding: Ubi4WidgetSliderBinding,
        val parameter: V3VtoscParameter,
    )

    private val rows = mutableMapOf<View, Row>()
    private var states: Map<V3VtoscParameter, V3VtoscSliderUiState> = emptyMap()
    private var destroyRegistered = false

    override fun Ubi4WidgetSliderBinding.onBind(item: VtoscSliderItem) {
        release(this)
        if (!destroyRegistered) {
            destroyRegistered = true
            onDestroyParent(::onDestroy)
        }
        // Layout is dual-slider; VTOS uses one parameter per row.
        secondSliderCl.visibility = View.GONE
        widgetSliderTitleTv.text = item.parameter.title
        val row = Row(this, item.parameter)
        rows[root] = row
        val range = item.parameter.range
        widgetSliderSb.max = (range.last - range.first).coerceAtLeast(0)
        widgetSliderSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (rows[root] !== row || !fromUser) return
                val value = progress + range.first
                widgetSliderNumTv.text = value.toString()
                onAction(V3VtoscAction.SliderValueChanged(row.parameter, value))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (rows[root] !== row) return
                onAction(V3VtoscAction.SliderChangeCommitted(row.parameter, seekBar.progress + range.first))
            }
        })
        minusBtnRipple.setOnClickListener {
            if (rows[root] !== row) return@setOnClickListener
            val state = states[row.parameter] ?: return@setOnClickListener
            if (!state.isEnabled) return@setOnClickListener
            val next = (state.value - 1).coerceIn(range)
            onAction(V3VtoscAction.SliderValueChanged(row.parameter, next))
            onAction(V3VtoscAction.SliderChangeCommitted(row.parameter, next))
        }
        plusBtnRipple.setOnClickListener {
            if (rows[root] !== row) return@setOnClickListener
            val state = states[row.parameter] ?: return@setOnClickListener
            if (!state.isEnabled) return@setOnClickListener
            val next = (state.value + 1).coerceIn(range)
            onAction(V3VtoscAction.SliderValueChanged(row.parameter, next))
            onAction(V3VtoscAction.SliderChangeCommitted(row.parameter, next))
        }
        render(row)
    }

    fun render(sliders: List<V3VtoscSliderUiState>) {
        states = sliders.associateBy { it.parameter }
        rows.values.forEach(::render)
    }

    private fun render(row: Row) {
        val state = states[row.parameter] ?: return
        val range = row.parameter.range
        val progress = (state.value - range.first).coerceIn(0, row.binding.widgetSliderSb.max)
        if (row.binding.widgetSliderSb.progress != progress) {
            row.binding.widgetSliderSb.progress = progress
        }
        row.binding.widgetSliderNumTv.text = state.value.toString()
        row.binding.widgetSliderSb.isEnabled = state.isEnabled
        row.binding.minusBtnRipple.isEnabled = state.isEnabled
        row.binding.plusBtnRipple.isEnabled = state.isEnabled
    }

    override fun Ubi4WidgetSliderBinding.onRecycled() = release(this)

    private fun release(binding: Ubi4WidgetSliderBinding) {
        rows.remove(binding.root)
        binding.widgetSliderSb.setOnSeekBarChangeListener(null)
        binding.minusBtnRipple.setOnClickListener(null)
        binding.plusBtnRipple.setOnClickListener(null)
    }

    fun onDestroy() {
        rows.values.toList().forEach { release(it.binding) }
        states = emptyMap()
        destroyRegistered = false
    }

    override fun isForViewType(item: Any): Boolean = item is VtoscSliderItem
    override fun VtoscSliderItem.getItemId(): Any = parameter.key
}
