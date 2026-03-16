package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Демонстрационный класс (не unit test) для ручной проверки поведения [BlockingQueueUbi4].
 *
 * Сценарии:
 * 1) Команды приходят с интервалом  < autoUnlockMs и      > autoUnlockMs, а продвижение очереди делается вручную через allowNext.
 * 2) Команды выполняются без allowNext — продвижение происходит авто-разблокировкой по autoUnlockMs.
 */
class BlockingQueueUbi4CoroutineDemo(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val demoTag = "BlockingQueueUbi4Demo"

    fun runAllScenarios(): Job = scope.launch {
        log("===== START DEMO =====")
        runManualAllowScenario()
//        delay(3000)
//        runAutoUnlockScenario()
        log("===== END DEMO =====")
    }

    private suspend fun runManualAllowScenario() {
        log("--- Scenario #1: выполнение только после allowNext (ручной режим) ---")
        val queue = BlockingQueueUbi4()
        val startedAt = currentTimeMillis()

        val worker = scope.launch {
            repeat(3) { index ->
                val task = queue.get()
                log("берём задачу на выполнение-${index + 1} at +${currentTimeMillis() - startedAt}ms")
                task.run()
            }
        }

        enqueue(queue, startedAt, name = "задача 1", enqueueOffsetMs = 0)
//        delay(100) // < autoUnlockMs
        enqueue(queue, startedAt, name = "задача 2", enqueueOffsetMs = 0)
//        delay(260) // > autoUnlockMs
        enqueue(queue, startedAt, name = "задача 3 ", enqueueOffsetMs = 0)
        delay(100)
        log("call allowNext #1 at +${currentTimeMillis() - startedAt}ms")
        queue.allowNext(deviceAddress = 1, parameterID = 1001, receiveDataString = "проталкивание очереди - 1")

        delay(1000)
        log("call allowNext #2 at +${currentTimeMillis() - startedAt}ms")
        queue.allowNext(deviceAddress = 1, parameterID = 1002, receiveDataString = "проталкивание очереди - 2")

        worker.join()
        log("--- Scenario #1 done ---")
    }

    private suspend fun runAutoUnlockScenario() {
        log("--- Scenario #2: без allowNext, задачи идут только по autoUnlockMs ---")
        val queue = BlockingQueueUbi4()
        val startedAt = currentTimeMillis()

        val worker = scope.launch {
            repeat(3) { index ->
                val task = queue.get()
                log("worker got task-${index + 1} at +${currentTimeMillis() - startedAt}ms")
                task.run()
            }
        }

        enqueue(queue, startedAt, name = "auto-1", enqueueOffsetMs = 0)
        delay(80) // < autoUnlockMs
        enqueue(queue, startedAt, name = "auto-2 (<autoUnlockMs)", enqueueOffsetMs = 80)
        delay(40)
        enqueue(queue, startedAt, name = "auto-3 (ждёт >autoUnlockMs из-за очереди)", enqueueOffsetMs = 120)

        log("allowNext НЕ вызываем: ожидаем две авто-разблокировки")
        worker.join()
        log("--- Scenario #2 done ---")
    }

    private fun enqueue(queue: BlockingQueueUbi4, startedAt: Long, name: String, enqueueOffsetMs: Long) {
        val payload = name.encodeToByteArray()
        log("enqueue '$name' (offset=${enqueueOffsetMs}ms, now=+${currentTimeMillis() - startedAt}ms)")
        queue.put(
            task = Runnable {
                log("RUN '$name' at +${currentTimeMillis() - startedAt}ms")
            },
            byteArray = payload
        )
    }

    private fun log(message: String) {
        platformLog(demoTag, message)
    }

    companion object {
        /** Удобный entry-point для локального ручного запуска. */
        fun runBlockingDemo() {
            runBlocking {
                BlockingQueueUbi4CoroutineDemo().runAllScenarios().join()
            }
        }
    }
}
