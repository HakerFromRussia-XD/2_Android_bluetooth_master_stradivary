package com.bailout.stickk.ubi4.versions.v3.data.sensors

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StopProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class V3SensorsCommandsRepositoryTest {
    private val originalMac = WidgetRepoProvider.mac()
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalRefresh = UiState.fullInitInProgress.value
    private val packets = mutableListOf<ByteArray>()
    private val events = mutableListOf<String>()
    private val repository = V3SensorsCommandsRepositoryImpl(
        enqueuePacket = { packets += it },
        refreshWidgets = { assertTrue(UiState.fullInitInProgress.value); events += "refresh" },
    )
    private val start = StartProsthesisMovementUseCaseV3(repository)
    private val stop = StopProsthesisMovementUseCaseV3(repository)
    private val refresh = RefreshSensorsUseCaseV3(repository)

    @BeforeEach
    fun setUp() {
        WidgetRepoProvider.setCurrentMac("first-device")
        UiState.isInterfaceV3Activated = true
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.fullInitInProgress.value = false
    }

    @AfterEach
    fun tearDown() {
        WidgetRepoProvider.setCurrentMac(originalMac)
        UiState.isInterfaceV3Activated = originalMode
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.fullInitInProgress.value = originalRefresh
    }

    @Test
    fun `movement and stop retain existing subcommand bytes and CRC`() {
        assertTrue(start("first-device", V3ProsthesisMovement.OPEN))
        stop("first-device")
        assertTrue(start("first-device", V3ProsthesisMovement.CLOSE))
        stop("first-device")
        assertEquals(listOf(1, 0, 2, 0), packets.map { it[2].toInt() })
        packets.forEach { packet ->
            assertEquals(5, packet.size)
            assertEquals(listOf(0, 15), packet.take(2).map { it.toInt() and 255 })
            assertEquals(0, packet[3].toInt())
            assertEquals(BLECommandsV3.calculationCRCRange(packet, 0, 4), packet[4].toInt() and 255)
        }
        assertTrue(events.isEmpty())
    }

    @Test
    fun `stop bypasses a new interaction lock and starts stay blocked`() {
        assertTrue(start("first-device", V3ProsthesisMovement.OPEN))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(start("first-device", V3ProsthesisMovement.CLOSE))
        assertFalse(repository.startMovement("first-device", V3ProsthesisMovement.CLOSE))
        UiState.isInterfaceV3Activated = false
        stop("first-device")
        assertEquals(listOf(1, 0), packets.map { it[2].toInt() })
    }

    @Test
    fun `old or missing device identity cannot target a different device`() {
        WidgetRepoProvider.setCurrentMac("second-device")
        for (address in listOf("first-device", "", "null")) {
            assertFalse(start(address, V3ProsthesisMovement.OPEN))
            stop(address)
            assertFalse(refresh(address))
        }
        WidgetRepoProvider.setCurrentMac("")
        stop("")
        assertFalse(start("", V3ProsthesisMovement.OPEN))
        assertFalse(refresh(""))
        assertTrue(packets.isEmpty())
        assertTrue(events.isEmpty())
        assertFalse(UiState.fullInitInProgress.value)
    }

    @Test
    fun `refresh sets its flag before dispatch and blocks reentry until controller completion`() {
        UiState.v3WidgetsInteractionEnabled.value = false
        assertTrue(refresh("first-device"))
        assertEquals(listOf("refresh"), events)
        assertTrue(UiState.fullInitInProgress.value)
        assertFalse(refresh("first-device"))
        assertFalse(repository.refreshSensors("first-device"))
        assertEquals(1, events.size)
        UiState.fullInitInProgress.value = false
        assertTrue(refresh("first-device"))
        assertEquals(listOf("refresh", "refresh"), events)
        assertTrue(packets.isEmpty())
    }

    @Test
    fun `outside V3 neither movement nor refresh starts`() {
        UiState.isInterfaceV3Activated = false
        assertFalse(start("first-device", V3ProsthesisMovement.OPEN))
        assertFalse(refresh("first-device"))
        assertFalse(UiState.fullInitInProgress.value)
        assertTrue(packets.isEmpty())
        assertTrue(events.isEmpty())
    }
}
