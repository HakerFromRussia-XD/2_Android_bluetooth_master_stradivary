package com.bailout.stickk.ubi4.versions.v3.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
