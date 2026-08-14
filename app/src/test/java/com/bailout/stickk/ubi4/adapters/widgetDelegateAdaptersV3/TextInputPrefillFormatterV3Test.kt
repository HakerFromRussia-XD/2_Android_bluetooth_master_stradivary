package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TextInputPrefillFormatterV3Test {

    @Test
    fun `received prefix-free name wins over prefixed connection fallback`() {
        assertEquals(
            "0000000000",
            TextInputCurrentNameResolverV3.resolve(
                receivedName = "0000000000",
                connectedName = "INDY3-0000000000",
                activityName = "FTHS3-0000000000",
                intentName = "FTHS3-0000000000"
            )
        )
    }
    @AfterEach
    fun resetProfile() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
    }

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

    @Test
    fun `prefixed INDY3 input keeps prefix for transport`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        assertEquals(
            "INDY3-MY-HAND",
            TextInputTransportFormatterV3.deviceName("MY-HAND", "INDY3-0000000000")
        )
    }

    @Test
    fun `prefix-free input stays prefix-free across repeated edits`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        val firstName = TextInputTransportFormatterV3.deviceName("1234567890", "0000000000")
        assertEquals("1234567890", firstName)
        assertEquals(
            "9876543210",
            TextInputTransportFormatterV3.deviceName("9876543210", firstName)
        )
    }

    @Test
    fun `manual prefix is removed before current mode is applied`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3

        assertEquals(
            "FTHS3-MY-HAND",
            TextInputTransportFormatterV3.deviceName("indy3-MY-HAND", "FTHS3-0000000000")
        )
        assertEquals(
            "MY-HAND",
            TextInputTransportFormatterV3.deviceName("fths3-MY-HAND", "0000000000")
        )
        assertNull(TextInputTransportFormatterV3.deviceName("INDY3-", "INDY3-0000000000"))
    }
}
