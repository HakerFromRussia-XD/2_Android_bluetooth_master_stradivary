package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class LoadAccountProfileUseCaseV3Test {
    private val trace = mutableListOf<String>()
    private val remote = FakeAccountProfileRemote(trace)
    private val local = FakeAccountProfileLocal(trace)
    private val load = LoadAccountProfileUseCaseV3(remote, local)
    private fun session() = V3AccountProfileLoadSession(V3AccountProfileContext("FEST-test", "ru"))

    @Test fun `success updates profile before saving manager and loads first exact device after it`() = runTest {
        val events = mutableListOf<V3AccountProfileLoadEvent>()
        load(session()).collect { events.add(it); trace.add("event:${it.javaClass.simpleName}") }
        assertEquals(listOf("token:FEST-test", "event:Authorized", "user:token:ru", "event:ProfileLoaded",
            "save:MANAGER_NAME", "save:MANAGER_PHONE", "devices:7:token:ru", "info:9:token:ru"), trace.take(8))
        assertEquals(2, events.size)
        assertEquals("Manager", local.values[V3AccountDetail.MANAGER_NAME])
        assertEquals("-", local.values[V3AccountDetail.ROTATOR])
        assertEquals("", local.values[V3AccountDetail.ACCUMULATOR])
        assertEquals("", local.values[V3AccountDetail.TOUCHSCREEN_FINGERS])
        assertEquals(0, local.cacheWrites)
    }

    @ParameterizedTest @ValueSource(strings = ["", " ", "\t"])
    fun `missing serial never authenticates or requests a profile`(serial: String) = runTest {
        load(V3AccountProfileLoadSession(V3AccountProfileContext(serial, "en"))).toList()
        assertEquals(0, remote.tokenCalls)
        assertTrue(trace.none { it.startsWith("user:") || it.startsWith("devices:") || it.startsWith("info:") })
    }

    @Test fun `four HTTP 500 attempts exhaust one view and refresh does not reset its budget`() = runTest {
        remote.token = { V3AccountProfileResult.Error(500, "HTTP 500") }
        val session = session()
        val events = load(session).toList()
        assertEquals(4, remote.tokenCalls)
        assertEquals(4, events.count { it == V3AccountProfileLoadEvent.RefreshFinished })
        assertEquals(listOf(V3AccountProfileLoadEvent.ProfileUnavailable, V3AccountProfileLoadEvent.NoUserData), events.takeLast(2))
        assertEquals(V3AccountDetail.entries.toList(), local.writes.map { it.detail })
        assertTrue(local.writes.all { it.value.isEmpty() })
        load(session).toList()
        assertEquals(5, remote.tokenCalls)
        load(session()).toList()
        assertEquals(9, remote.tokenCalls)
    }

    @Test fun `token succeeds on fourth attempt and later profile errors never erase stored details`() = runTest {
        remote.token = { if (remote.tokenCalls < 4) V3AccountProfileResult.Error(500, "HTTP 500") else V3AccountProfileResult.Success("ok") }
        remote.user = { _, _ -> V3AccountProfileResult.Error(503, "unavailable") }
        local.values[V3AccountDetail.MODEL] = "Keep"
        val events = load(session()).toList()
        assertEquals(4, remote.tokenCalls)
        assertTrue(events.contains(V3AccountProfileLoadEvent.Authorized))
        assertEquals(V3AccountProfileLoadEvent.ServerError("unavailable"), events.last())
        assertEquals("Keep", local.values[V3AccountDetail.MODEL])
        assertTrue(local.writes.isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["token", "user", "devices", "info"])
    fun `network errors keep stage-specific stopping clearing and messages`(stage: String) = runTest {
        setError(stage, V3AccountProfileResult.Error(401, "Unauthorized"))
        val events = load(session()).toList()
        assertEquals(1, remote.tokenCalls)
        assertEquals(V3AccountProfileLoadEvent.ServerError("Unauthorized"), events.last())
        assertEquals(stage == "token", events.contains(V3AccountProfileLoadEvent.ProfileUnavailable))
        assertEquals(stage != "token", events.contains(V3AccountProfileLoadEvent.Authorized))
        if (stage == "user") assertTrue(local.writes.isEmpty())
        if (stage == "devices" || stage == "info") assertEquals(listOf(V3AccountDetail.MANAGER_NAME, V3AccountDetail.MANAGER_PHONE), local.writes.map { it.detail })
    }

    @ParameterizedTest @ValueSource(strings = ["token", "user", "devices", "info"])
    fun `cancellation-like error messages finish refresh without toast retry or clearing`(stage: String) = runTest {
        setError(stage, V3AccountProfileResult.Error(500, "Job was CANCELLED"))
        val events = load(session()).toList()
        assertEquals(V3AccountProfileLoadEvent.RefreshFinished, events.last())
        assertFalse(events.any { it is V3AccountProfileLoadEvent.ServerError || it == V3AccountProfileLoadEvent.NoUserData })
        assertEquals(1, remote.tokenCalls)
        assertTrue(local.writes.none { it.detail == V3AccountDetail.MODEL })
    }

    @Test fun `first matching serial with null id clears details and is not replaced by later duplicate`() = runTest {
        remote.devices = { _, _, _ -> V3AccountProfileResult.Success(listOf(V3AccountDevice(null, "FEST-test"), V3AccountDevice(77, "FEST-test"))) }
        local.values[V3AccountDetail.SIDE] = "Right"
        val events = load(session()).toList()
        assertTrue(trace.none { it.startsWith("info:") })
        assertTrue(events.contains(V3AccountProfileLoadEvent.NoUserData))
        assertEquals(V3AccountDetail.entries.toSet(), local.values.keys)
        assertTrue(local.values.values.all { it.isEmpty() })
    }

    @Test fun `unmatched serial clears stale device information and reports no user data`() = runTest {
        local.values[V3AccountDetail.MODEL] = "ПРО MANIFESTO-F"
        local.values[V3AccountDetail.SIDE] = "Right"
        local.values[V3AccountDetail.TOUCHSCREEN_FINGERS] = "Yes (pointing + middle)"
        remote.devices = { _, _, _ -> V3AccountProfileResult.Success(listOf(V3AccountDevice(88, "other"))) }
        val events = load(session()).toList()
        assertTrue(trace.none { it.startsWith("info:") })
        assertTrue(events.contains(V3AccountProfileLoadEvent.NoUserData))
        assertEquals(V3AccountDetail.entries.toSet(), local.values.keys)
        assertTrue(local.values.values.all { it.isEmpty() })
    }

    @Test fun `device saving preserves substring and rotator defaults while clearing missing optional values`() = runTest {
        local.values[V3AccountDetail.ACCUMULATOR] = "old battery"
        local.values[V3AccountDetail.TOUCHSCREEN_FINGERS] = "old pads"
        remote.info = { _, _, _ -> V3AccountProfileResult.Success(V3AccountDeviceInfo("ПР", "", "", "", "", "", listOf(
            V3AccountDeviceOption(3, null), V3AccountDeviceOption(3, "null"), V3AccountDeviceOption(3, "  "),
            V3AccountDeviceOption(3, "Rotator"), V3AccountDeviceOption(100, "ignored"),
        ))) }
        load(session()).toList()
        assertEquals("П", local.values[V3AccountDetail.MODEL])
        assertEquals(listOf("-", "-", "-", "Rotator"), local.writes.filter { it.detail == V3AccountDetail.ROTATOR }.map { it.value })
        assertEquals("", local.values[V3AccountDetail.ACCUMULATOR])
        assertEquals("", local.values[V3AccountDetail.TOUCHSCREEN_FINGERS])
        remote.info = { _, _, _ -> V3AccountProfileResult.Success(V3AccountDeviceInfo("prefix ПР model", "", "", "", "", "", listOf(
            V3AccountDeviceOption(15, null), V3AccountDeviceOption(5, ""),
        ))) }
        load(session()).toList()
        assertEquals("ПР model", local.values[V3AccountDetail.MODEL])
        assertEquals("", local.values[V3AccountDetail.ACCUMULATOR])
        assertEquals("", local.values[V3AccountDetail.TOUCHSCREEN_FINGERS])
        assertEquals("-", local.values[V3AccountDetail.ROTATOR])
    }

    @Test fun `FEST HM 13261 loads left hand by exact serial within client 16207 device list`() = runTest {
        remote.user = { _, _ -> V3AccountProfileResult.Success(V3AccountProfile("", "", 16207, "", "")) }
        remote.devices = { _, _, _ -> V3AccountProfileResult.Success(listOf(
            V3AccountDevice(9352, "FEST-H-08670"), V3AccountDevice(14063, "FEST-HM-13261"),
            V3AccountDevice(14402, "FEST-HM-13557"),
        )) }
        remote.info = { id, _, _ ->
            assertEquals(14063, id)
            V3AccountProfileResult.Success(V3AccountDeviceInfo("FEST.HS.30.000.000", "L", "Left", "In repair", "", "", listOf(
                V3AccountDeviceOption(3, "Quick-release wireless rotator 1.1"),
                V3AccountDeviceOption(15, "3450 mA*h (18650)"),
            )))
        }
        local.values[V3AccountDetail.SIDE] = "Right"
        local.values[V3AccountDetail.TOUCHSCREEN_FINGERS] = "Yes (pointing + middle)"

        load(V3AccountProfileLoadSession(V3AccountProfileContext("FEST-HM-13261", "en"))).toList()

        assertEquals("token:FEST-HM-13261", trace.first())
        assertTrue(trace.contains("devices:16207:token:en"))
        assertEquals(listOf("info:14063:token:en"), trace.filter { it.startsWith("info:") })
        assertEquals("Left", local.values[V3AccountDetail.SIDE])
        assertEquals("Quick-release wireless rotator 1.1", local.values[V3AccountDetail.ROTATOR])
        assertEquals("3450 mA*h (18650)", local.values[V3AccountDetail.ACCUMULATOR])
        assertEquals("", local.values[V3AccountDetail.TOUCHSCREEN_FINGERS])
    }

    @Test fun `overlapping refreshes retain current shared token for each following request`() = runTest {
        val firstUser = CompletableDeferred<V3AccountProfileResult<V3AccountProfile>>()
        remote.token = { V3AccountProfileResult.Success("token${remote.tokenCalls}") }
        remote.user = { token, _ -> if (token == "token1") firstUser.await() else V3AccountProfileResult.Success(V3AccountProfile("Second", "", 2, "", "")) }
        val session = session()
        val first = launch { load(session).toList() }; runCurrent()
        load(session).toList()
        firstUser.complete(V3AccountProfileResult.Success(V3AccountProfile("First", "", 1, "", "")))
        first.join()
        assertTrue(trace.contains("devices:2:token2:ru"))
        assertTrue(trace.contains("devices:1:token2:ru"))
        assertEquals(2, remote.tokenCalls)
    }

    @Test fun `view metadata never treats Bluetooth name as serial and retains language versions and cache`() {
        val get = GetAccountProfileViewDataUseCaseV3(local)
        val cached = V3AccountProfileHeader("Cached", "Name")
        local.storedHeader = cached
        local.currentEnvironment = local.currentEnvironment.copy(device = local.currentEnvironment.device.copy(name = "00001", language = "en"))
        val initial = get(V3AccountProfileContext())
        assertEquals(V3AccountProfileContext(), initial.context)
        assertEquals(V3AccountProfileVersions("1.23", "2.34", "3.45"), initial.versions)
        assertEquals(cached, initial.cachedHeader)
        local.currentEnvironment = local.currentEnvironment.copy(device = local.currentEnvironment.device.copy(name = "FEST-new", language = "ru-RU", type = "FEST-X", driverVersion = "custom"))
        val changed = get(initial.context)
        assertEquals(V3AccountProfileContext("", "ru"), changed.context)
        assertEquals("custom", changed.versions.driver)
        local.currentEnvironment = local.currentEnvironment.copy(device = local.currentEnvironment.device.copy(name = null, language = "en", driverVersion = null))
        assertEquals(changed.context, get(changed.context).context)
        assertEquals("0.01", get(changed.context).versions.driver)
        assertEquals("", get(V3AccountProfileContext("FEST-old", "ru")).context.serialNumber)
    }

    private fun setError(stage: String, error: V3AccountProfileResult.Error) {
        when (stage) {
            "token" -> remote.token = { error }
            "user" -> remote.user = { _, _ -> error }
            "devices" -> remote.devices = { _, _, _ -> error }
            "info" -> remote.info = { _, _, _ -> error }
        }
    }
}
