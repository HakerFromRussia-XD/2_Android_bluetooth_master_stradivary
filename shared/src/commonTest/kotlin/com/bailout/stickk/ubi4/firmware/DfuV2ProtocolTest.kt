package com.bailout.stickk.ubi4.firmware

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DfuV2ProtocolTest {
    @Test
    fun `golden control and data packets match Dashboard and bootloader framing`() {
        assertContentEquals(
            "0003200025".hexBytes(),
            DfuV2Protocol.caps(address = 0)
        )
        assertContentEquals(
            "80031000d121001f00c1ed01008ec859fcf5001008d4".hexBytes(),
            DfuV2Protocol.begin(
                address = 0,
                flags = DfuV2Flags.REQUIRED,
                imageSize = 126_401,
                imageCrc32 = 0xFC59C88E,
                clientMaxFrame = 245
            )
        )
        assertContentEquals(
            "80030c007022007856341200010000aabb93".hexBytes(),
            DfuV2Protocol.data(
                address = 0,
                sessionId = 0x12345678,
                offset = 0x100,
                bytes = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
            )
        )
        assertContentEquals(
            "80030e00e1230078563412c1ed01008ec859fcc8".hexBytes(),
            DfuV2Protocol.status(0, 0x12345678, 126_401, 0xFC59C88E)
        )
        assertContentEquals(
            "800306009724007856341243".hexBytes(),
            DfuV2Protocol.abort(0, 0x12345678)
        )
    }

    @Test
    fun `capabilities parse required flags and negotiated frame`() {
        val caps = DfuV2Protocol.parseCapabilities(
            "200002001f00f50010080800".hexBytes()
        )

        assertEquals(DfuV2Status.OK, caps.status)
        assertEquals(2, caps.major)
        assertEquals(245, caps.maxFrame)
        assertEquals(16, caps.maxWindow)
        assertEquals(8, caps.ackEvery)
        assertEquals(8, caps.programUnit)
        assertTrue(caps.supportsRequiredFeatures)
        assertEquals(229, DfuV2Protocol.maxDataLength(250, caps.maxFrame))
        assertEquals(228, DfuV2Protocol.maxDataLength(244, caps.maxFrame))
    }

    @Test
    fun `CAPS accepts only OK and a valid ack cadence`() {
        val valid = "200002001f00f50010080800".hexBytes()

        val readyStatus = valid.copyOf().also { it[1] = DfuV2Status.READY.code.toByte() }
        assertTrue(!DfuV2Protocol.parseCapabilities(readyStatus).supportsRequiredFeatures)

        val ackLargerThanWindow = valid.copyOf().also {
            it[8] = 4
            it[9] = 8
        }
        assertTrue(!DfuV2Protocol.parseCapabilities(ackLargerThanWindow).supportsRequiredFeatures)
    }

    @Test
    fun `begin and cumulative ack are decoded little endian`() {
        val session = DfuV2Protocol.parseSession(
            "210278563412e000100800100000efbeaddefa00".hexBytes()
        )
        assertEquals(DfuV2Status.ERASE_RECONNECT, session.status)
        assertEquals(0x12345678, session.sessionId)
        assertEquals(224, session.dataLength)
        assertEquals(16, session.window)
        assertEquals(8, session.ackEvery)
        assertEquals(0x1000, session.committedOffset)
        assertEquals(0xDEADBEEF, session.prefixCrc32)
        assertEquals(250, session.ackTimeoutMs)

        val ack = DfuV2Protocol.parseAck(
            "2301785634120020000000240000002e0000efbeadde".hexBytes()
        )
        assertEquals(DfuV2Status.READY, ack.status)
        assertEquals(0x2000, ack.committedOffset)
        assertEquals(0x2400, ack.acceptedOffset)
        assertEquals(0x2E00, ack.sendLimitOffset)
        assertEquals(0xDEADBEEF, ack.prefixCrc32)
    }

    @Test
    fun `malformed or unknown responses never advance protocol state`() {
        assertFailsWith<IllegalArgumentException> {
            DfuV2Protocol.parseCapabilities(byteArrayOf(0x20, 0x00))
        }
        assertFailsWith<IllegalStateException> {
            DfuV2Protocol.parseAck(
                "237f785634120020000000240000002e0000efbeadde".hexBytes()
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DfuV2Protocol.parseSession(
                "220278563412e000100800100000efbeaddefa00".hexBytes()
            )
        }
    }

    @Test
    fun `Motorica CRC32 matches bootloader golden vectors`() {
        assertEquals(0L, MotoricaCrc32.calculate(byteArrayOf()))
        assertEquals(0xFC4F2BE9L, MotoricaCrc32.calculate("123456789".encodeToByteArray()))
        assertEquals(0xF8FCF427L, MotoricaCrc32.calculate(ByteArray(256) { it.toByte() }))
    }

    private fun String.hexBytes(): ByteArray {
        require(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
