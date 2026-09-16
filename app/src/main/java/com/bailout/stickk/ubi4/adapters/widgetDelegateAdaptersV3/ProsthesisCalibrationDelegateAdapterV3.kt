package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_START_CALIBRATE_COMMAND
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ProsthesisCalibrationUiState
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import java.util.concurrent.atomic.AtomicLong

class ProsthesisCalibrationDelegateAdapterV3(
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val onPressed: (Long) -> Unit,
    private val onReleased: (Long) -> Unit,
) : ViewBindingDelegateAdapter<ButtonsItemV3, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {
    private class Row(val binding: Ubi4Widget1ButtonBinding) {
        var isAttached = binding.root.isAttachedToWindow
        var pressId: Long? = null
    }

    private val rows = mutableMapOf<View, Row>()
    private var state: V3ProsthesisCalibrationUiState? = null
    private var destroyRegistered = false

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4Widget1ButtonBinding.onBind(item: ButtonsItemV3) {
        release(this)
        if (!destroyRegistered) {
            destroyRegistered = true
            onDestroyParent(::onDestroy)
        }
        widget1ButtonTv.text = item.title
        widget2ButtonTv.text = item.title2
        widget3ButtonTv.text = item.title3
        btn1Container.visibility = View.VISIBLE
        btn2Container.visibility = View.GONE
        btn3Container.visibility = View.GONE
        val row = Row(this)
        rows[root] = row
        widget1Button.setOnTouchListener { view, event ->
            if (rows[root] !== row) return@setOnTouchListener true
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!row.isAttached || state?.isEnabled != true || row.pressId != null) return@setOnTouchListener true
                    view.onTouchEvent(event)
                    val id = nextPressId.incrementAndGet()
                    row.pressId = id
                    onPressed(id)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.onTouchEvent(event)
                    finishPress(row)
                }
                else -> if (row.pressId != null) view.onTouchEvent(event)
            }
            true
        }
        widget2Button.setOnTouchListener(null)
        widget3Button.setOnTouchListener(null)
        render(row)
    }

    fun render(state: V3ProsthesisCalibrationUiState?) {
        this.state = state
        rows.values.forEach(::render)
    }

    private fun render(row: Row) {
        val enabled = row.isAttached && state?.isEnabled == true
        with(row.binding) {
            // Preserve the existing lock state, including the two hidden buttons.
            listOf(widget1Button, widget2Button, widget3Button).forEach {
                it.isEnabled = enabled
                it.isClickable = enabled
            }
            widget1Button.isPressed = enabled && row.pressId != null && state?.isPressed == true
        }
    }

    override fun Ubi4Widget1ButtonBinding.onAttachedToWindow() {
        rows[root]?.let { it.isAttached = true; render(it) }
    }

    override fun Ubi4Widget1ButtonBinding.onDetachedFromWindow() {
        rows[root]?.let { it.isAttached = false; finishPress(it); render(it) }
    }

    override fun Ubi4Widget1ButtonBinding.onRecycled() = release(this)

    private fun finishPress(row: Row) {
        val id = row.pressId
        row.pressId = null
        row.binding.widget1Button.isPressed = false
        if (id != null) onReleased(id)
    }

    private fun release(binding: Ubi4Widget1ButtonBinding) {
        rows.remove(binding.root)?.let(::finishPress)
        with(binding) {
            listOf(widget1Button, widget2Button, widget3Button).forEach {
                it.setOnTouchListener(null)
                it.isPressed = false
            }
        }
    }

    fun onDestroy() {
        rows.values.toList().forEach { release(it.binding) }
        state = null
        destroyRegistered = false
    }

    override fun isForViewType(item: Any): Boolean {
        val widget = ((item as? ButtonsItemV3)?.widget as? CommandParameterWidgetSStruct)
            ?.baseParameterWidgetSStruct?.baseParameterWidgetStruct ?: return false
        return widget.display == 4 && widget.parameterInfoSet.singleOrNull() == ParameterInfoRegistry.require(P_KEY_START_CALIBRATE_COMMAND)
    }

    override fun ButtonsItemV3.getItemId(): Any {
        val base = (widget as CommandParameterWidgetSStruct).baseParameterWidgetSStruct.baseParameterWidgetStruct
        val info = base.parameterInfoSet.single()
        return "buttons-${base.widgetPosition}-${info.deviceAddress}-${info.parameterID}-${info.dataCode}-${info.dataOffsets}"
    }

    private companion object {
        val nextPressId = AtomicLong()
    }
}
