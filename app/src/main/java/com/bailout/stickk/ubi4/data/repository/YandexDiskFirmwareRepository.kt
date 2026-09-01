package com.bailout.stickk.ubi4.data.repository

import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class RemoteFirmwareFile(
    val family: FirmwareBoardFamily,
    val name: String,
    val path: String,
    val size: Long
)

class YandexDiskFirmwareRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val publicUrl: String = PUBLIC_FIRMWARE_URL,
    private val apiBaseUrl: HttpUrl = YANDEX_API_BASE_URL.toHttpUrl()
) {
    suspend fun loadCatalog(): List<RemoteFirmwareFile> = withContext(Dispatchers.IO) {
        FirmwareBoardFamily.entries
            .filterNot { it == FirmwareBoardFamily.UNKNOWN }
            .flatMap(::loadFolder)
    }

    suspend fun download(file: RemoteFirmwareFile, cacheDirectory: File): File =
        withContext(Dispatchers.IO) {
            val downloadUrl = requestDownloadUrl(file.path)
            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Firmware download failed: HTTP ${response.code}" }
                val body = checkNotNull(response.body) { "Firmware download response is empty" }
                val targetDirectory = File(cacheDirectory, CACHE_DIRECTORY)
                check(targetDirectory.exists() || targetDirectory.mkdirs()) {
                    "Cannot create firmware cache directory"
                }

                val safeName = file.name.substringAfterLast('/').substringAfterLast('\\')
                val target = File(targetDirectory, safeName)
                val temporary = File(targetDirectory, ".$safeName.part")
                body.byteStream().use { input ->
                    FileOutputStream(temporary, false).use { output -> input.copyTo(output) }
                }
                if (file.size > 0L && temporary.length() != file.size) {
                    temporary.delete()
                    error("Downloaded firmware size does not match catalog metadata")
                }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
                target
            }
        }

    private fun loadFolder(family: FirmwareBoardFamily): List<RemoteFirmwareFile> {
        val url = publicResourceUrl(path = "/${family.folderName}")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                "Firmware catalog request failed for ${family.folderName}: HTTP ${response.code}"
            }
            val body = response.body?.string()
            val root = JSONObject(checkNotNull(body) { "Firmware catalog response is empty" })
            val items = root.getJSONObject("_embedded").getJSONArray("items")
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    if (item.optString("type") != "file") continue
                    val name = item.optString("name")
                    if (!name.endsWith(".zip", ignoreCase = true)) continue
                    add(
                        RemoteFirmwareFile(
                            family = family,
                            name = name,
                            path = item.getString("path"),
                            size = item.optLong("size", -1L)
                        )
                    )
                }
            }
        }
    }

    private fun requestDownloadUrl(path: String): String {
        val request = Request.Builder()
            .url(apiUrl(DOWNLOAD_RESOURCE_PATH, path, includeLimit = false))
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Firmware link request failed: HTTP ${response.code}" }
            val body = response.body?.string()
            return JSONObject(checkNotNull(body) { "Firmware link response is empty" })
                .getString("href")
        }
    }

    private fun publicResourceUrl(path: String): HttpUrl =
        apiUrl(PUBLIC_RESOURCE_PATH, path, includeLimit = true)

    private fun apiUrl(resourcePath: String, path: String, includeLimit: Boolean): HttpUrl =
        apiBaseUrl.newBuilder()
            .addPathSegments(resourcePath)
            .addQueryParameter("public_key", publicUrl)
            .addQueryParameter("path", path)
            .apply {
                if (includeLimit) addQueryParameter("limit", MAX_FILES_PER_FOLDER.toString())
            }
            .build()

    companion object {
        const val PUBLIC_FIRMWARE_URL = "https://disk.yandex.ru/d/hOV0LPW05OAdWw"

        private const val YANDEX_API_BASE_URL = "https://cloud-api.yandex.net/"
        private const val PUBLIC_RESOURCE_PATH = "v1/disk/public/resources"
        private const val DOWNLOAD_RESOURCE_PATH = "v1/disk/public/resources/download"
        private const val MAX_FILES_PER_FOLDER = 1000
        private const val CACHE_DIRECTORY = "yandex_firmware"
    }
}
