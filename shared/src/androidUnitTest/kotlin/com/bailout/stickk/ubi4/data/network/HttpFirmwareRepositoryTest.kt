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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class HttpFirmwareRepositoryTest {
    @Test
    fun `VDS is active while Yandex remains an inactive rollback source`() {
        assertIs<HttpFirmwareRepository>(FirmwareRepositoryProvider.active())
        assertIs<YandexDiskFirmwareRepository>(FirmwareRepositoryProvider.inactiveYandex())
    }

    @Test
    fun `catalog maps static JSON entries to firmware files`() = runBlocking {
        val client = mockClient { request ->
            assertEquals("/catalog.json", request.url.encodedPath)
            respond(
                content = """
                    {
                      "format": 1,
                      "files": [
                        {
                          "family": "GUI",
                          "name": "GUI_v1.2.3.zip",
                          "path": "GUI/GUI_v1.2.3.zip",
                          "size": 42,
                          "version": "1.2.3"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        try {
            val catalog = HttpFirmwareRepository(client, "https://firmware.test/").loadCatalog()
            assertEquals(
                listOf(
                    RemoteFirmwareFile(
                        family = FirmwareBoardFamily.GUI,
                        name = "GUI_v1.2.3.zip",
                        path = "GUI/GUI_v1.2.3.zip",
                        size = 42,
                        version = "1.2.3"
                    )
                ),
                catalog
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun `catalog maps FINGER folder to BLDC board family`() = runBlocking {
        val client = mockClient {
            respond(
                content = """
                    {
                      "format": 1,
                      "files": [
                        {
                          "family": "FINGER",
                          "name": "FH-MultiBoard_v0.7.5_24.zip",
                          "path": "FINGER/FH-MultiBoard_v0.7.5_24.zip",
                          "size": 42,
                          "version": "0.7.5"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        try {
            val catalog = HttpFirmwareRepository(client, "https://firmware.test/").loadCatalog()
            assertEquals(FirmwareBoardFamily.BLDC, catalog.single().family)
            assertEquals("FINGER/FH-MultiBoard_v0.7.5_24.zip", catalog.single().path)
        } finally {
            client.close()
        }
    }

    @Test
    fun `catalog rejects a version that is not sourced as a three part firmware version`() = runBlocking {
        val client = mockClient {
            respond(
                content = """
                    {
                      "format": 1,
                      "files": [
                        {
                          "family": "FAM",
                          "name": "FH-FAM_v9.9.9.zip",
                          "path": "FAM/FH-FAM_v9.9.9.zip",
                          "size": 42,
                          "version": "not-a-version"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }
        try {
            assertFailsWith<IllegalArgumentException> {
                HttpFirmwareRepository(client, "https://firmware.test/").loadCatalog()
            }
            Unit
        } finally {
            client.close()
        }
    }

    @Test
    fun `public file is read directly from static path`() = runBlocking {
        val expected = byteArrayOf(2, 4, 6)
        val client = mockClient { request ->
            assertEquals("/BLDC_Driver/main.zip", request.url.encodedPath)
            respond(expected, status = HttpStatusCode.OK)
        }
        try {
            assertContentEquals(
                expected,
                HttpFirmwareRepository(client, "https://firmware.test/")
                    .readPublicFile("BLDC_Driver/main.zip")
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun `invalid paths never reach network`() = runBlocking {
        val client = mockClient { error("Network must not be used") }
        try {
            val repository = HttpFirmwareRepository(client, "https://firmware.test/")
            assertFailsWith<IllegalArgumentException> {
                repository.readPublicFile("../assembly.json")
            }
            assertFailsWith<IllegalArgumentException> {
                repository.readPublicFile("GUI\\main.zip")
            }
            Unit
        } finally {
            client.close()
        }
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
