package com.bailout.stickk.ubi4.utility

class EncodeByteToHex {
    companion object {
        // Пример для UTF-8
        fun bytesToHexString(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (i in bytes.indices) {
                val hex = (0xFF and bytes[i].toInt()).toString(16)
                if (hex.length == 1) {
                    sb.append('0')
                }
                sb.append(hex)
            }
            return sb.toString()
        }

        // Пример для ISO-8859-1 (написан вручную)
        fun String.decodeHex(): String {
            require(length % 2 == 0) { "Must have an even length" }
            val bytes = chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            return decodeHexUTF8(bytes)
        }

        /**
         * Простейшая реализация ISO-8859-1:
         * Для каждого байта b берём b & 0xFF, приводим к Char.
         * По сути, это прямое соответствие 0..255 → 0..255 в таблице символов.
         */
        private fun decodeHexUTF8(bytes: ByteArray): String {
            val sb = StringBuilder(bytes.size)
            for (b in bytes) {
                val code = (b.toInt() and 0xFF)
                sb.append(code.toChar())
            }
            return sb.toString()
        }
    }
}