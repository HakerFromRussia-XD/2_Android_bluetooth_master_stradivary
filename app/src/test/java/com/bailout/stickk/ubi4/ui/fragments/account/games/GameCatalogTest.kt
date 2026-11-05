package com.bailout.stickk.ubi4.ui.fragments.account.games

import com.bailout.stickk.ubi4.data.games.GameCatalog
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameCatalogTest {
    private val packageName = "com.motorica.games.stk"

    @Test fun `parser loads all Android games and skips iOS-only entries`() {
        val parsed = GameCatalog.parseGames("""{"games":[
            {"id":"stk","title":"Fluxara Drift","android":{"packageName":"$packageName","launcherActivity":"$packageName.SuperTuxKartActivity","versionCode":1014}},
            {"id":"sushi_dash","title":"Sushi Dash","android":{"packageName":"com.unity.trashdash","launcherActivity":"com.unity3d.player.UnityPlayerGameActivity","versionCode":15,"usesMotoricaControl":true}},
            {"id":"ios_only","android":null,"ios":{"bundleId":"example.game"}},
            {"id":"ios_only_missing_android","ios":{"bundleId":"example.other"}}
        ]}""", packageName)
        assertEquals(listOf(packageName, "com.unity.trashdash"), parsed.map { it.packageName })
        assertEquals(1014, parsed.first().versionCode)
        assertEquals(true, parsed.last().usesMotoricaControl)
    }

    @Test fun `parser retains STK package validation and rejects duplicate or invalid package names`() {
        for (catalog in listOf(
            """{"games":[${game("stk", "com.example.untrusted")}]}""",
            """{"games":[${game("a", "example.game")},${game("b", "example.game")}]}""",
            """{"games":[${game("a", "example/game")}]}""",
        )) assertThrows(IllegalArgumentException::class.java) { GameCatalog.parseGames(catalog, packageName) }
    }

    @Test fun `parser accepts 200 entries with the same schema`() {
        val catalog = (1..200).joinToString(prefix = "{\"games\":[", postfix = "]}") { game("game$it", "example.game$it") }
        assertEquals(200, GameCatalog.parseGames(catalog, packageName).size)
    }

    private fun game(id: String, pkg: String) =
        """{"id":"$id","android":{"packageName":"$pkg","launcherActivity":"$pkg.Main","versionCode":1}}"""
}
