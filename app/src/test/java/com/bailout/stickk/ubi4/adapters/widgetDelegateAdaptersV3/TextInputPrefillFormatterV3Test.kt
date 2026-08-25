package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextInputPrefillFormatterV3Test {
    @Test
    fun `INDY3 prefix is hidden only for device name`() {
        val fullValue = "INDY3-0000000000"

        assertEquals("0000000000", TextInputPrefillFormatterV3.deviceName(fullValue))
        assertEquals(fullValue, TextInputPrefillFormatterV3.serialNumber(fullValue))
    }

    @Test
    fun `device name accepts thirteen ASCII bytes without prefix`() {
        assertEquals(13, TextInputNameLimitV3.MAX_INPUT_BYTES_WITHOUT_PREFIX)
        assertEquals("ABCDEFGHIJKLM", TextInputNameLimitV3.trimToLimit("ABCDEFGHIJKLMN"))
    }

    @Test
    fun `device name limit preserves complete UTF-8 characters`() {
        assertEquals("ПротезA", TextInputNameLimitV3.trimToLimit("ПротезAB"))
        assertEquals("ABC😀DEFGHI", TextInputNameLimitV3.trimToLimit("ABC😀DEFGHIJ"))
    }
}
