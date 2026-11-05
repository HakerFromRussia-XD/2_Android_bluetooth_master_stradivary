package com.bailout.stickk.ubi4.versions.v3.domain.games

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetGameAvailabilityUseCaseV3Test {
    @Test fun `every game uses its own OS installation and version`() {
        val repository = FakeV3GamesRepository()
        val get = GetGameAvailabilityUseCaseV3(repository)
        for (game in listOf(repository.game, repository.sushi.copy(versionCode = 10))) {
            for ((version, expected) in listOf(null to V3GameAction.INSTALL, 0L to V3GameAction.UPDATE,
                9L to V3GameAction.UPDATE, 10L to V3GameAction.PLAY, 11L to V3GameAction.PLAY)) {
                if (game.packageName == repository.packageName) repository.installed = version
                else if (version == null) repository.installedVersions.remove(game.packageName)
                else repository.installedVersions[game.packageName] = version
                val result = get(game)
                assertEquals(expected, result.action, "${game.packageName}: $version")
                assertEquals(version != null, result.isInstalled)
                assertEquals(game.packageName, repository.queriedPackages.last())
            }
        }
        assertEquals(0, repository.loads)
        assertEquals(0, repository.checks)
    }

    @Test fun `missing catalog does not make an absent game playable`() {
        val repository = FakeV3GamesRepository().apply { manifestUrl = ""; installed = 1 }
        val get = GetGameAvailabilityUseCaseV3(repository)
        assertEquals(V3GameAction.PLAY, get(repository.localGames.first()).action)
        assertEquals(V3GameAction.INSTALL, get(repository.sushi).action)
    }
}
