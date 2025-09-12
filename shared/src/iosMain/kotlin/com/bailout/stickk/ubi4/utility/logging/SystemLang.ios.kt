package com.bailout.stickk.ubi4.utility.logging

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

@Throws(Exception::class)
actual fun systemLang(): String {
    return NSLocale.currentLocale.languageCode ?: "en"
}