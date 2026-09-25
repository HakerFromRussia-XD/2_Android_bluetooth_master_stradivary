package com.bailout.stickk.ubi4.di

import com.bailout.stickk.ubi4.ble.BleCommandExecutor
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.data.sensors.V3SensorsCommandsRepositoryImpl
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
}
