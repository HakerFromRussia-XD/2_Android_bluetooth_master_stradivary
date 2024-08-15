package com.bailout.stickk.ubi4.utility

import kotlin.math.abs

class CastToUnsignedInt {
    object Companion {
        fun castUnsignedCharToInt(Ubyte: Byte): Int {
            var cast = Ubyte.toInt()
            if (cast < 0) {
                cast += 256
            }
            return cast
        }
    }
}