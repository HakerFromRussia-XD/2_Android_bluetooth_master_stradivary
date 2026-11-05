package com.bailout.stickk.ubi4.versions.v3.domain.games

class GetGameAvailabilityUseCaseV3(private val repository: V3GamesRepository) {
    operator fun invoke(game: V3Game): V3GameAvailability {
        val installed = repository.getInstalledVersionCode(game.packageName)
        val action = when {
            installed == null -> V3GameAction.INSTALL
            installed < game.versionCode -> V3GameAction.UPDATE
            else -> V3GameAction.PLAY
        }
        return V3GameAvailability(game, action, installed != null)
    }
}
