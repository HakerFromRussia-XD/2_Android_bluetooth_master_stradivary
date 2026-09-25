package com.bailout.stickk.ubi4.di

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.data.accountstatistics.V3AccountStatisticsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.versions.v3.data.sensors.V3SensorsCommandsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.di.createAccountProfileLocalRepository
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileDeviceContext
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.RequestAccountStatisticsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.RefreshSensorsUseCaseV3
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BleDependenciesTest {
    private val first = mockk<BleCommandExecutor>(relaxed = true)
    private val second = mockk<BleCommandExecutor>(relaxed = true)
    private val transport = BleDependencies.v3CommandTransport
    private val originalMac = WidgetRepoProvider.mac()
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalRefresh = UiState.fullInitInProgress.value

    @AfterEach fun releaseExecutors() {
        BleDependencies.unbindCommandExecutor(first)
        BleDependencies.unbindCommandExecutor(second)
        WidgetRepoProvider.setCurrentMac(originalMac)
        UiState.isInterfaceV3Activated = originalMode
        UiState.fullInitInProgress.value = originalRefresh
    }

    @Test fun `registered executor receives packet and completion without an Activity lookup`() {
        BleDependencies.bindCommandExecutor(first)
        val packet = byteArrayOf(1, 2, 3)
        var completed = false
        val callback = { completed = true }
        val sent = slot<() -> Unit>()
        every { first.bleCommandWithQueue(any(), any(), any(), capture(sent)) } just Runs

        transport.enqueue(packet, callback)

        verify(exactly = 1) { first.bleCommandWithQueue(refEq(packet), SERIALPORTCHAR_UUID, WRITE, refEq(callback)) }
        assertSame(callback, sent.captured)
        assertFalse(completed)
        sent.captured()
        assertTrue(completed)
    }

    @Test fun `existing transport follows replacement and stale cleanup cannot unbind the new executor`() {
        val before = byteArrayOf(1)
        val after = byteArrayOf(2)
        BleDependencies.bindCommandExecutor(first)
        transport.enqueue(before)
        BleDependencies.bindCommandExecutor(second)
        BleDependencies.unbindCommandExecutor(first)
        transport.enqueue(after)

        verify(exactly = 1) { first.bleCommandWithQueue(refEq(before), SERIALPORTCHAR_UUID, WRITE, any()) }
        verify(exactly = 1) { second.bleCommandWithQueue(refEq(after), SERIALPORTCHAR_UUID, WRITE, any()) }
        confirmVerified(first, second)
    }

    @Test fun `closing the active executor fails without fallback or premature completion`() {
        BleDependencies.bindCommandExecutor(first)
        BleDependencies.bindCommandExecutor(second)
        BleDependencies.unbindCommandExecutor(second)
        BleDependencies.unbindCommandExecutor(second)
        var completed = false

        assertThrows(IllegalStateException::class.java) {
            transport.enqueue(byteArrayOf(1)) { completed = true }
        }

        assertFalse(completed)
        verify { listOf(first, second) wasNot Called }
    }

    @Test fun `a new session resumes dispatch after the previous session has closed`() {
        BleDependencies.bindCommandExecutor(first)
        BleDependencies.unbindCommandExecutor(first)
        BleDependencies.bindCommandExecutor(second)
        val packet = byteArrayOf(3)

        transport.enqueue(packet)

        verify { first wasNot Called }
        verify(exactly = 1) { second.bleCommandWithQueue(refEq(packet), SERIALPORTCHAR_UUID, WRITE, any()) }
    }

    private fun sensorsRefresh(): RefreshSensorsUseCaseV3 {
        WidgetRepoProvider.setCurrentMac("test-device")
        UiState.isInterfaceV3Activated = true
        UiState.fullInitInProgress.value = false
        return RefreshSensorsUseCaseV3(V3SensorsCommandsRepositoryImpl(
            enqueuePacket = { error("Refresh must not enqueue movement commands") },
            refreshWidgets = BleDependencies::refreshSensors,
        ))
    }

    @Test fun `sensors refresh preserves flag then observation then controller order without repeat dispatch`() {
        val refresh = sensorsRefresh()
        val events = mutableListOf<String>()
        val controller = mockk<BLEController>()
        every { controller.refreshWidgetsV3BySwipe() } answers {
            assertTrue(UiState.fullInitInProgress.value)
            events += "controller"
        }
        BleDependencies.bindCommandExecutor(first)
        BleDependencies.bindSensorsRefresh(first, controller) {
            assertTrue(UiState.fullInitInProgress.value)
            events += "observation"
        }

        assertTrue(refresh("test-device"))
        assertEquals(listOf("observation", "controller"), events)
        assertFalse(refresh("test-device"))
        UiState.fullInitInProgress.value = false
        assertTrue(refresh("test-device"))
        assertEquals(listOf("observation", "controller", "observation", "controller"), events)
        verify(exactly = 2) { controller.refreshWidgetsV3BySwipe() }
    }

    @Test fun `existing repository follows new refresh registration while old cleanup cannot remove it`() {
        val refresh = sensorsRefresh()
        val oldController = mockk<BLEController>(relaxed = true)
        val currentController = mockk<BLEController>(relaxed = true)
        val observations = mutableListOf<String>()
        BleDependencies.bindCommandExecutor(first)
        BleDependencies.bindSensorsRefresh(first, oldController) { observations += "old" }
        assertTrue(refresh("test-device"))

        UiState.fullInitInProgress.value = false
        BleDependencies.bindCommandExecutor(second)
        // The next session cannot use the previous Activity's callbacks before its own binding.
        assertThrows(IllegalStateException::class.java) { BleDependencies.refreshSensors() }
        BleDependencies.bindSensorsRefresh(second, currentController) { observations += "current" }
        BleDependencies.unbindCommandExecutor(first)
        assertTrue(refresh("test-device"))

        assertEquals(listOf("old", "current"), observations)
        verify(exactly = 1) { oldController.refreshWidgetsV3BySwipe() }
        verify(exactly = 1) { currentController.refreshWidgetsV3BySwipe() }
    }

    @Test fun `closing the session releases refresh callbacks and stale binding cannot replace the current one`() {
        val controller = mockk<BLEController>(relaxed = true)
        val observe = mockk<() -> Unit>(relaxed = true)
        BleDependencies.bindCommandExecutor(second)
        BleDependencies.bindSensorsRefresh(second, controller, observe)
        assertThrows(IllegalStateException::class.java) {
            BleDependencies.bindSensorsRefresh(first, controller) { error("Stale UI callback") }
        }
        BleDependencies.refreshSensors()
        BleDependencies.unbindCommandExecutor(second)
        assertThrows(IllegalStateException::class.java) { BleDependencies.refreshSensors() }

        verify(exactly = 1) { observe() }
        verify(exactly = 1) { controller.refreshWidgetsV3BySwipe() }
    }

    @Test fun `observation failure stops refresh before controller and preserves the existing in progress flag`() {
        val refresh = sensorsRefresh()
        val controller = mockk<BLEController>(relaxed = true)
        val failure = IllegalStateException("Observation unavailable")
        BleDependencies.bindCommandExecutor(first)
        BleDependencies.bindSensorsRefresh(first, controller) { throw failure }

        assertSame(failure, assertThrows(IllegalStateException::class.java) { refresh("test-device") })
        assertTrue(UiState.fullInitInProgress.value)
        verify { controller wasNot Called }
    }

    private fun bindTelemetry(executor: BleCommandExecutor, controller: BLEController) {
        val owner = mockk<ViewModelStoreOwner> { every { viewModelStore } returns ViewModelStore() }
        BleDependencies.bindTelemetry(owner, mockk(), mockk(), controller, {}, executor)
    }

    private fun statisticsRequest() = RequestAccountStatisticsUseCaseV3(
        V3AccountStatisticsRepositoryImpl(mockk(), BleDependencies::requestV3TelemetryData),
    )

    @Test fun `statistics skips unavailable telemetry and resolves a later binding without retaining the controller`() {
        UiState.isInterfaceV3Activated = true
        val request = statisticsRequest()
        val controller = mockk<BLEController>(relaxed = true)
        BleDependencies.bindCommandExecutor(first)
        request()
        bindTelemetry(first, controller)
        request()
        BleDependencies.unbindCommandExecutor(first)
        BleDependencies.unbindCommandExecutor(first)
        request()

        verify(exactly = 1) { controller.requestTelemetryDataV3() }
        verify { first wasNot Called }
    }

    @Test fun `statistics follows session replacement and ignores stale cleanup without allowing stale telemetry binding`() {
        UiState.isInterfaceV3Activated = true
        val request = statisticsRequest()
        val oldController = mockk<BLEController>(relaxed = true)
        val currentController = mockk<BLEController>(relaxed = true)
        BleDependencies.bindCommandExecutor(first)
        bindTelemetry(first, oldController)
        request()

        BleDependencies.bindCommandExecutor(second)
        request() // No callback from the old session during replacement.
        bindTelemetry(second, currentController)
        BleDependencies.unbindCommandExecutor(first)
        request()
        assertThrows(IllegalStateException::class.java) { bindTelemetry(first, oldController) }
        request()

        verify(exactly = 1) { oldController.requestTelemetryDataV3() }
        verify(exactly = 1) { oldController.setOnConnectedListener(any()) }
        verify(exactly = 2) { currentController.requestTelemetryDataV3() }
    }

    @Test fun `statistics preserves the V3 guard and propagates controller errors without retry`() {
        val request = statisticsRequest()
        val controller = mockk<BLEController>(relaxed = true)
        val failure = IllegalStateException("Telemetry unavailable")
        every { controller.requestTelemetryDataV3() } throws failure
        BleDependencies.bindCommandExecutor(first)
        bindTelemetry(first, controller)

        UiState.isInterfaceV3Activated = false
        request()
        UiState.isInterfaceV3Activated = true
        assertSame(failure, assertThrows(IllegalStateException::class.java) { request() })

        verify(exactly = 1) { controller.requestTelemetryDataV3() }
    }

    private fun accountRepository(preferences: SharedPreferences) = createAccountProfileLocalRepository(
        mockk<Context> {
            every { applicationContext } answers { self as Context }
            every { getSharedPreferences(any(), Context.MODE_PRIVATE) } returns preferences
        },
    )

    @Test fun `account uses current identity without filling historical defaults or changing preference keys`() {
        val keys = mutableListOf<String>()
        val preferences = mockk<SharedPreferences> {
            every { getInt(any(), 1) } answers { keys += firstArg<String>(); 1 }
        }
        val repository = accountRepository(preferences)
        val identity = V3DeviceIdentityStore().apply { intentDeviceName = "scan name" }
        BleDependencies.bindCommandExecutor(first, identity)
        assertEquals(V3AccountProfileDeviceContext(null, "", null, null, null), repository.getEnvironment().device)
        identity.update("FTHS3-00001", "FTHS3-00001")
        assertEquals(V3AccountProfileDeviceContext("FTHS3-00001", "", null, null, null), repository.getEnvironment().device)
        identity.update("FEST-new", "FEST-new")
        assertEquals("FEST-new", repository.getEnvironment().device.name)
        assertEquals(9, keys.size)
        assertTrue(keys.all { it.startsWith("null") })
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `account follows replacement identity and loses the context only when the active owner closes`() {
        val repository = accountRepository(mockk { every { getInt(any(), 1) } returns 1 })
        val absent = V3AccountProfileDeviceContext(null, null, null, null, null)
        assertEquals(absent, repository.getEnvironment().device)
        val oldIdentity = V3DeviceIdentityStore().apply { update("old", "old") }
        val currentIdentity = V3DeviceIdentityStore().apply { update("current", "current") }
        BleDependencies.bindCommandExecutor(first, oldIdentity)
        assertEquals("old", repository.getEnvironment().device.name)
        BleDependencies.bindCommandExecutor(second, currentIdentity)
        BleDependencies.unbindCommandExecutor(first)
        oldIdentity.update("late", "late")
        assertEquals("current", repository.getEnvironment().device.name)
        BleDependencies.unbindCommandExecutor(second)
        assertEquals(absent, repository.getEnvironment().device)
    }
}
