package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

import kotlinx.coroutines.flow.Flow

data class V3DashboardSlotContentTarget(
    val deviceAddress: Int = 0,
    val dataCode: Int = 0,
    val title: String = "",
    val version: Int = 0,
    val subVersion: Int = 0,
    val declaredSize: Int = 0,
)

data class V3DashboardSlotContent(
    val slot: V3DashboardSlotContentTarget = V3DashboardSlotContentTarget(),
    val isLoading: Boolean = false,
    val data: List<Int> = emptyList(),
    val loadedSize: Int = 0,
    val editedValues: Map<String, String> = emptyMap(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

interface V3DashboardSlotContentRepository {
    fun observeContent(): Flow<V3DashboardSlotContent>
    /** Starts a read and waits for its timeout; cancellation stops the timeout. */
    suspend fun loadContent(slot: V3DashboardSlotContentTarget)
    fun updateParameterValue(path: String, value: String)
    /** Enqueues the current draft without saving to flash or waiting for acknowledgement. */
    fun sendContent(slot: V3DashboardSlotContentTarget)
    fun saveSlots(slot: V3DashboardSlotContentTarget)
    fun resetSlot(slot: V3DashboardSlotContentTarget)
    fun resetAllSlots(slot: V3DashboardSlotContentTarget)
}
