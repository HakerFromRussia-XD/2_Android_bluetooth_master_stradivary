package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.data.sensors.V3SensorsCommandsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.StopProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ReleaseProsthesisCalibrationButtonUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.StartProsthesisCalibrationUseCaseV3

class V3ProsthesisCalibrationRepositoryTest {
    private val originalAddress = WidgetRepoProvider.mac()
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalRefresh = UiState.fullInitInProgress.value
    private val packets = mutableListOf<ByteArray>()
    private val repository = V3ProsthesisCalibrationRepositoryImpl(packets::add)
    private val start = StartProsthesisCalibrationUseCaseV3(repository)
    private val release = ReleaseProsthesisCalibrationButtonUseCaseV3(repository)

    @BeforeEach fun setUp() {
        WidgetRepoProvider.setCurrentMac("first-device")
        UiState.isInterfaceV3Activated = true
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.fullInitInProgress.value = false
    }
    @AfterEach fun tearDown() {
        WidgetRepoProvider.setCurrentMac(originalAddress)
        UiState.isInterfaceV3Activated = originalMode
        UiState.activeV3DeviceProfile = originalProfile
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.fullInitInProgress.value = originalRefresh
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

    @ParameterizedTest
    @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `native command policy retains every repeated packet with missing stale or current device context`(profile: V3DeviceProfile) {
        UiState.activeV3DeviceProfile = profile
        UiState.isInterfaceV3Activated = false
        UiState.v3WidgetsInteractionEnabled.value = false
        val sensors = V3SensorsCommandsRepositoryImpl(
            enqueuePacket = packets::add,
            refreshWidgets = { fail<Unit>("Button commands must not refresh sensors") },
            validateCommandDeviceContext = false,
        )
        val calibration = V3ProsthesisCalibrationRepositoryImpl(
            enqueuePacket = packets::add,
            validateCommandDeviceContext = false,
        )
        val move = StartProsthesisMovementUseCaseV3(sensors, requireInteractionEnabled = false)
        val stop = StopProsthesisMovementUseCaseV3(sensors)
        val calibrate = StartProsthesisCalibrationUseCaseV3(calibration, requireInteractionEnabled = false)
        val releaseCalibration = ReleaseProsthesisCalibrationButtonUseCaseV3(calibration)
        val expectedBytes = mapOf(
            0 to byteArrayOf(0x00, 0x0F, 0x00, 0x00, 0x5F),
            1 to byteArrayOf(0x00, 0x0F, 0x01, 0x00, 0x9B.toByte()),
            2 to byteArrayOf(0x00, 0x0F, 0x02, 0x00, 0xCE.toByte()),
            3 to byteArrayOf(0x00, 0x0F, 0x03, 0x00, 0x0A),
        )
        val contexts = listOf("" to "", "first-device" to "null", "second-device" to "first-device", "first-device" to "first-device")
        for ((currentAddress, screenAddress) in contexts) {
            WidgetRepoProvider.setCurrentMac(currentAddress)
            val firstPacket = packets.size
            repeat(2) { assertTrue(move(screenAddress, V3ProsthesisMovement.OPEN)) }
            // A device switch between DOWN and UP must not suppress the native zero packet.
            WidgetRepoProvider.setCurrentMac("changed-device")
            repeat(2) { stop(screenAddress) }
            repeat(2) { assertTrue(move(screenAddress, V3ProsthesisMovement.CLOSE)) }
            repeat(2) { stop(screenAddress) }
            repeat(2) { assertTrue(calibrate(screenAddress)) }
            repeat(2) { releaseCalibration(screenAddress) }
            // Native releases also enqueue when no accepted press remains to match them.
            stop(screenAddress)
            releaseCalibration(screenAddress)
            val expectedCommands = listOf(1, 1, 0, 0, 2, 2, 0, 0, 3, 3, 0, 0, 0, 0)
            val queued = packets.drop(firstPacket)
            assertEquals(expectedCommands.size, queued.size)
            expectedCommands.zip(queued).forEach { (subcommand, packet) ->
                assertArrayEquals(expectedBytes.getValue(subcommand), packet)
            }
        }
        assertFalse(sensors.interactionEnabled.value)
        assertFalse(calibration.interactionEnabled.value)
        assertFalse(UiState.fullInitInProgress.value)
    }

    @Test fun `native data policy and start use case bypass are separate opt ins without replacing factual readiness`() {
        val sensors = V3SensorsCommandsRepositoryImpl(packets::add, {}, validateCommandDeviceContext = false)
        val calibration = V3ProsthesisCalibrationRepositoryImpl(packets::add, validateCommandDeviceContext = false)
        assertSame(UiState.v3WidgetsInteractionEnabled, sensors.interactionEnabled)
        assertSame(UiState.v3WidgetsInteractionEnabled, calibration.interactionEnabled)
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(StartProsthesisMovementUseCaseV3(sensors)("first-device", V3ProsthesisMovement.OPEN))
        assertFalse(StartProsthesisCalibrationUseCaseV3(calibration)("first-device"))
        val defaultSensors = V3SensorsCommandsRepositoryImpl(packets::add, {})
        val moveWithOnlyUseCaseBypass = StartProsthesisMovementUseCaseV3(defaultSensors, requireInteractionEnabled = false)
        val calibrateWithOnlyUseCaseBypass = StartProsthesisCalibrationUseCaseV3(repository, requireInteractionEnabled = false)
        assertFalse(moveWithOnlyUseCaseBypass("first-device", V3ProsthesisMovement.OPEN))
        assertFalse(calibrateWithOnlyUseCaseBypass("first-device"))
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.isInterfaceV3Activated = false
        assertFalse(moveWithOnlyUseCaseBypass("first-device", V3ProsthesisMovement.CLOSE))
        assertFalse(calibrateWithOnlyUseCaseBypass("first-device"))
        UiState.isInterfaceV3Activated = true
        for (address in listOf("", "null", "second-device")) {
            assertFalse(moveWithOnlyUseCaseBypass(address, V3ProsthesisMovement.OPEN))
            assertFalse(calibrateWithOnlyUseCaseBypass(address))
            StopProsthesisMovementUseCaseV3(defaultSensors)(address)
            release(address)
        }
        WidgetRepoProvider.setCurrentMac("second-device")
        StopProsthesisMovementUseCaseV3(defaultSensors)("first-device")
        release("first-device")
        assertTrue(packets.isEmpty())
        assertTrue(sensors.interactionEnabled.value)
        assertTrue(calibration.interactionEnabled.value)
        WidgetRepoProvider.setCurrentMac("first-device")
        assertTrue(StartProsthesisMovementUseCaseV3(defaultSensors)("first-device", V3ProsthesisMovement.CLOSE))
        assertTrue(start("first-device"))
        assertEquals(2, packets.size)
        assertPacket(packets[0], 2)
        assertPacket(packets[1], 3)
    }

    @Test fun `native command policy retains the sensor refresh identity mode and reentry guards`() {
        var refreshes = 0
        val sensors = V3SensorsCommandsRepositoryImpl(
            enqueuePacket = packets::add,
            refreshWidgets = {
                assertTrue(UiState.fullInitInProgress.value)
                refreshes++
            },
            validateCommandDeviceContext = false,
        )
        val calibration = V3ProsthesisCalibrationRepositoryImpl(packets::add, validateCommandDeviceContext = false)
        val refresh = RefreshSensorsUseCaseV3(sensors)
        assertSame(UiState.fullInitInProgress, sensors.refreshInProgress)
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.isInterfaceV3Activated = false
        assertFalse(refresh("first-device"))
        assertFalse(sensors.refreshSensors("first-device"))
        UiState.isInterfaceV3Activated = true
        WidgetRepoProvider.setCurrentMac("second-device")
        for (address in listOf("first-device", "", "null")) assertFalse(refresh(address))
        WidgetRepoProvider.setCurrentMac("first-device")
        UiState.fullInitInProgress.value = true
        assertFalse(refresh("first-device"))
        assertFalse(sensors.refreshSensors("first-device"))
        assertEquals(0, refreshes)
        UiState.fullInitInProgress.value = false
        assertTrue(refresh("first-device"))
        assertEquals(1, refreshes)
        assertTrue(sensors.refreshInProgress.value)
        assertFalse(refresh("first-device"))
        assertEquals(1, refreshes)
        assertFalse(sensors.interactionEnabled.value)
        assertFalse(calibration.interactionEnabled.value)
        UiState.v3WidgetsInteractionEnabled.value = true
        assertTrue(sensors.interactionEnabled.value)
        assertTrue(calibration.interactionEnabled.value)
        assertTrue(packets.isEmpty())
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
