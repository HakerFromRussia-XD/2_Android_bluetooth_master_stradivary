package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.utility.currentTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch

/**
 * Bulk FAM uploader. Negotiation is deliberately separate from legacy DFU:
 * until a complete, valid CAPS response is received no flash-changing v2
 * command is sent, so an old bootloader remains byte-for-byte on its v1 path.
 */
class FastDfuUploaderV2(
    private val transport: FirmwareBulkTransport,
    private val responses: SharedFlow<ByteArray>,
    private val logger: FirmwareUpdateLogger = NoOpFirmwareUpdateLogger
) {
    var currentSessionId: Long = 0L
        private set
    private var beginOutcomeUnknown = false

    suspend fun negotiate(address: Int): DfuCapabilitiesV2? {
        if (address != FAM_ADDRESS) {
            logger.debug(TRACE_TAG, "negotiate skip address=$address reason=not_fam")
            return null
        }
        logger.info(TRACE_TAG, "negotiate start address=$address")

        val payload = try {
            sendAndAwait(
                packet = DfuV2Protocol.caps(address),
                responseCommand = DfuV2Command.CAPS,
                timeoutMs = CAPS_TIMEOUT_MS
            ) ?: run {
                logger.warn(TRACE_TAG, "negotiate CAPS timeout timeout_ms=$CAPS_TIMEOUT_MS")
                return null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            logger.warn(TAG, "CAPS transport failure: ${error.message}; using legacy DFU")
            return null
        }

        val capabilities = runCatching { DfuV2Protocol.parseCapabilities(payload) }
            .getOrElse {
                logger.warn(TAG, "Malformed CAPS: ${it.message}; using legacy DFU")
                return null
            }
        logger.info(TRACE_TAG, "negotiate CAPS parsed=$capabilities")
        if (!capabilities.supportsRequiredFeatures) {
            logger.warn(TRACE_TAG, "negotiate reject reason=required_features_missing caps=$capabilities")
            return null
        }

        // Android may retain the old v1 GATT database for the same public MAC.
        // CAPS is intentionally sent over the unchanged WRITE property first.
        // Only a confirmed v2 bootloader is allowed to pay the reconnect/cache
        // refresh cost; an old bootloader still falls back after 500 ms.
        val wwrBeforeReconnect = transport.supportsWriteWithoutResponse()
        logger.info(TRACE_TAG, "negotiate wwr_before_reconnect=$wwrBeforeReconnect")
        if (!wwrBeforeReconnect) {
            logger.warn(TAG, "CAPS confirmed v2 but WWR is absent; refreshing GATT and reconnecting")
            try {
                transport.reconnect()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logger.warn(TAG, "GATT refresh reconnect failed: ${error.message}")
                return null
            }
            val wwrAfterReconnect = transport.supportsWriteWithoutResponse()
            logger.info(TRACE_TAG, "negotiate wwr_after_reconnect=$wwrAfterReconnect")
            if (!wwrAfterReconnect) {
                logger.warn(TAG, "WWR is still absent after confirmed-v2 reconnect")
                return null
            }
        }
        logger.info(TRACE_TAG, "negotiate complete protocol=v2")
        return capabilities
    }

    suspend fun upload(
        address: Int,
        firmware: ByteArray,
        expectedImageCrc32: Long,
        capabilities: DfuCapabilitiesV2,
        onProgress: (offset: Int, total: Int) -> Unit
    ) {
        val uploadStartedAt = currentTimeMillis()
        require(address == FAM_ADDRESS) { "DFU v2 is enabled only for FAM addr=0" }
        require(firmware.isNotEmpty()) { "Firmware image is empty" }

        val actualCrc = MotoricaCrc32.calculate(firmware)
        require(actualCrc == (expectedImageCrc32 and UINT32_MASK)) {
            "Firmware CRC mismatch before BLE: descriptor=0x${expectedImageCrc32.toString(16)}, " +
                "actual=0x${actualCrc.toString(16)}"
        }

        logger.info(
            TRACE_TAG,
            "upload start address=$address bytes=${firmware.size} crc32=0x${actualCrc.toString(16)} caps=$capabilities"
        )
        transport.setHighPerformanceMode()
        val platformFrame = transport.maximumWriteWithoutResponseSize()
            .coerceAtMost(DfuV2Protocol.MAX_GATT_FRAME)
        val maxDataLength = DfuV2Protocol.maxDataLength(platformFrame, capabilities.maxFrame)
        require(maxDataLength > 0) { "Negotiated BLE frame is too small: $platformFrame" }
        logger.info(
            TRACE_TAG,
            "upload transport platform_frame=$platformFrame boot_frame=${capabilities.maxFrame} max_data=$maxDataLength"
        )

        val eraseStartedAt = currentTimeMillis()
        // From this point BEGIN may have reached the bootloader and started an
        // erase even if its response is lost before we learn the session id.
        beginOutcomeUnknown = true
        val beginPayload = try {
            sendAndAwait(
                packet = DfuV2Protocol.begin(
                    address = address,
                    flags = DfuV2Flags.REQUIRED,
                    imageSize = firmware.size.toLong(),
                    imageCrc32 = actualCrc,
                    clientMaxFrame = platformFrame
                ),
                responseCommand = DfuV2Command.BEGIN,
                timeoutMs = CONTROL_TIMEOUT_MS
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw DfuV2TransferException(
                "BEGIN transport failure: ${error.message}",
                flashMayHaveChanged = true,
                cause = error
            )
        } ?: throw DfuV2TransferException(
            // BEGIN may already have reached the peripheral even when its
            // response was lost. Never classify this ambiguous case as safe.
            "BEGIN timeout",
            flashMayHaveChanged = true
        )

        if (beginPayload.size >= BEGIN_SESSION_END) {
            currentSessionId = beginPayload.readUInt32Le(BEGIN_SESSION_OFFSET)
            beginOutcomeUnknown = currentSessionId == 0L
        }
        val session = try {
            DfuV2Protocol.parseSession(beginPayload)
        } catch (error: Throwable) {
            // The peripheral may have accepted BEGIN and started erase before
            // a damaged notification reached us. Conservatively require the
            // post-erase recovery path instead of falling through as pre-erase.
            throw DfuV2TransferException("Malformed BEGIN: ${error.message}", true, error)
        }
        logger.info(TRACE_TAG, "upload BEGIN parsed=$session")
        if (session.status !in setOf(DfuV2Status.ERASE_RECONNECT, DfuV2Status.READY)) {
            currentSessionId = 0L
            beginOutcomeUnknown = false
            throw DfuV2TransferException(
                "BEGIN rejected: ${session.status}",
                flashMayHaveChanged = false
            )
        }
        validateSession(session, maxDataLength, capabilities)
        currentSessionId = session.sessionId
        beginOutcomeUnknown = false
        val prefixVerifier = MotoricaPrefixCrc32(firmware)

        if (session.status == DfuV2Status.ERASE_RECONNECT) {
            /* Keep Android on the live GATT while erase is in progress.  The
             * same v2 bootloader is already exercised this way by the Windows
             * Dashboard, and STATUS remains the authoritative READY gate.  A
             * real link loss is still handled by waitUntilReady's bounded
             * reconnect recovery below.  This branch is v2-only; the legacy
             * stop-and-wait updater is unchanged. */
            logger.info(
                TRACE_TAG,
                "upload erase_reconnect requested session=${session.sessionId} action=poll_same_link"
            )
        }

        var ack = waitUntilReady(address, firmware, actualCrc, session, prefixVerifier)
        logger.info(TRACE_TAG, "upload READY ack=$ack")
        logger.info(
            "DFU_METRIC",
            "protocol=v2 phase=erase_reconnect_ready duration_ms=${currentTimeMillis() - eraseStartedAt}"
        )
        var committed = ack.committedOffset.toInt()
        var nextOffset = committed
        var localWindow = session.window.coerceAtLeast(1)
        var cleanAcks = 0
        var sentFrames = 0
        var receivedAcks = 0
        onProgress(committed, firmware.size)

        val transferStartedAt = currentTimeMillis()
        coroutineScope {
            val ackChannel = Channel<DfuAckV2>(Channel.UNLIMITED)
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                responses
                    .filter { it.commandCode() == DfuV2Command.STATUS.code }
                    .collect { payload ->
                        val parsed = runCatching { DfuV2Protocol.parseAck(payload) }.getOrNull()
                        // Corrupt and foreign-session notifications cannot move
                        // the window. A valid ACK from this session remains queued
                        // even if it arrives while the client is still sending.
                        if (parsed?.sessionId == session.sessionId) ackChannel.send(parsed)
                    }
            }
            try {
                while (committed < firmware.size) {
                    try {
                        var framesInFlight = 0
                        val sendLimit = ack.sendLimitOffset.toInt().coerceAtMost(firmware.size)
                        while (nextOffset < firmware.size && nextOffset < sendLimit && framesInFlight < localWindow) {
                            val size = minOf(session.dataLength, firmware.size - nextOffset, sendLimit - nextOffset)
                            if (size <= 0) break
                            val packet = DfuV2Protocol.data(
                                address,
                                session.sessionId,
                                nextOffset.toLong(),
                                firmware.copyOfRange(nextOffset, nextOffset + size)
                            )
                            writeDataWithBackpressure(packet, session.ackTimeoutMs)
                            sentFrames++
                            nextOffset += size
                            framesInFlight++
                            if (sentFrames == 1 || sentFrames % DATA_TRACE_INTERVAL_FRAMES == 0) {
                                logger.debug(
                                    TRACE_TAG,
                                    "data TX progress frames=$sentFrames next=$nextOffset committed=$committed " +
                                        "limit=$sendLimit window=$localWindow"
                                )
                            }
                        }

                        val received = withTimeoutOrNull(
                            session.ackTimeoutMs.toLong().coerceAtLeast(MIN_ACK_TIMEOUT_MS)
                        ) { ackChannel.receive() }
                        if (received == null) {
                            logger.warn(
                                TRACE_TAG,
                                "data ACK timeout committed=$committed next=$nextOffset window=$localWindow " +
                                    "timeout_ms=${session.ackTimeoutMs}"
                            )
                            localWindow = (localWindow / 2).coerceAtLeast(1)
                            cleanAcks = 0
                            ack = queryStatus(address, firmware, actualCrc, session.sessionId)
                            validateAck(ack, firmware, session.sessionId, committed, prefixVerifier)
                            // Only durable bytes are safe after an ACK timeout. Duplicate
                            // DATA frames are explicitly idempotent in the bootloader.
                            nextOffset = ack.committedOffset.toInt()
                            logger.info(TRACE_TAG, "data STATUS recovery ack=$ack next=$nextOffset window=$localWindow")
                        } else {
                            validateAck(received, firmware, session.sessionId, committed, prefixVerifier)
                            ack = received
                            receivedAcks++
                            logger.info(
                                TRACE_TAG,
                                "data ACK index=$receivedAcks status=${ack.status} committed=${ack.committedOffset} " +
                                    "accepted=${ack.acceptedOffset} limit=${ack.sendLimitOffset} next=$nextOffset window=$localWindow"
                            )
                            cleanAcks++
                            if (cleanAcks >= CLEAN_ACKS_TO_GROW && localWindow < session.window) {
                                localWindow = (localWindow * 2).coerceAtMost(session.window)
                                cleanAcks = 0
                            }
                            if (nextOffset < ack.acceptedOffset.toInt()) {
                                nextOffset = ack.acceptedOffset.toInt()
                            }
                        }
                    } catch (error: DfuV2LinkException) {
                        logger.warn(TAG, "BLE link lost during DATA; reconnecting: ${error.message}")
                        localWindow = (localWindow / 2).coerceAtLeast(1)
                        cleanAcks = 0
                        reconnectOrFail("DATA recovery", error)
                        ack = try {
                            queryStatus(address, firmware, actualCrc, session.sessionId)
                        } catch (secondError: DfuV2LinkException) {
                            throw DfuV2TransferException(
                                "STATUS failed after reconnect: ${secondError.message}",
                                flashMayHaveChanged = true,
                                cause = secondError
                            )
                        }
                        validateAck(ack, firmware, session.sessionId, committed, prefixVerifier)
                        nextOffset = ack.committedOffset.toInt()
                    }

                    committed = ack.committedOffset.toInt()
                    onProgress(committed, firmware.size)
                }
            } finally {
                collector.cancelAndJoin()
                ackChannel.close()
            }
        }

        if (ack.prefixCrc32 != actualCrc) {
            throw DfuV2TransferException("Final prefix CRC mismatch", flashMayHaveChanged = true)
        }
        logger.info(
            "DFU_METRIC",
            "protocol=v2 phase=ble_transfer duration_ms=${currentTimeMillis() - transferStartedAt} " +
                "bytes=${firmware.size} total_upload_ms=${currentTimeMillis() - uploadStartedAt}"
        )
        logger.info(
            TRACE_TAG,
            "upload complete session=${session.sessionId} frames=$sentFrames acks=$receivedAcks " +
                "bytes=${firmware.size} duration_ms=${currentTimeMillis() - uploadStartedAt}"
        )
    }

    suspend fun abort(address: Int, sessionId: Long): Boolean {
        val payload = sendAndAwait(
            packet = DfuV2Protocol.abort(address, sessionId),
            responseCommand = DfuV2Command.ABORT,
            timeoutMs = CONTROL_TIMEOUT_MS
        ) ?: return false
        return runCatching {
            val response = DfuV2Protocol.parseAbort(payload)
            response.status == DfuV2Status.OK &&
                (response.sessionId == 0L || response.sessionId == sessionId)
        }.getOrDefault(false)
    }

    suspend fun abortCurrent(address: Int): Boolean {
        val sessionId = currentSessionId
        if (sessionId == 0L) return !beginOutcomeUnknown
        val aborted = abort(address, sessionId)
        if (aborted) {
            currentSessionId = 0L
            beginOutcomeUnknown = false
        }
        return aborted
    }

    /**
     * Called only after the legacy CALCULATE_CRC/COMPLITE_CRC sequence has
     * accepted the image and the bootloader is allowed to jump to main.
     */
    fun completeCurrent() {
        currentSessionId = 0L
        beginOutcomeUnknown = false
    }

    private suspend fun waitUntilReady(
        address: Int,
        firmware: ByteArray,
        imageCrc: Long,
        session: DfuSessionV2,
        prefixVerifier: MotoricaPrefixCrc32
    ): DfuAckV2 {
        var reconnectAttempts = 0
        repeat(READY_QUERY_ATTEMPTS) { attempt ->
            val ack = try {
                queryStatus(address, firmware, imageCrc, session.sessionId)
            } catch (error: DfuV2LinkException) {
                if (reconnectAttempts >= READY_RECONNECT_ATTEMPTS) {
                    throw DfuV2TransferException(
                        "STATUS unavailable while waiting for erase: ${error.message}",
                        flashMayHaveChanged = true,
                        cause = error
                    )
                }
                reconnectAttempts++
                reconnectOrFail("erase STATUS recovery", error)
                delay(READY_QUERY_DELAY_MS)
                return@repeat
            }
            logger.info(
                TRACE_TAG,
                "erase STATUS attempt=${attempt + 1}/$READY_QUERY_ATTEMPTS reconnects=$reconnectAttempts ack=$ack"
            )
            validateAck(ack, firmware, session.sessionId, 0, prefixVerifier)
            if (ack.status == DfuV2Status.READY) return ack
            if (ack.status !in setOf(DfuV2Status.OK, DfuV2Status.ERASE_RECONNECT)) {
                throw DfuV2TransferException("STATUS during erase: ${ack.status}", true)
            }
            delay(READY_QUERY_DELAY_MS)
        }
        throw DfuV2TransferException("Erase did not become READY", flashMayHaveChanged = true)
    }

    private suspend fun queryStatus(
        address: Int,
        firmware: ByteArray,
        imageCrc: Long,
        sessionId: Long
    ): DfuAckV2 {
        val payload = try {
            sendAndAwait(
                DfuV2Protocol.status(address, sessionId, firmware.size.toLong(), imageCrc),
                DfuV2Command.STATUS,
                CONTROL_TIMEOUT_MS,
                responseFilter = { response ->
                    // STATUS notifications are broadcast. Ignore delayed ACKs
                    // from an older session, but let a malformed short response
                    // reach the parser so it is reported as a protocol error.
                    response.size < STATUS_SESSION_END ||
                        response.readUInt32Le(STATUS_SESSION_OFFSET) == sessionId
                }
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw DfuV2LinkException("STATUS transport failure: ${error.message}", error)
        } ?: throw DfuV2LinkException("STATUS timeout")
        return try {
            DfuV2Protocol.parseAck(payload)
        } catch (error: Throwable) {
            throw DfuV2TransferException("Malformed STATUS: ${error.message}", true, error)
        }
    }

    private fun validateSession(
        session: DfuSessionV2,
        maxDataLength: Int,
        capabilities: DfuCapabilitiesV2
    ) {
        if (session.sessionId == 0L || session.dataLength !in 1..maxDataLength ||
            session.window !in 1..capabilities.maxWindow ||
            session.ackEvery !in 1..session.window || session.committedOffset != 0L ||
            session.prefixCrc32 != MotoricaCrc32.EMPTY
        ) {
            throw DfuV2TransferException("Invalid BEGIN response: $session", true)
        }
    }

    private fun validateAck(
        ack: DfuAckV2,
        firmware: ByteArray,
        sessionId: Long,
        previousCommitted: Int,
        prefixVerifier: MotoricaPrefixCrc32
    ) {
        val size = firmware.size.toLong()
        if (ack.sessionId != sessionId || ack.status !in RECOVERABLE_ACK_STATUSES ||
            ack.committedOffset < previousCommitted || ack.committedOffset > ack.acceptedOffset ||
            ack.acceptedOffset > ack.sendLimitOffset || ack.sendLimitOffset > size
        ) {
            throw DfuV2TransferException("Invalid cumulative ACK: $ack", true)
        }
        val expectedPrefix = prefixVerifier.at(ack.committedOffset.toInt())
        if (ack.prefixCrc32 != expectedPrefix) {
            throw DfuV2TransferException(
                "Prefix CRC mismatch at ${ack.committedOffset}",
                flashMayHaveChanged = true
            )
        }
    }

    private suspend fun sendAndAwait(
        packet: ByteArray,
        responseCommand: DfuV2Command,
        timeoutMs: Long,
        responseFilter: (ByteArray) -> Boolean = { true }
    ): ByteArray? = coroutineScope {
        val startedAt = currentTimeMillis()
        logger.debug(
            TRACE_TAG,
            "control TX command=${responseCommand.name} bytes=${packet.size} hex=${packet.traceHex()} timeout_ms=$timeoutMs"
        )
        val response = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(timeoutMs) {
                responses.filter {
                    it.commandCode() == responseCommand.code && responseFilter(it)
                }.first()
            }
        }
        try {
            transport.writeControl(packet)
            logger.debug(TRACE_TAG, "control WRITE_DONE command=${responseCommand.name}")
            val payload = response.await()
            if (payload == null) {
                logger.warn(
                    TRACE_TAG,
                    "control RX_TIMEOUT command=${responseCommand.name} elapsed_ms=${currentTimeMillis() - startedAt}"
                )
            } else {
                logger.debug(
                    TRACE_TAG,
                    "control RX command=${responseCommand.name} bytes=${payload.size} hex=${payload.traceHex()} " +
                        "elapsed_ms=${currentTimeMillis() - startedAt}"
                )
            }
            payload
        } catch (error: Throwable) {
            logger.error(
                TRACE_TAG,
                "control ERROR command=${responseCommand.name} elapsed_ms=${currentTimeMillis() - startedAt} " +
                    "type=${error::class.simpleName} message=${error.message}",
                error
            )
            throw error
        }
    }

    private suspend fun writeDataWithBackpressure(packet: ByteArray, ackTimeoutMs: Int) {
        var rejectedWrites = 0
        val written = withTimeoutOrNull(
            ackTimeoutMs.toLong().coerceAtLeast(MIN_ACK_TIMEOUT_MS)
        ) {
            while (true) {
                try {
                    transport.awaitWritable()
                    if (transport.writeWithoutResponse(packet)) {
                        if (rejectedWrites > 0) {
                            logger.debug(TRACE_TAG, "data WWR accepted_after_retries=$rejectedWrites bytes=${packet.size}")
                        }
                        return@withTimeoutOrNull true
                    }
                    rejectedWrites++
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    throw DfuV2LinkException("WWR transport failure: ${error.message}", error)
                }
                delay(WRITE_BUSY_RETRY_MS)
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
        if (written != true) {
            logger.warn(TRACE_TAG, "data WWR timeout retries=$rejectedWrites timeout_ms=$ackTimeoutMs")
            throw DfuV2LinkException("WWR remained unavailable")
        }
    }

    private suspend fun reconnectOrFail(context: String, cause: Throwable? = null) {
        val startedAt = currentTimeMillis()
        logger.info(TRACE_TAG, "reconnect start context=$context cause=${cause?.message}")
        try {
            transport.reconnect()
            logger.info(
                TRACE_TAG,
                "reconnect complete context=$context elapsed_ms=${currentTimeMillis() - startedAt}"
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw DfuV2TransferException(
                "$context failed: ${error.message}",
                flashMayHaveChanged = true,
                cause = cause ?: error
            )
        }
    }

    private fun ByteArray.commandCode(): Int = firstOrNull()?.toInt()?.and(0xFF) ?: -1

    private fun ByteArray.traceHex(limit: Int = 64): String {
        val shown = take(limit).joinToString("") { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }
        return if (size > limit) "$shown...(+${size - limit})" else shown
    }

    private fun ByteArray.readUInt32Le(offset: Int): Long =
        (this[offset].toLong() and 0xFFL) or
            ((this[offset + 1].toLong() and 0xFFL) shl 8) or
            ((this[offset + 2].toLong() and 0xFFL) shl 16) or
            ((this[offset + 3].toLong() and 0xFFL) shl 24)

    private companion object {
        const val TAG = "DFU_V2"
        const val TRACE_TAG = "DFU_V2_TRACE"
        const val FAM_ADDRESS = 0
        const val CAPS_TIMEOUT_MS = 500L
        const val CONTROL_TIMEOUT_MS = 1_000L
        const val MIN_ACK_TIMEOUT_MS = 250L
        const val READY_QUERY_DELAY_MS = 100L
        const val READY_QUERY_ATTEMPTS = 50
        const val READY_RECONNECT_ATTEMPTS = 2
        const val CLEAN_ACKS_TO_GROW = 3
        const val WRITE_BUSY_RETRY_MS = 2L
        const val DATA_TRACE_INTERVAL_FRAMES = 32
        const val STATUS_SESSION_OFFSET = 2
        const val STATUS_SESSION_END = STATUS_SESSION_OFFSET + 4
        const val BEGIN_SESSION_OFFSET = 2
        const val BEGIN_SESSION_END = BEGIN_SESSION_OFFSET + 4
        const val UINT32_MASK = 0xFFFF_FFFFL
        val RECOVERABLE_ACK_STATUSES = setOf(
            DfuV2Status.OK,
            DfuV2Status.READY,
            DfuV2Status.BAD_OFFSET,
            DfuV2Status.QUEUE_FULL
        )
    }
}

private class DfuV2LinkException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class DfuV2TransferException(
    message: String,
    val flashMayHaveChanged: Boolean,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/** CRC used by FAM. It intentionally differs from IEEE/zlib CRC-32. */
object MotoricaCrc32 {
    const val EMPTY = 0L
    private const val POLYNOMIAL = 0x04C1_1DB7L

    fun calculate(bytes: ByteArray, length: Int = bytes.size): Long {
        require(length in 0..bytes.size)
        var crc = 0xFFFF_FFFFL
        for (index in 0 until length) {
            crc = updateByte(crc, bytes[index])
        }
        return (crc xor 0xFFFF_FFFFL) and 0xFFFF_FFFFL
    }

    internal fun updateByte(rawState: Long, byte: Byte): Long {
        var crc = rawState xor (byte.toLong() and 0xFF)
        repeat(8) {
            crc = if (crc and 1L != 0L) (crc ushr 1) xor POLYNOMIAL else crc ushr 1
        }
        return crc and 0xFFFF_FFFFL
    }
}

class MotoricaPrefixCrc32(private val bytes: ByteArray) {
    private var offset = 0
    private var rawState = 0xFFFF_FFFFL

    fun at(requiredOffset: Int): Long {
        require(requiredOffset in 0..bytes.size)
        if (requiredOffset < offset) {
            offset = 0
            rawState = 0xFFFF_FFFFL
        }
        while (offset < requiredOffset) {
            rawState = MotoricaCrc32.updateByte(rawState, bytes[offset])
            offset++
        }
        return (rawState xor 0xFFFF_FFFFL) and 0xFFFF_FFFFL
    }
}
