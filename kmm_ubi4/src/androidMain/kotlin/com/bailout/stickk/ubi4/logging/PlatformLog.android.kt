package com.bailout.stickk.ubi4.logging

actual fun platformLog(tag: String, message: String) {
    android.util.Log.d(tag, message)
}