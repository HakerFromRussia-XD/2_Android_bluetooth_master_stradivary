package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription

/** Reads a fresh BLE response; the global parameter snapshot may belong to a previous connection. */
class V3AccountSerialNumberSource(private val enqueuePacket: (ByteArray, () -> Unit) -> Unit) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<String?> = BLEState.state.flatMapLatest { state ->
        if (state != BLEState.State.READY) return@flatMapLatest flowOf(null)
        val address = WidgetRepoProvider.mac()
        val profile = UiState.activeV3DeviceProfile
        val info = ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
        val key = ParameterStoreV3.toKey(info)
        flow<String?> {
            emit(null)
            emitAll(ParameterStoreV3.updates.onSubscription {
                // onSubscription registers the collector before enqueue can synchronously deliver a response.
                WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode)?.let { packet ->
                    enqueuePacket(packet) {}
                }
            }.filter { it == key }.map {
                if (BLEState.state.value != BLEState.State.READY || WidgetRepoProvider.mac() != address ||
                    UiState.activeV3DeviceProfile != profile
                ) null
                else (ParameterStoreV3.get(info) as? ParameterTypedValueV3.Text)?.value
                    ?.replace("\u0000", "")?.trim()?.takeIf { it.isNotEmpty() }
            })
        }
    }.distinctUntilChanged()
}
