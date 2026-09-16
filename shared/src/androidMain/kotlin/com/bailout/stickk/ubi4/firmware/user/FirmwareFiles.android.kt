package com.bailout.stickk.ubi4.firmware.user

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun firmwareSha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

actual suspend fun writeFirmwareJournal(path: String, text: String) = withContext(Dispatchers.IO) {
    val target = File(path)
    target.parentFile?.mkdirs()
    val temporary = File("$path.tmp")
    FileOutputStream(temporary).use { it.write(text.toByteArray()); it.fd.sync() }
    check(temporary.renameTo(target)) { "Cannot commit firmware journal" }
}
