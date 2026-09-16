package com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceNameBridgeV3Test {
    @AfterTest
    fun resetProfile() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
    }

    @Test
    fun `INDY3 display name hides transport prefix`() {
        assertEquals("MY-HAND", DeviceNameBridgeV3.displayName("INDY3-MY-HAND"))
        assertTrue(DeviceNameBridgeV3.hasTransportPrefix("indy3-MY-HAND"))
    }

    @Test
    fun `INDY3 active profile restores INDY3 transport prefix`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        assertEquals("INDY3-MY-HAND", DeviceNameBridgeV3.applyPrefixForTransport("MY-HAND"))
        assertEquals("INDY3-MY-HAND", DeviceNameBridgeV3.applyPrefixForTransport("FTHS3-MY-HAND"))
        assertEquals("INDY3-", DeviceNameBridgeV3.transportPrefix())
    }

    @Test
    fun `standard V3 transport behavior stays unchanged`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3

        assertEquals("FTHS3-MY-HAND", DeviceNameBridgeV3.applyPrefixForTransport("MY-HAND"))
        assertEquals("MY-HAND", DeviceNameBridgeV3.displayName("FTHS3-MY-HAND"))
    }

    @Test
    fun `device name change compares user-visible names`() {
        assertTrue(
            DeviceNameBridgeV3.hasDisplayNameChanged(
                currentDeviceName = "FTHS3-OLD-NAME",
                newDeviceName = "FTHS3-NEW-NAME"
            )
        )
        assertFalse(
            DeviceNameBridgeV3.hasDisplayNameChanged(
                currentDeviceName = "INDY3-SAME-NAME",
                newDeviceName = "FTHS3-SAME-NAME"
            )
        )
    }
}
