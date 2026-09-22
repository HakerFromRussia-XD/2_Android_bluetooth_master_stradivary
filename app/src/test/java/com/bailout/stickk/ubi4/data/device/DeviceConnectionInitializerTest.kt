package com.bailout.stickk.ubi4.data.device

import android.content.Intent
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeviceConnectionInitializerTest {
    private val previousName = runCatching { ConnectionState.connectedDeviceName }.getOrDefault("")
    private val previousAddress = runCatching { ConnectionState.connectedDeviceAddress }.getOrDefault("")
    private val previousProfile = UiState.activeV3DeviceProfile
    private val previousEnabled = UiState.isInterfaceV3Activated

    @AfterEach fun restoreContext() {
        ConnectionState.connectedDeviceName = previousName
        ConnectionState.connectedDeviceAddress = previousAddress
        UiState.activeV3DeviceProfile = previousProfile
        UiState.isInterfaceV3Activated = previousEnabled
    }

    private fun launch(name: String? = null, address: String? = null, profile: String? = null, type: String? = null) {
        val intent = mockk<Intent>()
        every { intent.getStringExtra(any()) } returns null
        every { intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME) } returns name
        every { intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS) } returns address
        every { intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_PROFILE) } returns profile
        every { intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_TYPE) } returns type
        DeviceConnectionInitializer.initialize(intent)
    }

    private fun assertProfile(expected: V3DeviceProfile) {
        assertEquals(expected, UiState.activeV3DeviceProfile)
        assertEquals(expected != V3DeviceProfile.NOT_V3, UiState.isInterfaceV3Activated)
    }

    @Test fun `explicit scan profile overrides device name including NOT_V3`() {
        launch(name = "00001", profile = "INDY3")
        assertProfile(V3DeviceProfile.INDY3)
        launch(name = "INDY3-00001", profile = "STANDARD_V3")
        assertProfile(V3DeviceProfile.STANDARD_V3)
        launch(name = "FTHS3-00001", profile = "NOT_V3")
        assertProfile(V3DeviceProfile.NOT_V3)
    }

    @Test fun `missing and invalid profile preserve name fallback for both versions`() {
        for (profile in listOf(null, "", "unknown", "indy3", " INDY3 ")) {
            launch(name = "FTHS3-00001", profile = profile)
            assertProfile(V3DeviceProfile.STANDARD_V3)
            launch(name = " indy3-00002 ", profile = profile)
            assertProfile(V3DeviceProfile.INDY3)
            launch(name = "UBIv4_CPU_Roma", profile = profile)
            assertProfile(V3DeviceProfile.NOT_V3)
        }
    }

    @Test fun `device type alone does not override final name classification`() {
        // The old startup first checked type, then unconditionally applied the name/profile bridge.
        launch(name = "00001", type = "FTHS3")
        assertProfile(V3DeviceProfile.NOT_V3)
        launch(name = "INDY3-00001", type = "UBIv4_CPU_Roma")
        assertProfile(V3DeviceProfile.INDY3)
    }

    @Test fun `new launch replaces prior context without normalizing name or address and missing extras clear it`() {
        launch(name = " FTHS3-00001 ", address = " aB:Cd ")
        assertEquals(" FTHS3-00001 ", ConnectionState.connectedDeviceName)
        assertEquals(" aB:Cd ", ConnectionState.connectedDeviceAddress)
        assertProfile(V3DeviceProfile.STANDARD_V3)
        launch(name = "INDY3-00002", address = "other")
        assertEquals("INDY3-00002", ConnectionState.connectedDeviceName)
        assertEquals("other", ConnectionState.connectedDeviceAddress)
        assertProfile(V3DeviceProfile.INDY3)
        launch()
        assertEquals("", ConnectionState.connectedDeviceName)
        assertEquals("", ConnectionState.connectedDeviceAddress)
        assertProfile(V3DeviceProfile.NOT_V3)
    }
}
