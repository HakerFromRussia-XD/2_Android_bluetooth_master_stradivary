package com.bailout.stickk.ubi4.firmware

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FastDfuUploaderV2Test {
    @Test
    fun `forceLegacy skips CAPS entirely`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 4)
        val transport = NoCapsTransport(responses, malformed = false)
        val updater = V3FirmwareUpdater(
            sender = FirmwareCommandSender { _, _ -> },
            bulkTransport = transport,
        )

        DfuDiagnostics.forceLegacy = true
        try {
            assertEquals(null, updater.negotiateFastDfu(0))
            assertEquals(0, transport.controlWrites)
        } finally {
            DfuDiagnostics.forceLegacy = false
        }
    }

    @Test
    fun `valid CAPS negotiates only for FAM and non-FAM falls back`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
        val transport = FakeBulkTransport(responses)
        val uploader = FastDfuUploaderV2(transport, responses)

        assertNotNull(uploader.negotiate(0))
        assertEquals(null, uploader.negotiate(1))
    }

    @Test
    fun `confirmed v2 refreshes stale v1 GATT cache before selecting fast mode`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 4)
        val transport = StaleGattCacheTransport(responses)

        assertNotNull(FastDfuUploaderV2(transport, responses).negotiate(0))
        assertEquals(1, transport.reconnects)
        assertTrue(transport.wwrAvailable)
    }

    @Test
    fun `CAPS timeout and malformed response fall back without BEGIN`() = runTest {
        val silentResponses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 4)
        val silent = NoCapsTransport(silentResponses, malformed = false)
        assertEquals(null, FastDfuUploaderV2(silent, silentResponses).negotiate(0))
        assertEquals(1, silent.controlWrites)

        val malformedResponses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 4)
        val malformed = NoCapsTransport(malformedResponses, malformed = true)
        assertEquals(null, FastDfuUploaderV2(malformed, malformedResponses).negotiate(0))
        assertEquals(1, malformed.controlWrites)
    }

    @Test
    fun `windowed upload ignores foreign ACK and retries WWR BUSY`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
        val transport = FakeBulkTransport(responses, busyOnce = true, foreignAckOnce = true)
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = ByteArray(65) { (it * 3).toByte() }
        val progress = mutableListOf<Int>()
        val caps = assertNotNull(uploader.negotiate(0))

        uploader.upload(
            address = 0,
            firmware = firmware,
            expectedImageCrc32 = MotoricaCrc32.calculate(firmware),
            capabilities = caps,
        ) { offset, _ -> progress += offset }

        assertEquals(firmware.toList(), transport.received.toList())
        assertTrue(transport.busyReturns == 1)
        assertEquals(firmware.size, progress.last())
        assertTrue(uploader.currentSessionId != 0L)
        uploader.completeCurrent()
        assertEquals(0L, uploader.currentSessionId)
    }

    @Test
    fun `abort confirms and clears active session`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
        val transport = FakeBulkTransport(responses)
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = ByteArray(8) { it.toByte() }

        uploader.upload(
            0,
            firmware,
            MotoricaCrc32.calculate(firmware),
            assertNotNull(uploader.negotiate(0)),
        ) { _, _ -> }

        assertTrue(uploader.abortCurrent(0))
        assertEquals(0L, uploader.currentSessionId)
    }

    @Test
    fun `lost cumulative ACK recovers from STATUS committed offset`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
        val transport = FakeBulkTransport(responses, dropDataAcks = true)
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = ByteArray(32) { (it + 7).toByte() }

        uploader.upload(
            0, firmware, MotoricaCrc32.calculate(firmware),
            assertNotNull(uploader.negotiate(0)),
        ) { _, _ -> }

        assertTrue(transport.statusRequests > 0)
        assertEquals(firmware.toList(), transport.received.toList())
    }

    @Test
    fun `ERASE_RECONNECT reconnects once and resumes from READY`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
        val transport = FakeBulkTransport(responses, eraseReconnect = true)
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = ByteArray(24) { it.toByte() }

        uploader.upload(
            0, firmware, MotoricaCrc32.calculate(firmware),
            assertNotNull(uploader.negotiate(0)),
        ) { _, _ -> }

        assertEquals(1, transport.reconnects)
    }

    @Test
    fun `persistent WWR unavailability reconnects and resumes from committed offset`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
        val transport = FakeBulkTransport(responses, disconnectDuringDataOnce = true)
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = ByteArray(48) { (it * 5).toByte() }

        uploader.upload(
            0, firmware, MotoricaCrc32.calculate(firmware),
            assertNotNull(uploader.negotiate(0)),
        ) { _, _ -> }

        assertEquals(1, transport.reconnects)
        assertEquals(firmware.toList(), transport.received.toList())
    }

    @Test
    fun `lost BEGIN response is conservatively classified as flash changed`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 8)
        val transport = BeginResponseLostTransport(responses)
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = byteArrayOf(1, 2, 3)

        val error = kotlin.test.assertFailsWith<DfuV2TransferException> {
            uploader.upload(
                0, firmware, MotoricaCrc32.calculate(firmware),
                assertNotNull(uploader.negotiate(0)),
            ) { _, _ -> }
        }

        assertTrue(error.flashMayHaveChanged)
        assertFalse(uploader.abortCurrent(0))
    }

    @Test
    fun `STATUS link loss during erase reconnects again before DATA`() = runTest {
        val responses = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
        val transport = FakeBulkTransport(
            responses,
            eraseReconnect = true,
            disconnectOnFirstStatusAfterErase = true,
        )
        val uploader = FastDfuUploaderV2(transport, responses)
        val firmware = ByteArray(24) { it.toByte() }

        uploader.upload(
            0, firmware, MotoricaCrc32.calculate(firmware),
            assertNotNull(uploader.negotiate(0)),
        ) { _, _ -> }

        assertEquals(2, transport.reconnects)
        assertEquals(firmware.toList(), transport.received.toList())
    }
}

