package com.bailout.stickk.ubi4.data.games

import okhttp3.Call
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class GameCatalogClient(
    private val expectedPackageName: String,
    private val failureMessage: () -> String,
    private val httpClient: Call.Factory = OkHttpClient(),
) {
    fun loadRemoteGames(manifestUrl: String): List<RemoteGame> {
        val request = Request.Builder()
            .url(manifestUrl)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(failureMessage())
            val body = response.body?.string() ?: error(failureMessage())
            return GameCatalog.parseGames(body, expectedPackageName)
        }
    }

    fun isPublishedInRuStore(packageName: String): Boolean {
        val request = Request.Builder()
            .url(ruStoreWebUrl(packageName))
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> true
                response.code == 404 -> false
                else -> error("RuStore returned HTTP ${response.code}")
            }
        }
    }

}

internal fun ruStoreWebUrl(packageName: String): String =
    "https://www.rustore.ru/catalog/app/$packageName"
