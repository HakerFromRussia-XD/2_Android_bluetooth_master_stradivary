package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.ble.BLEComponents
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFirmwareBulkTransport : FirmwareBulkTransport {
    private val manager get() = BLEComponents.bleManager

    override suspend fun maximumWriteWithoutResponseSize(): Int =
        manager.dfuMaximumWriteWithoutResponseSize()

    override suspend fun supportsWriteWithoutResponse(): Boolean =
        manager.dfuSupportsWriteWithoutResponse()

    override suspend fun setHighPerformanceMode() = Unit
    override suspend fun writeControl(packet: ByteArray) = manager.dfuWriteControl(packet)
    override suspend fun writeControlExpectDisconnect(packet: ByteArray) =
        manager.dfuWriteControlExpectDisconnect(packet)
    override suspend fun writeWithoutResponse(packet: ByteArray): Boolean =
        manager.dfuWriteWithoutResponse(packet)

    override suspend fun awaitWritable() = manager.dfuAwaitWritable()
    override suspend fun reconnect() = manager.dfuReconnect()
    override suspend fun awaitReconnect() = manager.dfuAwaitReconnect()
}
