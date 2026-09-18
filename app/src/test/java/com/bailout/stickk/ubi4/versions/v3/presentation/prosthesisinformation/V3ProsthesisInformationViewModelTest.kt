package com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3ProsthesisInformationViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3ProsthesisInformationViewModelTest {
    private val storage = FakeAccountProfileLocal()
    private var reads = 0
    private val repository = object : V3AccountProfileLocalRepository by storage {
        override fun getDetail(detail: V3AccountDetail): String {
            reads++
            return storage.getDetail(detail)
        }
    }
    private val vm = V3ProsthesisInformationViewModelFactory(repository).create(V3ProsthesisInformationViewModel::class.java)

    @Test fun `view reads once and state remains a snapshot until view recreation`() {
        assertNull(vm.uiState.value.information)
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        val original = vm.uiState.value
        storage.values[V3AccountDetail.MODEL] = "Updated"
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        assertEquals(original, vm.uiState.value)
        assertEquals(6, reads)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun `recreated view reads current characteristics without keeping old device data`() {
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        vm.onAction(V3ProsthesisInformationAction.ViewDetached)
        vm.onAction(V3ProsthesisInformationAction.ViewDetached)
        storage.values[V3AccountDetail.MODEL] = "New model"
        storage.values[V3AccountDetail.ROTATOR] = "New rotator"
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        assertEquals("New model", vm.uiState.value.information!!.prosthesisModel)
        assertEquals("New rotator", vm.uiState.value.information!!.rotatorType)
        assertEquals(12, reads)
    }

    @Test fun `cleared viewmodel ignores late lifecycle actions`() {
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        val original = vm.uiState.value
        ViewModelStore().apply { put("info", vm); clear() }
        storage.values[V3AccountDetail.MODEL] = "Updated"
        vm.onAction(V3ProsthesisInformationAction.ViewDetached)
        vm.onAction(V3ProsthesisInformationAction.ViewAttached)
        assertEquals(original, vm.uiState.value)
        assertEquals(6, reads)
    }
}
