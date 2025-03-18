package com.bailout.stickk.ubi4.logging

actual fun platformLog(tag: String, message: String) {
    println("$tag: $message")
}