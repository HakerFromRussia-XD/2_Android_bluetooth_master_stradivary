package com.bailout.stickk.ubi4.versions.v3.domain.games

interface V3GamesRepository {
    val localGames: List<V3Game>
    val manifestUrl: String
    fun getInstalledVersionCode(packageName: String): Long?
    suspend fun loadGames(): List<V3Game>
    suspend fun isPublishedInRuStore(packageName: String): Boolean
}
