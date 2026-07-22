package com.bailout.stickk.ubi4.utility

import kotlin.concurrent.Volatile
import kotlin.jvm.JvmStatic

object SprUploadTrace {
    @Volatile
    @JvmStatic
    var active: Boolean = false
}
