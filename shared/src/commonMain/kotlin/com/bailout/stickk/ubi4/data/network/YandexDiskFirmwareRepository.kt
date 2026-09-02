package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class RemoteFirmwareFile(
    val family: FirmwareBoardFamily,
    val name: String,
    val path: String,
    val size: Long
)

class YandexDiskFirmwareRepository(
    private val client: HttpClient = PlatformClientProvider.firmwareClient,
    private val publicUrl: String = PUBLIC_FIRMWARE_URL,
    private val apiBaseUrl: String = YANDEX_API_BASE_URL
) {
    suspend fun loadCatalog(): List<RemoteFirmwareFile> =
        FirmwareBoardFamily.entries
            .filterNot { it == FirmwareBoardFamily.UNKNOWN }
            .flatMap { family -> loadFolder(family) }

    suspend fun download(file: RemoteFirmwareFile, cacheDirectory: SharedFile): SharedFile {
        val downloadUrl = requestDownloadUrl(file.path)
        val response = client.get(downloadUrl)
        response.ensureSuccess("Firmware download failed")

        val bytes = response.body<ByteArray>()
        if (file.size > 0L && bytes.size.toLong() != file.size) {
            throw IOException("Downloaded firmware size does not match catalog metadata")
        }

        val safeName = file.name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .takeIf(String::isNotBlank)
            ?: throw IOException("Firmware file name is empty")
        val target = cacheDirectory.child(CACHE_DIRECTORY).child(safeName)
        target.writeBytes(bytes)
        return target
    }

    private suspend fun loadFolder(family: FirmwareBoardFamily): List<RemoteFirmwareFile> {
        val response = client.get(endpoint(PUBLIC_RESOURCE_PATH)) {
            parameter("public_key", publicUrl)
            parameter("path", "/${family.folderName}")
            parameter("limit", MAX_FILES_PER_FOLDER)
        }
        response.ensureSuccess("Firmware catalog request failed for ${family.folderName}")

        return response.body<YandexResourceResponse>()
            .embedded
            ?.items
            .orEmpty()
            .asSequence()
            .filter { it.type == RESOURCE_TYPE_FILE }
            .filter { it.name.endsWith(ZIP_EXTENSION, ignoreCase = true) }
            .filter { it.path.isNotBlank() }
            .map { item ->
                RemoteFirmwareFile(
                    family = family,
                    name = item.name,
                    path = item.path,
                    size = item.size
                )
            }
            .toList()
    }

    private suspend fun requestDownloadUrl(path: String): String {
        val response = client.get(endpoint(DOWNLOAD_RESOURCE_PATH)) {
            parameter("public_key", publicUrl)
            parameter("path", path)
        }
        response.ensureSuccess("Firmware link request failed")
        return response.body<YandexDownloadResponse>().href
            .takeIf(String::isNotBlank)
            ?: throw IOException("Firmware download link is empty")
    }

    private fun endpoint(path: String): String =
        "${apiBaseUrl.trimEnd('/')}/${path.trimStart('/')}"

    private fun HttpResponse.ensureSuccess(message: String) {
        if (status.value !in 200..299) {
            throw IOException("$message: HTTP ${status.value}")
        }
    }

    companion object {
        const val PUBLIC_FIRMWARE_URL = "https://disk.yandex.ru/d/hOV0LPW05OAdWw"

        private const val YANDEX_API_BASE_URL = "https://cloud-api.yandex.net/"
        private const val PUBLIC_RESOURCE_PATH = "v1/disk/public/resources"
        private const val DOWNLOAD_RESOURCE_PATH = "v1/disk/public/resources/download"
        private const val MAX_FILES_PER_FOLDER = 1000
        private const val CACHE_DIRECTORY = "yandex_firmware"
        private const val RESOURCE_TYPE_FILE = "file"
        private const val ZIP_EXTENSION = ".zip"
    }
}

@Serializable
private data class YandexResourceResponse(
    @SerialName("_embedded") val embedded: YandexEmbeddedResource? = null
)

@Serializable
private data class YandexEmbeddedResource(
    val items: List<YandexResourceItem> = emptyList()
)

@Serializable
private data class YandexResourceItem(
    val name: String = "",
    val path: String = "",
    val type: String = "",
    val size: Long = -1L
)

@Serializable
private data class YandexDownloadResponse(
    val href: String = ""
)
