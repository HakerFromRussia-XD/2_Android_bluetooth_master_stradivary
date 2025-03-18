package com.bailout.stickk.ubi4.utility

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object RetryUtils {

    @OptIn(DelicateCoroutinesApi::class)
    fun sendRequestWithRetry(
        request: suspend () -> Unit,
        isResponseReceived: () -> Boolean,
        maxRetries: Int = 5,
        delayMillis: Long = 1000L,
        scope: CoroutineScope = GlobalScope // Можно заменить на более конкретный скоуп
    ) {
        scope.launch(Dispatchers.Main) {
            var attempts = 0
            request()
            delay(delayMillis)
            while (!isResponseReceived() && attempts < maxRetries) {
                attempts++
                request()
                delay(delayMillis)
            }
        }
    }
}