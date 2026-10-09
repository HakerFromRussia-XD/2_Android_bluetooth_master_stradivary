package com.bailout.stickk.ubi4.versions.v3.data.settings

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.parser.ParameterCodecActionV3
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.parser.ParameterEncodedActionV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.versions.v3.data.device.deviceInteractionEnabledState
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.ParameterMetaV3
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.ble.WidgetKindV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderResponsesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Uses the existing V3 stores, profile persistence and command queue. */
class V3DeviceSettingsRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val saveBleValue: (ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3) -> Unit =
        SettingsProfileManager::saveBleValue,
    private val readCachedValues: Boolean = true,
    private val saveValueBeforeSending: Boolean = true,
    private val retainEmgGainDrafts: Boolean = false,
    private val beforeSpinnerValueSent: (ParameterInfo<Int, Int, Int, Int>, Int) -> Unit = { _, _ -> },
) : V3DeviceSettingsRepository, V3SliderResponsesRepository, V3ToggleSliderSettingsRepository, V3SpinnerSettingsRepository {
    private companion object {
        // Preserve the iOS pair cache lifetime across screen/repository recreation and reconnects.
        val emgGainDrafts = MutableStateFlow<Map<ParameterStoreKeyV3, ParameterTypedValueV3.EmgGains>>(emptyMap())
    }
    override val sliderInteractionEnabled = deviceInteractionEnabledState
    override val toggleSliderInteractionEnabled = deviceInteractionEnabledState
    override val spinnerInteractionEnabled = deviceInteractionEnabledState

    override fun getSpinnerValue(parameterKey: String): Int? =
        (readTypedValue(spinnerMeta(parameterKey)) as? ParameterTypedValueV3.Spinner)?.value?.spinnerValue

    override fun observeSpinnerValue(parameterKey: String) =
        ParameterStoreV3.values.map { getSpinnerValue(parameterKey) }.distinctUntilChanged()

    override fun requestSpinnerValue(parameterKey: String) =
        requestParameterValue(spinnerMeta(parameterKey).parameterInfo)

    override fun setSpinnerValue(parameterKey: String, value: Int) {
        val meta = spinnerMeta(parameterKey)
        beforeSpinnerValueSent(meta.parameterInfo, value)
        val typed = ParameterTypedValueV3.Spinner(SpinnerV3(spinnerValue = value))
        if (saveValueBeforeSending) saveTypedValue(meta, typed)
        enqueuePacket(BLECommandsV3.sendCommand(meta.parameterInfo.parameterID, meta.parameterInfo.dataCode, value))
    }

    private fun spinnerMeta(parameterKey: String): ParameterMetaV3 {
        val meta = ParameterInfoRegistry.getMeta(parameterKey)
        require(meta?.widgetKind == WidgetKindV3.SPINNER) { "Unsupported Spinner parameter: $parameterKey" }
        return requireNotNull(meta)
    }

    override fun observeToggleSliderValue(parameterKey: String) =
        ParameterStoreV3.values.map { getToggleSliderValue(parameterKey) }.distinctUntilChanged()

    override fun getToggleSliderValue(parameterKey: String): V3ToggleSliderValue? {
        val typed = readTypedValue(toggleSliderMeta(parameterKey)) as? ParameterTypedValueV3.Toggle ?: return null
        val packed = typed.value.toggleValue
        return V3ToggleSliderValue(timeTenths = packed and 0x7F, isEnabled = packed and 0x80 != 0)
    }

    override fun requestToggleSliderValue(parameterKey: String) =
        requestParameterValue(toggleSliderMeta(parameterKey).parameterInfo)

    override fun saveToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) {
        val meta = toggleSliderMeta(parameterKey)
        val typed = ParameterTypedValueV3.Toggle(ToggleV3(toggleValue = packToggleSliderValue(value)))
        saveTypedValue(meta, typed)
    }

    override fun sendToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) {
        val info = toggleSliderMeta(parameterKey).parameterInfo
        enqueuePacket(BLECommandsV3.sendCommand(info.parameterID, info.dataCode, packToggleSliderValue(value)))
    }

    private fun toggleSliderMeta(parameterKey: String): ParameterMetaV3 {
        val meta = ParameterInfoRegistry.getMeta(parameterKey)
        require(meta?.widgetKind == WidgetKindV3.TOGGLE_SLIDER) { "Unsupported ToggleSlider parameter: $parameterKey" }
        return requireNotNull(meta)
    }

    private fun packToggleSliderValue(value: V3ToggleSliderValue): Int =
        (if (value.isEnabled) 0x80 else 0) or value.timeTenths.coerceIn(0, 127)

    override fun observeSliderValue(parameterKey: String) =
        ParameterStoreV3.values.map { getSliderValue(parameterKey) }.distinctUntilChanged()

    override fun observeSliderResponses(parameterKey: String, onResponse: () -> Unit) =
        WidgetStateBridgeV3.observeUpdates { snapshot ->
            val info = sliderMeta(parameterKey).parameterInfo
            if (snapshot.addressDevice == info.deviceAddress &&
                snapshot.parameterID == info.parameterID && snapshot.dataCode == info.dataCode
            ) onResponse()
        }

    override fun getSliderValue(parameterKey: String): Int? {
        val meta = sliderMeta(parameterKey)
        return when (val typedValue = readTypedValue(meta)) {
            is ParameterTypedValueV3.Slider -> typedValue.value.sliderValue
            is ParameterTypedValueV3.EmgGains -> {
                if (retainEmgGainDrafts) {
                    // Existing iOS JSON omits zero fields; it consumes only complete, nonzero RX pairs.
                    if (!typedValue.hasReceivedPair()) return null
                    retainEmgGainDraft(meta, typedValue)
                }
                when (meta.parameterInfo.dataOffsets) {
                    0 -> typedValue.value.openGain
                    1 -> typedValue.value.closeGain
                    else -> null
                }
            }
            else -> null
        }
    }

    override fun requestSliderValue(parameterKey: String) =
        requestParameterValue(sliderMeta(parameterKey).parameterInfo)

    private fun requestParameterValue(info: ParameterInfo<Int, Int, Int, Int>) {
        val packet = WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode) ?: return
        logGlobalFingerPositionTx(info, null, packet, isReadRequest = true)
        enqueuePacket(packet)
    }

    override fun setSliderValue(parameterKey: String, value: Int) {
        val meta = sliderMeta(parameterKey)
        val parameterInfo = meta.parameterInfo
        val currentTyped = if (retainEmgGainDrafts && meta.codecId == ParameterCodecIdV3.EMG_GAINS) {
            emgGainDrafts.value[ParameterStoreV3.toKey(parameterInfo)]
                ?: (readTypedValue(meta) as? ParameterTypedValueV3.EmgGains)?.takeIf { it.hasReceivedPair() }
                ?: run {
                    // As before on iOS, request the missing partner and drop this edit without retrying it.
                    requestSliderValue(parameterKey)
                    return
                }
        } else readTypedValue(meta)
        val action = ParameterCodecRegistryV3.encodeAction(
            codecId = meta.codecId,
            currentValue = currentTyped,
            action = ParameterCodecActionV3.SetInt(value, parameterInfo.dataOffsets),
        )
        val (newTyped, packet) = when (action) {
            is ParameterEncodedActionV3.IntValue ->
                ParameterTypedValueV3.Slider(SliderV3(action.value)) to BLECommandsV3.sendCommand(
                    command = parameterInfo.parameterID,
                    subcommand = parameterInfo.dataCode,
                    parameter = action.value,
                )
            is ParameterEncodedActionV3.EmgGainsValue ->
                ParameterTypedValueV3.EmgGains(EMGGainsV3(action.openGain, action.closeGain)) to
                    BLECommandsV3.sendGaines(action.openGain, action.closeGain)
            else -> return
        }

        // Preserve the existing optimistic update and scheduling order.
        if (saveValueBeforeSending) saveTypedValue(meta, newTyped)
        if (retainEmgGainDrafts && newTyped is ParameterTypedValueV3.EmgGains) retainEmgGainDraft(meta, newTyped)
        logGlobalFingerPositionTx(parameterInfo, value, packet)
        enqueuePacket(packet)
        platformLog("sendProgress", "parameter=$parameterKey value=$newTyped")
    }

    private fun ParameterTypedValueV3.EmgGains.hasReceivedPair(): Boolean =
        value.openGain != 0 && value.closeGain != 0

    private fun retainEmgGainDraft(meta: ParameterMetaV3, value: ParameterTypedValueV3.EmgGains) {
        emgGainDrafts.update { it + (ParameterStoreV3.toKey(meta.parameterInfo) to value) }
    }

    private fun saveTypedValue(meta: ParameterMetaV3, value: ParameterTypedValueV3) {
        ParameterStoreV3.put(meta.parameterInfo, value)
        saveBleValue(meta.parameterInfo, value)
        ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, value)?.let { encoded ->
            ParameterProvider.getParameterV3(meta.parameterInfo).data = encoded
        }
    }

    private fun sliderMeta(parameterKey: String): ParameterMetaV3 {
        val meta = ParameterInfoRegistry.getMeta(parameterKey)
        require(meta?.widgetKind == WidgetKindV3.SLIDER) {
            "Unsupported Slider parameter: $parameterKey"
        }
        return requireNotNull(meta)
    }

    private fun readTypedValue(meta: ParameterMetaV3): ParameterTypedValueV3? {
        return ParameterStoreV3.get(meta.parameterInfo) ?: if (readCachedValues) {
            val serialized = ParameterProvider.getParameterV3(meta.parameterInfo).data
            ParameterCodecRegistryV3.decodeFromSerialized(meta.codecId, serialized)
        } else null
    }

    private fun logGlobalFingerPositionTx(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        value: Int?,
        packet: ByteArray,
        isReadRequest: Boolean = false,
    ) {
        if (parameterInfo.parameterID != PROSTHESIS_MODULE_CONTROL.number.toInt()) return
        val parameterName = when (parameterInfo.dataCode) {
            PWCE_SET_PINCH_THUMB_POSITION.number.toInt() -> "thumb"
            PWCE_SET_PINCH_FINGER_POSITION.number.toInt() -> "index_middle"
            else -> return
        }
        val command = if (isReadRequest) when (parameterInfo.dataCode) {
            PWCE_SET_PINCH_THUMB_POSITION.number.toInt() -> PWCE_GET_PINCH_THUMB_POSITION.number.toInt()
            else -> PWCE_GET_PINCH_FINGER_POSITION.number.toInt()
        } else parameterInfo.dataCode
        platformLog(
            "V3_FINGER_POSITION",
            "${if (isReadRequest) "TX_GET" else "TX"} parameter=$parameterName " +
                "${if (isReadRequest) "command" else "set"}=0x${command.toString(16).padStart(2, '0')} " +
                "value=${value ?: "-"} packet=${EncodeByteToHex.bytesToHexString(packet)}"
        )
    }
}
