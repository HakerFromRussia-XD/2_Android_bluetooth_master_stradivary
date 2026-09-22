package com.bailout.stickk.ubi4.versions.v3.data.device

import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.DeviceNameBridgeV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared by repositories in one main Activity scope; contains no Activity references. */
class V3DeviceIdentityStore {
    private val state = MutableStateFlow<V3DeviceIdentity?>(null)
    val identity = state.asStateFlow()
    var intentDeviceName: String? = null

    fun update(serial: String, deviceName: String?): V3DeviceIdentity =
        V3DeviceIdentity(serial, deviceName, DeviceNameBridgeV3.displayName(serial)).also { state.value = it }

    fun applyDeviceName(name: String) {
        if (name.isBlank()) return
        ConnectionState.connectedDeviceName = name
        update(name, name)
    }
}
