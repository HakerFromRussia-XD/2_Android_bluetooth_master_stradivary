package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.models.network.SerialTokenRequest
import com.bailout.stickk.ubi4.models.network.TakeDataRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.BufferedSource
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NetworkCoverageTest {

    @Test
    fun `requests api should handle safe get and safe post branches`() {
        runBlocking {
        val okClient = mockClient { req ->
            when {
                req.url.encodedPath.endsWith("/v1/auth/login") -> respond(
                    content = "{\"token\":\"abc\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/ser_n_token/") -> respond(
                    content = "{\"access_token\":\"jwt\",\"token_type\":\"bearer\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/clients_table/clients/") -> respond(
                    content = "[{\"client_id\":1,\"corp_id\":2,\"name\":\"n\",\"password\":\"p\"}]",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/get_passports_data/") -> respond(
                    content = "{\"content\":\"yaml\",\"filename\":\"passport.yaml\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/take_data/") -> respond(
                    content = "zip-body",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                )
                req.url.encodedPath.endsWith("/passport_data/") -> respond(
                    content = "event: complete\\ndata: {\"checkpoint\":\"cp_1\"}\\n\\n",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
                )
                else -> respond("bad", HttpStatusCode.BadRequest)
            }
        }

        val api = Ubi4RequestsApi(userClient = okClient, passportClient = okClient)

        val token = api.getToken("Basic xxx")
        assertIs<NetworkResult.Success<*>>(token)

        val login = api.loginBySerial("api-key", SerialTokenRequest("SER", "PWD"))
        assertIs<NetworkResult.Success<*>>(login)

        val clients = api.getClients("Bearer x")
        assertIs<NetworkResult.Success<*>>(clients)

        val passport = api.getPassportData("Bearer x", "SER")
        assertIs<NetworkResult.Success<*>>(passport)

        val downloadResp = api.downloadArchive("Bearer x", TakeDataRequest(listOf("cp")))
        assertEquals(HttpStatusCode.OK, downloadResp.status)

        val sseResp = api.uploadTrainingDataSseRaw(
            auth = "Bearer x",
            content = io.ktor.client.request.forms.MultiPartFormDataContent(
                io.ktor.client.request.forms.formData {
                    append("serial", "SER")
                }
            )
        )
        assertEquals(HttpStatusCode.OK, sseResp.status)

        val non200 = api.getUserInfo(token = "t", lang = "ru")
        assertIs<NetworkResult.Error>(non200)
        }
    }

    @Test
    fun `requests api should map network and unknown exceptions`() {
        runBlocking {
        val ioFail = mockClient { throw IOException("offline") }
        val unknownFail = mockClient { throw IllegalStateException("boom") }

        val ioApi = Ubi4RequestsApi(userClient = ioFail, passportClient = ioFail)
        val unknownApi = Ubi4RequestsApi(userClient = unknownFail, passportClient = unknownFail)

        val ioGet = ioApi.getToken("Basic a")
        val ioPost = ioApi.loginBySerial("k", SerialTokenRequest("s", "p"))
        val ioPassport = ioApi.getPassportData("Bearer x", "SER")

        assertIs<NetworkResult.Error>(ioGet)
        assertIs<NetworkResult.Error>(ioPost)
        assertIs<NetworkResult.Error>(ioPassport)

        val unknownGet = unknownApi.getToken("Basic a")
        val unknownPost = unknownApi.loginBySerial("k", SerialTokenRequest("s", "p"))
        val unknownPassport = unknownApi.getPassportData("Bearer x", "SER")
        assertIs<NetworkResult.Error>(unknownGet)
        assertIs<NetworkResult.Error>(unknownPost)
        assertIs<NetworkResult.Error>(unknownPassport)
        }
    }

    @Test
    fun `requests api should cover remaining endpoints and http error branches`() {
        runBlocking {
        val okClient = mockClient { req ->
            when (req.url.encodedPath) {
                "/v1/user/info" -> respond(
                    content = "{\"user_info\":{\"username\":\"u\",\"email\":\"e\",\"sex\":1,\"phone\":\"1\",\"fio\":\"f\",\"country_code\":\"RU\",\"photo\":\"p\",\"fname\":\"n\",\"sname\":\"s\",\"city\":\"c\",\"birth_date\":0,\"client_id\":1,\"manager\":{\"fio\":\"m\",\"email\":\"m@x\",\"phone\":\"2\",\"photo\":\"ph\"}}}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                "/v1/clients/42/devices" -> respond(
                    content = "[{\"id\":1,\"image\":\"i\",\"status\":1,\"serial_number\":\"SN\",\"name\":\"D\",\"model_id\":2,\"model_name\":\"M\",\"date_transfer\":123}]",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                "/v1/devices/7/info" -> respond(
                    content = "{\"id\":7,\"serial_number\":\"SN7\",\"model\":{\"id\":1,\"name\":\"M\"},\"version\":{\"id\":1,\"name\":\"V\"},\"status\":{\"id\":1,\"name\":\"S\"},\"device_model_version\":5,\"date_transfer\":\"d\",\"guarantee_period\":\"g\",\"side\":{\"id\":\"L\",\"name\":\"Left\"},\"fingers\":\"5\",\"size\":{\"id\":2,\"name\":\"L\"},\"options\":[]}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                "/v1/device-mobile-app/SERIAL-1" -> respond(
                    content = "{\"GAME_LAUNCH_RATE\":\"1\",\"MAXIMUM_POINTS\":\"2\",\"NUMBER_OF_CUPS\":\"3\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond("HTTP FAIL", HttpStatusCode.BadRequest)
            }
        }

        val api = Ubi4RequestsApi(userClient = okClient, passportClient = okClient)
        assertIs<NetworkResult.Success<*>>(api.getUserInfoV2(token = "t", lang = "ru"))
        assertIs<NetworkResult.Success<*>>(api.getDevicesList(userId = 42, token = "t", lang = "ru"))
        assertIs<NetworkResult.Success<*>>(api.getDeviceInfo(deviceId = 7, token = "t", lang = "ru"))
        assertIs<NetworkResult.Success<*>>(api.getProthesisSettings(deviceId = "SERIAL-1", token = "t"))

        // safePost http error branch
        val failedLogin = api.loginBySerial("api", SerialTokenRequest("SER", "PWD"))
        assertIs<NetworkResult.Error>(failedLogin)
        }
    }

    @Test
    fun `repository should handle token passport and archive flows`() {
        runBlocking {
        val zipBytes = buildZip(
            "checkpoint/file1.txt" to "one",
            "checkpoint/file2.txt" to "two"
        )

        val client = mockClient { req ->
            when {
                req.url.encodedPath.endsWith("/ser_n_token/") -> respond(
                    content = "{\"access_token\":\"jwt\",\"token_type\":\"bearer\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/get_passports_data/") -> respond(
                    content = "{\"content\":\"abc\",\"filename\":\"passport.yml\"}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                req.url.encodedPath.endsWith("/take_data/") -> respond(
                    content = zipBytes,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                )
                else -> respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            }
        }

        val repo = Ubi4TrainingRepository(Ubi4RequestsApi(userClient = client, passportClient = client))
        val tempDir = File.createTempFile("ubi4", "repo").apply {
            delete()
            mkdirs()
        }
        val root = sharedFile(tempDir.absolutePath)

        val token = repo.fetchTokenBySerial("api", "SER", "PWD")
        assertEquals("Bearer jwt", token)

        val passportFile = repo.fetchAndSavePassport("Bearer jwt", "SER", root)
        assertTrue(passportFile.exists())
        assertEquals("abc", String(passportFile.readBytes()))

        val (zipFile, unpacked) = repo.downloadAndUnpackCheckpoint("Bearer jwt", "cp1", root)
        assertTrue(zipFile.exists())
        assertTrue(unpacked.size >= 2)
        assertTrue(unpacked.all { it.exists() })
        }
    }

    @Test
    fun `repository should throw on error responses`() {
        runBlocking {
        val failClient = mockClient { req ->
            when {
                req.url.encodedPath.endsWith("/ser_n_token/") -> respond("fail", HttpStatusCode.BadRequest)
                req.url.encodedPath.endsWith("/get_passports_data/") -> respond("fail", HttpStatusCode.InternalServerError)
                req.url.encodedPath.endsWith("/take_data/") -> respond("fail", HttpStatusCode.BadRequest)
                else -> respond("fail", HttpStatusCode.BadRequest)
            }
        }
        val repo = Ubi4TrainingRepository(Ubi4RequestsApi(userClient = failClient, passportClient = failClient))
        val root = sharedFile(createTempDir(prefix = "ubi4-net").absolutePath)

        assertFailsWith<IOException> { repo.fetchTokenBySerial("k", "s", "p") }
        assertFailsWith<IOException> { repo.fetchAndSavePassport("t", "s", root) }
        assertFailsWith<IOException> { repo.downloadAndUnpackCheckpoint("t", "cp", root) }
        }
    }

    @Test
    fun `shared file operations should work for text bytes and channel`() {
        runBlocking {
        val dir = createTempDir(prefix = "ubi4-sf")
        val root = sharedFile(dir.absolutePath)
        val textFile = root.child("a.txt")
        val bytesFile = root.child("b.bin")
        val channelFile = root.child("c.bin")

        textFile.writeText("hello")
        bytesFile.writeBytes(byteArrayOf(1, 2, 3))
        channelFile.writeFromChannel(ByteReadChannel(byteArrayOf(4, 5, 6)))

        assertEquals("a.txt", textFile.name)
        assertTrue(textFile.path.endsWith("a.txt"))
        assertTrue(textFile.toFile().exists())
        assertEquals("hello", String(textFile.readBytes()))
        assertArrayEquals(byteArrayOf(1, 2, 3), bytesFile.readBytes())
        assertArrayEquals(byteArrayOf(4, 5, 6), channelFile.readBytes())

        val (zip, unpacked) = unzipArchive(makeZipFile(root, "demo.zip"), root.child("unzipped"))
        assertTrue(zip.exists())
        assertTrue(unpacked.isNotEmpty())
        }
    }

    @Test
    fun `upload training parser internals should parse progress and checkpoint`() {
        runBlocking {
        val linesMethod = uploadPlatformClass.getDeclaredMethod("parseEventBlock", List::class.java).apply {
            isAccessible = true
        }

        val progressOnly = linesMethod.invoke(null, listOf("data: 35%"))
        assertEquals(35, parseResultField(progressOnly, "progress"))
        assertEquals(null, parseResultField(progressOnly, "checkpoint"))

        val completeJson = linesMethod.invoke(
            null,
            listOf("event: complete", "data: {\"progress\":120,\"checkpoint\":\"cp_done\"}")
        )
        assertEquals(100, parseResultField(completeJson, "progress"))
        assertEquals("cp_done", parseResultField(completeJson, "checkpoint"))

        val fallbackJsonWithoutDataPrefix = linesMethod.invoke(
            null,
            listOf("{\"message\":\"checkpoint.final\"}")
        )
        assertEquals("checkpoint.final", parseResultField(fallbackJsonWithoutDataPrefix, "checkpoint"))

        val invalidJson = linesMethod.invoke(
            null,
            listOf("event: complete", "data: {broken-json")
        )
        assertEquals(null, parseResultField(invalidJson, "checkpoint"))
        }
    }

    @Test
    fun `collect sse checkpoint should stream progress and fail when checkpoint missing`() {
        runBlocking {
        val progress = mutableListOf<Int>()
        val sourceWithCheckpoint: BufferedSource = Buffer().writeUtf8(
            ": ping\r\n" +
                "data: 5%\r\n" +
                "\r\n" +
                "event: complete\r\n" +
                "data: {\"progress\":67,\"checkpoint\":\"checkpoint_67\"}\r\n" +
                "\r\n"
        )

        val checkpoint = invokeCollectSseCheckpoint(sourceWithCheckpoint) { progress += it }
        assertEquals("checkpoint_67", checkpoint)
        assertEquals(listOf(5, 67), progress)

        val sourceWithoutCheckpoint: BufferedSource = Buffer().writeUtf8(
            "data: 10%\n\n" +
                "data: 50%"
        )
        val thrown = try {
            invokeCollectSseCheckpoint(sourceWithoutCheckpoint) { }
            null
        } catch (t: Throwable) {
            t
        }
        assertIs<RuntimeException>(thrown)
        }
    }

    @Test
    fun `upload training internal should process multipart and sse from mock server`() {
        runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                // без завершающей пустой строки, чтобы покрыть EOF-ветку parseEventBlock
                .setBody(
                    "data: 12%\n\n" +
                        "event: complete\n" +
                        "data: {\"checkpoint\":\"cp_mock\",\"progress\":100}"
                )
        )
        server.start()

        try {
            val rootDir = sharedFile(createTempDir(prefix = "ubi4-upload").absolutePath)
            val left = rootDir.child("left.bin")
            val right = rootDir.child("right.bin")
            left.writeBytes(byteArrayOf(1, 2, 3))
            right.writeBytes(byteArrayOf(4, 5))

            val progress = mutableListOf<Int>()
            val checkpoint = uploadTrainingDataSsePlatformInternal(
                token = "Bearer token",
                serial = "SERIAL-42",
                pairs = listOf(left to right),
                onProgress = { progress += it },
                baseUrl = server.url("/").toString(),
                client = OkHttpClient()
            )

            assertEquals("cp_mock", checkpoint)
            assertTrue(progress.contains(12))

            val request = server.takeRequest()
            assertEquals("/passport_data/", request.path)
            assertEquals("Bearer token", request.getHeader("Authorization"))
            assertEquals("text/event-stream", request.getHeader("Accept"))
        } finally {
            server.shutdown()
        }
        }
    }

    @Test
    fun `upload training internal should throw on non success status`() {
        runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500).setBody("fail"))
        server.start()
        try {
            val rootDir = sharedFile(createTempDir(prefix = "ubi4-upload-fail").absolutePath)
            val left = rootDir.child("left.bin")
            val right = rootDir.child("right.bin")
            left.writeBytes(byteArrayOf(1))
            right.writeBytes(byteArrayOf(2))

            val thrown = try {
                uploadTrainingDataSsePlatformInternal(
                    token = "Bearer token",
                    serial = "SERIAL-42",
                    pairs = listOf(left to right),
                    onProgress = {},
                    baseUrl = server.url("/").toString(),
                    client = OkHttpClient()
                )
                null
            } catch (t: Throwable) {
                t
            }
            assertIs<IOException>(thrown)
        } finally {
            server.shutdown()
        }
        }
    }

    @Test
    fun `sse client factory should create http11 client`() {
        val client = createSseHttpClient()
        assertTrue(client.protocols.contains(okhttp3.Protocol.HTTP_1_1))
    }

    private fun makeZipFile(root: SharedFile, name: String): SharedFile = runBlocking {
        val zipBytes = buildZip("x.txt" to "X", "y/z.txt" to "YZ")
        val zip = root.child(name)
        zip.writeBytes(zipBytes)
        zip
    }

    private fun buildZip(vararg entries: Pair<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine { request ->
            handler(request)
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }

    private val uploadPlatformClass: Class<*> by lazy {
        Class.forName("com.bailout.stickk.ubi4.data.network.UploadTrainingPlatform_androidKt")
    }

    private fun parseResultField(result: Any?, field: String): Any? {
        requireNotNull(result)
        val f = result.javaClass.getDeclaredField(field).apply { isAccessible = true }
        return f.get(result)
    }

    private suspend fun invokeCollectSseCheckpoint(
        source: BufferedSource,
        onProgress: (Int) -> Unit
    ): String {
        val m = uploadPlatformClass.declaredMethods.first {
            it.name == "collectSseCheckpoint" && it.parameterTypes.size == 3
        }.apply { isAccessible = true }

        return suspendCoroutine { continuation ->
            try {
                val result = m.invoke(
                    null,
                    source,
                    onProgress,
                    object : Continuation<String> {
                        override val context = continuation.context
                        override fun resumeWith(result: Result<String>) {
                            result.fold(
                                onSuccess = { continuation.resume(it) },
                                onFailure = { continuation.resumeWithException(it) }
                            )
                        }
                    }
                )
                if (result !== COROUTINE_SUSPENDED) {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(result as String)
                }
            } catch (t: Throwable) {
                continuation.resumeWithException(t.cause ?: t)
            }
        }
    }

}
