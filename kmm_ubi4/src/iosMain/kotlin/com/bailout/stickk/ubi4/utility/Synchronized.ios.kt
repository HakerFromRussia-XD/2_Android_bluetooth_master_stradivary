package com.bailout.stickk.ubi4.utility

actual inline fun <R> synchronized(lock: Any, block: () -> R): R = block()