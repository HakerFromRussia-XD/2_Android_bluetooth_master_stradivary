package com.bailout.stickk.ubi4.utility.logging

private const val MAX_LOG_MESSAGE_LENGTH = 8_000

expect fun platformLog(tag: String, message: String)

fun sanitizeLogMessage(message: String, maxLength: Int = MAX_LOG_MESSAGE_LENGTH): String {
    if (message.length <= maxLength) return message
    return buildString {
        append(message.take(maxLength))
        append("… [truncated ")
        append(message.length - maxLength)
        append(" chars]")
    }
}