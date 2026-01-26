package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.Runnable


class BlockingQueueUbi4 {
//    private val tasks = mutableListOf<Runnable>()
    private data class QueueEntry(
        val task: Runnable,
        val enqueuedAt: Long,
        val description: String
    )

    private val tasks = mutableListOf<QueueEntry>()
    private var canTake: Boolean = true // Флаг, разрешающий извлечение задачи
    private var lastAllowTime: Long = 0L // Время последнего события (например, dataReceive)
    private val autoUnlockMs = 1000L

    fun get(): Runnable {
        // Используем цикл с коротким сном, чтобы избежать busy loop
        while (true) {
            synchronized(this) {
                if (tasks.isNotEmpty() && canTake) {
//                    val task = tasks.first()
//                    tasks.removeAt(0)
                    val entry = tasks.removeAt(0)
                    val waitMs = currentTimeMillis() - entry.enqueuedAt
                    lastAllowTime = currentTimeMillis()
                    canTake = false
                    platformLog(
                        "sendBytesKmm",
                        "BlockingQueueUbi4: берём задачу спустя ${waitMs}мс, в очереди осталось ${tasks.size}. data=${entry.description}"
                    )
                    return entry.task
                } else if (tasks.isNotEmpty() && !canTake) {
                    val elapsed = currentTimeMillis() - lastAllowTime
                    if (elapsed >= autoUnlockMs) {
                        canTake = true // Автоматическая разблокировка спустя задержку
                        platformLog(
                            "sendBytesKmm",
                            "BlockingQueueUbi4: авто-разблокировка через ${elapsed}мс, в очереди ${tasks.size}"
                        )
                    }
                }
            }
            sleep(50) // Короткий сон (50 мс) для экономии ресурсов
        }
    }

    fun put(task: Runnable, byteArray: ByteArray) {
        synchronized(this) {
//            tasks.add(task)
//            // Если требуется, можно сформировать строковое представление байтового массива:
//            val byteArrayS = buildString {
//                for (b in byteArray) {
//                    append(" $b")
//                }
//            }
            val description = "len=${byteArray.size} hex=${EncodeByteToHex.bytesToHexString(byteArray)}"
            tasks.add(QueueEntry(task = task, enqueuedAt = currentTimeMillis(), description = description))
        }
    }

    fun size(): Int {
        return synchronized(this) {
            tasks.size
        }
    }

    fun allowNext(deviceAddress: Int, parameterID: Int,  receiveDataString: String) {
        synchronized(this) {
//            platformLog("sendBytesKmm", "А тут разрешаем протолкнуть следующую команду allowNext  deviceAddress = $deviceAddress   parameterID = $parameterID   data = $receiveDataString")
            platformLog(
                "sendBytesKmm",
                "А тут разрешаем протолкнуть следующую команду allowNext  deviceAddress = $deviceAddress   parameterID = $parameterID   data = $receiveDataString"
            )
            canTake = true
            lastAllowTime = currentTimeMillis() // Фиксируем время события
        }
    }
}