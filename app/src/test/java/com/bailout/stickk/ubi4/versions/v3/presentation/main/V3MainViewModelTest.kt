package com.bailout.stickk.ubi4.versions.v3.presentation.main

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.main.*
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceIdentity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val updates = MutableSharedFlow<V3MainUpdate>(extraBufferCapacity = 8)
    private var displays = mutableSetOf(0, 1, 2)
    private var reads = 0
    private var subscriptions = 0
    private var connectedName = "V3-00001"
    private val identity = MutableStateFlow<V3DeviceIdentity?>(null)
    private val profileWrites = mutableListOf<String>()
    private val uploads = mutableListOf<String>()
    private var uploadFailure: RuntimeException? = null
    private val connectionOperations = mutableListOf<String>()
    private val repository: V3MainRepository = object : V3MainRepository {
        override fun rememberLastConnection() {
            assertEquals(identity.value, vm.uiState.value.deviceIdentity)
            connectionOperations += "remember"
        }
        override fun restoreSavedConnection() { connectionOperations += "restore" }
        override fun resetLastConnection() { connectionOperations += "reset" }
        override fun initializeWidgetStorage() { connectionOperations += "storage" }
        override fun cancelAppCloseProfileUpload() { uploads += "cancel" }
        override fun enqueueAppCloseProfileUpload(language: String) {
            uploads += "enqueue:$language"
            uploadFailure?.let { throw it }
        }
        override fun loadDeviceIdentity() = V3DeviceIdentity(connectedName, null, connectedName.removePrefix("V3-")).also { identity.value = it }
        override fun applySerialNumber(serial: String) = V3DeviceIdentity(serial, serial, serial).also {
            profileWrites += serial; identity.value = it
        }
        override fun observeDeviceIdentity() = identity
        override fun getVisibleDisplays(): Set<Int> { reads++; return displays }
        override fun observeUpdates() = flow {
            subscriptions++
            try { emitAll(updates) } finally { subscriptions-- }
        }
    }
    private val vm = V3MainViewModel(
        GetMainVisibleDisplaysUseCaseV3(repository), ObserveMainUpdatesUseCaseV3(repository),
        UpdateMainDeviceIdentityUseCaseV3(repository),
        ScheduleProfileUploadUseCaseV3(repository),
        ManageMainConnectionUseCaseV3(repository),
    )
    private val store = ViewModelStore()
    @BeforeEach fun setup() { Dispatchers.setMain(dispatcher); store.put("main", vm) }
    @AfterEach fun cleanup() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun send(update: V3MainUpdate) { assertTrue(updates.tryEmit(update)); dispatcher.scheduler.runCurrent() }
    private fun create() { vm.onAction(V3MainAction.ViewCreated); dispatcher.scheduler.runCurrent() }

    @Test fun `connection operations are synchronous and rendering never repeats them`() {
        vm.onAction(V3MainAction.DeviceConnected)
        assertEquals(listOf("remember"), connectionOperations)
        vm.onAction(V3MainAction.WidgetStorageInitializationRequested)
        assertEquals(listOf("remember", "storage"), connectionOperations)
        create()
        vm.onAction(V3MainAction.ViewResumed)
        assertEquals(2, connectionOperations.size)
        vm.onAction(V3MainAction.SavedConnectionRestoreRequested)
        assertEquals("restore", connectionOperations.last())
        send(V3MainUpdate.WidgetsChanged)
        send(V3MainUpdate.BatteryChanged(80))
        vm.onAction(V3MainAction.ViewDestroyed)
        create()
        vm.onAction(V3MainAction.LastConnectionResetRequested)
        assertEquals(listOf("remember", "storage", "restore", "reset"), connectionOperations)
        store.clear()
        vm.onAction(V3MainAction.DeviceConnected)
        vm.onAction(V3MainAction.WidgetStorageInitializationRequested)
        vm.onAction(V3MainAction.SavedConnectionRestoreRequested)
        vm.onAction(V3MainAction.LastConnectionResetRequested)
        assertEquals(4, connectionOperations.size)
    }

    @Test fun `stop then destroy schedules once and resume cancels synchronously before next stop`() {
        create()
        vm.onAction(V3MainAction.ViewResumed)
        val stop = V3MainAction.ViewStopped(false, true, "ru_RU")
        vm.onAction(stop)
        vm.onAction(V3MainAction.ViewDestroyed)
        vm.onAction(stop) // Activity's onDestroy fallback, after detaching observations.
        assertEquals(listOf("cancel", "enqueue:ru_RU"), uploads)
        create() // Same ViewModelStore after recreation.
        vm.onAction(V3MainAction.ViewResumed)
        vm.onAction(stop)
        assertEquals(listOf("cancel", "enqueue:ru_RU", "cancel", "enqueue:ru_RU"), uploads)
    }

    @Test fun `disconnected and scan transitions do not consume upload request`() {
        create()
        vm.onAction(V3MainAction.ViewStopped(false, false, "ru"))
        vm.onAction(V3MainAction.ViewStopped(true, true, "ru"))
        vm.onAction(V3MainAction.ViewStopped(true, false, "ru"))
        assertTrue(uploads.isEmpty())
        vm.onAction(V3MainAction.ViewDestroyed)
        vm.onAction(V3MainAction.ViewStopped(false, true, "ru"))
        assertEquals(listOf("enqueue:ru"), uploads)
    }

    @Test fun `blank language falls back to English but nonblank language is not trimmed`() {
        for ((language, expected) in listOf("" to "en", " \t " to "en", " ru " to " ru ")) {
            vm.onAction(V3MainAction.ViewResumed)
            vm.onAction(V3MainAction.ViewStopped(false, true, language))
            assertEquals("enqueue:$expected", uploads.last())
        }
    }

    @Test fun `scheduler failure keeps request guard until resume just like activity`() {
        create()
        val stop = V3MainAction.ViewStopped(false, true, "en")
        uploadFailure = IllegalStateException("scheduler unavailable")
        assertThrows(IllegalStateException::class.java) { vm.onAction(stop) }
        uploadFailure = null
        vm.onAction(stop)
        assertEquals(listOf("enqueue:en"), uploads)
        vm.onAction(V3MainAction.ViewResumed)
        vm.onAction(stop)
        assertEquals(listOf("enqueue:en", "cancel", "enqueue:en"), uploads)
    }

    @Test fun `navigation and rendering never schedule upload and cleared viewmodel ignores lifecycle callbacks`() {
        create()
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        send(V3MainUpdate.BatteryChanged(45))
        assertTrue(uploads.isEmpty())
        store.clear()
        vm.onAction(V3MainAction.ViewResumed)
        vm.onAction(V3MainAction.ViewStopped(false, true, "en"))
        assertTrue(uploads.isEmpty())
    }

    @Test fun `connection initializes serial before binding without filling device name or writing profiles`() {
        vm.onAction(V3MainAction.DeviceConnected)
        val identity = V3DeviceIdentity("V3-00001", null, "00001")
        assertEquals(identity, vm.uiState.value.deviceIdentity)
        create()
        send(V3MainUpdate.BatteryChanged(37))
        send(V3MainUpdate.WidgetsChanged)
        assertEquals(identity, vm.uiState.value.deviceIdentity)
        assertEquals("V3-00001", connectedName)
        assertTrue(profileWrites.isEmpty())
    }

    @Test fun `identity updates preserve navigation and refreshing does not write profiles`() {
        vm.onAction(V3MainAction.DeviceConnected)
        create()
        val revision = vm.uiState.value.navigationRevision
        identity.value = V3DeviceIdentity(" V3-custom ", " V3-custom ", "custom")
        dispatcher.scheduler.runCurrent()
        assertEquals(identity.value, vm.uiState.value.deviceIdentity)
        assertTrue(profileWrites.isEmpty())
        assertEquals(revision, vm.uiState.value.navigationRevision)
        val state = vm.uiState.value
        // Rendering/refreshing the same state must not replay a rename.
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        create()
        assertEquals(state.deviceIdentity, vm.uiState.value.deviceIdentity)
        assertTrue(profileWrites.isEmpty())
    }

    @Test fun `device info serial updates profiles and public values without overwriting connection name`() {
        vm.onAction(V3MainAction.DeviceConnected)
        vm.onAction(V3MainAction.SerialNumberReceived("CPU-000001"))
        assertEquals(V3DeviceIdentity("CPU-000001", "CPU-000001", "CPU-000001"), vm.uiState.value.deviceIdentity)
        assertEquals(listOf("CPU-000001"), profileWrites)
        assertEquals("V3-00001", connectedName)
        connectedName = "V3-00002"
        vm.onAction(V3MainAction.DeviceConnected)
        create()
        assertEquals(V3DeviceIdentity("V3-00002", null, "00002"), vm.uiState.value.deviceIdentity)
        assertEquals(1, profileWrites.size)
    }

    @Test fun `cleared viewmodel ignores identity callbacks and storage writes`() {
        vm.onAction(V3MainAction.DeviceConnected)
        store.clear()
        val state = vm.uiState.value
        identity.value = V3DeviceIdentity("V3-other", "V3-other", "other")
        vm.onAction(V3MainAction.SerialNumberReceived("CPU-123456"))
        connectedName = "V3-new"
        vm.onAction(V3MainAction.DeviceConnected)
        assertEquals(state, vm.uiState.value)
        assertTrue(profileWrites.isEmpty())
    }

    @Test fun `initial navigation is synchronous and state does not retain mutable storage`() {
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        assertEquals(0, reads)
        vm.onAction(V3MainAction.ViewCreated)
        assertEquals(setOf(0, 1, 2), vm.uiState.value.visibleDisplays)
        assertNull(vm.uiState.value.batteryPercent)
        displays.clear()
        assertEquals(setOf(0, 1, 2), vm.uiState.value.visibleDisplays)
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        assertTrue(vm.uiState.value.visibleDisplays.isEmpty())
    }

    @Test fun `battery updates preserve navigation and raw percentages without extra storage reads`() {
        create()
        val revision = vm.uiState.value.navigationRevision
        for (percent in listOf(19, 20, 40, 41, -1, 110)) {
            send(V3MainUpdate.BatteryChanged(percent))
            assertEquals(percent, vm.uiState.value.batteryPercent)
            assertEquals(revision, vm.uiState.value.navigationRevision)
            assertEquals(setOf(0, 1, 2), vm.uiState.value.visibleDisplays)
        }
        assertEquals(1, reads)
    }

    @Test fun `widget updates refresh current displays and repeated refreshes still request navigation rendering`() {
        create()
        send(V3MainUpdate.BatteryChanged(35))
        displays = mutableSetOf(1, 4)
        send(V3MainUpdate.WidgetsChanged)
        assertEquals(setOf(1, 4), vm.uiState.value.visibleDisplays)
        val revision = vm.uiState.value.navigationRevision
        send(V3MainUpdate.WidgetsChanged)
        assertEquals(revision + 1, vm.uiState.value.navigationRevision)
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        assertEquals(revision + 2, vm.uiState.value.navigationRevision)
        assertEquals(35, vm.uiState.value.batteryPercent)
    }

    @Test fun `destroy cancels observations and recreate has one subscription while clear prevents restart`() {
        create(); create()
        assertEquals(1, subscriptions)
        vm.onAction(V3MainAction.ViewDestroyed)
        dispatcher.scheduler.runCurrent()
        val before = vm.uiState.value
        val previousReads = reads
        send(V3MainUpdate.BatteryChanged(12))
        send(V3MainUpdate.WidgetsChanged)
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        assertEquals(before, vm.uiState.value)
        assertEquals(previousReads, reads)
        assertEquals(0, subscriptions)
        create()
        assertEquals(1, subscriptions)
        assertNull(vm.uiState.value.batteryPercent)
        store.clear(); dispatcher.scheduler.runCurrent()
        val finalReads = reads
        vm.onAction(V3MainAction.ViewCreated)
        assertEquals(finalReads, reads)
        assertEquals(0, subscriptions)
    }
}
