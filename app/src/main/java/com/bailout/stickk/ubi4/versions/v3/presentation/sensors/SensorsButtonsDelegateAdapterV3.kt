package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsUiState
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import java.util.concurrent.atomic.AtomicLong

/** Sensors OPEN/CLOSE controls. Commands and accepted presses belong to the screen ViewModel. */
class SensorsButtonsDelegateAdapterV3(
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val onAction: (V3SensorsButtonsAction) -> Unit,
) : ViewBindingDelegateAdapter<ButtonsItemV3, Ubi4Widget1ButtonBinding>(Ubi4Widget1ButtonBinding::inflate) {
    private class Row(val binding: Ubi4Widget1ButtonBinding) {
        var isAttached = binding.root.isAttachedToWindow
        val presses = mutableMapOf<View, Long>()
    }

    private val rows = mutableMapOf<View, Row>()
    private var state = V3SensorsButtonsUiState()
    private var destroyRegistered = false

    override fun Ubi4Widget1ButtonBinding.onBind(item: ButtonsItemV3) {
        release(this)
        if (!destroyRegistered) {
            destroyRegistered = true
            onDestroyParent(::onDestroy)
        }
        widget1ButtonTv.text = item.title
        widget2ButtonTv.text = item.title2
        widget3ButtonTv.text = item.title3
        val count = (item.widget as CommandParameterWidgetSStruct)
            .baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.size
        btn1Container.visibility = View.VISIBLE
        btn2Container.visibility = if (count >= 2) View.VISIBLE else View.GONE
        btn3Container.visibility = if (count >= 3) View.VISIBLE else View.GONE
        val row = Row(this)
        rows[root] = row
        bindButton(row, widget1Button, V3ProsthesisMovement.OPEN)
        bindButton(row, widget2Button, V3ProsthesisMovement.CLOSE)
        widget3Button.setOnTouchListener(null)
        widget3Button.isEnabled = false
        widget3Button.isClickable = false
        render(row)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindButton(row: Row, button: View, movement: V3ProsthesisMovement) {
        button.setOnTouchListener { view, event ->
            if (rows[row.binding.root] !== row) return@setOnTouchListener true
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!row.isAttached || !state.isEnabled || movement !in state.availableMovements || view in row.presses) {
                        return@setOnTouchListener true
                    }
                    view.onTouchEvent(event)
                    val id = nextPressId.incrementAndGet()
                    row.presses[view] = id
                    onAction(V3SensorsButtonsAction.ButtonPressed(movement, id))
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Release remains valid when interaction was locked after DOWN.
                    view.onTouchEvent(event)
                    finishPress(row, view)
                }
                else -> if (view in row.presses) view.onTouchEvent(event)
            }
            true
        }
    }

    fun render(state: V3SensorsButtonsUiState) {
        this.state = state
        rows.values.forEach(::render)
    }

    private fun render(row: Row) {
        renderButton(row, row.binding.widget1Button, V3ProsthesisMovement.OPEN)
        renderButton(row, row.binding.widget2Button, V3ProsthesisMovement.CLOSE)
    }

    private fun renderButton(row: Row, button: View, movement: V3ProsthesisMovement) {
        val enabled = row.isAttached && state.isEnabled && movement in state.availableMovements
        button.isEnabled = enabled
        button.isClickable = enabled
        button.isPressed = enabled && button in row.presses && movement in state.pressedMovements
    }

    override fun Ubi4Widget1ButtonBinding.onAttachedToWindow() {
        rows[root]?.let { it.isAttached = true; render(it) }
    }

    override fun Ubi4Widget1ButtonBinding.onDetachedFromWindow() {
        rows[root]?.let { row ->
            row.isAttached = false
            finishPresses(row)
            render(row)
        }
    }

    override fun Ubi4Widget1ButtonBinding.onRecycled() = release(this)

    private fun finishPress(row: Row, button: View) {
        val id = row.presses.remove(button)
        button.isPressed = false
        if (id != null) onAction(V3SensorsButtonsAction.ButtonReleased(id))
    }

    private fun finishPresses(row: Row) = row.presses.keys.toList().forEach { finishPress(row, it) }

    private fun release(binding: Ubi4Widget1ButtonBinding) {
        rows.remove(binding.root)?.let(::finishPresses)
        with(binding) {
            listOf(widget1Button, widget2Button, widget3Button).forEach {
                it.setOnTouchListener(null)
                it.isPressed = false
            }
        }
    }

    fun onDestroy() {
        rows.values.toList().forEach { release(it.binding) }
        state = V3SensorsButtonsUiState()
        destroyRegistered = false
    }

    override fun isForViewType(item: Any): Boolean = item is ButtonsItemV3 &&
        (item.widget as? CommandParameterWidgetSStruct)?.baseParameterWidgetSStruct?.baseParameterWidgetStruct?.display == 1

    override fun ButtonsItemV3.getItemId(): Any {
        val base = (widget as CommandParameterWidgetSStruct).baseParameterWidgetSStruct.baseParameterWidgetStruct
        val params = base.parameterInfoSet.sortedWith(
            compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                .thenBy { it.deviceAddress }.thenBy { it.parameterID }.thenBy { it.dataCode },
        ).joinToString("_") { "${it.deviceAddress}-${it.parameterID}-${it.dataCode}-${it.dataOffsets}" }
        return "buttons-${base.widgetPosition}-${params}"
    }

    private companion object {
        val nextPressId = AtomicLong()
    }
}
