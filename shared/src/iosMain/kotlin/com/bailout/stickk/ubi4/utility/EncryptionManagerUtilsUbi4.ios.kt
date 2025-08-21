package com.bailout.stickk.ubi4.utility

actual class EncryptionManagerUtilsUbi4 actual constructor() {
    /**
     * Шифрует входную строку и возвращает Base64(IV + cipherText) или null при ошибке.
     */
    actual fun encrypt(plain: String): String? {
        TODO("Not yet implemented")
    }

    actual companion object {
        /** Синглтон-экземпляр для каждой платформы */
        actual val instance: EncryptionManagerUtilsUbi4
            get() = TODO("Not yet implemented")
    }

}