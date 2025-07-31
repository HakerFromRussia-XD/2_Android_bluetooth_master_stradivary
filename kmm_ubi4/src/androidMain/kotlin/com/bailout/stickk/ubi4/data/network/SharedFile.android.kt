package com.bailout.stickk.ubi4.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class SharedFile internal constructor(private val delegate: java.io.File) {
    actual val name: String get() = delegate.name
    actual val path: String get() = delegate.path

    fun toFile(): java.io.File = delegate


    actual fun child(name: String): SharedFile = SharedFile(java.io.File(delegate, name))

    actual suspend fun writeText(text: String) = withContext(Dispatchers.IO) {
        delegate.parentFile?.mkdirs()
        delegate.writeText(text)
    }

    actual suspend fun writeBytes(bytes: ByteArray) = withContext(Dispatchers.IO) {
        delegate.parentFile?.mkdirs()
        delegate.writeBytes(bytes)
    }

    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        delegate.readBytes()
    }

    actual fun exists(): Boolean = delegate.exists()
}

actual fun sharedFile(path: String): SharedFile = SharedFile(java.io.File(path))