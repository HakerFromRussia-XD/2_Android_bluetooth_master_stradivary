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
    fun `both profiles send only the temporary calibration packet`(profile: V3DeviceProfile) {
        UiState.activeV3DeviceProfile = profile
        assertTrue(start("first-device"))
        release("first-device")
        assertEquals(1, packets.size)
        assertArrayEquals(byteArrayOf(0x00, 0x0E, 0x09, 0xFF.toByte(), 0x73), packets[0])
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "null", "second-device"])
    fun `invalid or different device receives neither start nor release`(address: String) {
        assertFalse(start(address)); release(address)
        assertTrue(packets.isEmpty())
    }

    @Test fun `lock rejects start and release sends no additional packet during the test`() {
        assertTrue(start("first-device"))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(start("first-device"))
        assertFalse(repository.startCalibration("first-device"))
        release("first-device")
        assertEquals(1, packets.size)
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

}
