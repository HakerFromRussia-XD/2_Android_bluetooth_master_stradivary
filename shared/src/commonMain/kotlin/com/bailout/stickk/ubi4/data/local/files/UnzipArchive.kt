package com.bailout.stickk.ubi4.data.network

// объявляем ожидание unzip-а
expect suspend fun unzipArchive(
    zipFile: SharedFile,
    outputDir: SharedFile
): Pair<SharedFile, List<SharedFile>>