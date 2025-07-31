package com.bailout.stickk.ubi4.data.network

actual class SharedFile {
    actual val name: String
        get() = TODO("Not yet implemented")
    actual val path: String
        get() = TODO("Not yet implemented")

    actual fun child(name: String): SharedFile {
        TODO("Not yet implemented")
    }

    actual suspend fun writeText(text: String) {
    }

    actual suspend fun writeBytes(bytes: ByteArray) {
    }

    actual suspend fun readBytes(): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun exists(): Boolean {
        TODO("Not yet implemented")
    }

}

actual fun sharedFile(path: String): SharedFile {
    TODO("Not yet implemented")
}