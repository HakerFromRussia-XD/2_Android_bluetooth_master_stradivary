package com.bailout.stickk.ubi4.versions.v3.data.dashboard

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentState
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContent
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContentRepository
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContentTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

class V3DashboardSlotContentRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
) : V3DashboardSlotContentRepository {
    override fun observeContent() = DashboardSlotContentState.stateFlow.map { state ->
        V3DashboardSlotContent(
            slot = V3DashboardSlotContentTarget(state.deviceAddress, state.dataCode, state.title,
                state.version, state.subVersion, state.declaredSize),
            isLoading = state.isLoading,
            data = state.data,
            loadedSize = state.loadedSize,
            editedValues = state.editedValues,
            statusMessage = state.statusMessage,
            errorMessage = state.errorMessage,
        )
    }

    override suspend fun loadContent(slot: V3DashboardSlotContentTarget) {
        DashboardSlotContentState.requestStarted(slot.deviceAddress, slot.dataCode, slot.title,
            slot.version, slot.subVersion, slot.declaredSize)
        if (slot.declaredSize > CHUNK_SIZE_THRESHOLD) {
            var offset = 0
            while (offset < slot.declaredSize) {
                val size = minOf(CHUNK_DATA_SIZE, slot.declaredSize - offset)
                sendCommand(slot, "READ_DATA_PART", BLECommandsV3.requestSlotDataPart(
                    slot.deviceAddress, slot.dataCode, offset, size))
                offset += size
            }
        } else {
            sendCommand(slot, "READ_DATA", BLECommandsV3.requestSlotData(slot.deviceAddress, slot.dataCode))
        }
        // Preserve the original timeout calculation even for a single READ_DATA packet.
        val chunkCount = if (slot.declaredSize <= 0) 1 else (slot.declaredSize + 199) / CHUNK_DATA_SIZE
        delay(5_000L + chunkCount * 1_000L)
        DashboardSlotContentState.requestFailed(slot.deviceAddress, slot.dataCode,
            "Плата не ответила на запрос содержимого")
    }

    override fun updateParameterValue(path: String, value: String) {
        DashboardSlotContentState.updateParameterValue(path, value)
    }

    override fun sendContent(slot: V3DashboardSlotContentTarget) {
        val current = DashboardSlotContentState.stateFlow.value
        if (current.data.isEmpty()) {
            DashboardSlotContentState.updateStatus("Нет данных для отправки")
            return
        }
        val data = ByteArray(current.data.size) { current.data[it].toByte() }
        if (data.size > CHUNK_SIZE_THRESHOLD) {
            var offset = 0
            while (offset < data.size) {
                val size = minOf(CHUNK_DATA_SIZE, data.size - offset)
                sendCommand(slot, "WRITE_DATA_PART", BLECommandsV3.writeSlotDataPart(
                    slot.deviceAddress, slot.dataCode, offset, data.copyOfRange(offset, offset + size)))
                offset += size
            }
        } else {
            sendCommand(slot, "WRITE_DATA", BLECommandsV3.writeSlotData(slot.deviceAddress, slot.dataCode, data))
        }
        DashboardSlotContentState.updateStatus("Данные отправлены")
    }

    override fun saveSlots(slot: V3DashboardSlotContentTarget) {
        sendCommand(slot, "SAVE_DATA", BLECommandsV3.saveSlots(slot.deviceAddress))
    }

    override fun resetSlot(slot: V3DashboardSlotContentTarget) {
        sendCommand(slot, "RESET_TO_FACTORY", BLECommandsV3.resetSlot(slot.deviceAddress, slot.dataCode))
    }

    override fun resetAllSlots(slot: V3DashboardSlotContentTarget) {
        sendCommand(slot, "RESET_TO_FACTORY_ALL", BLECommandsV3.resetAllSlots(slot.deviceAddress))
    }

    private fun sendCommand(slot: V3DashboardSlotContentTarget, commandName: String, packet: ByteArray) {
        platformLog(
            "DASHBOARD_SLOT_CONTENT",
            "TX $commandName deviceAddress=0x${slot.deviceAddress.toHexByte()} " +
                "dataCode=0x${slot.dataCode.toHexByte()} packet=${packet.joinToString(" ") { (it.toInt() and 0xFF).toHexByte() }}"
        )
        enqueuePacket(packet)
    }

    private companion object {
        const val CHUNK_SIZE_THRESHOLD = 250
        const val CHUNK_DATA_SIZE = 200
    }
}

private fun Int.toHexByte(): String = toString(16).uppercase().padStart(2, '0')
