package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YandexDiskFirmwareRepositoryTest {

    @Test
    fun `catalog loads zip files from every known board folder`() = runBlocking {
        val requestedFolders = mutableListOf<String>()
        val client = mockClient { request ->
            val folder = request.url.parameters["path"].orEmpty().removePrefix("/")
            requestedFolders += folder
            respond(
                content = """
                    {
                      "_embedded": {
                        "items": [
                          {
                            "name": "${folder}_v1.2.3.zip",
                            "path": "disk:/firmware/$folder/${folder}_v1.2.3.zip",
                            "type": "file",
                            "size": 3
                          },
                          {
                            "name": "readme.txt",
                            "path": "disk:/firmware/$folder/readme.txt",
                            "type": "file",
                            "size": 1
                          }
                        ]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val catalog = YandexDiskFirmwareRepository(
            client = client,
            publicUrl = "https://disk.yandex.test/public",
            apiBaseUrl = "https://cloud-api.yandex.test/"
        ).loadCatalog()

        assertEquals(listOf("FAM", "GUI", "EMG", "BLDC_Driver"), requestedFolders)
        assertEquals(
            listOf(
                FirmwareBoardFamily.FAM,
                FirmwareBoardFamily.GUI,
                FirmwareBoardFamily.EMG,
                FirmwareBoardFamily.BLDC
            ),
            catalog.map(RemoteFirmwareFile::family)
        )
        assertTrue(catalog.all { it.name.endsWith(".zip") })
    }

    @Test
    fun `download resolves public link and saves bytes through SharedFile`() = runBlocking {
        val firmwareBytes = byteArrayOf(1, 2, 3, 4)
        val client = mockClient { request ->
            when (request.url.encodedPath) {
                "/v1/disk/public/resources/download" -> respond(
                    content = "{\"href\":\"https://download.yandex.test/firmware.zip\"}",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )

                "/firmware.zip" -> respond(
                    content = ByteReadChannel(firmwareBytes),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                )

                else -> error("Unexpected URL: ${request.url}")
            }
        }
        val cacheDirectory = createTempDir(prefix = "firmware-cache-")
        val repository = YandexDiskFirmwareRepository(
            client = client,
            publicUrl = "https://disk.yandex.test/public",
            apiBaseUrl = "https://cloud-api.yandex.test/"
        )

        val downloaded = repository.download(
            file = RemoteFirmwareFile(
                family = FirmwareBoardFamily.GUI,
                name = "GUI_v0.4.6.zip",
                path = "disk:/firmware/GUI/GUI_v0.4.6.zip",
                size = firmwareBytes.size.toLong()
            ),
            cacheDirectory = sharedFile(cacheDirectory.absolutePath)
        )

        assertTrue(downloaded.exists())
        assertEquals(
            File(cacheDirectory, "yandex_firmware/GUI_v0.4.6.zip").absolutePath,
            File(downloaded.path).absolutePath
        )
        assertContentEquals(firmwareBytes, downloaded.readBytes())
    }

    private fun mockClient(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(
            io.ktor.client.request.HttpRequestData
        ) -> io.ktor.client.request.HttpResponseData
    ): HttpClient = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
