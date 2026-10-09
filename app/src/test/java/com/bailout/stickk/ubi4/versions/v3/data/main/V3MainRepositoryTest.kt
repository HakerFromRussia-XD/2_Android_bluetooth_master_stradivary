package com.bailout.stickk.ubi4.versions.v3.data.main

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.data.network.SettingsProfileUploadWorkScheduler
import io.mockk.*
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterWidgetCode
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainUpdate
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainDisplaysRepository
import com.bailout.stickk.ubi4.versions.v3.domain.main.GetMainVisibleDisplaysUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.main.ObserveMainStatusUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3MainRepositoryTest {
    private val appContext = mockk<Context>()
    private val context = mockk<Context> { every { applicationContext } returns appContext }
    private val preferences = mockk<SharedPreferences>()

    @Test fun `remember and reset use existing last MAC key and literal null without clearing saved connection`() {
        val previous = runCatching { ConnectionState.connectedDeviceAddress }.getOrDefault("")
        val editor = mockk<SharedPreferences.Editor>()
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs
        try {
            val repository = V3MainRepositoryImpl(V3DeviceIdentityStore(), context, preferences)
            ConnectionState.connectedDeviceAddress = " AA:BB "
            repository.rememberLastConnection()
            repository.resetLastConnection()
            verifySequence {
                preferences.edit()
                editor.putString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, " AA:BB ")
                editor.apply()
                preferences.edit()
                editor.putString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "null")
                editor.apply()
            }
            confirmVerified(preferences, editor)
            assertEquals(" AA:BB ", ConnectionState.connectedDeviceAddress)
        } finally { ConnectionState.connectedDeviceAddress = previous }
    }

    @Test fun `restoring saved connection preserves raw values and defaults without changing identity or widget storage`() {
        val previousName = runCatching { ConnectionState.connectedDeviceName }.getOrDefault("")
        val previousAddress = runCatching { ConnectionState.connectedDeviceAddress }.getOrDefault("")
        val previousMac = WidgetRepoProvider.mac()
        try {
            val identity = V3DeviceIdentityStore()
            val header = identity.update("FTHS3-00001", null)
            val repository = V3MainRepositoryImpl(identity, context, preferences)
            ConnectionState.connectedDeviceAddress = "launch-address"
            repository.initializeWidgetStorage()
            for (saved in listOf(" raw ", "", "NOT SET!", null)) {
                every { preferences.getString(PreferenceKeysUbi4.CONNECTED_DEVICE, "NOT SET!") } returns saved
                every { preferences.getString(PreferenceKeysUbi4.CONNECTED_DEVICE_ADDRESS, "NOT SET!") } returns saved
                repository.restoreSavedConnection()
                assertEquals(saved.toString(), ConnectionState.connectedDeviceName)
                assertEquals(saved.toString(), ConnectionState.connectedDeviceAddress)
                assertEquals("launch-address", WidgetRepoProvider.mac())
                assertEquals(header, identity.identity.value)
            }
            verify(exactly = 0) { preferences.edit() }
        } finally {
            ConnectionState.connectedDeviceName = previousName
            ConnectionState.connectedDeviceAddress = previousAddress
            WidgetRepoProvider.setCurrentMac(previousMac)
        }
    }

    @Test fun `profile upload uses application context and existing scheduler without changing language`() {
        mockkObject(SettingsProfileUploadWorkScheduler)
        try {
            every { SettingsProfileUploadWorkScheduler.cancelAppCloseUpload(any()) } just Runs
            every { SettingsProfileUploadWorkScheduler.enqueueAppCloseUpload(any(), any()) } just Runs
            val repository = V3MainRepositoryImpl(V3DeviceIdentityStore(), context, preferences)
            repository.cancelAppCloseProfileUpload()
            repository.enqueueAppCloseProfileUpload(" ru_RU ")
            verifySequence {
                SettingsProfileUploadWorkScheduler.cancelAppCloseUpload(appContext)
                SettingsProfileUploadWorkScheduler.enqueueAppCloseUpload(appContext, " ru_RU ")
            }
        } finally { unmockkObject(SettingsProfileUploadWorkScheduler) }
    }

    @Test fun `identity storage preserves full names while display keeps existing standard and INDY3 prefixes`() {
        // ConnectionState may be uninitialized in an isolated JVM test.
        val previous = runCatching { ConnectionState.connectedDeviceName }.getOrDefault("")
        try {
            val repository = V3MainRepositoryImpl(V3DeviceIdentityStore(), context, preferences)
            for ((fullName, displayName) in listOf(
                " FTHS3-00001 " to "00001", "INDY3-00002" to "00002",
                "CPU-000001" to "CPU-000001", "" to "",
            )) {
                ConnectionState.connectedDeviceName = fullName
                val identity = repository.loadDeviceIdentity()
                assertEquals(fullName, ConnectionState.connectedDeviceName)
                assertEquals(fullName, identity.serial)
                assertNull(identity.deviceName)
                assertEquals(displayName, identity.displayName)
            }
        } finally { ConnectionState.connectedDeviceName = previous }
    }

    @Test fun `visibility uses renderable widgets not just display metadata and keeps zero through four range`() {
        val previous = UiState.listWidgets
        fun widget(display: Int, code: Int = ParameterWidgetCode.PWCE_BUTTON_V3.number.toInt()) =
            BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = display, widgetCode = code), "Title")
        try {
            UiState.listWidgets = mutableSetOf(widget(0), widget(2), widget(4), widget(9),
                widget(1, ParameterWidgetCode.PWCE_UNKNOW.number.toInt()), "unsupported")
            val repository = V3MainRepositoryImpl(V3DeviceIdentityStore(), context, preferences)
            assertEquals(setOf(0, 2, 4), repository.getVisibleDisplays())
            UiState.listWidgets = mutableSetOf(widget(1))
            assertEquals(setOf(1), repository.getVisibleDisplays())
            UiState.listWidgets.clear()
            assertTrue(repository.getVisibleDisplays().isEmpty())
        } finally { UiState.listWidgets = previous }
    }

    @Test fun `common display source preserves native renderable visibility for every display combination`() {
        val previous = UiState.listWidgets
        fun widget(display: Int, code: Int = ParameterWidgetCode.PWCE_BUTTON_V3.number.toInt()) =
            BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = display, widgetCode = code), "Title")
        val common = GetMainVisibleDisplaysUseCaseV3(V3MainDisplaysRepositoryImpl())
        val android = V3MainRepositoryImpl(V3DeviceIdentityStore(), context, preferences)
        try {
            for (mask in 0 until 32) {
                val selected = (0..4).filter { (mask and (1 shl it)) != 0 }.toSet()
                val widgets = mutableSetOf<Any>(
                    widget(-1), widget(5), widget(9), "unsupported",
                    widget(1, ParameterWidgetCode.PWCE_UNKNOW.number.toInt()),
                )
                selected.mapTo(widgets) { widget(it) }
                UiState.listWidgets = widgets
                assertEquals(selected, common(), "display mask=$mask")
                assertEquals(android.getVisibleDisplays(), common(), "Android parity mask=$mask")
                assertSame(widgets, UiState.listWidgets)
            }
            verify(exactly = 0) { preferences.edit() }
        } finally { UiState.listWidgets = previous }
    }

    @Test fun `narrow display getter reads each action and returns an independent snapshot`() {
        val source = mutableSetOf(0, 4)
        var reads = 0
        val repository = object : V3MainDisplaysRepository {
            override fun getVisibleDisplays(): Set<Int> { reads++; return source }
        }
        val get = GetMainVisibleDisplaysUseCaseV3(repository)
        assertEquals(0, reads)
        val first = get()
        source.clear()
        source.add(2)
        assertEquals(setOf(0, 4), first)
        assertEquals(setOf(2), get())
        source.clear()
        assertTrue(get().isEmpty())
        assertEquals(3, reads)
        assertTrue(source.isEmpty())
    }

    @Test fun `status ready getter rereads actual BLE state without subscriptions or Android dependencies`() {
        val previousState = BLEState.state.value
        val previousBattery = WidgetState.batteryPercentFlow
        val battery = MutableSharedFlow<Int>(replay = 1)
        try {
            WidgetState.batteryPercentFlow = battery
            val observe = ObserveMainStatusUseCaseV3(V3MainStatusRepositoryImpl())
            BLEState.publishDisconnect()
            assertFalse(observe.currentConnectionReady())
            BLEState.publishConnecting()
            assertFalse(observe.currentConnectionReady())
            BLEState.publishReady()
            assertTrue(observe.currentConnectionReady())
            BLEState.publishError()
            assertFalse(observe.currentConnectionReady())
            BLEState.publishReady()
            assertTrue(observe.currentConnectionReady())
            assertEquals(0, battery.subscriptionCount.value)
            verify { preferences wasNot Called; context wasNot Called; appContext wasNot Called }
        } finally {
            restoreBleState(previousState)
            WidgetState.batteryPercentFlow = previousBattery
        }
    }

    @Test fun `ready callbacks retain state replay equal false transitions conflation and cancellation`() = runTest {
        val previousState = BLEState.state.value
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var job: Job? = null
        var reopened: Job? = null
        try {
            BLEState.publishDisconnect()
            val observe = ObserveMainStatusUseCaseV3(V3MainStatusRepositoryImpl())
            val received = mutableListOf<Boolean>()
            job = observe.observeConnectionReady(received::add)
            assertTrue(received.isEmpty())
            runCurrent()
            assertEquals(listOf(false), received)
            BLEState.publishConnecting(); runCurrent()
            BLEState.publishError(); runCurrent()
            assertEquals(listOf(false, false, false), received)
            BLEState.publishReady(); runCurrent()
            BLEState.publishReady(); runCurrent()
            assertEquals(listOf(false, false, false, true), received)
            BLEState.publishDisconnect()
            BLEState.publishConnecting()
            BLEState.publishError()
            runCurrent()
            assertEquals(listOf(false, false, false, true, false), received)
            job.cancelAndJoin()
            BLEState.publishReady(); runCurrent()
            assertEquals(listOf(false, false, false, true, false), received)
            val current = mutableListOf<Boolean>()
            reopened = observe.observeConnectionReady(current::add)
            runCurrent()
            assertEquals(listOf(true), current)
            assertTrue(observe.currentConnectionReady())
            verify { preferences wasNot Called; context wasNot Called; appContext wasNot Called }
        } finally {
            job?.cancelAndJoin()
            reopened?.cancelAndJoin()
            restoreBleState(previousState)
            Dispatchers.resetMain()
        }
    }

    @Test fun `battery callbacks collect current source raw replay and repeats with independent cancellation`() = runTest {
        val previousBattery = WidgetState.batteryPercentFlow
        val previousState = BLEState.state.value
        val previousProfile = UiState.activeV3DeviceProfile
        val previousMac = WidgetRepoProvider.mac()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val jobs = mutableListOf<Job>()
        try {
            val original = MutableSharedFlow<Int>(replay = 1)
            original.emit(111)
            WidgetState.batteryPercentFlow = original
            val repository = V3MainStatusRepositoryImpl()
            val coldSource = repository.observeBatteryPercent()
            val observe = ObserveMainStatusUseCaseV3(repository)
            val received = mutableListOf<Int>()
            val first = observe.observeBatteryPercent(received::add).also(jobs::add)
            val current = MutableSharedFlow<Int>(replay = 1)
            current.emit(-17)
            WidgetState.batteryPercentFlow = current
            val direct = mutableListOf<Int>()
            val directJob = backgroundScope.launch { coldSource.toList(direct) }
            jobs += directJob
            assertTrue(received.isEmpty())
            runCurrent()
            assertEquals(listOf(-17), received)
            assertEquals(listOf(-17), direct)
            assertEquals(0, original.subscriptionCount.value)
            directJob.cancelAndJoin()
            assertEquals(1, current.subscriptionCount.value)
            for (raw in listOf(-17, Int.MAX_VALUE, Int.MIN_VALUE)) {
                current.emit(raw); runCurrent()
            }
            assertEquals(listOf(-17, -17, Int.MAX_VALUE, Int.MIN_VALUE), received)
            val secondReceived = mutableListOf<Int>()
            val second = observe.observeBatteryPercent(secondReceived::add).also(jobs::add)
            runCurrent()
            assertEquals(listOf(Int.MIN_VALUE), secondReceived)
            repeat(2) { current.emit(37); runCurrent() }
            assertEquals(listOf(Int.MIN_VALUE, 37, 37), secondReceived)
            assertEquals(listOf(-17, -17, Int.MAX_VALUE, Int.MIN_VALUE, 37, 37), received)
            first.cancelAndJoin()
            assertEquals(1, current.subscriptionCount.value)
            current.emit(38); runCurrent()
            assertEquals(listOf(Int.MIN_VALUE, 37, 37, 38), secondReceived)
            assertEquals(6, received.size)
            second.cancelAndJoin()
            assertEquals(0, current.subscriptionCount.value)
            current.emit(99); runCurrent()
            val reopened = mutableListOf<Int>()
            jobs += observe.observeBatteryPercent(reopened::add)
            runCurrent()
            assertEquals(listOf(99), reopened)
            assertEquals(previousState, BLEState.state.value)
            assertEquals(previousProfile, UiState.activeV3DeviceProfile)
            assertEquals(previousMac, WidgetRepoProvider.mac())
            verify { preferences wasNot Called; context wasNot Called; appContext wasNot Called }
        } finally {
            jobs.forEach { it.cancelAndJoin() }
            WidgetState.batteryPercentFlow = previousBattery
            Dispatchers.resetMain()
        }
    }

    private fun restoreBleState(state: BLEState.State) {
        when (state) {
            BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
            BLEState.State.CONNECTING -> BLEState.publishConnecting()
            BLEState.State.READY -> BLEState.publishReady()
            BLEState.State.ERROR -> BLEState.publishError()
        }
    }

    @Test fun `global streams preserve replay repeated widget updates and unsubscribe on cancellation`() = runTest {
        val oldUpdates = UiState.updateFlow
        val oldBattery = WidgetState.batteryPercentFlow
        UiState.updateFlow = MutableSharedFlow(replay = 1)
        WidgetState.batteryPercentFlow = MutableSharedFlow(replay = 1)
        val received = mutableListOf<V3MainUpdate>()
        var job: Job? = null
        try {
            WidgetState.batteryPercentFlow.emit(37)
            UiState.updateFlow.emit(0)
            job = backgroundScope.launch { V3MainRepositoryImpl(V3DeviceIdentityStore(), context, preferences).observeUpdates().toList(received) }
            runCurrent()
            assertEquals(2, received.size)
            assertTrue(received.contains(V3MainUpdate.BatteryChanged(37)))
            assertTrue(received.contains(V3MainUpdate.WidgetsChanged))
            UiState.updateFlow.emit(0); runCurrent()
            assertEquals(2, received.count { it == V3MainUpdate.WidgetsChanged })
            job.cancelAndJoin()
            assertEquals(0, UiState.updateFlow.subscriptionCount.value)
            assertEquals(0, WidgetState.batteryPercentFlow.subscriptionCount.value)
            WidgetState.batteryPercentFlow.emit(19); runCurrent()
            assertEquals(3, received.size)
        } finally {
            job?.cancelAndJoin()
            UiState.updateFlow = oldUpdates
            WidgetState.batteryPercentFlow = oldBattery
        }
    }
}
