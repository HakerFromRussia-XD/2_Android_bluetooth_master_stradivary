package com.bailout.stickk.ubi4.versions.v3.domain.service

data class V3DeviceInfoTextEdit(val text: String, val limitReached: Boolean)

class EditDeviceInfoTextUseCaseV3 {
    operator fun invoke(field: V3DeviceInfoField, text: String): V3DeviceInfoTextEdit {
        val result = if (field == V3DeviceInfoField.DEVICE_NAME) V3DeviceNameInputRules.trimToLimit(text) else text
        return V3DeviceInfoTextEdit(result, result != text)
    }
}

object V3DeviceNameInputRules {
    const val MAX_INPUT_BYTES_WITHOUT_PREFIX = 13

    fun trimToLimit(value: String): String {
        var charIndex = 0
        var bytesUsed = 0
        while (charIndex < value.length) {
            val codePoint = Character.codePointAt(value, charIndex)
            val bytes = String(Character.toChars(codePoint)).encodeToByteArray().size
            if (bytesUsed + bytes > MAX_INPUT_BYTES_WITHOUT_PREFIX) break
            bytesUsed += bytes
            charIndex += Character.charCount(codePoint)
        }
        return if (charIndex == value.length) value else value.substring(0, charIndex)
    }
}
