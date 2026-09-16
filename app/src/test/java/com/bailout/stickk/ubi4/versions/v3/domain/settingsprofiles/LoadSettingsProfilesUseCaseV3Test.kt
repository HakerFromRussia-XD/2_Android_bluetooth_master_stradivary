package com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoadSettingsProfilesUseCaseV3Test {
    private val repository = Repository()
    private val load = LoadSettingsProfilesUseCaseV3(GetSettingsProfilesUseCaseV3(repository), repository)

    @Test fun `loads and caches normalized selection without applying a profile`() = runTest {
        val result = load("first")
        assertEquals(listOf(1, 3), result.profiles.map { it.profileId })
        assertEquals(3, result.activeProfileId)
        assertEquals(listOf("first" to result), repository.cached)
    }

    @Test fun `read from the previous device cannot replace the current selection cache`() = runTest {
        repository.read = { repository.serial = "second"; repository.profiles }
        load("first")
        assertTrue(repository.cached.isEmpty())
    }

    @Test fun `cancelled screen load cannot cache a late noncancellable database result`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<V3SettingsProfiles>()
        repository.read = {
            entered.complete(Unit)
            withContext(NonCancellable) { response.await() }
        }
        val job = launch { load("first") }
        entered.await()
        job.cancel()
        response.complete(repository.profiles)
        job.join()
        assertTrue(repository.cached.isEmpty())
    }

    private class Repository : V3SettingsProfilesRepository {
        override val updates = emptyFlow<Unit>()
        var serial = "first"
        val profiles = V3SettingsProfiles(listOf(V3SettingsProfile(3, "Sport"), V3SettingsProfile(1, null)), 3)
        var read: suspend () -> V3SettingsProfiles = { profiles }
        val cached = mutableListOf<Pair<String, V3SettingsProfiles>>()
        override fun currentSerial() = serial
        override suspend fun getProfiles(serial: String) = read()
        override fun cacheSelection(serial: String, profiles: V3SettingsProfiles) { cached += serial to profiles }
        override suspend fun selectProfile(serial: String, profileId: Int) = error("Loading must not apply a profile")
        override suspend fun createProfile(serial: String) = error("Loading must not create a profile")
        override suspend fun renameProfile(serial: String, profileId: Int, name: String) = error("Loading must not rename a profile")
    }
}
