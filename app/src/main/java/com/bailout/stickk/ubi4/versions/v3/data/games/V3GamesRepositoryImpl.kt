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
    override val packageName = BuildConfig.MOTORICA_STK_PACKAGE
    override val manifestUrl = BuildConfig.MOTORICA_GAMES_MANIFEST_URL
    private val client = GameCatalogClient(packageName, { this.context.getString(R.string.game_manifest_load_failed) })

    override fun getInstalledVersionCode() = context.installedGameVersionCode(packageName)
    override suspend fun loadGame(): V3Game = withContext(Dispatchers.IO) {
        val game = client.loadRemoteGame(manifestUrl)
        V3Game(game.title, game.packageName, game.launcherActivity, game.versionCode)
    }
    override suspend fun isPublishedInRuStore(packageName: String): Boolean = withContext(Dispatchers.IO) {
        client.isPublishedInRuStore(packageName)
    }
}
