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
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Uses the existing V3 stores and queue for speed and force Slider parameters. */
class V3DeviceSettingsRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val saveBleValue: (ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3) -> Unit =
        SettingsProfileManager::saveBleValue,
) : V3DeviceSettingsRepository {
    override val sliderInteractionEnabled = UiState.v3WidgetsInteractionEnabled

    override fun observeSliderValue(parameterKey: String) =
        ParameterStoreV3.values.map { getSliderValue(parameterKey) }.distinctUntilChanged()

    override fun getSliderValue(parameterKey: String): Int? =
        (readTypedValue(parameterKey) as? ParameterTypedValueV3.Slider)?.value?.sliderValue

    override fun setSliderValue(parameterKey: String, value: Int) {
        val currentTyped = readTypedValue(parameterKey)
        val meta = ParameterInfoRegistry.requireMeta(parameterKey)
        val parameterInfo = meta.parameterInfo
        val action = ParameterCodecRegistryV3.encodeAction(
            codecId = meta.codecId,
            currentValue = currentTyped,
            action = ParameterCodecActionV3.SetInt(value, parameterInfo.dataOffsets),
        ) as? ParameterEncodedActionV3.IntValue ?: return
        val packet = BLECommandsV3.sendCommand(
            command = parameterInfo.parameterID,
            subcommand = parameterInfo.dataCode,
            parameter = action.value,
        )
        val newTyped = ParameterTypedValueV3.Slider(SliderV3(sliderValue = action.value))

        // Preserve the existing optimistic update and scheduling order.
        ParameterStoreV3.put(parameterInfo, newTyped)
        saveBleValue(parameterInfo, newTyped)
        ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, newTyped)?.let { encoded ->
            ParameterProvider.getParameterV3(parameterInfo).data = encoded
        }
        enqueuePacket(packet)
        platformLog("sendProgress", "sliderValue: ${action.value}")
    }

    private fun readTypedValue(parameterKey: String): ParameterTypedValueV3? {
        require(parameterKey == P_KEY_SPEED_SETTINGS || parameterKey == P_KEY_FORCE_SETTINGS) {
            "Slider parameter is not supported yet: $parameterKey"
        }
        val meta = ParameterInfoRegistry.requireMeta(parameterKey)
        return ParameterStoreV3.get(meta.parameterInfo) ?: run {
            val serialized = ParameterProvider.getParameterV3(meta.parameterInfo).data
            ParameterCodecRegistryV3.decodeFromSerialized(meta.codecId, serialized)
        }
    }
}
