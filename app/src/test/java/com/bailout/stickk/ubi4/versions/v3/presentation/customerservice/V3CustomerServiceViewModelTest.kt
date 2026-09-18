package com.bailout.stickk.ubi4.versions.v3.presentation.customerservice

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3CustomerServiceViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.FakeAccountProfileLocal
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3CustomerServiceViewModelTest {
    private val repository = FakeAccountProfileLocal()
    private val vm = V3CustomerServiceViewModelFactory(repository).create(V3CustomerServiceViewModel::class.java)

    @Test fun `view reads once and recreation reads current data`() {
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        val original = vm.uiState.value
        repository.values[V3AccountDetail.MANAGER_NAME] = "Updated"
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        assertEquals(original, vm.uiState.value)
        vm.onAction(V3CustomerServiceAction.ViewDetached)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        assertEquals("Updated", vm.uiState.value.info!!.managerName)
        assertNull(vm.uiState.value.phoneToDial)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `click rereads only the phone without changing the visible card`() {
        repository.values[V3AccountDetail.MANAGER_PHONE] = "first"
        vm.onAction(V3CustomerServiceAction.ViewAttached)
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

    @Test fun `missing and empty phone still request the previous dialer behavior`() {
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("null", vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.DialerHandled)
        repository.values[V3AccountDetail.MANAGER_PHONE] = ""
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals("", vm.uiState.value.phoneToDial)
    }

    @Test fun `detached views discard pending dialer and ignore clicks`() {
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertNull(vm.uiState.value.phoneToDial)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        vm.onAction(V3CustomerServiceAction.ViewDetached)
        val detached = vm.uiState.value
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals(detached, vm.uiState.value)
        assertNull(vm.uiState.value.phoneToDial)
    }

    @Test fun `cleared viewmodel cannot reload data or launch dialer`() {
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        val original = vm.uiState.value
        ViewModelStore().apply { put("support", vm); clear() }
        repository.values[V3AccountDetail.MANAGER_NAME] = "Updated"
        vm.onAction(V3CustomerServiceAction.ViewDetached)
        vm.onAction(V3CustomerServiceAction.ViewAttached)
        vm.onAction(V3CustomerServiceAction.ManagerClicked)
        assertEquals(original, vm.uiState.value)
    }
}
