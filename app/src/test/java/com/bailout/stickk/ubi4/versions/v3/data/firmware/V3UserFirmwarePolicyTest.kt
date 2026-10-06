package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.firmware.user.*
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwarePolicy
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Android consumers exercise the real common policy and its existing serialized models. */
class V3UserFirmwarePolicyTest {
    private val installed = UserFirmwareVersion(0, 6, 9)
    private val available = UserFirmwareVersion(0, 6, 10)
    private fun target(address: Int) = UserFirmwareTarget(
        AssemblyModule(address, file = "$address.zip", sha256 = "a".repeat(64)), available, "/cache/$address.zip",
    )

    @Test fun `interaction blocking preserves the existing phase rules and status defaults`() {
        val phases = mapOf("idle" to false, "checking" to false, "unavailable" to false,
            "waiting" to false, "unknown" to false, "offered" to true, "preparing" to true,
            "updating" to true, "verifying" to true, "complete" to true)
        phases.forEach { (phase, expected) ->
            assertEquals(expected, V3UserFirmwarePolicy.blocksInteraction(phase), phase)
            assertEquals(expected, UserFirmwareUiState(phase).blocksInteraction, phase)
        }
        // Existing Android callers supply the blocking flag explicitly.
        assertFalse(V3UserFirmwareStatus("offered").blocksInteraction)
        assertEquals("", V3UserFirmwareStatus().detail)
    }

    @Test fun `queue preserves archive objects and board order with address zero last`() {
        val targets = listOf(0, 33, 9, 32, 34, 35).map(::target)
        val boards = listOf(
            UserFirmwareBoard(0, installed, true), UserFirmwareBoard(33, installed, false),
            UserFirmwareBoard(9, available, true), UserFirmwareBoard(32, UserFirmwareVersion(1, 0, 0), true),
            UserFirmwareBoard(35, installed, true),
        )
        val queue = UserFirmwarePolicy.queue(boards, targets)
        assertEquals(listOf(33, 35, 0), queue.map { it.module.address })
        queue.forEach { assertSame(targets.single { target -> target.module.address == it.module.address }, it) }
        assertEquals(queue, V3UserFirmwarePolicy.queue(boards.associate { it.address to it.version }, targets,
            { it.module.address }, { it.version }))
    }

    @Test fun `unknown present version fails while unrelated boards and overwritten readings keep existing behavior`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            UserFirmwarePolicy.queue(listOf(UserFirmwareBoard(9, null, true)), listOf(target(9)))
        }
        assertEquals("Unknown firmware version at 9", error.message)
        assertTrue(UserFirmwarePolicy.queue(listOf(UserFirmwareBoard(9, null, true)), listOf(target(32))).isEmpty())
        assertTrue(UserFirmwarePolicy.queue(listOf(UserFirmwareBoard(9, installed, true),
            UserFirmwareBoard(9, available, true)), listOf(target(9))).isEmpty())
    }

    @Test fun `completion and retry preserve main program bootloader and version conditions`() {
        for (main in listOf(false, true)) {
            for (version in listOf(null, installed, available, UserFirmwareVersion(1, 0, 0))) {
                val board = UserFirmwareBoard(32, version, main)
                assertEquals(main && version == available, UserFirmwarePolicy.completed(board, target(32)))
                assertEquals(!main && version != null && version != available, UserFirmwarePolicy.retry(board, target(32)))
            }
        }
    }

    @Test fun `version parsing and comparison keep byte limits and numeric component ordering`() {
        assertTrue(UserFirmwareVersion(1, 0, 0) > UserFirmwareVersion(0, 255, 255))
        assertTrue(UserFirmwareVersion(0, 7, 0) > UserFirmwareVersion(0, 6, 255))
        assertTrue(available > installed)
        assertEquals(0, available.compareTo(UserFirmwareVersion(0, 6, 10)))
        assertEquals(available, UserFirmwareVersion.parse("0.6.10"))
        assertEquals("0.6.10", available.toString())
        assertEquals(UserFirmwareVersion(255, 255, 255), UserFirmwareVersion.parse("255.255.255"))
        listOf(null, "—", "0.x.10", "0.6.10.20", "-1.0.0", "0.256.0", "0.0.256").forEach {
            assertNull(UserFirmwareVersion.parse(it))
        }
    }

    @Test fun `frozen queue keeps the existing journal format and restores without lost targets`() {
        val target = target(32)
        val text = """{"deviceId":"00001","targets":[{"module":{"address":32,"file":"32.zip","sha256":"${"a".repeat(64)}"},"version":{"major":0,"minor":6,"patch":10},"path":"/cache/32.zip"}],"completed":[32],"attempted":[32],"formatVersion":2}"""
        val journal = UserFirmwareJournal("00001", listOf(target), setOf(32), setOf(32), 2)
        assertEquals(journal, Json.decodeFromString<UserFirmwareJournal>(text))
        assertEquals(Json.parseToJsonElement(text), Json.parseToJsonElement(Json.encodeToString(journal)))
    }
}
