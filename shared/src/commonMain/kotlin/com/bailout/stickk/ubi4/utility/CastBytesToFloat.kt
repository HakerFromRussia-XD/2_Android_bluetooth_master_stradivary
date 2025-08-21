package com.bailout.stickk.ubi4.utility

object CastBytesToFloat {

    /**
     * Преобразует массив байтов в список чисел Float.
     * Каждый 4 байта интерпретируются как float в little-endian.
     */
    fun castBytesToFloatArray(bytes: ByteArray): List<Float> {
        val floatList = mutableListOf<Float>()
        val n = bytes.size / 4
        for (i in 0 until n) {
            val index = i * 4
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = bytes[index + 1].toInt() and 0xFF
            val b2 = bytes[index + 2].toInt() and 0xFF
            val b3 = bytes[index + 3].toInt() and 0xFF
            // Little-endian: младший байт первый.
            val intBits = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            floatList.add(Float.fromBits(intBits))
        }
        return floatList
    }
}