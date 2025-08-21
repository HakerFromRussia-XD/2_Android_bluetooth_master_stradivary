package com.bailout.stickk.ubi4.utility

expect inline fun <R> synchronized(lock: Any, block: () -> R): R