package com.bailout.stickk.ubi4.versions.v3.data.transport

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE

/** Forwards commands to the existing queue; completion keeps the executor's semantics. */
class V3CommandTransport(
    private val executorProvider: () -> BleCommandExecutor,
) {
    fun enqueue(packet: ByteArray, onSent: () -> Unit = {}) {
        executorProvider().bleCommandWithQueue(packet, SERIALPORTCHAR_UUID, WRITE, onSent)
    }
}
