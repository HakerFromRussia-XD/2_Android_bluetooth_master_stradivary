package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4

/**
 * V3 device-name display/transport rules.
 * UI can show stripped name, while storage/transport keeps full name.
 */
object DeviceNameBridgeV3 {
    private val prefix = ConstantManagerUBI4.DEVICE_NAME_PREFIX

    fun displayName(deviceName: String?): String {
        val normalized = deviceName?.trim().orEmpty()
        if (normalized.isEmpty()) return ""

        return if (normalized.startsWith(prefix, ignoreCase = true)) {
            normalized.removePrefix(prefix)
        } else {
            normalized
        }
    }

    fun applyPrefixForTransport(rawName: String?): String {
        val normalized = rawName?.trim().orEmpty()
        if (normalized.isEmpty()) return prefix
        if (normalized.startsWith(prefix, ignoreCase = true)) return normalized
        return prefix + normalized.removePrefix("-")
    }

    fun hasTransportPrefix(deviceName: String?): Boolean {
        val normalized = deviceName?.trim().orEmpty()
        return normalized.startsWith(prefix, ignoreCase = true)
    }

    fun transportPrefix(): String = prefix
}
