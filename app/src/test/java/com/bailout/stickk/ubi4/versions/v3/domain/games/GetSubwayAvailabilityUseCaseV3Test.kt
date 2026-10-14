package com.bailout.stickk.ubi4.versions.v3.domain.games

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetSubwayAvailabilityUseCaseV3Test {
    @Test fun `Subway installation is independent of STK and needs no catalog or store`() {
        val repository = FakeV3GamesRepository().apply { installed = 123; manifestUrl = "" }
        val get = GetSubwayAvailabilityUseCaseV3(repository)
        assertFalse(get())
        repository.subwayInstalled = 0
        assertTrue(get())
        repository.installed = null
        assertTrue(get())
        repository.subwayInstalled = null
        assertFalse(get())
        assertEquals(List(4) { "com.motorica.games.subway" }, repository.queriedPackages)
        assertEquals(0, repository.loads)
        assertEquals(0, repository.checks)
    }

    @Test fun `local launch metadata targets the installed Unity game`() {
        assertEquals(V3Game("Subway Surf", "com.motorica.games.subway", "com.unity3d.player.UnityPlayerActivity", 0),
            GetSubwayAvailabilityUseCaseV3.GAME)
    }
}
