package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetTextInputBinding
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceName
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.EXTRAS_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class TextInputDelegateAdapterV3(
    private val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit
) : ViewBindingDelegateAdapter<TextInputItemV3, Ubi4WidgetTextInputBinding>(
    Ubi4WidgetTextInputBinding::inflate
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
    private val overlayViews = arrayListOf<android.view.View>()

    override fun Ubi4WidgetTextInputBinding.onBind(item: TextInputItemV3) {
        onDestroyParent { onDestroy() }

        val hintText = item.title.takeUnless { it.isBlank() || it.equals("no name", ignoreCase = true) }
            ?: main.getString(SharedRes.strings.enter_text.resourceId)
        val buttonTitle = item.buttonTitle.takeUnless { it.isBlank() || it.equals("no name", ignoreCase = true) }
            ?: main.getString(SharedRes.strings.send.resourceId)

        widgetInputEt.hint = hintText
        sendBtnTv.text = buttonTitle
        widgetInputEt.setTextColor(ContextCompat.getColor(root.context, R.color.ubi4_white))
        widgetInputEt.setHintTextColor(ContextCompat.getColor(root.context, R.color.ubi4_deactivate_text))
        widgetInputUnderline.setBackgroundColor(
            ContextCompat.getColor(root.context, R.color.ubi4_deactivate_text)
        )
        val parameterInfo = extractParameterInfo(item)
        when {
            parameterInfo != null && isDeviceNameParameter(parameterInfo) -> {
                setupByteLimitWatcher(widgetInputEt)
                setupCurrentDeviceNamePrefill(widgetInputEt, parameterInfo)
            }
            parameterInfo != null && isSerialNumberParameter(parameterInfo) -> {
                removeByteLimitWatcher(widgetInputEt)
                setupCurrentSerialNumberPrefill(widgetInputEt, parameterInfo)
            }
            else -> {
                removeByteLimitWatcher(widgetInputEt)
                widgetInputEt.setOnClickListener(null)
                widgetInputEt.setOnTouchListener(null)
            }
        }
        overlayViews.add(sendBtnOverlay)
        applyTextInputSendButtonLockState(sendBtnOverlay)
        observeInteractionState()

        sendBtnOverlay.setOnClickListener {
            if (!isInteractionEnabled) return@setOnClickListener
            val enteredText = widgetInputEt.text?.toString()?.trim().orEmpty()
            if (enteredText.isEmpty()) {
                main.showToast(main.getString(SharedRes.strings.enter_text.resourceId))
                return@setOnClickListener
            }

            if (parameterInfo == null) {
                main.showToast(main.getString(SharedRes.strings.command_parameter_not_found.resourceId))
//                Log.w("TextInputDelegateAdapterV3", "No parameterInfo for widget: ${item.widget::class.java.simpleName}")
                return@setOnClickListener
            }

            val isDeviceName = isDeviceNameParameter(parameterInfo)
            val isSerialNumber = isSerialNumberParameter(parameterInfo)
            val currentFullName = if (isDeviceName) resolveCurrentDeviceName(parameterInfo) else null
            val transportText = if (isDeviceName) {
                TextInputTransportFormatterV3.deviceName(
                    enteredText = enteredText,
                    currentFullName = currentFullName
                ) ?: run {
                    main.showToast(main.getString(SharedRes.strings.enter_text.resourceId))
                    return@setOnClickListener
                }
            } else {
                enteredText
            }

            val payload = WidgetCommandBridgeV3.buildSetText(
                parameterID = parameterInfo.parameterID,
                dataCode = parameterInfo.dataCode,
                deviceAddress = parameterInfo.deviceAddress,
                text = transportText
            ) ?: run {
                if (isSerialNumber) {
                    main.showToast(main.getString(SharedRes.strings.failed_prepare_serial_number.resourceId))
                } else {
                    main.showToast(main.getString(SharedRes.strings.failed_prepare_command.resourceId))
                }
                return@setOnClickListener
            }

            val readAfterSetPayload = if (isSerialNumber) {
                WidgetCommandBridgeV3.buildReadRequest(
                    parameterID = parameterInfo.parameterID,
                    dataCode = parameterInfo.dataCode
                )
            } else {
                null
            }

            if (isSerialNumber) {
                Log.d(
                    "DeviceSerialV3",
                    "TX set_serial input=\"$enteredText\" packet=${EncodeByteToHex.bytesToHexString(payload)}"
                )
            }
            if (isDeviceName) {
                Log.d(
                    "DeviceNameV3",
                    "TX current=\"$currentFullName\" input=\"$enteredText\" transport=\"$transportText\" " +
                        "packet=${EncodeByteToHex.bytesToHexString(payload)}"
                )
            }

            main.bleCommandWithQueue(payload, SERIALPORTCHAR_UUID, WRITE) {
                if (isSerialNumber && readAfterSetPayload != null) {
                    Log.d(
                        "DeviceSerialV3",
                        "TX get_serial_after_set packet=${EncodeByteToHex.bytesToHexString(readAfterSetPayload)}"
                    )
                    main.bleCommandWithQueue(readAfterSetPayload, SERIALPORTCHAR_UUID, WRITE) {}
                }
            }
            when {
                isDeviceName -> {
                    WidgetStateBridgeV3.setTextValue(
                        addressDevice = parameterInfo.deviceAddress,
                        parameterID = parameterInfo.parameterID,
                        dataCode = parameterInfo.dataCode,
                        value = transportText
                    )
                    main.applyDeviceNameImmediately(transportText)
                    main.showToast(main.getString(SharedRes.strings.name_set.resourceId))
                }
                isSerialNumber ->
                    main.showToast(main.getString(SharedRes.strings.serial_number_set.resourceId))
                else ->
                    main.showToast(main.getString(SharedRes.strings.value_sent.resourceId))
            }
        }
    }

    override fun isForViewType(item: Any): Boolean = item is TextInputItemV3

    override fun TextInputItemV3.getItemId(): Any = "$title-$buttonTitle"

    private fun extractParameterInfo(item: TextInputItemV3): ParameterInfo<Int, Int, Int, Int>? {
        return when (val widget = item.widget) {
            is CommandParameterWidgetSStruct ->
                widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
            is CommandParameterWidgetEStruct ->
                widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.firstOrNull()
            else -> null
        }
    }

    private fun isDeviceNameParameter(parameterInfo: ParameterInfo<Int, Int, Int, Int>): Boolean {
        val deviceNameInfo = ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME)
        return parameterInfo.parameterID == deviceNameInfo.parameterID &&
            parameterInfo.dataCode == deviceNameInfo.dataCode &&
            parameterInfo.deviceAddress == deviceNameInfo.deviceAddress
    }

    private fun isSerialNumberParameter(parameterInfo: ParameterInfo<Int, Int, Int, Int>): Boolean {
        val serialNumberInfo = ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
        return parameterInfo.parameterID == serialNumberInfo.parameterID &&
            parameterInfo.dataCode == serialNumberInfo.dataCode &&
            parameterInfo.deviceAddress == serialNumberInfo.deviceAddress
    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return
        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                overlayViews.forEach { applyTextInputSendButtonLockState(it) }
            }
        }
    }

    private fun applyTextInputSendButtonLockState(sendBtnOverlay: android.view.View) {
        sendBtnOverlay.isEnabled = isInteractionEnabled
        sendBtnOverlay.isClickable = isInteractionEnabled
    }

    private fun setupByteLimitWatcher(input: EditText) {
        removeByteLimitWatcher(input)

        val watcher = object : TextWatcher {
            private var isInternalChange = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange || s == null) return

                val currentText = s.toString()
                val trimmed = TextInputNameLimitV3.trimToLimit(currentText)
                if (trimmed == currentText) return

                isInternalChange = true
                input.setText(trimmed)
                input.setSelection(trimmed.length)
                isInternalChange = false
                main.showToast(main.getString(SharedRes.strings.text_limit_reached.resourceId))
            }
        }

        input.addTextChangedListener(watcher)
        input.setTag(R.id.tag_text_input_limit_watcher, watcher)
    }

    private fun removeByteLimitWatcher(input: EditText) {
        val existingWatcher = input.getTag(R.id.tag_text_input_limit_watcher) as? TextWatcher
        if (existingWatcher != null) {
            input.removeTextChangedListener(existingWatcher)
            input.setTag(R.id.tag_text_input_limit_watcher, null)
        }
    }/**/

    private fun setupCurrentDeviceNamePrefill(
        input: EditText,
        parameterInfo: ParameterInfo<Int, Int, Int, Int>
    ) {
        input.setOnClickListener(null)
        input.setOnTouchListener(null)

        val displayName = resolveCurrentDeviceName(parameterInfo)
            ?.let(TextInputPrefillFormatterV3::deviceName)
            .orEmpty()

        input.setText(displayName)
        input.setSelection(input.text?.length ?: 0)
    }

    private fun setupCurrentSerialNumberPrefill(
        input: EditText,
        parameterInfo: ParameterInfo<Int, Int, Int, Int>
    ) {
        input.setOnClickListener(null)
        input.setOnTouchListener(null)

        val rawSerialNumber = (ParameterStoreV3.get(parameterInfo) as? ParameterTypedValueV3.Text)
            ?.value
            ?.takeUnless { it.isBlank() }
            ?: main.getCurrentSerial()?.takeUnless { it.isBlank() }
            ?: main.mDeviceName?.takeUnless { it.isBlank() }
            ?: runCatching { connectedDeviceName }.getOrNull()?.takeUnless { it.isBlank() }
            ?: main.intent?.getStringExtra(EXTRAS_DEVICE_NAME)?.takeUnless { it.isBlank() }

        val displaySerialNumber = rawSerialNumber
            ?.let(TextInputPrefillFormatterV3::serialNumber)
            .orEmpty()

        input.setText(displaySerialNumber)
        input.setSelection(input.text?.length ?: 0)
    }

    private fun resolveCurrentDeviceName(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>
    ): String? = TextInputCurrentNameResolverV3.resolve(
        receivedName = WidgetStateBridgeV3.getTextValue(
            addressDevice = parameterInfo.deviceAddress,
            parameterID = parameterInfo.parameterID,
            dataCode = parameterInfo.dataCode
        ),
        connectedName = runCatching { connectedDeviceName }.getOrNull(),
        activityName = main.mDeviceName,
        intentName = main.intent?.getStringExtra(EXTRAS_DEVICE_NAME)
    )

    private fun onDestroy() {
        overlayViews.clear()
        scope.coroutineContext.cancelChildren()
        interactionJob?.cancel()
        interactionJob = null
    }
}

