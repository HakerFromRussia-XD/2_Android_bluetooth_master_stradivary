package com.bailout.stickk.ubi4.firmware.user

import com.bailout.stickk.ubi4.data.network.sharedFile
import kotlinx.cinterop.*
import platform.CoreCrypto.CC_SHA256

@OptIn(ExperimentalForeignApi::class)
actual fun firmwareSha256(bytes: ByteArray): String {
    val result = UByteArray(32)
    result.usePinned { output ->
        if (bytes.isEmpty()) CC_SHA256(null, 0u, output.addressOf(0))
        else bytes.usePinned { CC_SHA256(it.addressOf(0), bytes.size.toUInt(), output.addressOf(0)) }
    }
    return result.joinToString("") { it.toString(16).padStart(2, '0') }
}
actual suspend fun writeFirmwareJournal(path: String, text: String) = sharedFile(path).writeText(text)