private class FakeBulkTransport(
    private val responses: MutableSharedFlow<ByteArray>,
    private val busyOnce: Boolean = false,
    private val foreignAckOnce: Boolean = false,
    private val dropDataAcks: Boolean = false,
    private val eraseReconnect: Boolean = false,
    private val disconnectDuringDataOnce: Boolean = false,
    private val disconnectOnFirstStatusAfterErase: Boolean = false,
) : FirmwareBulkTransport {
    val received = ArrayList<Byte>()
    var busyReturns = 0
    var statusRequests = 0
    var reconnects = 0
    private var busyReturned = false
    private var foreignSent = false
    private var dataDisconnectPending = disconnectDuringDataOnce
    private var eraseStatusDisconnectPending = disconnectOnFirstStatusAfterErase
    private val session = 0x1234ABCDL
    private var imageSize = 0L

    override suspend fun maximumWriteWithoutResponseSize() = 64
    override suspend fun supportsWriteWithoutResponse() = true
    override suspend fun setHighPerformanceMode() = Unit
    override suspend fun awaitWritable() = Unit
    override suspend fun reconnect() {
        reconnects++
        dataDisconnectPending = false
    }

    override suspend fun writeControl(packet: ByteArray) {
        when (packet.subcommand()) {
            DfuV2Command.CAPS.code -> responses.emit(
                byteArrayOf(0x20, 0x00, 0x02, 0x00, 0x1F, 0x00, 64, 0, 4, 2, 8, 0)
            )
            DfuV2Command.BEGIN.code -> {
                imageSize = packet.u32(9)
                responses.emit(beginResponse())
            }
            DfuV2Command.STATUS.code -> {
                if (eraseStatusDisconnectPending) {
                    eraseStatusDisconnectPending = false
                    error("simulated disconnect during erase STATUS")
                }
                statusRequests++
                responses.emit(statusResponse(session))
            }
            DfuV2Command.ABORT.code -> responses.emit(
                byteArrayOf(0x24, 0x00) + session.le32()
            )
        }
    }

    override suspend fun writeWithoutResponse(packet: ByteArray): Boolean {
        if (dataDisconnectPending) return false
        if (busyOnce && !busyReturned) {
            busyReturned = true
            busyReturns++
            return false
        }
        val offset = packet.u32(11).toInt()
        val data = packet.copyOfRange(15, packet.lastIndex)
        require(offset == received.size)
        received.addAll(data.toList())
        if (foreignAckOnce && !foreignSent) {
            foreignSent = true
            responses.emit(statusResponse(session + 1))
        }
        if (!dropDataAcks) responses.emit(statusResponse(session))
        return true
    }

    private fun beginResponse() =
        byteArrayOf(0x21, if (eraseReconnect) 0x02 else 0x01) + session.le32() +
            byteArrayOf(16, 0, 4, 2) + 0L.le32() + 0L.le32() + byteArrayOf(0xFA.toByte(), 0)

    private fun statusResponse(responseSession: Long): ByteArray {
        val offset = received.size.toLong()
        return byteArrayOf(0x23, 0x01) + responseSession.le32() +
            offset.le32() + offset.le32() + imageSize.le32() +
            MotoricaCrc32.calculate(received.toByteArray()).le32()
    }

    private fun ByteArray.subcommand(): Int =
        if (first().toInt() and 0x80 != 0) this[5].toInt() and 0xFF
        else this[2].toInt() and 0xFF

    private fun ByteArray.u32(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24)

    private fun Long.le32() = byteArrayOf(
        toByte(), (this shr 8).toByte(), (this shr 16).toByte(), (this shr 24).toByte()
    )
}

