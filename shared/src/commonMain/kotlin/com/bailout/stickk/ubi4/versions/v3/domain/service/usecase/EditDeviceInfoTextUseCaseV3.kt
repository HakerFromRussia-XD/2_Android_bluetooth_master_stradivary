package com.bailout.stickk.ubi4.versions.v3.domain.service.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField

data class V3DeviceInfoTextEdit(val text: String, val limitReached: Boolean)

class EditDeviceInfoTextUseCaseV3(
    private val trimDeviceName: (String) -> String = V3DeviceNameInputRules::trimToLimit,
) {
    operator fun invoke(field: V3DeviceInfoField, text: String): V3DeviceInfoTextEdit {
        val result = if (field == V3DeviceInfoField.DEVICE_NAME) trimDeviceName(text) else text
        return V3DeviceInfoTextEdit(result, result != text)
    }
}

object V3DeviceNameInputRules {
    const val MAX_INPUT_BYTES_WITHOUT_PREFIX = 13

    fun trimToLimit(value: String): String {
        var charIndex = 0
        var bytesUsed = 0
        while (charIndex < value.length) {
            val code = value[charIndex].code
            val nextCode = value.getOrNull(charIndex + 1)?.code
            val charCount = if (code in 0xD800..0xDBFF && nextCode != null && nextCode in 0xDC00..0xDFFF) 2 else 1
            val bytes = when {
                charCount == 2 -> 4
                code < 0x80 -> 1
                code < 0x800 -> 2
                // Preserve the previous JVM encoder's one-byte replacement for an unpaired surrogate.
                code in 0xD800..0xDFFF -> 1
                else -> 3
            }
            if (bytesUsed + bytes > MAX_INPUT_BYTES_WITHOUT_PREFIX) break
            bytesUsed += bytes
            charIndex += charCount
        }
        return if (charIndex == value.length) value else value.substring(0, charIndex)
    }
}
