package com.bailout.stickk.ubi4.versions.v3.data.transport

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.SendDeviceSubcommandUseCaseV3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3DeviceCommandsRepositoryTest {
    @Test fun `button fallback retains zero parameter exact bytes repeats and synchronous queue order without session checks`() {
        val oldInterface = UiState.isInterfaceV3Activated
        val oldInteraction = UiState.v3WidgetsInteractionEnabled.value
        val oldMac = WidgetRepoProvider.mac()
        val oldValues = ParameterStoreV3.values.value
        val oldStartup = UiState.startupInProgress.value
        val oldFullInit = UiState.fullInitInProgress.value
        val packets = mutableListOf<ByteArray>()
        val order = mutableListOf<String>()
        val send = SendDeviceSubcommandUseCaseV3(V3DeviceCommandsRepositoryImpl {
            packets.add(it)
            order += "queued"
        })
        assertTrue(packets.isEmpty())
        try {
            UiState.v3WidgetsInteractionEnabled.value = false
            for (isV3 in listOf(false, true)) for (mac in listOf("", "null", "another-device")) {
                UiState.isInterfaceV3Activated = isV3
                WidgetRepoProvider.setCurrentMac(mac)
                val commands = listOf(1, 0, 2, 0, 3, 0, 0, 255, 256, -1, Int.MIN_VALUE, Int.MAX_VALUE)
                for (command in commands) {
                    val count = packets.size
                    send(command)
                    order += "returned"
                    assertEquals(count + 1, packets.size)
                    assertArrayEquals(WidgetCommandBridgeV3.buildSendSubcommand(command, 0), packets.last())
                    assertEquals(listOf("queued", "returned"), order.takeLast(2))
                    assertEquals(oldValues, ParameterStoreV3.values.value)
                    assertEquals(oldStartup, UiState.startupInProgress.value)
                    assertEquals(oldFullInit, UiState.fullInitInProgress.value)
                    assertFalse(UiState.v3WidgetsInteractionEnabled.value)
                    assertEquals(mac, WidgetRepoProvider.mac())
                }
            }
        } finally {
            UiState.isInterfaceV3Activated = oldInterface
            UiState.v3WidgetsInteractionEnabled.value = oldInteraction
            WidgetRepoProvider.setCurrentMac(oldMac)
        }
    }

    @Test fun `queue failure propagates without retry while next release remains a separate command`() {
        val failure = IllegalStateException("queue failed")
        val packets = mutableListOf<ByteArray>()
        var fail = true
        val send = SendDeviceSubcommandUseCaseV3(V3DeviceCommandsRepositoryImpl {
            packets.add(it)
            if (fail) throw failure
        })
        assertSame(failure, assertThrows(IllegalStateException::class.java) { send(7) })
        assertEquals(1, packets.size)
        fail = false
        send(0)
        assertEquals(2, packets.size)
        assertArrayEquals(WidgetCommandBridgeV3.buildSendSubcommand(7, 0), packets[0])
        assertArrayEquals(WidgetCommandBridgeV3.buildSendSubcommand(0, 0), packets[1])
    }
}
