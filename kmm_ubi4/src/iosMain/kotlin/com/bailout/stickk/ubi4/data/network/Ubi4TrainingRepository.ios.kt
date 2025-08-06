package com.bailout.stickk.ubi4.data.network

import io.ktor.utils.io.ByteReadChannel

actual suspend fun SharedFile.writeFromChannel(channel: ByteReadChannel) {
}