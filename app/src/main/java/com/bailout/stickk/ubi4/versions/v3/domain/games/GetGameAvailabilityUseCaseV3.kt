package com.bailout.stickk.ubi4.versions.v3.domain.games

class GetGameAvailabilityUseCaseV3(private val repository: V3GamesRepository) {
    operator fun invoke(remote: V3Game?): V3GameAvailability {
        val installed = repository.getInstalledVersionCode()
        val action = when {
            installed != null && (remote == null || installed >= remote.versionCode) -> V3GameAction.PLAY
            remote == null -> V3GameAction.UNAVAILABLE
            installed == null -> V3GameAction.INSTALL
            else -> V3GameAction.UPDATE
        }
        val packageName = repository.packageName
        return V3GameAvailability(
            remote ?: V3Game("", packageName, "$packageName.SuperTuxKartActivity", 0),
            action, installed != null, repository.manifestUrl.isNotBlank(),
        )
    }
}
