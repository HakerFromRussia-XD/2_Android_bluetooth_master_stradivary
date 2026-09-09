package com.bailout.stickk.ubi4.versions.v3.data.settings

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.parser.ParameterCodecActionV3
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.parser.ParameterEncodedActionV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.ParameterMetaV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.ble.WidgetKindV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Uses the existing V3 stores, profile persistence and command queue. */
class V3DeviceSettingsRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val saveBleValue: (ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3) -> Unit =
        SettingsProfileManager::saveBleValue,
) : V3DeviceSettingsRepository, V3ToggleSliderSettingsRepository {
    override val sliderInteractionEnabled = UiState.v3WidgetsInteractionEnabled
    override val toggleSliderInteractionEnabled = UiState.v3WidgetsInteractionEnabled

    override fun observeToggleSliderValue(parameterKey: String) =
        ParameterStoreV3.values.map { getToggleSliderValue(parameterKey) }.distinctUntilChanged()

    override fun getToggleSliderValue(parameterKey: String): V3ToggleSliderValue? {
        val typed = readTypedValue(toggleSliderMeta(parameterKey)) as? ParameterTypedValueV3.Toggle ?: return null
        val packed = typed.value.toggleValue
        return V3ToggleSliderValue(timeTenths = packed and 0x7F, isEnabled = packed and 0x80 != 0)
    }

    override fun saveToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) {
        val meta = toggleSliderMeta(parameterKey)
        val typed = ParameterTypedValueV3.Toggle(ToggleV3(toggleValue = packToggleSliderValue(value)))
        ParameterStoreV3.put(meta.parameterInfo, typed)
        saveBleValue(meta.parameterInfo, typed)
        ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, typed)?.let { encoded ->
            ParameterProvider.getParameterV3(meta.parameterInfo).data = encoded
        }
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

    override fun getSliderValue(parameterKey: String): Int? {
        val meta = sliderMeta(parameterKey)
        return when (val typedValue = readTypedValue(meta)) {
            is ParameterTypedValueV3.Slider -> typedValue.value.sliderValue
            is ParameterTypedValueV3.EmgGains -> when (meta.parameterInfo.dataOffsets) {
                0 -> typedValue.value.openGain
                1 -> typedValue.value.closeGain
                else -> null
            }
            else -> null
        }
    }

    override fun setSliderValue(parameterKey: String, value: Int) {
        val meta = sliderMeta(parameterKey)
        val currentTyped = readTypedValue(meta)
        val parameterInfo = meta.parameterInfo
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
        ParameterStoreV3.put(parameterInfo, newTyped)
        saveBleValue(parameterInfo, newTyped)
        ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, newTyped)?.let { encoded ->
            ParameterProvider.getParameterV3(parameterInfo).data = encoded
        }
        logGlobalFingerPositionTx(parameterInfo, value, packet)
        enqueuePacket(packet)
        platformLog("sendProgress", "parameter=$parameterKey value=$newTyped")
    }

    private fun sliderMeta(parameterKey: String): ParameterMetaV3 {
        val meta = ParameterInfoRegistry.getMeta(parameterKey)
        require(meta?.widgetKind == WidgetKindV3.SLIDER) {
            "Unsupported Slider parameter: $parameterKey"
        }
        return requireNotNull(meta)
    }

    private fun readTypedValue(meta: ParameterMetaV3): ParameterTypedValueV3? {
        return ParameterStoreV3.get(meta.parameterInfo) ?: run {
            val serialized = ParameterProvider.getParameterV3(meta.parameterInfo).data
            ParameterCodecRegistryV3.decodeFromSerialized(meta.codecId, serialized)
        }
    }

    private fun logGlobalFingerPositionTx(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        value: Int,
        packet: ByteArray,
    ) {
        if (parameterInfo.parameterID != PROSTHESIS_MODULE_CONTROL.number.toInt()) return
        val parameterName = when (parameterInfo.dataCode) {
            PWCE_SET_PINCH_THUMB_POSITION.number.toInt() -> "thumb"
            PWCE_SET_PINCH_FINGER_POSITION.number.toInt() -> "index_middle"
            else -> return
        }
        platformLog(
            "V3_FINGER_POSITION",
            "TX platform=Android parameter=$parameterName " +
                "set=0x${parameterInfo.dataCode.toString(16).padStart(2, '0')} " +
                "value=$value packet=${EncodeByteToHex.bytesToHexString(packet)}"
        )
    }
}
