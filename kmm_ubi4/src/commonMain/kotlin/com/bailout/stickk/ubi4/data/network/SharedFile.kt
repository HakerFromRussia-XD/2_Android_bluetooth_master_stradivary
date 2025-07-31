package com.bailout.stickk.ubi4.data.network

expect class SharedFile {
    val name: String
    val path: String

    fun child(name: String): SharedFile

    suspend fun writeText(text: String)
    suspend fun writeBytes(bytes: ByteArray)
    suspend fun readBytes(): ByteArray
    fun exists(): Boolean
}

// фабрика, удобная для создания
expect fun sharedFile(path: String): SharedFile