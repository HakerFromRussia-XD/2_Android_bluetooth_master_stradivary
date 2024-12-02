package com.bailout.stickk.ubi4.utility

class EncodeByteToHex {
    companion object {
        @JvmStatic
        fun bytesToHexString(bytes: ByteArray): String {
            val sb = StringBuffer()
            for (i in bytes.indices) {
                val hex = Integer.toHexString(0xFF and bytes[i].toInt())
                if (hex.length == 1) {
                    sb.append('0')
                }
                sb.append(hex)
            }
            return sb.toString()
        }
        fun String.decodeHex(): String {
            require(length % 2 == 0) {"Must have an even length"}
            return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
                .toString(Charsets.ISO_8859_1)  // Or whichever encoding your input uses
        }

        fun String.decodeHexUTF8(): String {
            require(length % 2 == 0) {"Must have an even length"}
            return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
                .toString(Charsets.UTF_8)  // Or whichever encoding your input uses
        }


    }
}