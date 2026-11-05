package com.bailout.stickk.ubi4.versions.v3.domain.games

class LoadGameManifestUseCaseV3(private val repository: V3GamesRepository) {
    val localGames: List<V3Game> get() = repository.localGames
    val hasManifestUrl: Boolean get() = repository.manifestUrl.isNotBlank()
    suspend operator fun invoke(): List<V3Game> = repository.loadGames()
}
