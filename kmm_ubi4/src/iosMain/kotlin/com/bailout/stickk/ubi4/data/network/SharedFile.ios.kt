package com.bailout.stickk.ubi4.data.network

import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.posix.memcpy

actual class SharedFile internal constructor(private val url: NSURL) {
    actual val name: String
        get() = url.lastPathComponent ?: ""

    actual val path: String
        get() = url.path ?: ""

    actual fun child(name: String): SharedFile {
        val childUrl = url.URLByAppendingPathComponent(name)
            ?: error("Cannot append component '$name' to URL $url")
        return SharedFile(childUrl)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun writeText(text: String) = withContext(Dispatchers.Default) {
        val nsStr = NSString.create(string = text)
        val success = nsStr.writeToURL(
            url,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        if (!success) {
            throw IOException("Failed to write text to $path")
        }
    }

    actual suspend fun writeBytes(bytes: ByteArray) = withContext(Dispatchers.Default) {
        val data = bytes.toNSData()
        val success = data.writeToURL(url, atomically = true)
        if (!success) {
            throw IOException("Failed to write bytes to $path")
        }
    }

    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.Default) {
        val data = NSData.dataWithContentsOfURL(url) ?: throw IOException("Failed to read bytes from $path")
        data.toByteArray() ?: throw IOException("Failed to convert NSData to ByteArray for $path")
    }

    actual fun exists(): Boolean {
        val fm = NSFileManager.defaultManager
        return fm.fileExistsAtPath(path)
    }
}

actual fun sharedFile(path: String): SharedFile {
    val fileUrl = NSURL.fileURLWithPath(path)
    return SharedFile(fileUrl)
}

// --- helpers ---

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray? {
    val length = this.length.toInt()
    val bytes = this.bytes ?: return null
    return ByteArray(length).also { byteArray ->
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length.convert<platform.posix.size_t>())
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned { pinned ->
    NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
}