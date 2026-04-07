package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetTextInputBinding
import com.bailout.stickk.new_electronic_by_Rodeon.utils.NameUtil
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceName
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DEVICE_NAME_PREFIX
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.EXTRAS_DEVICE_NAME
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import java.nio.charset.StandardCharsets

class TextInputDelegateAdapterV3(
    private val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit
) : ViewBindingDelegateAdapter<TextInputItemV3, Ubi4WidgetTextInputBinding>(
    Ubi4WidgetTextInputBinding::inflate
) {
    companion object {
        private const val MAX_INPUT_BYTES = 10
        private const val BYTE_LIMIT_WARNING = "Лимит 10 байт исчерпан"
    }

    override fun Ubi4WidgetTextInputBinding.onBind(item: TextInputItemV3) {
        onDestroyParent { onDestroy() }

        val hintText = item.title.takeUnless { it.isBlank() || it.equals("no name", ignoreCase = true) }
            ?: "Введите текст"
        val buttonTitle = item.buttonTitle.takeUnless { it.isBlank() || it.equals("no name", ignoreCase = true) }
            ?: "Отправить"

        widgetInputEt.hint = hintText
        sendBtnTv.text = buttonTitle
        widgetInputEt.setTextColor(ContextCompat.getColor(root.context, R.color.ubi4_white))
        widgetInputEt.setHintTextColor(ContextCompat.getColor(root.context, R.color.ubi4_deactivate_text))
        widgetInputUnderline.setBackgroundColor(
            ContextCompat.getColor(root.context, R.color.ubi4_deactivate_text)
        )
        setupByteLimitWatcher(widgetInputEt)
        setupCurrentDeviceNamePrefill(widgetInputEt)

        sendBtnOverlay.setOnClickListener {
            val enteredText = widgetInputEt.text?.toString()?.trim().orEmpty()
            if (enteredText.isEmpty()) {
                main.showToast("Введите текст")
                return@setOnClickListener
            }

            val parameterInfo = when (val widget = item.widget) {
                is CommandParameterWidgetSStruct ->
                    widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
                is CommandParameterWidgetEStruct ->
                    widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
                else -> null
            }

            if (parameterInfo == null) {
                main.showToast("Параметр команды не найден")
//                Log.w("TextInputDelegateAdapterV3", "No parameterInfo for widget: ${item.widget::class.java.simpleName}")
                return@setOnClickListener
            }

            val normalizedSuffix = enteredText
                .removePrefix(DEVICE_NAME_PREFIX)
                .removePrefix("-")
            val valueWithPrefix = DEVICE_NAME_PREFIX + normalizedSuffix
            val nullTerminatedData = valueWithPrefix.toByteArray(StandardCharsets.UTF_8) + byteArrayOf(0x00)
            val payload = BLECommandsV3.sendLongCommand(
                command = parameterInfo.parameterID,
                subcommand = parameterInfo.dataCode,
                data = nullTerminatedData
            )

            main.bleCommandWithQueue(payload, SERIALPORTCHAR_UUID, WRITE) {}
            main.applyDeviceNameImmediately(valueWithPrefix)
            main.showToast("Имя установлено")
        }
    }

    override fun isForViewType(item: Any): Boolean = item is TextInputItemV3

    override fun TextInputItemV3.getItemId(): Any = "$title-$buttonTitle"

    private fun setupByteLimitWatcher(input: EditText) {
        val existingWatcher = input.getTag(R.id.tag_text_input_limit_watcher) as? TextWatcher
        if (existingWatcher != null) {
            input.removeTextChangedListener(existingWatcher)
        }

        val watcher = object : TextWatcher {
            private var isInternalChange = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange || s == null) return

                val currentText = s.toString()
                val trimmed = trimToUtf8ByteLimit(currentText, MAX_INPUT_BYTES)
                if (trimmed == currentText) return

                isInternalChange = true
                input.setText(trimmed)
                input.setSelection(trimmed.length)
                isInternalChange = false
                main.showToast(BYTE_LIMIT_WARNING)
            }
        }

        input.addTextChangedListener(watcher)
        input.setTag(R.id.tag_text_input_limit_watcher, watcher)
    }

    private fun setupCurrentDeviceNamePrefill(input: EditText) {
        fun fillCurrentName() {
            val rawDeviceName = main.getCurrentSerial()
                ?.takeUnless { it.isBlank() }
                ?: main.mDeviceName?.takeUnless { it.isBlank() }
                ?: runCatching { connectedDeviceName }.getOrNull()?.takeUnless { it.isBlank() }
                ?: main.intent?.getStringExtra(EXTRAS_DEVICE_NAME)?.takeUnless { it.isBlank() }
                ?: return

            val displayName = NameUtil.getDisplayName(rawDeviceName)
            if (displayName.isBlank()) return

            input.setText(displayName)
            input.setSelection(displayName.length)
        }

        input.setOnClickListener { fillCurrentName() }
        input.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                fillCurrentName()
            }
            false
        }
    }

    private fun trimToUtf8ByteLimit(value: String, maxBytes: Int): String {
        var charIndex = 0
        var bytesUsed = 0

        while (charIndex < value.length) {
            val codePoint = Character.codePointAt(value, charIndex)
            val codePointChars = String(Character.toChars(codePoint))
            val codePointBytes = codePointChars.toByteArray(StandardCharsets.UTF_8).size
            if (bytesUsed + codePointBytes > maxBytes) break

            bytesUsed += codePointBytes
            charIndex += Character.charCount(codePoint)
        }

        return if (charIndex == value.length) value else value.substring(0, charIndex)
    }

    private fun onDestroy() = Unit
}