private class BeginResponseLostTransport(
    private val responses: MutableSharedFlow<ByteArray>,
) : FirmwareBulkTransport {
    override suspend fun maximumWriteWithoutResponseSize() = 64
    override suspend fun supportsWriteWithoutResponse() = true
    override suspend fun setHighPerformanceMode() = Unit
    override suspend fun awaitWritable() = Unit
    override suspend fun reconnect() = Unit
    override suspend fun writeWithoutResponse(packet: ByteArray) = true
    override suspend fun writeControl(packet: ByteArray) {
        if (packet.subcommandForTest() == DfuV2Command.CAPS.code) {
            responses.emit(byteArrayOf(0x20, 0x00, 0x02, 0x00, 0x1F, 0x00, 64, 0, 4, 2, 8, 0))
        }
    }
}

private fun ByteArray.subcommandForTest(): Int =
    if (first().toInt() and 0x80 != 0) this[5].toInt() and 0xFF
    else this[2].toInt() and 0xFF

private class NoCapsTransport(
    private val responses: MutableSharedFlow<ByteArray>,
    private val malformed: Boolean,
) : FirmwareBulkTransport {
    var controlWrites = 0
    override suspend fun maximumWriteWithoutResponseSize() = 64
    override suspend fun supportsWriteWithoutResponse() = true
    override suspend fun setHighPerformanceMode() = Unit
    override suspend fun awaitWritable() = Unit
    override suspend fun reconnect() = Unit
    override suspend fun writeWithoutResponse(packet: ByteArray) = true
    override suspend fun writeControl(packet: ByteArray) {
        controlWrites++
        if (malformed) responses.emit(byteArrayOf(0x20, 0x00))
    }
}

private class StaleGattCacheTransport(
    private val responses: MutableSharedFlow<ByteArray>,
) : FirmwareBulkTransport {
    var reconnects = 0
    var wwrAvailable = false
    override suspend fun maximumWriteWithoutResponseSize() = 64
    override suspend fun supportsWriteWithoutResponse() = wwrAvailable
    override suspend fun setHighPerformanceMode() = Unit
    override suspend fun awaitWritable() = Unit
    override suspend fun writeWithoutResponse(packet: ByteArray) = wwrAvailable
    override suspend fun reconnect() {
        reconnects++
        wwrAvailable = true
    }
    override suspend fun writeControl(packet: ByteArray) {
        responses.emit(byteArrayOf(0x20, 0x00, 0x02, 0x00, 0x1F, 0x00, 64, 0, 4, 2, 8, 0))
    }
}
