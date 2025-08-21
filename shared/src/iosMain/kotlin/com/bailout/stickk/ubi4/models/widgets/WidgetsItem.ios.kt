package com.bailout.stickk.ubi4.models.widgets

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile

actual class PlatformFile actual constructor(path: String) {
    actual val path: String = path

    @OptIn(ExperimentalForeignApi::class)
    actual fun delete(): Boolean {
        val fileManager = NSFileManager.defaultManager
        // Если не требуется подробная обработка ошибки, можно передать null для ошибки
        return fileManager.removeItemAtPath(path, null)
    }

    actual val name: String
        get() = path.substringAfterLast("/")

    @OptIn(ExperimentalForeignApi::class)
    actual fun readBytes(): ByteArray {
        val data: NSData = NSData.dataWithContentsOfFile(path) ?: return ByteArray(0)
        val length = data.length.toInt()
        val byteArray = ByteArray(length)
        val pointer = data.bytes?.reinterpret<ByteVar>()
        if (pointer != null) {
            for (i in 0 until length) {
                byteArray[i] = pointer[i]
            }
        }
        return byteArray
    }
}