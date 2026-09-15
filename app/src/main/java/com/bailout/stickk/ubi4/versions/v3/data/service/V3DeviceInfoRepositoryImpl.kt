package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.data.local.repository.AchievementEventManager
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository

/** Keeps the existing transport, name update and send-completion behavior behind a domain contract. */
class V3DeviceInfoRepositoryImpl(
    private val currentSerial: () -> String?,
    private val deviceName: () -> String?,
    private val intentDeviceName: () -> String?,
    private val applyDeviceName: (String) -> Unit,
    private val enqueuePacket: (ByteArray, () -> Unit) -> Unit,
    private val recordNameCustomization: () -> Unit = AchievementEventManager::recordDeviceNameCustomization,
    private val currentDeviceAddress: () -> String? = WidgetRepoProvider::mac,
    private val connectedName: () -> String? = { runCatching { ConnectionState.connectedDeviceName }.getOrNull() },
) : V3DeviceInfoRepository {
    override val interactionEnabled = UiState.v3WidgetsInteractionEnabled

    private fun currentDeviceName(): String? = currentSerial()?.takeUnless { it.isBlank() }
        ?: deviceName()?.takeUnless { it.isBlank() }
        ?: connectedName()?.takeUnless { it.isBlank() }
        ?: intentDeviceName()?.takeUnless { it.isBlank() }

    override fun getTextForInput(field: V3DeviceInfoField): String? = when (field) {
        V3DeviceInfoField.DEVICE_NAME -> currentDeviceName()?.let(DeviceNameBridgeV3::displayName)
        V3DeviceInfoField.SERIAL_NUMBER -> (ParameterStoreV3.get(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER))
            as? ParameterTypedValueV3.Text)?.value?.takeUnless { it.isBlank() } ?: currentDeviceName()
    }

    override fun sendText(field: V3DeviceInfoField, text: String): Boolean {
        val isName = field == V3DeviceInfoField.DEVICE_NAME
        val info = ParameterInfoRegistry.require(if (isName) P_KEY_SET_DEVICE_NAME else P_KEY_SET_SERIAL_NUMBER)
        val transportText = if (isName) DeviceNameBridgeV3.applyPrefixForTransport(text) else text
        val nameChanged = isName && DeviceNameBridgeV3.hasDisplayNameChanged(currentDeviceName(), transportText)
        val packet = WidgetCommandBridgeV3.buildSetText(info.parameterID, info.dataCode, info.deviceAddress, transportText) ?: return false
        val readPacket = if (isName) null else WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode)
        val address = currentDeviceAddress()
        val profile = UiState.activeV3DeviceProfile
        var sentHandled = false
        enqueuePacket(packet) {
            if (!sentHandled && address == currentDeviceAddress() && profile == UiState.activeV3DeviceProfile) {
                sentHandled = true
                if (nameChanged) recordNameCustomization()
                if (readPacket != null) enqueuePacket(readPacket) {}
            }
        }
        if (isName) applyDeviceName(transportText)
        return true
    }
}
