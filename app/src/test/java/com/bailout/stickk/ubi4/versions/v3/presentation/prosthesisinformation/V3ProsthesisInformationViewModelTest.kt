package com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3ProsthesisInformationViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3ProsthesisInformationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val storage = FakeAccountProfileLocal()
    private lateinit var vm: V3ProsthesisInformationViewModel

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3ProsthesisInformationViewModelFactory(storage).create(V3ProsthesisInformationViewModel::class.java)
        store.put("info", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun `already open screen displays fresh server characteristics and duplicate attach creates no observer`() = runTest(dispatcher) {
        storage.values[V3AccountDetail.MODEL] = "ПРО MANIFESTO-F"
        storage.values[V3AccountDetail.SIDE] = "Right"
        assertNull(vm.uiState.value.information)
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        runCurrent()
        assertEquals("Right", vm.uiState.value.information?.handSide)
        assertEquals(1, storage.detailsRevision.subscriptionCount.value)
        assertTrue(storage.writes.isEmpty())

        storage.saveDetails(listOf(
            V3AccountDetailValue(V3AccountDetail.MODEL, "FEST.HS.30.000.000"),
            V3AccountDetailValue(V3AccountDetail.SIDE, "Left"),
            V3AccountDetailValue(V3AccountDetail.ROTATOR, "Quick-release wireless rotator 1.1"),
        ))
        runCurrent()
        assertEquals("FEST.HS.30.000.000", vm.uiState.value.information?.prosthesisModel)
        assertEquals("Left", vm.uiState.value.information?.handSide)
        assertEquals("Quick-release wireless rotator 1.1", vm.uiState.value.information?.rotatorType)
        assertEquals(1, storage.detailsRevision.subscriptionCount.value)
    }

    @Test fun `detached view stops receiving changes and reattach reads the latest characteristics`() = runTest(dispatcher) {
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        runCurrent()
        vm.onAction(V3ProsthesisInformationAction.ViewDetached)
        vm.onAction(V3ProsthesisInformationAction.ViewDetached)
        runCurrent()
        val detached = vm.uiState.value
        assertEquals(0, storage.detailsRevision.subscriptionCount.value)
        storage.saveDetails(listOf(
            V3AccountDetailValue(V3AccountDetail.MODEL, "New model"),
            V3AccountDetailValue(V3AccountDetail.ROTATOR, "New rotator"),
        ))
        runCurrent()
        assertEquals(detached, vm.uiState.value)

        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        runCurrent()
        assertEquals("New model", vm.uiState.value.information?.prosthesisModel)
        assertEquals("New rotator", vm.uiState.value.information?.rotatorType)
        assertEquals(1, storage.detailsRevision.subscriptionCount.value)
    }

    @Test fun `device change clears the previous hand while the information screen stays open`() = runTest(dispatcher) {
        storage.values[V3AccountDetail.SIDE] = "Right"
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        runCurrent()
        assertEquals("Right", vm.uiState.value.information?.handSide)

        storage.selectSerialNumber(null)
        runCurrent()
        assertEquals("", vm.uiState.value.information?.handSide)

        storage.selectSerialNumber("FEST-HM-13261")
        storage.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.SIDE, "Left")))
        runCurrent()
        assertEquals("Left", vm.uiState.value.information?.handSide)
    }

    @Test fun `cleared viewmodel cancels observation and ignores late lifecycle actions`() = runTest(dispatcher) {
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        runCurrent()
        val original = vm.uiState.value
        store.clear()
        storage.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.MODEL, "Updated")))
        vm.onAction(V3ProsthesisInformationAction.ViewDetached)
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        runCurrent()
        assertEquals(original, vm.uiState.value)
        assertEquals(0, storage.detailsRevision.subscriptionCount.value)
    }
}
