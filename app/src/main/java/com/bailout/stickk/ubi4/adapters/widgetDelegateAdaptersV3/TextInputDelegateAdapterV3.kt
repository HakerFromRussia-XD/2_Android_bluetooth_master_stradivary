package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetTextInputBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceTextInputUiState
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class TextInputDelegateAdapterV3(
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val onTextChanged: (V3DeviceInfoField, String) -> Unit = { _, _ -> },
    private val onPrefillRequested: (V3DeviceInfoField) -> Unit = {},
    private val onSendClicked: (V3DeviceInfoField) -> Unit = {},
) : ViewBindingDelegateAdapter<TextInputItemV3, Ubi4WidgetTextInputBinding>(Ubi4WidgetTextInputBinding::inflate) {
    private class InputBinding(val binding: Ubi4WidgetTextInputBinding) {
        lateinit var watcher: TextWatcher
        var rendering = false
        var cursorRevision: Long? = null
    }
    private val bindings = mutableMapOf<V3DeviceInfoField, InputBinding>()
    private var states: Map<V3DeviceInfoField, V3ServiceTextInputUiState> = emptyMap()
    private var destroyRegistered = false
    private val parameterInfoByField = mapOf(
        V3DeviceInfoField.DEVICE_NAME to ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME),
        V3DeviceInfoField.SERIAL_NUMBER to ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER),
    )

    fun renderTextInputs(value: Map<V3DeviceInfoField, V3ServiceTextInputUiState>) {
        states = value
        bindings.forEach { (field, holder) -> render(holder, value[field]) }
    }

    override fun Ubi4WidgetTextInputBinding.onBind(item: TextInputItemV3) {
        if (!destroyRegistered) { onDestroyParent(::onDestroy); destroyRegistered = true }
        release(this)
        val info = when (val widget = item.widget) {
            is CommandParameterWidgetSStruct -> widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
            is CommandParameterWidgetEStruct -> widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
            else -> null
        }
        val field = parameterInfoByField.entries.firstOrNull { (_, expected) ->
            info?.parameterID == expected.parameterID && info.dataCode == expected.dataCode && info.deviceAddress == expected.deviceAddress
        }?.key ?: return
        bindings[field]?.let { release(it.binding) }
        val context = root.context
        widgetInputEt.hint = item.title.takeUnless { it.isBlank() || it.equals("no name", true) }
            ?: context.getString(SharedRes.strings.enter_text.resourceId)
        sendBtnTv.text = item.buttonTitle.takeUnless { it.isBlank() || it.equals("no name", true) }
            ?: context.getString(SharedRes.strings.send.resourceId)
        widgetInputEt.setTextColor(ContextCompat.getColor(context, R.color.ubi4_white))
        widgetInputEt.setHintTextColor(ContextCompat.getColor(context, R.color.ubi4_deactivate_text))
        widgetInputUnderline.setBackgroundColor(ContextCompat.getColor(context, R.color.ubi4_deactivate_text))

        val holder = InputBinding(this)
        bindings[field] = holder
        holder.watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!holder.rendering && bindings[field] === holder) this@TextInputDelegateAdapterV3.onTextChanged(field, s?.toString().orEmpty())
            }
        }
        widgetInputEt.addTextChangedListener(holder.watcher)
        widgetInputEt.setOnClickListener {
            if (bindings[field] === holder) onPrefillRequested(field)
        }
        widgetInputEt.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP && bindings[field] === holder) onPrefillRequested(field)
            false
        }
        sendBtnOverlay.setOnClickListener {
            if (bindings[field] === holder && states[field]?.canSend == true) onSendClicked(field)
        }
        render(holder, states[field])
    }

    private fun render(holder: InputBinding, state: V3ServiceTextInputUiState?) {
        val value = state ?: V3ServiceTextInputUiState()
        val input = holder.binding.widgetInputEt
        holder.rendering = true
        try {
            val changed = input.text?.toString().orEmpty() != value.text
            if (changed) input.setText(value.text)
            if (changed || holder.cursorRevision != value.cursorRevision) input.setSelection(value.text.length)
            holder.cursorRevision = value.cursorRevision
        } finally { holder.rendering = false }
        holder.binding.sendBtnOverlay.isEnabled = value.canSend
        holder.binding.sendBtnOverlay.isClickable = value.canSend
    }

    private fun release(binding: Ubi4WidgetTextInputBinding) {
        bindings.entries.removeAll { (_, holder) ->
            if (holder.binding !== binding) return@removeAll false
            binding.widgetInputEt.removeTextChangedListener(holder.watcher)
            binding.widgetInputEt.setOnClickListener(null)
            binding.widgetInputEt.setOnTouchListener(null)
            binding.sendBtnOverlay.setOnClickListener(null)
            true
        }
    }

    override fun Ubi4WidgetTextInputBinding.onRecycled() = release(this)
    private fun onDestroy() {
        bindings.values.toList().forEach { release(it.binding) }
        states = emptyMap()
        destroyRegistered = false
    }
    override fun isForViewType(item: Any): Boolean = item is TextInputItemV3
    override fun TextInputItemV3.getItemId(): Any = "$title-$buttonTitle"
}
