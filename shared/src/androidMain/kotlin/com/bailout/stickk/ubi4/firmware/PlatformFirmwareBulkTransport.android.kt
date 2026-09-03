package com.bailout.stickk.ubi4.firmware

import android.util.Log
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleEnvironment
import java.util.concurrent.atomic.AtomicInteger

actual object PlatformFirmwareBulkTransport : FirmwareBulkTransport {
    private const val TRACE_TAG = "DFU_V2_TRACE"
    private val wwrCalls = AtomicInteger(0)
    private fun executor() = BleEnvironment.getBleCommandExecutor()

    override suspend fun maximumWriteWithoutResponseSize(): Int {
        val size = executor().dfuMaximumWriteWithoutResponseSize()
        Log.i(TRACE_TAG, "transport maximum_wwr_size=$size")
        return size
    }

    override suspend fun supportsWriteWithoutResponse(): Boolean {
        val supported = executor().dfuSupportsWriteWithoutResponse()
        Log.i(TRACE_TAG, "transport supports_wwr=$supported")
        return supported
    }

    override suspend fun setHighPerformanceMode() {
        Log.i(TRACE_TAG, "transport high_performance request")
        executor().dfuSetHighPerformanceMode()
    }

    override suspend fun writeControl(packet: ByteArray) {
        Log.d(TRACE_TAG, "transport control_write start bytes=${packet.size}")
        executor().dfuWriteControl(packet)
        Log.d(TRACE_TAG, "transport control_write callback_complete bytes=${packet.size}")
    }

    override suspend fun writeControlExpectDisconnect(packet: ByteArray) {
        Log.d(TRACE_TAG, "transport reset_write start bytes=${packet.size}")
        executor().dfuWriteControlExpectDisconnect(packet)
        Log.d(TRACE_TAG, "transport reset_write accepted bytes=${packet.size}")
    }

    override suspend fun writeWithoutResponse(packet: ByteArray): Boolean {
        val call = wwrCalls.incrementAndGet()
        val accepted = executor().dfuWriteWithoutResponse(packet)
        if (!accepted || call == 1 || call % 64 == 0) {
            Log.d(TRACE_TAG, "transport wwr call=$call accepted=$accepted bytes=${packet.size}")
        }
        return accepted
    }

    override suspend fun awaitWritable() = executor().dfuAwaitWritable()
    override suspend fun reconnect() {
        Log.i(TRACE_TAG, "transport reconnect invoke")
        executor().dfuReconnect()
        Log.i(TRACE_TAG, "transport reconnect return")
    }

    override suspend fun awaitReconnect() {
        Log.i(TRACE_TAG, "transport await_reconnect invoke")
        executor().dfuAwaitReconnect()
        Log.i(TRACE_TAG, "transport await_reconnect return")
    }
}
