package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import java.util.concurrent.atomic.AtomicInteger

/** Owns the existing queue worker. Pending count tracks Runnable return, not a device ACK. */
class BleCommandQueue(val queue: BlockingQueueUbi4 = BlockingQueueUbi4()) {
    private val remainingTasks = AtomicInteger(0)
    val pendingCount: Int get() = remainingTasks.get()
    @Volatile private var queueWorkerRunning = false
    private var queueWorker: Thread? = null

    fun enqueue(task: Runnable, packet: ByteArray) {
        queue.put(task, packet)
        remainingTasks.incrementAndGet()
    }

    fun stop() {
        queueWorkerRunning = false
        queueWorker?.interrupt()
        queueWorker = null
    }

    fun start() {
        if (queueWorkerRunning) return
        queueWorkerRunning = true
        val worker = Thread {
            try {
                while (queueWorkerRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        val task: Runnable = queue.get()
                        if (!queueWorkerRunning || Thread.currentThread().isInterrupted) break
                        Log.d(
                            "BLE_Q",
                            "DEQ start thread=${Thread.currentThread().name} remaining=${remainingTasks.get()}"
                        )
                        task.run()
                        Log.d(
                            "BLE_Q",
                            "DEQ done  thread=${Thread.currentThread().name} remaining=${remainingTasks.get()}"
                        )
                        remainingTasks.decrementAndGet()
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (t: Throwable) {
                        if (!queueWorkerRunning) break
                        Log.e("BLE_Q", "Queue worker failed: ${t.message}", t)
                    }
                }
            } finally {
                queueWorkerRunning = false
            }
        }
        worker.name = "BLE-Queue-Worker"
        queueWorker = worker
        worker.start()
    }
}
