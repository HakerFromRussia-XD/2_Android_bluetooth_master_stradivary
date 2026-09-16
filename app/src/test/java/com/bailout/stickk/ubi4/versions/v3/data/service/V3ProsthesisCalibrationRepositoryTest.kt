package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class V3ProsthesisCalibrationRepositoryTest {
    private val originalAddress = WidgetRepoProvider.mac()
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val packets = mutableListOf<ByteArray>()
    private val repository = V3ProsthesisCalibrationRepositoryImpl(packets::add)
    private val start = StartProsthesisCalibrationUseCaseV3(repository)
    private val release = ReleaseProsthesisCalibrationButtonUseCaseV3(repository)

    @BeforeEach fun setUp() {
        WidgetRepoProvider.setCurrentMac("first-device")
        UiState.isInterfaceV3Activated = true
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3
        UiState.v3WidgetsInteractionEnabled.value = true
    }
    @AfterEach fun tearDown() {
        WidgetRepoProvider.setCurrentMac(originalAddress)
        UiState.isInterfaceV3Activated = originalMode
        UiState.activeV3DeviceProfile = originalProfile
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
    }

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles preserve calibration and zero release packets including CRC`(profile: V3DeviceProfile) {
        UiState.activeV3DeviceProfile = profile
        assertTrue(start("first-device"))
        release("first-device")
        assertEquals(2, packets.size)
        assertPacket(packets[0], 3)
        assertPacket(packets[1], 0)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "null", "second-device"])
    fun `invalid or different device receives neither start nor release`(address: String) {
        assertFalse(start(address)); release(address)
        assertTrue(packets.isEmpty())
    }

    @Test fun `lock rejects start but permits the release of an already accepted button press`() {
        assertTrue(start("first-device"))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(start("first-device"))
        assertFalse(repository.startCalibration("first-device"))
        release("first-device")
        assertEquals(2, packets.size)
        assertPacket(packets.last(), 0)
    }

    @Test fun `UBI4 cannot start V3 calibration and device switch blocks a late release`() {
        UiState.isInterfaceV3Activated = false
        assertFalse(start("first-device"))
        UiState.isInterfaceV3Activated = true
        assertTrue(start("first-device"))
        WidgetRepoProvider.setCurrentMac("second-device")
        release("first-device")
        assertEquals(1, packets.size)
    }

    private fun assertPacket(packet: ByteArray, subcommand: Int) {
        val header = byteArrayOf(0, 15, subcommand.toByte(), 0)
        var crc = 0
        header.forEach { byte ->
            crc = crc xor (byte.toInt() and 255)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8C else crc ushr 1 }
        }
        assertArrayEquals(header + crc.toByte(), packet)
    }
}
