package com.bailout.stickk.ubi4.data.games

import com.bailout.stickk.ubi4.versions.v3.domain.games.FakeV3GamesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.games.GetGameAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3Game
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3GameAction
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
    private val serverUrl = "https://firmware-ms.157.22.203.6.nip.io/games/manifest.json"
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

    @Test fun `direct manifest retains URL fields and expected package validation`() {
        responses.add(200 to catalog(packageName))
        val game = client.loadRemoteGames("https://example.invalid/catalog.json?x=1").single()
        assertEquals("https://example.invalid/catalog.json?x=1", requests.single().url.toString())
        assertEquals("GET", requests.single().method)
        assertTrue(requests.single().cacheControl.noCache)
        assertEquals(RemoteGame("stk", "Title", packageName, "$packageName.Main", "", 42), game)
        responses.add(200 to catalog("unexpected.package"))
        assertThrows(IllegalArgumentException::class.java) { client.loadRemoteGames("https://example.invalid/catalog.json") }
    }

    @Test fun `server catalog refresh detects Sushi update without a store URL or Yandex request`() {
        val repository = FakeV3GamesRepository().apply { installedVersions["com.unity.trashdash"] = 350 }
        val availability = GetGameAvailabilityUseCaseV3(repository)
        responses.add(200 to serverCatalog(sushiVersion = 350))
        responses.add(200 to serverCatalog(sushiVersion = 351))

        val actions = List(2) {
            val games = client.loadRemoteGames(serverUrl)
            assertEquals(listOf(packageName, "com.unity.trashdash"), games.map { it.packageName })
            assertEquals(1016, games.first().versionCode)
            val sushi = games.last()
            assertEquals("1.0", sushi.versionName)
            assertEquals(true, sushi.usesMotoricaControl)
            availability(V3Game(sushi.title, sushi.packageName, sushi.launcherActivity, sushi.versionCode)).action
        }

        assertEquals(listOf(V3GameAction.PLAY, V3GameAction.UPDATE), actions)
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.url.toString() == serverUrl && it.method == "GET" && it.cacheControl.noCache })
        assertEquals(0, repository.checks)
    }

    @Test fun `HTTP manifest failure retains localized error`() {
        responses.add(503 to "unavailable")
        val error = assertThrows(IllegalStateException::class.java) { client.loadRemoteGames("https://example.invalid/catalog.json") }
        assertEquals("Localized failure", error.message)
    }

    @Test fun `invalid catalog remains a parse error`() {
        responses.add(200 to "{}")
        val error = assertThrows(IllegalStateException::class.java) { client.loadRemoteGames("https://example.invalid/catalog.json") }
        assertEquals("Games are missing from games catalog", error.message)
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

    private fun serverCatalog(sushiVersion: Int) = """{"games":[
        {"id":"stk","title":"Super Tux Kart","packageName":"$packageName","launcherActivity":"$packageName.SuperTuxKartActivity","versionName":"1.0.16","versionCode":1016},
        {"id":"sushidash","title":"Sushi Dash","android":{"packageName":"com.unity.trashdash","launcherActivity":"com.unity3d.player.UnityPlayerGameActivity","versionName":"1.0","versionCode":$sushiVersion,"usesMotoricaControl":true},"ios":{"bundleId":"com.hakerfromrussia.trashdash","versionCode":42,"appStoreUrl":""}},
        {"id":"ios_only","android":null,"ios":{"bundleId":"example.ios.game"}}
    ]}"""
}
