package com.bailout.stickk.ubi4.data.games

import android.net.Uri
import io.mockk.*
import okhttp3.Call
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class GameCatalogClientTest {
    private val packageName = "com.motorica.games.stk"
    private val requests = mutableListOf<Request>()
    private val responses = ArrayDeque<Pair<Int, String>>()
    private val calls = Call.Factory { request ->
        requests.add(request)
        val (code, body) = responses.removeFirst()
        mockk<Call>().also { call ->
            every { call.execute() } returns Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(code).message("response").body(body.toResponseBody()).build()
        }
    }
    private val client = GameCatalogClient(packageName, { "Localized failure" }, calls)
    @BeforeEach fun setUp() {
        mockkStatic(Uri::class)
        val uri = mockk<Uri>()
        every { Uri.parse(any()) } returns uri
        every { uri.host } returns "example.invalid"
    }
    @AfterEach fun tearDown() { unmockkStatic(Uri::class) }

    @Test fun `direct manifest retains URL fields and expected package validation`() {
        responses.add(200 to catalog(packageName))
        val game = client.loadRemoteGame("https://example.invalid/catalog.json?x=1")
        assertEquals("https://example.invalid/catalog.json?x=1", requests.single().url.toString())
        assertEquals("GET", requests.single().method)
        assertEquals(RemoteGame("stk", "Title", packageName, "$packageName.Main", "", 42), game)
        responses.add(200 to catalog("unexpected.package"))
        assertThrows(IllegalArgumentException::class.java) { client.loadRemoteGame("https://example.invalid/catalog.json") }
    }

    @Test fun `HTTP manifest failure retains localized error`() {
        responses.add(503 to "unavailable")
        val error = assertThrows(IllegalStateException::class.java) { client.loadRemoteGame("https://example.invalid/catalog.json") }
        assertEquals("Localized failure", error.message)
    }

    @Test fun `invalid catalog remains a parse error`() {
        responses.add(200 to "{}")
        val error = assertThrows(IllegalStateException::class.java) { client.loadRemoteGame("https://example.invalid/catalog.json") }
        assertEquals("STK is missing from games catalog", error.message)
    }

    @Test fun `RuStore uses unchanged GET URL and distinguishes success not found and other errors`() {
        responses.add(200 to "ok"); responses.add(404 to "absent"); responses.add(502 to "bad gateway")
        assertTrue(client.isPublishedInRuStore(packageName))
        assertFalse(client.isPublishedInRuStore(packageName))
        val error = assertThrows(IllegalStateException::class.java) { client.isPublishedInRuStore(packageName) }
        assertEquals("RuStore returned HTTP 502", error.message)
        assertTrue(requests.all { it.url.toString() == "https://www.rustore.ru/catalog/app/$packageName" && it.method == "GET" })
    }

    private fun catalog(pkg: String) = """{"games":[{"id":"stk","title":"Title","android":{"packageName":"$pkg","launcherActivity":"$pkg.Main","versionCode":42}}]}"""
}
