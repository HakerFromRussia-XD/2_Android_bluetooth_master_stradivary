package com.bailout.stickk.ubi4.utility

class BlockingQueue2Ubi4 {

    var tasks: ArrayList<Runnable> = ArrayList()
    private var canTake = true // Флаг, разрешающий извлечение задачи
    private var lastAllowTime: Long = 0 // Время последнего события dataReceive

    @Synchronized
    fun get(): Runnable {
        while (tasks.isEmpty() || !canTake) {
            try {
                if (!tasks.isEmpty() && !canTake) {
                    // Проверяем, прошла ли секунда с последнего dataReceive
                    val elapsed = System.currentTimeMillis() - lastAllowTime
                    if (elapsed >= 1000) {
                        canTake = true // Автоматическая разблокировка
                    } else {
                        // Ждём оставшееся время до секунды
                        (this as Object).wait(1000 - elapsed)
                    }
                } else {
                    (this as Object).wait()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        val task = tasks[0]
        canTake = false
        tasks.remove(task)
        return task
    }

    @Synchronized
    fun put(task: Runnable, byteArray: ByteArray) {
        tasks.add(task)
        val byteArrayS = StringBuilder()
        for (b in byteArray) {
            byteArrayS.append(" $b")
        }
        (this as Object).notify()
    }

    @Synchronized
    fun size(): Int {
        return tasks.size
    }

    @Synchronized
    fun allowNext() {
        canTake = true
        lastAllowTime = System.currentTimeMillis() // Фиксируем время события
        (this as Object).notify() // Разрешаем извлечение следующей задачи
    }
}