package com.bailout.stickk.ubi4.ui.fragments.account.games

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameCatalogTest {
    private val packageName = "com.motorica.games.stk"

    @Test
    fun `not installed game from RuStore can be installed`() {
        assertEquals(GameAction.INSTALL, GameCatalog.action(remoteGame(), null))
    }

    @Test
    fun `older installed game can be updated`() {
        assertEquals(GameAction.UPDATE, GameCatalog.action(remoteGame(versionCode = 1014), 1013))
    }

    @Test
    fun `current or newer installed game can be played`() {
        assertEquals(GameAction.PLAY, GameCatalog.action(remoteGame(versionCode = 1014), 1014))
        assertEquals(GameAction.PLAY, GameCatalog.action(remoteGame(versionCode = 1014), 1015))
    }

    @Test
    fun `installed game can be played when catalog is unavailable`() {
        assertEquals(GameAction.PLAY, GameCatalog.action(null, 1014))
    }

    @Test
    fun `missing game and unavailable catalog produces unavailable state`() {
        assertEquals(GameAction.UNAVAILABLE, GameCatalog.action(null, null))
    }

    @Test
    fun `parser accepts catalog without legacy apk fields`() {
        val parsed = GameCatalog.parseStk(
            """
            {
              "games": [{
                "id": "stk",
                "title": "Super Tux Kart",
                "android": {
                  "packageName": "$packageName",
                  "launcherActivity": "$packageName.SuperTuxKartActivity",
                  "versionName": "1.0.14",
                  "versionCode": 1014
                }
              }]
            }
            """.trimIndent(),
            packageName
        )

        assertEquals(1014, parsed.versionCode)
    }

    @Test
    fun `parser rejects another package`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameCatalog.parseStk(catalog(packageName = "com.example.untrusted"), packageName)
        }
    }

    private fun catalog(packageName: String = this.packageName): String =
        """
        {
          "games": [{
            "id": "stk",
            "android": {
              "packageName": "$packageName",
              "launcherActivity": "$packageName.SuperTuxKartActivity",
              "versionName": "1.0.14",
              "versionCode": 1014,
              "apkUrl": "https://example.invalid/legacy.apk",
              "sha256": "legacy-field-is-ignored"
            }
          }]
        }
        """.trimIndent()

    private fun remoteGame(versionCode: Long = 1014): RemoteGame =
        RemoteGame(
            id = RemoteGame.STK_ID,
            title = "Super Tux Kart",
            packageName = packageName,
            launcherActivity = "$packageName.SuperTuxKartActivity",
            versionName = "1.0.14",
            versionCode = versionCode
        )
}
