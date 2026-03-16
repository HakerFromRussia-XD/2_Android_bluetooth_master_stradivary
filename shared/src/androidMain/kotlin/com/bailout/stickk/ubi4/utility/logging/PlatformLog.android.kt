package com.bailout.stickk.ubi4.utility.logging

private const val MAX_ANDROID_LOG_CHUNK_SIZE = 3_000

actual fun platformLog(tag: String, message: String) {
    val safeMessage = sanitizeLogMessage(message)
    if (safeMessage.length <= MAX_ANDROID_LOG_CHUNK_SIZE) {
        android.util.Log.d(tag, safeMessage)
        return
    }

    safeMessage.chunked(MAX_ANDROID_LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
        android.util.Log.d(tag, "[$index] $chunk")
    }
}