package com.bailout.stickk.ubi4.utility

import platform.posix.usleep

actual fun sleep(millis: Long) {
    usleep((millis * 1000).toUInt())
}