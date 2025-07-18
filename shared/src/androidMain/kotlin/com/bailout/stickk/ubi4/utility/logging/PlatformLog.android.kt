package com.bailout.stickk.ubi4.utility.logging

actual fun platformLog(tag: String, message: String) {
//    android.util.Log.d(tag, message)
    val chunkSize = 4000
    if (message.length <= chunkSize) {
        android.util.Log.d(tag, message)
    } else {
        var index = 0
        val length = message.length
        while (index < length) {
            val end = kotlin.math.min(length, index + chunkSize)
            android.util.Log.d(tag, message.substring(index, end))
            index = end
        }
    }
}