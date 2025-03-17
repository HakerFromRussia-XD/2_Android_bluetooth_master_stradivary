package com.bailout.stickk.ubi4.models.widgets

import java.io.File

// Объявляем ожидаемый тип для представления файлов в KMM
actual class PlatformFile actual constructor(path: String) {
    private val file = File(path)
    actual val path: String
        get() = file.path
    actual fun delete(): Boolean = file.delete()
    actual val name: String
        get() = file.name
    actual fun readBytes(): ByteArray = file.readBytes()
}