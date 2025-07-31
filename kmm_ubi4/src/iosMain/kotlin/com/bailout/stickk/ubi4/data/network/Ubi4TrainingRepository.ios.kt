package com.bailout.stickk.ubi4.data.network

import io.ktor.utils.io.ByteReadChannel

actual suspend fun unzipArchive(
    zipFile: SharedFile,
    outputDir: SharedFile
): Pair<SharedFile, List<SharedFile>> {
    TODO("Not yet implemented")
}

actual suspend fun SharedFile.writeFromChannel(channel: ByteReadChannel) {
}