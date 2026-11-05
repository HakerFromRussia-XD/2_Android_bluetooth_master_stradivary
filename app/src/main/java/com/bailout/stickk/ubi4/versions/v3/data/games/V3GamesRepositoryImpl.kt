package com.bailout.stickk.ubi4.versions.v3.data.games

import android.content.Context
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.games.GameCatalogClient
import com.bailout.stickk.ubi4.data.games.installedGameVersionCode
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3Game
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3GamesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class V3GamesRepositoryImpl(context: Context) : V3GamesRepository {
    private val context = context.applicationContext
    override val manifestUrl = BuildConfig.MOTORICA_GAMES_MANIFEST_URL
    override val localGames = listOf(
        V3Game(this.context.getString(R.string.motorica_stk_title), BuildConfig.MOTORICA_STK_PACKAGE,
            "${BuildConfig.MOTORICA_STK_PACKAGE}.SuperTuxKartActivity", 0),
        V3Game(this.context.getString(R.string.motorica_sushi_title), "com.unity.trashdash",
            "com.unity3d.player.UnityPlayerGameActivity", 0, usesMotoricaControl = true),
    )
    private val client = GameCatalogClient(BuildConfig.MOTORICA_STK_PACKAGE,
        { this.context.getString(R.string.game_manifest_load_failed) })

    override fun getInstalledVersionCode(packageName: String) = context.installedGameVersionCode(packageName)
    override suspend fun loadGames(): List<V3Game> = withContext(Dispatchers.IO) {
        val localByPackage = localGames.associateBy { it.packageName }
        client.loadRemoteGames(manifestUrl).map { game ->
            val local = localByPackage[game.packageName]
            V3Game(game.title.ifBlank { local?.title ?: game.packageName }, game.packageName,
                game.launcherActivity, game.versionCode, game.usesMotoricaControl ?: local?.usesMotoricaControl ?: false)
        }
    }
    override suspend fun isPublishedInRuStore(packageName: String): Boolean = withContext(Dispatchers.IO) {
        client.isPublishedInRuStore(packageName)
    }
}
