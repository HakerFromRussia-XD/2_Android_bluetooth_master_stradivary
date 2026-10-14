package com.bailout.stickk.ubi4.versions.v3.domain.games

interface V3GamesRepository {
    val packageName: String
    val manifestUrl: String
    fun getInstalledVersionCode(): Long?
    fun getInstalledVersionCode(packageName: String): Long?
    suspend fun loadGame(): V3Game
    suspend fun isPublishedInRuStore(packageName: String): Boolean
}
