package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendFlag
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import java.util.concurrent.*

class BleCommandWriterTest {
    @BeforeEach fun prepare() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        canSendFlag = false
    }

    @AfterEach fun cleanup() {
        canSendFlag = false
        unmockkStatic(Log::class)
    }

    private fun withWorker(action: () -> Unit, check: (Thread, FutureTask<Unit>) -> Unit) {
        val result = FutureTask(Callable { action() })
        val worker = Thread(result, "writer-test").apply { isDaemon = true; start() }
        try {
            check(worker, result)
        } finally {
            worker.interrupt()
            worker.join(2_000)
            assertFalse(worker.isAlive, "Write must release its monitor on interruption")
        }
    }

    private fun awaitWaiting(worker: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (worker.state != Thread.State.WAITING && worker.isAlive && System.nanoTime() < deadline) {
            Thread.yield()
        }
        assertEquals(Thread.State.WAITING, worker.state)
    }

    @Test fun `rejected dispatch forwards exact arguments and returns without waiting`() {
        val packet = byteArrayOf(1, 2)
        var calls = 0
        val writer = BleCommandWriter { bytes, command, type ->
            assertSame(packet, bytes)
            assertEquals("uuid", command)
            assertEquals("WRITE", type)
            assertFalse(canSendFlag)
            calls++
            false
        }
        withWorker({ writer.write(packet, "uuid", "WRITE") }) { _, result ->
            result.get(2, TimeUnit.SECONDS)
            assertTrue(canSendFlag)
            assertEquals(1, calls)
        }
    }

    @Test fun `accepted dispatch waits for completion and reset does not complete it`() {
        val dispatched = CountDownLatch(1)
        val writer = BleCommandWriter { _, _, _ -> dispatched.countDown(); true }
        withWorker({ writer.write(byteArrayOf(1), "uuid", "WRITE") }) { worker, result ->
            assertTrue(dispatched.await(2, TimeUnit.SECONDS))
            awaitWaiting(worker)
            writer.reset()
            assertFalse(canSendFlag)
            assertThrows(TimeoutException::class.java) { result.get(100, TimeUnit.MILLISECONDS) }
            writer.onWriteCompleted()
            result.get(2, TimeUnit.SECONDS)
            assertTrue(canSendFlag)
        }
    }

    @Test fun `completion during dispatch is not lost and nullable non-write arguments are retained`() {
        lateinit var writer: BleCommandWriter
        writer = BleCommandWriter { bytes, command, type ->
            assertNull(bytes)
            assertEquals("other", command)
            assertEquals("READ", type)
            writer.onWriteCompleted()
            true
        }
        withWorker({ writer.write(null, "other", "READ") }) { _, result ->
            result.get(2, TimeUnit.SECONDS)
        }
    }

    @Test fun `completion before a new write does not bypass its wait`() {
        val writer = BleCommandWriter { _, _, _ -> true }
        writer.onWriteCompleted()
        withWorker({ writer.write(null, "uuid", "NOTIFY") }) { worker, result ->
            awaitWaiting(worker)
            assertFalse(result.isDone)
            writer.onWriteCompleted()
            result.get(2, TimeUnit.SECONDS)
        }
    }

    @Test fun `dispatch exception propagates without retry and monitor remains usable`() {
        val failure = IllegalStateException("dispatch failed")
        var calls = 0
        val writer = BleCommandWriter { _, _, _ -> calls++; throw failure }
        assertSame(failure, assertThrows(IllegalStateException::class.java) { writer.write(null, "uuid", "WRITE") })
        assertEquals(1, calls)
        assertFalse(canSendFlag)
        writer.onWriteCompleted()
        assertTrue(canSendFlag)
    }

    @Test fun `interrupted wait propagates without marking completion and releases monitor`() {
        val writer = BleCommandWriter { _, _, _ -> true }
        withWorker({ writer.write(null, "uuid", "WRITE") }) { worker, result ->
            awaitWaiting(worker)
            worker.interrupt()
            val failure = assertThrows(ExecutionException::class.java) { result.get(2, TimeUnit.SECONDS) }
            assertTrue(failure.cause is InterruptedException)
            assertFalse(canSendFlag)
            writer.onWriteCompleted()
            assertTrue(canSendFlag)
        }
    }

    @Test fun `reset preserves the shared flag used by existing callers`() {
        val writer = BleCommandWriter { _, _, _ -> false }
        canSendFlag = true
        writer.reset()
        assertFalse(canSendFlag)
        writer.onWriteCompleted()
        assertTrue(canSendFlag)
    }
}
