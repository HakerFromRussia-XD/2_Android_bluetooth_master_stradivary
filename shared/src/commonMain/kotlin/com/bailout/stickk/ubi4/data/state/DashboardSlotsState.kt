package com.bailout.stickk.ubi4.data.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardSlotInfo(
    val deviceAddress: Int,
    val dataCode: Int,
    val dataType: Int,
    val dataTypeVersion: Int,
    val dataTypeSubVersion: Int,
    val dataSize: Int,
    val startAddressShift: Int,
    val crc: Int
)

data class DashboardSlotsUiState(
    val deviceAddress: Int = 0,
    val isLoading: Boolean = false,
    val slots: List<DashboardSlotInfo> = emptyList(),
    val errorMessage: String? = null
)

object DashboardSlotsState {
    private val _stateFlow = MutableStateFlow(DashboardSlotsUiState())
    val stateFlow = _stateFlow.asStateFlow()

    fun requestStarted(deviceAddress: Int) {
        _stateFlow.value = DashboardSlotsUiState(
            deviceAddress = deviceAddress,
            isLoading = true,
            errorMessage = null
        )
    }

    fun updateSlots(deviceAddress: Int, slots: List<DashboardSlotInfo>) {
        _stateFlow.value = DashboardSlotsUiState(
            deviceAddress = deviceAddress,
            isLoading = false,
            slots = slots,
            errorMessage = null
        )
    }

    fun requestFailed(deviceAddress: Int, message: String) {
        val current = _stateFlow.value
        if (current.deviceAddress != deviceAddress || !current.isLoading) return
        _stateFlow.value = current.copy(
            isLoading = false,
            errorMessage = message
        )
    }
}
