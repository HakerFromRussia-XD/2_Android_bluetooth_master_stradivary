package com.bailout.stickk.ubi4.versions.v3.data.sensors

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsCommandsRepository

class V3SensorsCommandsRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val showSyncProgress: () -> Unit,
    private val refreshWidgets: () -> Unit,
) : V3SensorsCommandsRepository {
    override val interactionEnabled = UiState.v3WidgetsInteractionEnabled
    override val refreshInProgress = UiState.fullInitInProgress

    override fun startMovement(deviceAddress: String, movement: V3ProsthesisMovement): Boolean {
        if (!isCurrentDevice(deviceAddress) || !UiState.isInterfaceV3Activated || !interactionEnabled.value) return false
        val command = when (movement) {
            V3ProsthesisMovement.OPEN -> ProsthesisModuleControlEnum.PMCE_OPEN_COMMAND
            V3ProsthesisMovement.CLOSE -> ProsthesisModuleControlEnum.PMCE_CLOSE_COMMAND
        }
        enqueuePacket(BLECommandsV3.sendSubcommand(command.number.toInt(), 0))
        return true
    }

    override fun stopMovement(deviceAddress: String) {
        if (isCurrentDevice(deviceAddress)) enqueuePacket(BLECommandsV3.sendSubcommand(0, 0))
    }

    override fun refreshSensors(deviceAddress: String): Boolean {
        if (!isCurrentDevice(deviceAddress) || !UiState.isInterfaceV3Activated || refreshInProgress.value) return false
        // Retain the old BaseWidgetsFragment order; BLEController owns completion and retries.
        UiState.fullInitInProgress.value = true
        showSyncProgress()
        refreshWidgets()
        return true
    }

    private fun isCurrentDevice(address: String) = address.isNotBlank() && address != "null" && address == WidgetRepoProvider.mac()
}