internal object TextInputNameLimitV3 {
    const val MAX_INPUT_BYTES_WITHOUT_PREFIX = 13

    fun trimToLimit(value: String): String {
        var charIndex = 0
        var bytesUsed = 0

        while (charIndex < value.length) {
            val codePoint = Character.codePointAt(value, charIndex)
            val codePointChars = String(Character.toChars(codePoint))
            val codePointBytes = codePointChars.toByteArray(StandardCharsets.UTF_8).size
            if (bytesUsed + codePointBytes > MAX_INPUT_BYTES_WITHOUT_PREFIX) break

            bytesUsed += codePointBytes
            charIndex += Character.charCount(codePoint)
        }

        return if (charIndex == value.length) value else value.substring(0, charIndex)
    }
}

internal object TextInputPrefillFormatterV3 {
    fun deviceName(rawValue: String): String = DeviceNameBridgeV3.displayName(rawValue)

    fun serialNumber(rawValue: String): String = rawValue
}

internal object TextInputTransportFormatterV3 {
    fun deviceName(enteredText: String, currentFullName: String?): String? =
        DeviceNameBridgeV3.transportName(enteredText, currentFullName)
}

internal object TextInputCurrentNameResolverV3 {
    fun resolve(
        receivedName: String?,
        connectedName: String?,
        activityName: String?,
        intentName: String?
    ): String? = sequenceOf(receivedName, connectedName, activityName, intentName)
        .mapNotNull { it?.trim()?.takeUnless(String::isBlank) }
        .firstOrNull()
}
