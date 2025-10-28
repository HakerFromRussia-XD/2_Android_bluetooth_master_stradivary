package com.bailout.stickk.ubi4.utility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Runnable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class BlockingQueueUbi4Test {
    @Test
    fun `second task waits for allowNext before starting`() {
        val queue = BlockingQueueUbi4()
        val executionLog = mutableListOf<String>()

        queue.put(Runnable { executionLog += "first" }, byteArrayOf(1))
        queue.put(Runnable { executionLog += "second" }, byteArrayOf(2))

        val firstTask = queue.get()
        firstTask.run()
        val firstCompletedAt = currentTimeMillis()
        println("[test] First task finished at $firstCompletedAt")

        val secondStartedAt = AtomicLong(-1L)
        val latch = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.execute {
                val nextTask = queue.get()
                val started = currentTimeMillis()
                println("[test] Second task unblocked at $started")
                secondStartedAt.set(started)
                nextTask.run()
                latch.countDown()
            }

            // ensure the second task does not start before we allow it
            Thread.sleep(200)
            assertEquals(-1L, secondStartedAt.get(), "Second task should be blocked before allowNext")

            queue.allowNext(deviceAddress = 1, parameterID = 2, receiveDataString = "ok")
            assertTrue(latch.await(2, TimeUnit.SECONDS), "Second task should start after allowNext")
        } finally {
//            executor.shutdownNow()
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.SECONDS)
        }

        assertEquals(listOf("first", "second"), executionLog, "Tasks should run sequentially")
        val secondStart = secondStartedAt.get()
        assertTrue(secondStart >= firstCompletedAt, "Second task should start no earlier than first completion")
    }
}