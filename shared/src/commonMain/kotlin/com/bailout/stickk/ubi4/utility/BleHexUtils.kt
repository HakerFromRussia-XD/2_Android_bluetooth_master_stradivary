package com.bailout.stickk.ubi4.utility

object BleHexUtils {

    /**
     * Парсит CRC32 из 4 байтов hex в little-endian.
     * Пример: "3412AABB" → CRC long.
     */
    fun crc32FromHexLE(hex: String): Long {
        require(hex.length >= 8) { "CRC hex must be at least 8 characters long" }

        val b0 = hex.substring(0, 2).toInt(16)
        val b1 = hex.substring(2, 4).toInt(16)
        val b2 = hex.substring(4, 6).toInt(16)
        val b3 = hex.substring(6, 8).toInt(16)

        return ((b0) or
                (b1 shl 8) or
                (b2 shl 16) or
                (b3 shl 24))
            .toLong() and 0xFFFFFFFFL
    }
}