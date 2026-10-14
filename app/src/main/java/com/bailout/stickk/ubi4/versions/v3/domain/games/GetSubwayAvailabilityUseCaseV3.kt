package com.bailout.stickk.ubi4.versions.v3.domain.games

/** Subway is launched from its local installation and has no remote catalog entry. */
class GetSubwayAvailabilityUseCaseV3(private val repository: V3GamesRepository) {
    operator fun invoke(): Boolean = repository.getInstalledVersionCode(GAME.packageName) != null

    companion object {
        val GAME = V3Game("Subway Surf", "com.motorica.games.subway", "com.unity3d.player.UnityPlayerActivity", 0)
    }
}
