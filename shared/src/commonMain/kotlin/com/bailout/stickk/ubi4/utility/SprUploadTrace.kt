package com.bailout.stickk.ubi4.utility

import kotlin.concurrent.Volatile

object SprUploadTrace {
    @Volatile
    @JvmStatic
    var active: Boolean = false
}
