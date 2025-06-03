package com.bailout.stickk.ubi4.utility.logging

import platform.Foundation.NSLog

actual fun platformLog(tag: String, message: String) {
    NSLog("[$tag] $message")
}