package com.bailout.stickk.ubi4.versions.v3.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.V3DeviceNameInputRules

class V3DeviceNameInputRulesTest {
    @Test
    fun `device name accepts thirteen ASCII bytes without prefix`() {
        assertEquals(13, V3DeviceNameInputRules.MAX_INPUT_BYTES_WITHOUT_PREFIX)
        assertEquals("ABCDEFGHIJKLM", V3DeviceNameInputRules.trimToLimit("ABCDEFGHIJKLMN"))
    }

    @Test
    fun `device name limit preserves complete UTF-8 characters`() {
        assertEquals("ПротезA", V3DeviceNameInputRules.trimToLimit("ПротезAB"))
        assertEquals("ABC😀DEFGHI", V3DeviceNameInputRules.trimToLimit("ABC😀DEFGHIJ"))
    }

    @Test
    fun `common rule matches previous JVM truncation for every UTF16 code unit near the limit`() {
        for (prefixSize in 10..13) {
            val prefix = "A".repeat(prefixSize)
            for (code in Char.MIN_VALUE.code..Char.MAX_VALUE.code) {
                val input = prefix + code.toChar() + "Z"
                assertEquals(previousJvmTrim(input), V3DeviceNameInputRules.trimToLimit(input)) {
                    "Different truncation for prefix $prefixSize and UTF16 code $code"
                }
            }
        }
    }

    @Test
    fun `common rule preserves paired and unpaired surrogates at every input boundary`() {
        val samples = listOf(
            "😀😀😀😀", "\uD800\uDC00", "\uDBFF\uDFFF",
            "\uD800", "\uDC00", "\uD800\uD800\uDC00", "\uDC00\uD800",
            "\uDC00\uD800\uDC00", "я界😀", "e\u0301😀",
        )
        for (prefixSize in 0..14) {
            for (sample in samples) {
                val input = "A".repeat(prefixSize) + sample + "XYZ"
                assertEquals(previousJvmTrim(input), V3DeviceNameInputRules.trimToLimit(input))
            }
        }
    }

    // The pre-migration Android algorithm is the reference for behavior preservation.
    private fun previousJvmTrim(value: String): String {
        var charIndex = 0
        var bytesUsed = 0
        while (charIndex < value.length) {
            val codePoint = Character.codePointAt(value, charIndex)
            val bytes = String(Character.toChars(codePoint)).encodeToByteArray().size
            if (bytesUsed + bytes > 13) break
            bytesUsed += bytes
            charIndex += Character.charCount(codePoint)
        }
        return if (charIndex == value.length) value else value.substring(0, charIndex)
    }
}
