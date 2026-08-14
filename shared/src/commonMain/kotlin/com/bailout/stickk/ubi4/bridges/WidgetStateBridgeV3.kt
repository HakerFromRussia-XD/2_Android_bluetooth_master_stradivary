package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class ParameterSnapshotV3Bridge(
    val addressDevice: Int,
    val parameterID: Int,
    val dataCode: Int,
    val codecId: String,
    val widgetKind: String,
    val valuePath: String,
    val serializedValue: String
)

/**
 * V3-only bridge for iOS widgets:
 * one typed source of truth from ParameterStoreV3.
 */
object WidgetStateBridgeV3 {
    private val coroutineScope: CoroutineScope = MainScope()

    fun observeUpdates(callback: (ParameterSnapshotV3Bridge) -> Unit): Job =
        coroutineScope.launch {
            ParameterStoreV3.updates.collect { key ->
                snapshotByKey(key)?.let { callback(it) }
            }
        }

    fun getCurrent(
        addressDevice: Int,
        parameterID: Int,
        dataCode: Int
    ): ParameterSnapshotV3Bridge? {
        return snapshotByKey(
            ParameterStoreKeyV3(
                parameterID = parameterID,
                dataCode = dataCode,
                deviceAddress = addressDevice
            )
        )
    }

    fun getSpinnerValueOrDefault(
        addressDevice: Int,
        parameterID: Int,
        dataCode: Int,
        defaultValue: Int
    ): Int {
        val parameterInfo = ParameterInfo(
            parameterID = parameterID,
            dataCode = dataCode,
            deviceAddress = addressDevice,
            dataOffsets = 0
        )
        return (ParameterStoreV3.get(parameterInfo) as? ParameterTypedValueV3.Spinner)
            ?.value
            ?.spinnerValue
            ?: defaultValue
    }

    fun getTextValue(
        addressDevice: Int,
        parameterID: Int,
        dataCode: Int
    ): String? {
        val parameterInfo = ParameterInfo(
            parameterID = parameterID,
            dataCode = dataCode,
            deviceAddress = addressDevice,
            dataOffsets = 0
        )
        return (ParameterStoreV3.get(parameterInfo) as? ParameterTypedValueV3.Text)
            ?.value
            ?.takeUnless { it.isBlank() }
    }

    fun setTextValue(
        addressDevice: Int,
        parameterID: Int,
        dataCode: Int,
        value: String
    ) {
        val normalized = value.trim()
        if (normalized.isEmpty()) return

        ParameterStoreV3.put(
            ParameterInfo(
                parameterID = parameterID,
                dataCode = dataCode,
                deviceAddress = addressDevice,
                dataOffsets = 0
            ),
            ParameterTypedValueV3.Text(normalized)
        )
    }

    fun setSpinnerValue(
        addressDevice: Int,
        parameterID: Int,
        dataCode: Int,
        value: Int
    ) {
        ParameterStoreV3.put(
            ParameterInfo(
                parameterID = parameterID,
                dataCode = dataCode,
                deviceAddress = addressDevice,
                dataOffsets = 0
            ),
            ParameterTypedValueV3.Spinner(SpinnerV3(spinnerValue = value))
        )
    }

    private fun snapshotByKey(key: ParameterStoreKeyV3): ParameterSnapshotV3Bridge? {
        val parameterInfo = ParameterInfo(
            parameterID = key.parameterID,
            dataCode = key.dataCode,
            deviceAddress = key.deviceAddress,
            dataOffsets = 0
        )
        val typed = ParameterStoreV3.get(parameterInfo) ?: return null
        val meta = PreferenceKeysUbi4.ParameterInfoRegistry.getMeta(parameterInfo)
        val serialized = meta?.let {
            ParameterCodecRegistryV3
                .encodeToSerialized(codecId = it.codecId, typedValue = typed)
                .orEmpty()
        }.orEmpty()

        return ParameterSnapshotV3Bridge(
            addressDevice = key.deviceAddress,
            parameterID = key.parameterID,
            dataCode = key.dataCode,
            codecId = meta?.codecId?.name ?: "NONE",
            widgetKind = meta?.widgetKind?.name ?: "UNKNOWN",
            valuePath = meta?.valuePath.orEmpty(),
            serializedValue = serialized
        )
    }
}
