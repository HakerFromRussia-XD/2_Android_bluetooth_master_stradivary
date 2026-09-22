package com.bailout.stickk.ubi4.data.device

import android.content.Intent
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceAddress
import com.bailout.stickk.ubi4.data.state.ConnectionState.connectedDeviceName
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.UiInterfaceModeBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4

/** Android launch adapter for the shared connection context, before BLE registration. */
internal object DeviceConnectionInitializer {
    fun initialize(intent: Intent) {
        connectedDeviceName = intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME).orEmpty()
        connectedDeviceAddress = intent.getStringExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS).orEmpty()
        val deviceType = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_TYPE).orEmpty()
        UiState.isInterfaceV3Activated = ConstantManager.V3_TYPES.any { marker ->
            connectedDeviceName.contains(marker, ignoreCase = true) ||
                deviceType.contains(marker, ignoreCase = true)
        }
        val selectedProfile = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_PROFILE)
            ?.let { runCatching { V3DeviceProfile.valueOf(it) }.getOrNull() }
        if (selectedProfile != null) {
            UiInterfaceModeBridgeV3.setActiveProfile(selectedProfile)
        } else {
            UiInterfaceModeBridgeV3.updateFromDeviceName(connectedDeviceName)
        }
    }
}
