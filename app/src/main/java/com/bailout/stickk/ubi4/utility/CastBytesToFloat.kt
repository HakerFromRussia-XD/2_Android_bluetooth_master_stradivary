package com.bailout.stickk.ubi4.utility

import java.nio.ByteBuffer
import java.nio.ByteOrder

class CastBytesToFloat {
    companion object{
        fun castBytesToFloatArray(bytes: ByteArray): ArrayList<Float>  {
            val floatArray = ArrayList<Float> (bytes.size / 4)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            while (buffer.remaining() >= 4) {
                floatArray.add(buffer.float)
            }
            return floatArray
        }

    }
}