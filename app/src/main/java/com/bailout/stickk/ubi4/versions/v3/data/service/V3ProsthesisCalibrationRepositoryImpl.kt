package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3ProsthesisCalibrationRepository

class V3ProsthesisCalibrationRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
) : V3ProsthesisCalibrationRepository {
    override val interactionEnabled = UiState.v3WidgetsInteractionEnabled

    override fun startCalibration(deviceAddress: String): Boolean {
        if (!isCurrentDevice(deviceAddress) || !UiState.isInterfaceV3Activated || !interactionEnabled.value) return false
        enqueuePacket(BLECommandsV3.sendSubcommand(ProsthesisModuleControlEnum.PMCE_START_CALIBRATE_COMMAND.number.toInt(), 0))
        return true
    }

    override fun releaseCalibrationButton(deviceAddress: String) {
        // Preserve the former UP/CANCEL packet. This is not a calibration completion/ACK.
        if (isCurrentDevice(deviceAddress)) enqueuePacket(BLECommandsV3.sendSubcommand(0, 0))
    }

    private fun isCurrentDevice(address: String) = address.isNotBlank() && address != "null" && address == WidgetRepoProvider.mac()
}
