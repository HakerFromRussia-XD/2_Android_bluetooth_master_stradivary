package com.bailout.stickk.ubi4.utility

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCKeySizeAES256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

actual class EncryptionManagerUtilsUbi4 actual constructor() {
    /**
     * Шифрует входную строку и возвращает Base64(IV + cipherText) или null при ошибке.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun encrypt(plain: String): String? {
        val iv = ByteArray(kCCBlockSizeAES128.toInt())
        val randomStatus = iv.usePinned {
            SecRandomCopyBytes(kSecRandomDefault, iv.size.convert(), it.addressOf(0))
        }
        if (randomStatus != 0) return null

        val key = TOKEN_KEY.encodeToByteArray()
        val input = plain.encodeToByteArray()
        val output = ByteArray(input.size + kCCBlockSizeAES128.toInt())

        return memScoped {
            val bytesEncrypted = alloc<ULongVar>()
            val status = key.usePinned { keyPinned ->
                iv.usePinned { ivPinned ->
                    input.usePinned { inputPinned ->
                        output.usePinned { outputPinned ->
                            CCCrypt(
                                op = kCCEncrypt,
                                alg = kCCAlgorithmAES,
                                options = kCCOptionPKCS7Padding,
                                key = keyPinned.addressOf(0),
                                keyLength = kCCKeySizeAES256.convert(),
                                iv = ivPinned.addressOf(0),
                                dataIn = inputPinned.addressOf(0),
                                dataInLength = input.size.convert(),
                                dataOut = outputPinned.addressOf(0),
                                dataOutAvailable = output.size.convert(),
                                dataOutMoved = bytesEncrypted.ptr
                            )
                        }
                    }
                }
            }

            if (status != kCCSuccess) return@memScoped null

            val blockSize = kCCBlockSizeAES128.toInt()
            val encryptedLength = ((input.size / blockSize) + 1) * blockSize
            val encrypted = output.copyOf(encryptedLength)
            encodeBase64(iv + encrypted)
        }
    }

    actual companion object {
        /** Синглтон-экземпляр для каждой платформы */
        actual val instance: EncryptionManagerUtilsUbi4
            by lazy { EncryptionManagerUtilsUbi4() }
        private const val TOKEN_KEY = "swBpZm3SXMYCIf7O9bZioZ74UwdJkexu"
    }

}

private fun encodeBase64(bytes: ByteArray): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val output = StringBuilder(((bytes.size + 2) / 3) * 4)
    var index = 0
    while (index < bytes.size) {
        val b0 = bytes[index].toInt() and 0xFF
        val hasB1 = index + 1 < bytes.size
        val hasB2 = index + 2 < bytes.size
        val b1 = if (hasB1) bytes[index + 1].toInt() and 0xFF else 0
        val b2 = if (hasB2) bytes[index + 2].toInt() and 0xFF else 0

        output.append(alphabet[b0 shr 2])
        output.append(alphabet[((b0 and 0x03) shl 4) or (b1 shr 4)])
        output.append(if (hasB1) alphabet[((b1 and 0x0F) shl 2) or (b2 shr 6)] else '=')
        output.append(if (hasB2) alphabet[b2 and 0x3F] else '=')
        index += 3
    }
    return output.toString()
}
