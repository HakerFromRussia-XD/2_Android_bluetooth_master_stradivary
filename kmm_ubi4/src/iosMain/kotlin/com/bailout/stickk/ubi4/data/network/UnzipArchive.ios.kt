package com.bailout.stickk.ubi4.data.network

import kotlinx.coroutines.suspendCancellableCoroutine

actual suspend fun unzipArchive(
    zipFile: SharedFile,
    outputDir: SharedFile
): Pair<SharedFile, List<SharedFile>> = suspendCancellableCoroutine { cont ->

}