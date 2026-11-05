package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3ProsthesisCalibrationRepository

class V3ProsthesisCalibrationRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
) : V3ProsthesisCalibrationRepository {
    override val interactionEnabled = UiState.v3WidgetsInteractionEnabled

    override fun startCalibration(deviceAddress: String): Boolean {
        if (!isCurrentDevice(deviceAddress) || !UiState.isInterfaceV3Activated || !interactionEnabled.value) return false
        enqueuePacket(BLECommandsV3.startCalibration())
        return true
    }

    override fun releaseCalibrationButton(deviceAddress: String) {
        // Calibration is a one-shot command. Releasing the UI press sends no device packet.
    }

    private fun isCurrentDevice(address: String) = address.isNotBlank() && address != "null" && address == WidgetRepoProvider.mac()
}
