package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

import kotlinx.coroutines.flow.Flow

data class V3DashboardSlot(
    val deviceAddress: Int,
    val dataCode: Int,
    val dataType: Int,
    val dataTypeVersion: Int,
    val dataTypeSubVersion: Int,
    val dataSize: Int,
    val startAddressShift: Int,
    val crc: Int,
)

data class V3DashboardSlotsSnapshot(
    val deviceAddress: Int = 0,
    val isLoading: Boolean = false,
    val slots: List<V3DashboardSlot> = emptyList(),
    val errorMessage: String? = null,
)

interface V3DashboardSlotsRepository {
    fun observeSlots(): Flow<V3DashboardSlotsSnapshot>
    fun requestSlots(deviceAddress: Int)
    fun requestTimedOut(deviceAddress: Int)
}
