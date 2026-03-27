package com.bailout.stickk.ubi4.utility.logging

private const val MAX_ANDROID_LOG_CHUNK_SIZE = 3_000

actual fun platformLog(tag: String, message: String) {
    val safeMessage = sanitizeLogMessage(message)
    if (safeMessage.length <= MAX_ANDROID_LOG_CHUNK_SIZE) {
        logChunk(tag, safeMessage)
        return
    }

    safeMessage.chunked(MAX_ANDROID_LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
        logChunk(tag, "[$index] $chunk")
    }
}

private fun logChunk(tag: String, message: String) {
    try {
        android.util.Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Android logger is unavailable in local JVM unit tests.
        println("$tag: $message")
    }
}
