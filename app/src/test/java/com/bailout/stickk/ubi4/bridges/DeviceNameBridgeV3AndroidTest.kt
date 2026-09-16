package com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceNameBridgeV3AndroidTest {
    @AfterEach
    fun resetProfile() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
    }

    @Test
    fun `INDY3 rename keeps INDY3 prefix`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        assertEquals("MY-HAND", DeviceNameBridgeV3.displayName("INDY3-MY-HAND"))
        assertEquals("INDY3-MY-HAND", DeviceNameBridgeV3.applyPrefixForTransport("MY-HAND"))
    }
}
