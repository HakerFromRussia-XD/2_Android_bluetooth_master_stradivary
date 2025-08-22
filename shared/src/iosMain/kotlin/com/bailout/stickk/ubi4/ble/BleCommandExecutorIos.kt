package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.data.DeviceInfoStructs
import com.bailout.stickk.ubi4.utility.BlockingQueueUbi4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * iOS implementation of [BleCommandExecutor] that sends commands through a blocking queue.
 * Each enqueued task uses the provided [dispatcher] to transmit data and waits until
 * [allowNext] is invoked from the parser before processing the next command.
 */
class BleCommandExecutorIos(
    private val dispatcher: (ByteArray, String, String, () -> Unit) -> Unit
) : BleCommandExecutor {

    private val queue = BlockingQueueUbi4()
    private val remainingTasks: AtomicInt = atomic(0) // Счётчик оставшихся задач

    init {
        // Start worker thread that executes tasks sequentially from the queue
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val task = queue.get()
                task.run()
                remainingTasks.decrementAndGet()
                platformLog("sendBytesKmm", "команд в очереди: $remainingTasks")
            }
        }
    }

    override fun getRemainingTasksCount(): Int {
        return remainingTasks.value // Возвращаем текущее количество оставшихся задач
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
            remainingTasks.incrementAndGet()
            platformLog("sendBytesKmm", "команд в очереди: $remainingTasks")
        }
    }

    override fun sendWidgetsArray() {
        // Обновляем хранилище/модель, затем…
        WidgetStore.shared.setWidgets(widgets)

        // …рассылаем сигнал
        updateWidgets.send()
    }

    override fun updateSerialNumber(deviceInfo: DeviceInfoStructs) { /* Not required on iOS yet */ }
}
