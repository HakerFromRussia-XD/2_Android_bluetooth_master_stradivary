package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.content.Context
import java.io.File

object FirmwareAssets {
    /** Рекурсивно собираем все ZIP из assets начиная с [dir] ("" — корень) */
    fun collectAssetZips(context: Context, dir: String = ""): List<Pair<String, String>> {
        val am = context.assets
        val names = am.list(dir).orEmpty()
        val result = mutableListOf<Pair<String, String>>() // (displayName, assetPath)

        for (name in names) {
            val path = if (dir.isEmpty()) name else "$dir/$name"
            val children = am.list(path).orEmpty()
            if (children.isNotEmpty()) {
                result += collectAssetZips(context, path)
            } else if (name.endsWith(".zip", ignoreCase = true)) {
                result += (name to path)
            }
        }
        return result
    }

    /** Копирует asset по пути [assetPath] в cacheDir и возвращает File */
    fun copyToCache(context: Context, assetPath: String): File {
        val out = File(context.cacheDir, assetPath.substringAfterLast('/'))
        context.assets.open(assetPath).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }
}