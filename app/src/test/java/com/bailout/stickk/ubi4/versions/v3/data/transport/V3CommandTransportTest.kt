package com.bailout.stickk.ubi4.versions.v3.data.transport

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3CommandTransportTest {
    private data class Call(
        val packet: ByteArray?, val uuid: String, val type: String,
        val onSent: () -> Unit, val thread: Thread,
    )

    private class RecordingExecutor(val failure: RuntimeException? = null) : BleCommandExecutor {
        val calls = mutableListOf<Call>()
        override fun bleCommandWithQueue(byteArray: ByteArray?, command: String, typeCommand: String, onChunkSent: () -> Unit) {
            calls += Call(byteArray, command, typeCommand, onChunkSent, Thread.currentThread())
            failure?.let { throw it }
        }
        override fun getQueueUBI4(): BlockingQueueUbi4 = error("Transport must not access the queue directly")
        override fun getRemainingTasksCount(): Int = error("Transport must not wait for the queue")
        override fun sendWidgetsArray(): Unit = error("Unexpected widget update")
        override fun updateSerialNumber(deviceInfo: DeviceInfoStructs): Unit = error("Unexpected device update")
    }

    @Test fun `forwards exact packet and callback synchronously without early completion`() {
        val executor = RecordingExecutor()
        val transport = V3CommandTransport { executor }
        val packet = byteArrayOf(1, 2, 3)
        var completed = 0
        val onSent = { completed++; Unit }

        transport.enqueue(packet, onSent)

        val call = executor.calls.single()
        assertSame(packet, call.packet)
        assertSame(onSent, call.onSent)
        assertEquals(SERIALPORTCHAR_UUID, call.uuid)
        assertEquals(WRITE, call.type)
        assertSame(Thread.currentThread(), call.thread)
        assertEquals(0, completed)
        call.onSent()
        assertEquals(1, completed)
    }

    @Test fun `resolves current executor on every call and preserves enqueue order`() {
        val first = RecordingExecutor()
        val second = RecordingExecutor()
        var current = first
        var lookups = 0
        val transport = V3CommandTransport { lookups++; current }
        val packets = listOf(byteArrayOf(1), byteArrayOf(2), byteArrayOf(3))
        assertEquals(0, lookups)

        transport.enqueue(packets[0])
        transport.enqueue(packets[1])
        current = second
        transport.enqueue(packets[2])

        assertEquals(3, lookups)
        assertEquals(2, first.calls.size)
        assertSame(packets[0], first.calls[0].packet)
        assertSame(packets[1], first.calls[1].packet)
        assertSame(packets[2], second.calls.single().packet)
    }

    @Test fun `commands without completion use a harmless callback`() {
        val executor = RecordingExecutor()
        V3CommandTransport { executor }.enqueue(byteArrayOf())
        val callback = executor.calls.single().onSent
        callback()
        callback()
        assertEquals(1, executor.calls.size)
    }

    @Test fun `missing executor error is propagated without completing or retrying`() {
        val failure = IllegalStateException("Activity unavailable")
        var lookups = 0
        var completed = false
        val transport = V3CommandTransport { lookups++; throw failure }

        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            transport.enqueue(byteArrayOf(1)) { completed = true }
        })
        assertEquals(1, lookups)
        assertFalse(completed)
    }

    @Test fun `executor error is propagated without retrying or invoking completion`() {
        val failure = IllegalStateException("Queue unavailable")
        val executor = RecordingExecutor(failure)
        var completed = false

        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            V3CommandTransport { executor }.enqueue(byteArrayOf(1)) { completed = true }
        })
        assertEquals(1, executor.calls.size)
        assertFalse(completed)
    }
}
