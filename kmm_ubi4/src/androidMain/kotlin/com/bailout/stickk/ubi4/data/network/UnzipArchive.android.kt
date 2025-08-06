package com.bailout.stickk.ubi4.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

actual suspend fun unzipArchive(
    zipFile: SharedFile,
    outputDir: SharedFile
): Pair<SharedFile, List<SharedFile>> = withContext(Dispatchers.IO) {
    val zip = ZipFile(File(zipFile.path))
    val unpacked = mutableListOf<SharedFile>()
    zip.use {
        zip.entries().asSequence().forEach { entry ->
            val outFile = File(outputDir.path, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    outFile.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
                unpacked += SharedFile(outFile)
            }
        }
    }
    zipFile to unpacked
}
