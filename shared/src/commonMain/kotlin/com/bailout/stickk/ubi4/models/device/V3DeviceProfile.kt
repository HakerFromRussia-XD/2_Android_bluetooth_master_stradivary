package com.bailout.stickk.ubi4.models.device

enum class V3DeviceProfile {
    INDY3,
    STANDARD_V3,
    NOT_V3
}

/**
 * Single source of truth for device-name classification used by Android and iOS.
 * Rules are intentionally ordered from the most specific family to broad legacy markers.
 */
object DeviceSerialClassifier {
    private const val INDY3_PREFIX = "INDY3"
    private const val LEGACY_UBI_MARKER = "UBIV4"

    private val standardV3Markers = listOf(
        "FTFS3",
        "FTFO3",
        "FTHS3",
        "FTHO3",
        "FTEP3",
        "FTEB3"
    )

    private val legacySupportedMarkers = listOf(
        "HRSTM",
        "BLE_TEST_SERVICE",
        "MLT",
        "FNG",
        "FNS",
        "MLX",
        "FNX",
        "STR",
        "CBY",
        "IND",
        "HND",
        "NEMO",
        "STAND",
        "BT05",
        "FEST"
    )

    fun classifyV3(deviceName: String?): V3DeviceProfile {
        val serial = normalize(deviceName)
        if (serial.isEmpty()) return V3DeviceProfile.NOT_V3

        if (serial.startsWith(INDY3_PREFIX)) return V3DeviceProfile.INDY3
        if (standardV3Markers.any(serial::contains)) return V3DeviceProfile.STANDARD_V3

        return V3DeviceProfile.NOT_V3
    }

    fun isKnownDeviceName(deviceName: String?): Boolean {
        val serial = normalize(deviceName)
        if (serial.isEmpty()) return false

        return classifyV3(serial) != V3DeviceProfile.NOT_V3 ||
            serial.contains(LEGACY_UBI_MARKER) ||
            legacySupportedMarkers.any(serial::contains)
    }

    fun isUbiDeviceFamily(deviceName: String?): Boolean {
        val serial = normalize(deviceName)
        return serial.contains(LEGACY_UBI_MARKER) ||
            classifyV3(serial) != V3DeviceProfile.NOT_V3
    }

    fun v3TransportPrefix(deviceName: String?): String? {
        val serial = normalize(deviceName)
        return when {
            serial.startsWith(INDY3_PREFIX) -> "$INDY3_PREFIX-"
            else -> standardV3Markers
                .firstOrNull(serial::startsWith)
                ?.let { "$it-" }
        }
    }

    private fun normalize(deviceName: String?): String =
        deviceName?.trim()?.uppercase().orEmpty()
}
