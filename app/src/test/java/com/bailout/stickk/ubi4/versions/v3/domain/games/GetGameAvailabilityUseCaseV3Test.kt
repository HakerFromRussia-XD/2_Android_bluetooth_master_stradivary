package com.bailout.stickk.ubi4.versions.v3.domain.games

import com.bailout.stickk.ubi4.data.games.GameCatalog
import com.bailout.stickk.ubi4.data.games.RemoteGame
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetGameAvailabilityUseCaseV3Test {
    @Test fun `decision matrix matches unchanged UBI4 rules including offline and newer installed versions`() {
        val repository = FakeV3GamesRepository()
        val get = GetGameAvailabilityUseCaseV3(repository)
        for (installed in listOf(null, 0L, 9L, 10L, 11L)) {
            for (remote in listOf(null, repository.game)) {
                repository.installed = installed
                val result = get(remote)
                val original = remote?.let { RemoteGame("stk", it.title, it.packageName, it.launcherActivity, "", it.versionCode) }
                assertEquals(GameCatalog.action(original, installed).name, result.action.name)
                assertEquals(installed != null, result.isInstalled)
                assertTrue(result.hasManifestUrl)
            }
        }
        assertEquals(0, repository.loads)
        assertEquals(0, repository.checks)
    }

    @Test fun `fallback uses expected package and launcher without inventing title or version`() {
        val repository = FakeV3GamesRepository().apply { manifestUrl = "  " }
        val result = GetGameAvailabilityUseCaseV3(repository)(null)
        assertEquals(V3Game("", repository.packageName, "${repository.packageName}.SuperTuxKartActivity", 0), result.game)
        assertFalse(result.hasManifestUrl)
    }
}
