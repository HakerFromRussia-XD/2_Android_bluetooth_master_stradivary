package com.bailout.stickk.ubi4.versions.v3.domain.games

class FakeV3GamesRepository : V3GamesRepository {
    override val packageName = "com.motorica.games.stk"
    override var manifestUrl = "https://example.invalid/catalog.json"
    var installed: Long? = null
    var subwayInstalled: Long? = null
    val queriedPackages = mutableListOf<String>()
    var loads = 0
    var checks = 0
    val game = V3Game("Game", packageName, "$packageName.CustomActivity", 10)
    var load: suspend () -> V3Game = { game }
    var check: suspend (String) -> Boolean = { true }
    override fun getInstalledVersionCode() = installed
    override fun getInstalledVersionCode(packageName: String): Long? {
        queriedPackages += packageName
        return when (packageName) {
            this.packageName -> installed
            "com.motorica.games.subway" -> subwayInstalled
            else -> null
        }
    }
    override suspend fun loadGame(): V3Game { loads++; return load() }
    override suspend fun isPublishedInRuStore(packageName: String): Boolean { checks++; return check(packageName) }
}
