package com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    fun `INDY3 prefixed current name restores INDY3 transport prefix`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        assertEquals(
            "INDY3-MY-HAND",
            DeviceNameBridgeV3.transportName("MY-HAND", "INDY3-0000000000")
        )
        assertEquals(
            "INDY3-MY-HAND",
            DeviceNameBridgeV3.transportName("fths3-MY-HAND", "INDY3-0000000000")
        )
        assertEquals("INDY3-", DeviceNameBridgeV3.transportPrefix())
    }

    @Test
    fun `all standard V3 prefixes keep standard prefixed mode`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3

        listOf("FTFS3", "FTFO3", "FTHS3", "FTHO3", "FTEP3", "FTEB3").forEach { marker ->
            assertEquals(
                "FTHS3-MY-HAND",
                DeviceNameBridgeV3.transportName("MY-HAND", "$marker-0000000000")
            )
        }
        assertEquals("MY-HAND", DeviceNameBridgeV3.displayName("FTHS3-MY-HAND"))
    }

    @Test
    fun `prefix-free current name stays prefix-free for both active profiles`() {
        listOf(V3DeviceProfile.INDY3, V3DeviceProfile.STANDARD_V3).forEach { profile ->
            UiState.activeV3DeviceProfile = profile

            assertEquals(
                "MY-HAND",
                DeviceNameBridgeV3.transportName("MY-HAND", "0000000000")
            )
            assertEquals(
                "NEXT-NAME",
                DeviceNameBridgeV3.transportName("INDY3-NEXT-NAME", "MY-HAND")
            )
        }
    }

    @Test
    fun `missing current name uses prefix-free mode`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        assertEquals("MY-HAND", DeviceNameBridgeV3.transportName(" MY-HAND ", null))
        assertEquals("MY-HAND", DeviceNameBridgeV3.transportName("-MY-HAND", ""))
    }

    @Test
    fun `prefix-only input is rejected`() {
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3

        assertNull(DeviceNameBridgeV3.transportName("INDY3-", "INDY3-0000000000"))
        assertNull(DeviceNameBridgeV3.transportName(" fths3- ", "FTHS3-0000000000"))
        assertNull(DeviceNameBridgeV3.transportName("   ", "0000000000"))
    }
}
