package com.bailout.stickk.ubi4.utility


import kotlinx.datetime.Clock

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

expect fun sleep(millis: Long)