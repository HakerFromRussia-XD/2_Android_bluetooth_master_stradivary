package com.bailout.stickk.ubi4.versions.v3.presentation.customerservice

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3CustomerServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.FakeAccountProfileLocal
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetailValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3CustomerServiceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeAccountProfileLocal()
    private lateinit var vm: V3CustomerServiceViewModel

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3CustomerServiceViewModelFactory(repository).create(V3CustomerServiceViewModel::class.java)
        store.put("support", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun `already open view receives fresh support details and duplicate attach keeps one observer`() = runTest(dispatcher) {
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        assertEquals(1, repository.detailsRevision.subscriptionCount.value)
        assertTrue(repository.writes.isEmpty())
        repository.saveDetails(listOf(
            V3AccountDetailValue(V3AccountDetail.MANAGER_NAME, "Updated"),
            V3AccountDetailValue(V3AccountDetail.MANAGER_PHONE, "new phone"),
            V3AccountDetailValue(V3AccountDetail.TRANSFER_DATE, "01.01.2024"),
            V3AccountDetailValue(V3AccountDetail.STATUS, "Issued"),
        ))
        runCurrent()
        assertEquals("Updated", vm.uiState.value.info!!.managerName)
        assertEquals("new phone", vm.uiState.value.info!!.managerPhone)
        assertEquals("01.01.2027", vm.uiState.value.info!!.warrantyExpirationDate)
        assertEquals("Issued", vm.uiState.value.info!!.prosthesisStatus)
        assertNull(vm.uiState.value.phoneToDial)
        assertEquals(1, repository.detailsRevision.subscriptionCount.value)
    }

    @Test fun `click rereads only the phone without changing the visible card`() = runTest(dispatcher) {
        repository.values[V3AccountDetail.MANAGER_PHONE] = "first"
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        val info = vm.uiState.value.info
        repository.values[V3AccountDetail.MANAGER_PHONE] = "second"
        repository.values[V3AccountDetail.TRANSFER_DATE] = "invalid date"
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("second", vm.uiState.value.phoneToDial)
        assertEquals(info, vm.uiState.value.info)
        vm.onAction(V3CustomerServiceAction.DialerHandled)
        assertNull(vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("second", vm.uiState.value.phoneToDial)
    }

    @Test fun `missing and empty phone still request the previous dialer behavior`() = runTest(dispatcher) {
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("null", vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.DialerHandled)
        repository.values[V3AccountDetail.MANAGER_PHONE] = ""
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("", vm.uiState.value.phoneToDial)
    }

    @Test fun `detached views discard pending dialer and stop observing until reattached`() = runTest(dispatcher) {
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertNull(vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        vm.onAction(V3CustomerServiceAction.ViewDetached)
        runCurrent()
        val detached = vm.uiState.value
        repository.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.MANAGER_NAME, "Updated")))
        runCurrent()
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals(detached, vm.uiState.value)
        assertNull(vm.uiState.value.phoneToDial)
        assertEquals(0, repository.detailsRevision.subscriptionCount.value)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        assertEquals("Updated", vm.uiState.value.info?.managerName)
        assertNull(vm.uiState.value.phoneToDial)
    }

    @Test fun `device change clears support details without replaying a handled dialer action`() = runTest(dispatcher) {
        repository.values[V3AccountDetail.MANAGER_NAME] = "Previous manager"
        repository.values[V3AccountDetail.MANAGER_PHONE] = "previous phone"
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("previous phone", vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.DialerHandled)

        repository.selectSerialNumber(null)
        runCurrent()
        assertEquals("", vm.uiState.value.info?.managerName)
        assertEquals("", vm.uiState.value.info?.managerPhone)
        assertNull(vm.uiState.value.phoneToDial)

        repository.selectSerialNumber("FEST-HM-13261")
        repository.saveDetails(listOf(
            V3AccountDetailValue(V3AccountDetail.MANAGER_NAME, "Current manager"),
            V3AccountDetailValue(V3AccountDetail.MANAGER_PHONE, "current phone"),
        ))
        runCurrent()
        assertEquals("Current manager", vm.uiState.value.info?.managerName)
        assertEquals("current phone", vm.uiState.value.info?.managerPhone)
        assertNull(vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("current phone", vm.uiState.value.phoneToDial)
    }

    @Test fun `cleared viewmodel cannot reload data or launch dialer`() = runTest(dispatcher) {
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        runCurrent()
        val original = vm.uiState.value
        store.clear()
        repository.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.MANAGER_NAME, "Updated")))
        vm.onAction(V3CustomerServiceAction.ViewDetached)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        runCurrent()
        assertEquals(original, vm.uiState.value)
        assertEquals(0, repository.detailsRevision.subscriptionCount.value)
    }
}
