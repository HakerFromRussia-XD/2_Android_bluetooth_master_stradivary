package com.bailout.stickk.ubi4.utility
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class EncryptionManagerUtilsUbi4 actual constructor() {

    actual fun encrypt(plain: String): String? = try {
        // Генерируем случайный IV (16 байт)
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        // Создаём спецификацию ключа AES
        val keySpec = SecretKeySpec(TOKEN_KEY.toByteArray(Charsets.UTF_8), "AES")

        // Инициализируем шифр
        val cipher = Cipher.getInstance("AES").apply {
            init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        }

        // Шифруем
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        // Объединяем IV и шифртекст, кодируем в Base64
        val ivAndCipherText = getCombinedArray(iv, cipherText)
        Base64.encodeToString(ivAndCipherText, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    actual companion object {
        actual val instance: EncryptionManagerUtilsUbi4 by lazy { EncryptionManagerUtilsUbi4() }
        private const val TOKEN_KEY = "swBpZm3SXMYCIf7O9bZioZ74UwdJkexu"
    }
}