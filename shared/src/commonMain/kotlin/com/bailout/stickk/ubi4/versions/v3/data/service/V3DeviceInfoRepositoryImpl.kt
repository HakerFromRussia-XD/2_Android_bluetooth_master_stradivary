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
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.versions.v3.data.device.deviceInteractionEnabledState

/** Keeps the existing transport, name update and send-completion behavior behind a domain contract. */
class V3DeviceInfoRepositoryImpl private constructor(
    private val deviceIdentity: V3DeviceIdentityStore?,
    private val enqueuePacket: (ByteArray, () -> Unit) -> Unit,
    private val recordNameCustomization: () -> Unit,
    private val currentDeviceAddress: () -> String?,
    private val connectedName: () -> String?,
    private val readDeviceNameInput: (() -> String?)?,
    private val onDeviceNameQueued: ((String) -> Unit)?,
) : V3DeviceInfoRepository {
    constructor(
        deviceIdentity: V3DeviceIdentityStore,
        enqueuePacket: (ByteArray, () -> Unit) -> Unit,
        recordNameCustomization: () -> Unit = AchievementEventManager::recordDeviceNameCustomization,
        currentDeviceAddress: () -> String? = WidgetRepoProvider::mac,
        connectedName: () -> String? = { kotlin.runCatching { ConnectionState.connectedDeviceName }.getOrNull() },
    ) : this(deviceIdentity, enqueuePacket, recordNameCustomization, currentDeviceAddress, connectedName, null, null)

    /** iOS name storage and notification are platform operations, performed immediately after enqueue. */
    constructor(
        readDeviceNameInput: () -> String?,
        enqueuePacket: (ByteArray, () -> Unit) -> Unit,
        onDeviceNameQueued: (String) -> Unit,
    ) : this(null, enqueuePacket, AchievementEventManager::recordDeviceNameCustomization, WidgetRepoProvider::mac,
        { kotlin.runCatching { ConnectionState.connectedDeviceName }.getOrNull() }, readDeviceNameInput, onDeviceNameQueued)

    override val interactionEnabled = deviceInteractionEnabledState

    private fun currentDeviceName(): String? = deviceIdentity?.identity?.value?.serial?.takeUnless { it.isBlank() }
        ?: deviceIdentity?.identity?.value?.deviceName?.takeUnless { it.isBlank() }
        ?: connectedName()?.takeUnless { it.isBlank() }
        ?: deviceIdentity?.intentDeviceName?.takeUnless { it.isBlank() }

    override fun getTextForInput(field: V3DeviceInfoField): String? = when (field) {
        V3DeviceInfoField.DEVICE_NAME -> if (readDeviceNameInput != null) {
            DeviceNameBridgeV3.displayName(readDeviceNameInput())
        } else currentDeviceName()?.let(DeviceNameBridgeV3::displayName)
        V3DeviceInfoField.SERIAL_NUMBER -> (ParameterStoreV3.get(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER))
            as? ParameterTypedValueV3.Text)?.value?.takeUnless { it.isBlank() } ?: currentDeviceName()
    }

    override fun sendText(field: V3DeviceInfoField, text: String): Boolean {
        val isName = field == V3DeviceInfoField.DEVICE_NAME
        val info = ParameterInfoRegistry.require(if (isName) P_KEY_SET_DEVICE_NAME else P_KEY_SET_SERIAL_NUMBER)
        val transportText = if (isName) DeviceNameBridgeV3.applyPrefixForTransport(text) else text
        val nameChanged = isName && onDeviceNameQueued == null && DeviceNameBridgeV3.hasDisplayNameChanged(currentDeviceName(), transportText)
        val packet = WidgetCommandBridgeV3.buildSetText(info.parameterID, info.dataCode, info.deviceAddress, transportText) ?: return false
        if (isName && onDeviceNameQueued != null) {
            enqueuePacket(packet) {}
            onDeviceNameQueued(transportText)
            return true
        }
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
        if (isName) deviceIdentity?.applyDeviceName(transportText)
        return true
    }
}
