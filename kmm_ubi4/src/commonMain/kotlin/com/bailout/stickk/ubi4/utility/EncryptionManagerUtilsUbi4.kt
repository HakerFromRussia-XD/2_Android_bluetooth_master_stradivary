package com.bailout.stickk.ubi4.utility

expect class EncryptionManagerUtilsUbi4() {
    /**
     * Шифрует входную строку и возвращает Base64(IV + cipherText) или null при ошибке.
     */
    fun encrypt(plain: String): String?

    companion object {
        /** Синглтон-экземпляр для каждой платформы */
        val instance: EncryptionManagerUtilsUbi4
    }
}

/**
 * Объединяет два байтовых массива.
 */
internal fun getCombinedArray(one: ByteArray, two: ByteArray): ByteArray = one + two

/**
 * Преобразует байтовый массив в hex-строку.
 */
internal fun ByteArray.toHexString(): String {
    val sb = StringBuilder(this.size * 2)
    for (b in this) {
        val hex = (b.toInt() and 0xFF).toString(16)
        if (hex.length == 1) sb.append('0')
        sb.append(hex)
    }
    return sb.toString()
}