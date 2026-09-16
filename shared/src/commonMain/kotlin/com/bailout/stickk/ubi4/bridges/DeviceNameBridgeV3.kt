package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.DeviceSerialClassifier
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4

/**
 * V3 device-name display/transport rules.
 * UI can show stripped name, while storage/transport keeps full name.
 */
object DeviceNameBridgeV3 {
    private val defaultPrefix = ConstantManagerUBI4.DEVICE_NAME_PREFIX
    private const val INDY3_PREFIX = "INDY3-"

    fun displayName(deviceName: String?): String {
        val normalized = deviceName?.trim().orEmpty()
        if (normalized.isEmpty()) return ""

        val prefix = DeviceSerialClassifier.v3TransportPrefix(normalized) ?: return normalized
        return normalized.substring(prefix.length)
    }

    fun applyPrefixForTransport(rawName: String?): String {
        val normalized = rawName?.trim().orEmpty()
        val prefix = activeTransportPrefix()
        if (normalized.isEmpty()) return prefix
        val enteredPrefix = DeviceSerialClassifier.v3TransportPrefix(normalized)
        val normalizedSuffix = enteredPrefix
            ?.let { normalized.substring(it.length) }
            ?: normalized.removePrefix("-")
        return prefix + normalizedSuffix.removePrefix("-")
    }

    fun hasTransportPrefix(deviceName: String?): Boolean {
        val normalized = deviceName?.trim().orEmpty()
        return DeviceSerialClassifier.v3TransportPrefix(normalized) != null
    }

    fun hasDisplayNameChanged(
        currentDeviceName: String?,
        newDeviceName: String?
    ): Boolean {
        val newDisplayName = displayName(newDeviceName)
        return newDisplayName.isNotBlank() &&
            newDisplayName != displayName(currentDeviceName)
    }

    fun transportPrefix(): String = activeTransportPrefix()

    private fun activeTransportPrefix(): String =
        if (UiState.activeV3DeviceProfile == V3DeviceProfile.INDY3) {
            INDY3_PREFIX
        } else {
            defaultPrefix
        }
}
