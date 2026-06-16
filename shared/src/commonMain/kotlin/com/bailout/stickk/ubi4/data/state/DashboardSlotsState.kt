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

data class DashboardSlotContentUiState(
    val deviceAddress: Int = 0,
    val dataCode: Int = 0,
    val title: String = "",
    val version: Int = 0,
    val subVersion: Int = 0,
    val declaredSize: Int = 0,
    val isLoading: Boolean = false,
    val data: List<Int> = emptyList(),
    val loadedSize: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

object DashboardSlotContentState {
    private val _stateFlow = MutableStateFlow(DashboardSlotContentUiState())
    val stateFlow = _stateFlow.asStateFlow()

    fun requestStarted(
        deviceAddress: Int,
        dataCode: Int,
        title: String,
        version: Int,
        subVersion: Int,
        declaredSize: Int
    ) {
        _stateFlow.value = DashboardSlotContentUiState(
            deviceAddress = deviceAddress,
            dataCode = dataCode,
            title = title,
            version = version,
            subVersion = subVersion,
            declaredSize = declaredSize,
            isLoading = true,
            statusMessage = "Запрос содержимого слота..."
        )
    }

    fun updateData(dataCode: Int, data: List<Int>) {
        val current = _stateFlow.value
        if (current.dataCode != dataCode) return
        _stateFlow.value = current.copy(
            isLoading = false,
            data = data,
            loadedSize = data.size,
            statusMessage = "Данные загружены",
            errorMessage = null
        )
    }

    fun updateDataPart(dataCode: Int, offset: Int, data: List<Int>) {
        val current = _stateFlow.value
        if (current.dataCode != dataCode) return

        val totalSize = current.declaredSize.coerceAtLeast(offset + data.size)
        val buffer = current.data.toMutableList()
        while (buffer.size < totalSize) {
            buffer += 0
        }
        data.forEachIndexed { index, value ->
            val targetIndex = offset + index
            if (targetIndex in buffer.indices) {
                buffer[targetIndex] = value
            }
        }

        val loadedSize = (offset + data.size).coerceAtMost(totalSize).coerceAtLeast(current.loadedSize)
        val isComplete = loadedSize >= totalSize
        _stateFlow.value = current.copy(
            isLoading = !isComplete,
            data = if (isComplete) buffer.take(totalSize) else buffer,
            loadedSize = loadedSize,
            statusMessage = if (isComplete) {
                "Данные загружены"
            } else {
                "Загружено $loadedSize/$totalSize байт"
            },
            errorMessage = null
        )
    }

    fun updateParameterValue(path: String, value: String) {
        val current = _stateFlow.value
        if (current.data.isEmpty()) return
        _stateFlow.value = current.copy(
            data = DashboardSlotContentSchemas.updateValue(current, path, value),
            statusMessage = "Есть несохраненные изменения",
            errorMessage = null
        )
    }

    fun updateStatus(message: String) {
        val current = _stateFlow.value
        _stateFlow.value = current.copy(
            isLoading = false,
            statusMessage = message,
            errorMessage = null
        )
    }

    fun requestFailed(deviceAddress: Int, dataCode: Int, message: String) {
        val current = _stateFlow.value
        if (current.deviceAddress != deviceAddress || current.dataCode != dataCode || !current.isLoading) return
        _stateFlow.value = current.copy(
            isLoading = false,
            errorMessage = message,
            statusMessage = null
        )
    }
}
