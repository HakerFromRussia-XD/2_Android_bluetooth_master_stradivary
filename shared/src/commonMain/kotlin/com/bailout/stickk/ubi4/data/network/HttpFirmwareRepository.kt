package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

class HttpFirmwareRepository(
    private val client: HttpClient = PlatformClientProvider.firmwareClient,
    private val baseUrl: String = FirmwareRepositoryProvider.ACTIVE_BASE_URL
) : FirmwareRepository {
    override suspend fun readPublicFile(path: String): ByteArray {
        val normalizedPath = normalizedPath(path)
        return requestImmediately {
            val response = client.get(resourceUrl(normalizedPath))
            response.ensureSuccess("Firmware resource download failed")
            response.body<ByteArray>()
        }
    }

    override suspend fun loadCatalog(): List<RemoteFirmwareFile> {
        val response = requestImmediately {
            client.get(resourceUrl(CATALOG_FILE)).also {
                it.ensureSuccess("Firmware catalog request failed")
            }
        }
        return response.body<HttpFirmwareCatalog>()
            .files
            .map { item ->
                val family = familyForCatalogName(item.family)
                    ?: throw IOException("Unknown firmware family: ${item.family}")
                val path = normalizedPath(item.path)
                require(path.startsWith("${item.family}/")) {
                    "Firmware catalog path does not match family"
                }
                require(item.name == path.substringAfterLast('/')) {
                    "Firmware catalog name does not match path"
                }
                require(item.name.endsWith(ZIP_EXTENSION, ignoreCase = true)) {
                    "Firmware catalog contains a non-ZIP file"
                }
                require(item.size >= 0L) { "Firmware catalog contains an invalid size" }
                require(VERSION_PATTERN.matches(item.version)) {
                    "Firmware catalog contains an invalid version"
                }
                RemoteFirmwareFile(family, item.name, path, item.size, item.version)
            }
    }

    override suspend fun download(
        file: RemoteFirmwareFile,
        cacheDirectory: SharedFile
    ): SharedFile {
        val bytes = readPublicFile(file.path)
        if (file.size > 0L && bytes.size.toLong() != file.size) {
            throw IOException("Downloaded firmware size does not match catalog metadata")
        }
        val target = cacheDirectory.child(CACHE_DIRECTORY).child(file.name)
        target.writeBytes(bytes)
        return target
    }

    private fun resourceUrl(path: String): String =
        URLBuilder(baseUrl.trimEnd('/') + "/")
            .apply { appendPathSegments(path.split('/')) }
            .buildString()

    private fun normalizedPath(path: String): String {
        val normalized = path.trimStart('/')
        require(normalized.isNotBlank()) { "Firmware path is empty" }
        require('\\' !in normalized && ':' !in normalized) { "Invalid firmware path" }
        require(normalized.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "Invalid firmware path"
        }
        return normalized
    }

    private fun familyForCatalogName(name: String): FirmwareBoardFamily? =
        when (name) {
            FINGER_FAMILY_ALIAS -> FirmwareBoardFamily.BLDC
            else -> FirmwareBoardFamily.entries.firstOrNull {
                it != FirmwareBoardFamily.UNKNOWN && it.folderName == name
            }
        }

    private suspend fun <T> requestImmediately(request: suspend () -> T): T {
        var failure: IOException? = null
        repeat(3) {
            try {
                return request()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                failure = error
            }
        }
        throw failure ?: IOException("Firmware request failed")
    }

    private fun HttpResponse.ensureSuccess(message: String) {
        if (status.value !in 200..299) {
            throw IOException("$message: HTTP ${status.value}")
        }
    }

    private companion object {
        const val CATALOG_FILE = "catalog.json"
        const val CACHE_DIRECTORY = "http_firmware"
        const val FINGER_FAMILY_ALIAS = "FINGER"
        const val ZIP_EXTENSION = ".zip"
        val VERSION_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+$")
    }
}

@Serializable
private data class HttpFirmwareCatalog(
    val format: Int,
    val files: List<HttpFirmwareCatalogItem>
) {
    init {
        require(format == 1) { "Unsupported firmware catalog format" }
    }
}

@Serializable
private data class HttpFirmwareCatalogItem(
    val family: String,
    val name: String,
    val path: String,
    val size: Long,
    val version: String
)
