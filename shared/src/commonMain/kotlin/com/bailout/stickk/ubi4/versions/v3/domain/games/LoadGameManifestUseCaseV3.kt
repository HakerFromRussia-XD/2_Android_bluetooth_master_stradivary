package com.bailout.stickk.ubi4.versions.v3.domain.games

class LoadGameManifestUseCaseV3(private val repository: V3GamesRepository) {
    suspend operator fun invoke(): V3Game = repository.loadGame()
}
