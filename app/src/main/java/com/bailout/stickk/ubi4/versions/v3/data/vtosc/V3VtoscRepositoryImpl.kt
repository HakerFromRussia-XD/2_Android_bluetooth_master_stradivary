package com.bailout.stickk.ubi4.versions.v3.data.vtosc

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentState
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscConfig
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.V3VtoscRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class V3VtoscRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
) : V3VtoscRepository {
    override fun observeConfig(): Flow<V3VtoscConfig> =
        DashboardSlotContentState.stateFlow.mapNotNull { state ->
            if (state.deviceAddress != V3VtoscConfig.DEVICE_ADDRESS ||
                state.dataCode != V3VtoscConfig.DATA_CODE ||
                state.isLoading ||
                state.data.isEmpty()
            ) {
                null
            } else {
                V3VtoscConfig.decode(state.data)
            }
        }.distinctUntilChanged()

    override fun observeLoading(): Flow<Boolean> =
        DashboardSlotContentState.stateFlow
            .map { state ->
                state.deviceAddress == V3VtoscConfig.DEVICE_ADDRESS &&
                    state.dataCode == V3VtoscConfig.DATA_CODE &&
                    state.isLoading
            }
            .distinctUntilChanged()

    override fun loadConfig() {
        DashboardSlotContentState.requestStarted(
            deviceAddress = V3VtoscConfig.DEVICE_ADDRESS,
            dataCode = V3VtoscConfig.DATA_CODE,
            title = "VTFB Settings",
            version = V3VtoscConfig.VERSION,
            subVersion = V3VtoscConfig.SUB_VERSION,
            declaredSize = V3VtoscConfig.DATA_SIZE,
        )
        enqueuePacket(
            BLECommandsV3.requestSlotData(V3VtoscConfig.DEVICE_ADDRESS, V3VtoscConfig.DATA_CODE),
        )
    }

    override fun writeConfig(config: V3VtoscConfig) {
        val data = config.encode()
        DashboardSlotContentState.updateData(V3VtoscConfig.DATA_CODE, data.map { it.toInt() and 0xFF })
        enqueuePacket(
            BLECommandsV3.writeSlotData(
                V3VtoscConfig.DEVICE_ADDRESS,
                V3VtoscConfig.DATA_CODE,
                data,
            ),
        )
    }

    override fun saveConfig() {
        enqueuePacket(BLECommandsV3.saveSlots(V3VtoscConfig.DEVICE_ADDRESS))
    }
}
