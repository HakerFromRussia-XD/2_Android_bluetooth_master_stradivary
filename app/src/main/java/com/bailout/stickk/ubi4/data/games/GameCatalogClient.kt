package com.bailout.stickk.ubi4.data.games

import android.net.Uri
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

internal class GameCatalogClient(
    private val expectedPackageName: String,
    private val failureMessage: () -> String,
    private val httpClient: Call.Factory = OkHttpClient(),
) {
    fun loadRemoteGame(manifestUrl: String): RemoteGame {
        val request = Request.Builder().url(resolveManifestUrl(manifestUrl)).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(failureMessage())
            val body = response.body?.string() ?: error(failureMessage())
            return GameCatalog.parseStk(body, expectedPackageName)
        }
    }

    private fun resolveManifestUrl(url: String): String {
        if (!isYandexDiskPublicUrl(url)) return url
        val uri = Uri.parse(url)
        val publicKey = uri.buildUpon().clearQuery().fragment(null).build().toString()
        val apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("public_key", publicKey)
            .apply {
                uri.getQueryParameter("path")?.takeIf { it.isNotBlank() }?.let {
                    addQueryParameter("path", it)
                }
            }
            .build()
        val request = Request.Builder().url(apiUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(failureMessage())
            val body = response.body?.string() ?: error(failureMessage())
            return JSONObject(body).getString("href")
        }
    }

    private fun isYandexDiskPublicUrl(url: String): Boolean {
        val host = Uri.parse(url).host.orEmpty()
        return host == "disk.yandex.ru" || host == "yadi.sk"
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
