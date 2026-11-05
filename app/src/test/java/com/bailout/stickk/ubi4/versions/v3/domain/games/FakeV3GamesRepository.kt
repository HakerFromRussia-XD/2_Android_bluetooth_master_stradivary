package com.bailout.stickk.ubi4.versions.v3.domain.games

class FakeV3GamesRepository : V3GamesRepository {
    val packageName = "com.motorica.games.stk"
    override var manifestUrl = "https://example.invalid/catalog.json"
    var installed: Long? = null
    var subwayInstalled: Long? = null
    val installedVersions = mutableMapOf<String, Long>()
    val queriedPackages = mutableListOf<String>()
    var loads = 0
    var checks = 0
    val game = V3Game("Game", packageName, "$packageName.CustomActivity", 10)
    val sushi = V3Game("Sushi Dash", "com.unity.trashdash", "com.unity3d.player.UnityPlayerGameActivity", 0, true)
    override val localGames get() = listOf(game.copy(launcherActivity = "$packageName.SuperTuxKartActivity", versionCode = 0), sushi)
    var load: suspend () -> List<V3Game> = { listOf(game) }
    var check: suspend (String) -> Boolean = { true }
    override fun getInstalledVersionCode(packageName: String): Long? {
        queriedPackages += packageName
        return when (packageName) {
            this.packageName -> installed
            "com.motorica.games.subway" -> subwayInstalled
            else -> installedVersions[packageName]
        }
    }
    override suspend fun loadGames(): List<V3Game> { loads++; return load() }
    override suspend fun isPublishedInRuStore(packageName: String): Boolean { checks++; return check(packageName) }
}
