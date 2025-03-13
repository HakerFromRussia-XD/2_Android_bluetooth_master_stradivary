//package com.bailout.stickk.ubi4.utility
//
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlinx.datetime.Clock
//
//class BlockingQueueUbi4 {
//    private val tasks = mutableListOf<() -> Unit>()
//    private var canTake = true // Флаг, разрешающий извлечение задачи
//    private var lastAllowTime = 0L // Время последнего события dataReceive
//    private val mutex = Mutex()
//
//    // suspend-функция get() возвращает задачу, когда она готова
//    suspend fun get(): (() -> Unit) {
//        while (true) {
//            mutex.withLock {
//                if (tasks.isNotEmpty() && canTake) {
//                    val task = tasks.first()
//                    tasks.removeAt(0)
//                    canTake = false
//                    return task
//                }
//                // Если задачи есть, но извлечение временно заблокировано,
//                // проверяем, прошла ли секунда с последнего разрешения
//                if (tasks.isNotEmpty() && !canTake) {
//                    val elapsed = Clock.System.now().toEpochMilliseconds() - lastAllowTime
//                    if (elapsed >= 1000) {
//                        println("BlockingQueueUbi4: не дождались ответа!!!")
//                        canTake = true // автоматическая разблокировка
//                    }
//                }
//            }
//            delay(100) // Ждем немного перед повторной проверкой
//        }
//    }
//
//    // suspend-функция put() добавляет задачу и логирует переданный массив байт
//    suspend fun put(task: () -> Unit, byteArray: ByteArray) {
//        mutex.withLock {
//            tasks.add(task)
//            val byteArrayS = byteArray.joinToString(" ") { it.toString() }
//            println("BlockingQueueUbi4: запрос byteArrayS = $byteArrayS")
//        }
//    }
//
//    // suspend-функция size() возвращает текущий размер очереди
//    suspend fun size(): Int = mutex.withLock { tasks.size }
//
//    // suspend-функция allowNext() разрешает извлечение следующей задачи,
//    // фиксируя время события
//    suspend fun allowNext() {
//        mutex.withLock {
//            canTake = true
//            lastAllowTime = Clock.System.now().toEpochMilliseconds()
//            println("BlockingQueueUbi4: ответ")
//        }
//    }
//}