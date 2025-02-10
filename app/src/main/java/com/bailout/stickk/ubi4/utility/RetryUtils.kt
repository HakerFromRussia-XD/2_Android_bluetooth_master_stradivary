package com.bailout.stickk.ubi4.utility

import android.os.Handler
import android.os.Looper

object RetryUtils {
    fun sendRequestWithRetry(
        request: () -> Unit,
        isResponseReceived: () -> Boolean,
        maxRetries: Int = 5,
        delayMillis: Long = 1000L
    ) {
        val handler = Handler(Looper.getMainLooper())
        var attempts = 0

        // Функция, которая пытается отправить запрос, если ответа ещё нет
        fun attempt() {
            // Если ответ получен – завершаем попытки
            if (isResponseReceived()) return

            // Если ещё не исчерпали лимит повторов, отправляем запрос снова
            if (attempts < maxRetries) {
                attempts++
                request()
                handler.postDelayed({ attempt() }, delayMillis)
            }
        }

        // Начинаем с первоначальной отправки запроса
        request()
        // Запускаем первый таймаут для проверки ответа через delayMillis
        handler.postDelayed({ attempt() }, delayMillis)
    }
}