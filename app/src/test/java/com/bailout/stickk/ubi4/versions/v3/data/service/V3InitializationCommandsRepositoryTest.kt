package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Calendar

class V3InitializationCommandsRepositoryTest {
    private fun date(second: Int = 7) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 28, 19, 28, second) }

    @Test fun `all eighteen outgoing packets match screenshot order and CRC without replies or duplicates`() {
        val packets = mutableListOf<ByteArray>()
        val repository = V3InitializationCommandsRepositoryImpl({ packets += it }, { date() })
        val commands = repository.getCommands()
        assertEquals(listOf(
            "00 06 01 00 15", "80 10 09 00 21 0B EA 07 0A 1C 02 13 1C 07 DD",
            "00 0F 30 00 72", "00 12 02 00 94", "00 12 06 00 AF", "00 12 04 00 3E",
            "00 0F 16 00 19", "00 10 0D 00 C3", "00 0F 3A 00 95", "00 10 0F 00 52",
            "00 10 10 00 A6", "00 0F 3C 00 3F", "00 0F 2E 00 42", "00 0F 3E 00 AE",
            "00 0F 40 00 C4", "00 01 0A 00 4C", "00 0F 45 00 3B", "00 0F 47 00 AA",
        ), commands.map { it.hex })
        assertEquals(18, commands.map { it.command }.distinct().size)
        assertEquals(18, commands.map { it.hex }.distinct().size)
        assertTrue(packets.isEmpty(), "Reading a widget must never enqueue a command")
    }

    @Test fun `one click enqueues one exact packet and date is generated at click time`() = withReadyDevice {
        val packets = mutableListOf<ByteArray>()
        var now = date()
        val repository = V3InitializationCommandsRepositoryImpl({ packets += it }, { now })
        val preview = repository.getCommands().first { it.command == PHONE_DATE_TIME }
        now = date(8)
        val sent = repository.enqueue("test-device", PHONE_DATE_TIME)!!
        assertEquals(1, packets.size)
        assertNotEquals(preview.hex, sent.hex)
        assertEquals(8, packets.single()[13].toInt())
        V3InitializationCommand.entries.filterNot { it == PHONE_DATE_TIME }.forEach {
            val before = packets.size
            val queued = repository.enqueue("test-device", it)!!
            assertEquals(before + 1, packets.size)
            assertEquals(repository.getCommands().first { entry -> entry.command == it }, queued)
        }
        assertEquals(18, packets.size)
    }

    @Test fun `disconnection wrong device and UBI4 block commands`() = withReadyDevice {
        val packets = mutableListOf<ByteArray>()
        val repository = V3InitializationCommandsRepositoryImpl({ packets += it })
        assertNull(repository.enqueue("other-device", THRESHOLD))
        BLEState.publishDisconnect()
        assertNull(repository.enqueue("test-device", THRESHOLD))
        assertFalse(runBlocking { repository.availability.first() })
        BLEState.publishReady()
        UiState.isInterfaceV3Activated = false
        assertNull(repository.enqueue("test-device", THRESHOLD))
        assertTrue(packets.isEmpty())
    }

    private fun withReadyDevice(block: () -> Unit) {
        val oldMac = WidgetRepoProvider.mac()
        val oldV3 = UiState.isInterfaceV3Activated
        val oldState = BLEState.state.value
        try {
            WidgetRepoProvider.setCurrentMac("test-device")
            UiState.isInterfaceV3Activated = true
            BLEState.publishReady()
            block()
        } finally {
            WidgetRepoProvider.setCurrentMac(oldMac)
            UiState.isInterfaceV3Activated = oldV3
            when (oldState) {
                BLEState.State.READY -> BLEState.publishReady()
                BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
                BLEState.State.CONNECTING -> BLEState.publishConnecting()
                BLEState.State.ERROR -> BLEState.publishError()
            }
        }
    }
}
