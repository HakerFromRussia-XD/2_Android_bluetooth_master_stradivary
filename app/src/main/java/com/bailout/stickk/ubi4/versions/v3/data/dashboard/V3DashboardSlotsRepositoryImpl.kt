package com.bailout.stickk.ubi4.versions.v3.data.dashboard

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.data.state.DashboardSlotsState
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlot
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotsSnapshot
import kotlinx.coroutines.flow.map

class V3DashboardSlotsRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
) : V3DashboardSlotsRepository {
    override fun observeSlots() = DashboardSlotsState.stateFlow.map { state ->
        V3DashboardSlotsSnapshot(state.deviceAddress, state.isLoading, state.slots.map { slot ->
            V3DashboardSlot(slot.deviceAddress, slot.dataCode, slot.dataType,
                slot.dataTypeVersion, slot.dataTypeSubVersion, slot.dataSize, slot.startAddressShift, slot.crc)
        }, state.errorMessage)
    }

    override fun requestSlots(deviceAddress: Int) {
        DashboardSlotsState.requestStarted(deviceAddress)
        val packet = BLECommandsV3.requestAvailableSlots(deviceAddress)
        platformLog(
            "DASHBOARD_SLOTS",
            "TX READ_AVAILABLE_SLOTS deviceAddress=0x${deviceAddress.toHexByte()} " +
                "uuid=$SERIALPORTCHAR_UUID packet=${packet.joinToString(" ") { (it.toInt() and 0xFF).toHexByte() }}"
        )
        enqueuePacket(packet)
    }

    override fun requestTimedOut(deviceAddress: Int) {
        DashboardSlotsState.requestFailed(deviceAddress, "Плата не ответила на запрос слотов")
    }
}

private fun Int.toHexByte(): String = toString(16).uppercase().padStart(2, '0')
