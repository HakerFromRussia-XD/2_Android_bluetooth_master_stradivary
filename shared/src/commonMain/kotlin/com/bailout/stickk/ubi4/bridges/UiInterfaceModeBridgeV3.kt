package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState

/**
 * Separate bridge for V3 mode detection/activation from iOS.
 * Legacy UBI4 flow is kept intact.
 */
object UiInterfaceModeBridgeV3 {
    private const val legacyUbiMarker = "UBIv4"
    private val v3NameMarkers = listOf(
        "FTFS3",
        "FTFO3",
        "FTHS3",
        "FTHO3",
        "FTEP3",
        "FTEB3"
    )

    fun isEnabled(): Boolean = UiState.isInterfaceV3Activated

    fun setEnabled(enabled: Boolean) {
        UiState.isInterfaceV3Activated = enabled
    }

    fun isV3DeviceName(deviceName: String?): Boolean {
        val normalized = deviceName?.trim().orEmpty()
        if (normalized.isEmpty()) return false
        return v3NameMarkers.any { marker ->
            normalized.contains(marker, ignoreCase = true)
        }
    }

    fun isUbiDeviceFamily(deviceName: String?): Boolean {
        val normalized = deviceName?.trim().orEmpty()
        if (normalized.isEmpty()) return false
        return normalized.contains(legacyUbiMarker, ignoreCase = true) ||
            isV3DeviceName(normalized)
    }

    fun updateFromDeviceName(deviceName: String?): Boolean {
        val isV3 = isV3DeviceName(deviceName)
        UiState.isInterfaceV3Activated = isV3
        return isV3
    }
}
