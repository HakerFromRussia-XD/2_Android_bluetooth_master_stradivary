package com.bailout.stickk.ubi4.utility

import kotlinx.coroutines.Runnable


class BlockingQueueUbi4 {
    private val tasks = mutableListOf<Runnable>()
    private var canTake: Boolean = true // Флаг, разрешающий извлечение задачи
    private var lastAllowTime: Long = 0L // Время последнего события (например, dataReceive)

    fun get(): Runnable {
        // Используем цикл с коротким сном, чтобы избежать busy loop
        while (true) {
            synchronized(this) {
                if (tasks.isNotEmpty() && canTake) {
                    val task = tasks.first()
                    tasks.removeAt(0)
                    canTake = false
                    return task
                } else if (tasks.isNotEmpty() && !canTake) {
                    val elapsed = currentTimeMillis() - lastAllowTime
                    if (elapsed >= 1000) {
                        canTake = true // Автоматическая разблокировка спустя 1 секунду
                    }
                }
            }
            sleep(50) // Короткий сон (50 мс) для экономии ресурсов
        }
    }

    fun put(task: Runnable, byteArray: ByteArray) {
        synchronized(this) {
            tasks.add(task)
            // Если требуется, можно сформировать строковое представление байтового массива:
            val byteArrayS = buildString {
                for (b in byteArray) {
                    append(" $b")
                }
            }
            // При необходимости можно добавить логирование или иные действия
        }
    }

    fun size(): Int {
        return synchronized(this) {
            tasks.size
        }
    }

    fun allowNext() {
        synchronized(this) {
            canTake = true
            lastAllowTime = currentTimeMillis() // Фиксируем время события
        }
    }
}