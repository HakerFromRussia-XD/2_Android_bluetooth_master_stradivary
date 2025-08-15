package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * iOS implementation of [BleCommandExecutor] that sends commands through a blocking queue.
 * Each enqueued task uses the provided [dispatcher] to transmit data and waits until
 * [allowNext] is invoked from the parser before processing the next command.
 */
class BleCommandExecutorIos(
    private val dispatcher: (ByteArray, String, String, () -> Unit) -> Unit
) : BleCommandExecutor {

    private val queue = BlockingQueueUbi4()

    init {
        // Start worker thread that executes tasks sequentially from the queue
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val task = queue.get()
                task.run()
            }
        }
    }

    override fun getQueueUBI4(): BlockingQueueUbi4 = queue

    override fun bleCommandWithQueue(
        byteArray: ByteArray?,
        command: String,
        typeCommand: String,
        onChunkSent: () -> Unit
    ) {
        if (byteArray != null) {
            val runnable = Runnable {
                dispatcher(byteArray, command, typeCommand, onChunkSent)
            }
            queue.put(runnable, byteArray)
        }
    }

    override fun sendWidgetsArray() { /* Not required on iOS yet */ }

    override fun updateSerialNumber(deviceInfo: DeviceInfoStructs) { /* Not required on iOS yet */ }
}
