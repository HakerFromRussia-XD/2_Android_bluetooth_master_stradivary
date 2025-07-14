package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt

object EncodeHexToInt {

    fun String.hexToBatteryPercent(): Int = try {
        substring(0, 2)        // например "44"
            .toInt(16)         // парсинг по основанию 16 → 68
            .coerceIn(0, 100)
    } catch (e: Exception) {
        0
    }

    }
