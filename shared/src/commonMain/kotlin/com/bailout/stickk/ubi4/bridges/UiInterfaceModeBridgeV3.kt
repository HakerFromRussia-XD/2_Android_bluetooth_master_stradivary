package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.DeviceSerialClassifier
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

/**
 * Separate bridge for V3 mode detection/activation from iOS.
 * Legacy UBI4 flow is kept intact.
 */
object UiInterfaceModeBridgeV3 {
    fun isEnabled(): Boolean = UiState.isInterfaceV3Activated

    fun activeProfile(): V3DeviceProfile = UiState.activeV3DeviceProfile

    fun setActiveProfile(profile: V3DeviceProfile) {
        UiState.activeV3DeviceProfile = profile
        UiState.isInterfaceV3Activated = profile != V3DeviceProfile.NOT_V3
    }

    fun setEnabled(enabled: Boolean) {
        UiState.isInterfaceV3Activated = enabled
        UiState.activeV3DeviceProfile = when {
            !enabled -> V3DeviceProfile.NOT_V3
            UiState.activeV3DeviceProfile == V3DeviceProfile.NOT_V3 -> V3DeviceProfile.STANDARD_V3
            else -> UiState.activeV3DeviceProfile
        }
    }

    fun isV3DeviceName(deviceName: String?): Boolean {
        return DeviceSerialClassifier.classifyV3(deviceName) != V3DeviceProfile.NOT_V3
    }

    fun isUbiDeviceFamily(deviceName: String?): Boolean {
        return DeviceSerialClassifier.isUbiDeviceFamily(deviceName)
    }

    fun updateFromDeviceName(deviceName: String?): Boolean {
        val profile = DeviceSerialClassifier.classifyV3(deviceName)
        setActiveProfile(profile)
        return UiState.isInterfaceV3Activated
    }
}
