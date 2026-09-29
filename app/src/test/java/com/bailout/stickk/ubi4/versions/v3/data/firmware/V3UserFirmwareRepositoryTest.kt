package com.bailout.stickk.ubi4.versions.v3.data.firmware

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.bailout.stickk.ubi4.firmware.user.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareStatus
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class V3UserFirmwareRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferences = mockk<SharedPreferences>(relaxed = true)
    private val connectivity = mockk<ConnectivityManager>(relaxed = true)
    private val engine = mockk<UserFirmwareUpdates>(relaxed = true)
    private val roleListener = slot<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val network = slot<ConnectivityManager.NetworkCallback>()
    private val stateListener = slot<(UserFirmwareUiState) -> Unit>()
    private val observation = Job()
    private val sessionFlags = mutableListOf<Boolean>()
    private var role = 2
    private var v3 = true
    private var resumeCount = 0
    private var engineCount = 0
    private var prepare: suspend () -> Boolean = { true }
    private lateinit var repository: V3UserFirmwareRepositoryImpl
    @TempDir lateinit var directory: Path

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { preferences.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, 2) } answers { role }
        every { preferences.registerOnSharedPreferenceChangeListener(capture(roleListener)) } just Runs
        every { connectivity.registerDefaultNetworkCallback(capture(network)) } just Runs
        every { engine.observe(capture(stateListener)) } returns observation
        repository = V3UserFirmwareRepositoryImpl(
            preferences, connectivity, directory.toString(), { prepare() }, sessionFlags::add,
            { resumeCount++ }, { v3 }, { path, _ ->
                assertEquals(directory.toString(), path)
                engineCount++
                engine
            },
        )
        repository.observe()
    }

    @AfterEach fun cleanup() {
        repository.close()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test fun `role and foreground keep the existing V3 user-only gate`() {
        verify(exactly = 1) { engine.setUserRole(true) }
        role = 1
        roleListener.captured.onSharedPreferenceChanged(preferences, PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED)
        verify(exactly = 1) { engine.setUserRole(false) }
        role = 2
        v3 = false
        repository.refreshEnvironment()
        verify(exactly = 2) { engine.setUserRole(false) }
        verify(exactly = 1) { engine.environmentChanged() }
        v3 = true
        repository.refreshEnvironment()
        verify(exactly = 2) { engine.setUserRole(true) }
        verify(exactly = 0) { engine.start() }
        repository.observe()
        assertEquals(1, engineCount)
    }

    @Test fun `completion resumes BLE once regardless of UI subscriptions and foreground`() = runTest(dispatcher) {
        for (phase in listOf("offered", "preparing", "updating", "verifying", "complete", "complete")) {
            stateListener.captured(UserFirmwareUiState(phase, 2, 3, 67))
        }
        assertEquals(listOf(false, true, true, true, false, false), sessionFlags)
        assertEquals(1, resumeCount)
        repeat(3) {
            assertEquals(V3UserFirmwareStatus("complete", 2, 3, 67, true), repository.observe().first())
            repository.refreshEnvironment()
        }
        assertEquals(1, resumeCount)
        verify(exactly = 0) { engine.start(); engine.acknowledge() }
    }

    @Test fun `actions delegate to shared sequencing and network only triggers an environment check`() {
        repository.startUpdate()
        repository.postponeUpdate()
        repository.acknowledgeCompletion()
        network.captured.onAvailable(mockk<Network>())
        dispatcher.scheduler.runCurrent()
        verifyOrder { engine.start(); engine.postpone(); engine.acknowledge(); engine.environmentChanged() }
        assertEquals(0, resumeCount)
    }

    @Test fun `close cancels transport and ignores late callbacks from the previous session`() {
        val ready = CompletableDeferred<Boolean>()
        prepare = { ready.await() }
        var callbackCount = 0
        repository.prepareTransfer { callbackCount++ }
        dispatcher.scheduler.runCurrent()
        val oldNetwork = network.captured
        val oldRole = roleListener.captured
        val oldState = stateListener.captured
        clearMocks(engine, answers = false)
        repository.close()
        repository.close()
        ready.complete(true)
        oldNetwork.onAvailable(mockk<Network>())
        oldRole.onSharedPreferenceChanged(preferences, PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED)
        oldState(UserFirmwareUiState("complete"))
        repository.startUpdate()
        repository.refreshEnvironment()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, callbackCount)
        assertEquals(0, resumeCount)
        assertEquals(listOf(false), sessionFlags)
        assertTrue(observation.isCancelled)
        verify(exactly = 1) { engine.close() }
        verify(exactly = 0) { engine.start(); engine.environmentChanged(); engine.setUserRole(any()) }
        verify { preferences.unregisterOnSharedPreferenceChangeListener(oldRole) }
        verify { connectivity.unregisterNetworkCallback(oldNetwork) }
    }

    @Test fun `archive reading preserves nested case-insensitive names and rejects ambiguous images`() = runTest(dispatcher) {
        suspend fun read(vararg entries: Pair<String, ByteArray>): UserFirmwareArchive? {
            val file = directory.resolve("firmware.zip").toFile()
            ZipOutputStream(file.outputStream()).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
            }
            val result = CompletableDeferred<UserFirmwareArchive?>()
            repository.read(file.path) { result.complete(it) }
            return result.await()
        }
        val payload = byteArrayOf(1, 2, 3)
        val archive = read("nested/fw_INI.INI" to "descriptor".encodeToByteArray(), "image.BIN" to payload)
        assertEquals("descriptor", archive?.descriptorText)
        assertArrayEquals(payload, archive?.payload)
        assertNull(read("FW_ini.ini" to byteArrayOf(), "a.bin" to payload, "b.bin" to payload))
        assertNull(read("a.bin" to payload))
    }
}
