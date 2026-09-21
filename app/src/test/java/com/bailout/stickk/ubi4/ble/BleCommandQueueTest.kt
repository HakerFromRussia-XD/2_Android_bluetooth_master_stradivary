package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BleCommandQueueTest {
    private val tasks = LinkedBlockingQueue<Runnable>()
    private val rawQueue = mockk<BlockingQueueUbi4>()
    private val queue = BleCommandQueue(rawQueue)
    private val worker = AtomicReference<Thread>()
    private val started = CountDownLatch(1)

    @BeforeEach fun prepare() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { rawQueue.put(any(), any()) } answers { tasks.put(firstArg()); Unit }
        every { rawQueue.get() } answers {
            worker.set(Thread.currentThread())
            started.countDown()
            tasks.take()
        }
    }

    @AfterEach fun cleanup() {
        queue.stop()
        worker.get()?.let { it.join(2_000); assertFalse(it.isAlive) }
        unmockkStatic(Log::class)
    }

    private fun awaitCount(expected: Int) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (queue.pendingCount != expected && System.nanoTime() < deadline) Thread.sleep(1)
        assertEquals(expected, queue.pendingCount)
    }

    @Test fun `enqueue preserves queue packet task and increments only after put`() {
        val packet = byteArrayOf(1, 2)
        val task = Runnable {}
        every { rawQueue.put(any(), any()) } answers {
            assertSame(task, firstArg<Runnable>())
            assertSame(packet, secondArg<ByteArray>())
            assertEquals(0, queue.pendingCount)
        }
        assertSame(rawQueue, queue.queue)
        queue.enqueue(task, packet)
        assertEquals(1, queue.pendingCount)
        val failure = IllegalStateException("put failed")
        every { rawQueue.put(any(), any()) } throws failure
        assertSame(failure, assertThrows(IllegalStateException::class.java) { queue.enqueue(task, packet) })
        assertEquals(1, queue.pendingCount)
    }

    @Test fun `worker runs tasks in order and decrements after return with one start`() {
        val observed = LinkedBlockingQueue<Pair<String, Int>>()
        val release = CountDownLatch(1)
        queue.enqueue(Runnable {
            observed.put("first" to queue.pendingCount)
            release.await()
        }, byteArrayOf(1))
        queue.enqueue(Runnable { observed.put("second" to queue.pendingCount) }, byteArrayOf(2))
        queue.start()
        assertEquals("first" to 2, observed.poll(2, TimeUnit.SECONDS))
        queue.start()
        assertNull(observed.poll(100, TimeUnit.MILLISECONDS))
        release.countDown()
        assertEquals("second" to 1, observed.poll(2, TimeUnit.SECONDS))
        awaitCount(0)
        assertEquals("BLE-Queue-Worker", worker.get().name)
    }

    @Test fun `task failure is not retried or decremented and worker continues`() {
        val failure = IllegalStateException("task failed")
        val completed = CountDownLatch(1)
        queue.enqueue(Runnable { throw failure }, byteArrayOf(1))
        queue.enqueue(Runnable { completed.countDown() }, byteArrayOf(2))
        queue.start()
        assertTrue(completed.await(2, TimeUnit.SECONDS))
        awaitCount(1)
        verify(exactly = 1) { Log.e("BLE_Q", any(), failure) }
    }

    @Test fun `stop interrupts pending get without clearing queued tasks or count`() {
        queue.start()
        assertTrue(started.await(2, TimeUnit.SECONDS))
        queue.stop()
        worker.get().join(2_000)
        assertFalse(worker.get().isAlive)
        val task = Runnable { error("must not execute after stop") }
        queue.enqueue(task, byteArrayOf(1))
        assertEquals(1, queue.pendingCount)
        assertSame(task, tasks.peek())
    }

    @Test fun `interrupted task stops worker without decrement or running next task`() {
        val entered = CountDownLatch(1)
        queue.enqueue(Runnable { entered.countDown(); CountDownLatch(1).await() }, byteArrayOf(1))
        val next = Runnable { error("must remain queued") }
        queue.enqueue(next, byteArrayOf(2))
        queue.start()
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        queue.stop()
        worker.get().join(2_000)
        assertFalse(worker.get().isAlive)
        assertEquals(2, queue.pendingCount)
        assertSame(next, tasks.peek())
    }
}
