package com.bailout.stickk.ubi4.versions.v3.data.gestures

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.data.device.deviceInteractionEnabledState
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.ParameterSnapshotV3Bridge
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3ActiveGesture
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GesturesRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class V3GesturesRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val saveBleValue: (ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3) -> Unit =
        SettingsProfileManager::saveBleValue,
    private val saveActiveGestureBeforeSending: Boolean = true,
    private val validateActiveGestureDeviceContext: Boolean = true,
    private val useActiveGestureSnapshots: Boolean = false,
    private val activeGestureTarget: ParameterRef? = null,
    private val saveRotationGroupBeforeSending: Boolean = true,
    private val validateRotationGroupBeforeSending: Boolean = true,
    private val observeRotationGroupSnapshots: Boolean = false,
    private val rotationGroupTarget: ParameterRef? = null,
) : V3GesturesRepository {
    private val rotationGroupInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
    override val rotationGroupUpdates = ParameterStoreV3.updates.filter {
        it == ParameterStoreV3.toKey(rotationGroupInfo) && getRotationGroupGestureIds() != null
    }.map { Unit }

    override val updates = merge(
        ParameterStoreV3.values.map { Unit },
        deviceInteractionEnabledState.map { Unit },
        UiState.updateFlow.map { Unit },
    )

    override fun getActiveGesture(): V3ActiveGesture {
        val address = WidgetRepoProvider.mac()
        val info = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
        val gestureId = if (useActiveGestureSnapshots) {
            activeGestureTarget?.let {
                WidgetStateBridgeV3.getCurrent(it.addressDevice, it.parameterID, it.dataCode)
            }?.let(::activeGestureId)
        } else {
            val typed = ParameterStoreV3.get(info) ?: ParameterInfoRegistry.getMeta(info)?.let { meta ->
                ParameterCodecRegistryV3.decodeFromSerialized(meta.codecId, ParameterProvider.getParameterV3(info).data)
            }
            (typed as? ParameterTypedValueV3.CurrentGesture)?.value?.currentGesture
        }
        val hasGestureParameter = GlobalParameters.baseSubDevicesInfoStructSetV3.any { device ->
            device.deviceAddress == info.deviceAddress && device.parametersList.any {
                it.ID == info.parameterID && it.dataCode == info.dataCode
            }
        }
        return V3ActiveGesture(
            address, gestureId,
            UiState.isInterfaceV3Activated && hasGestureParameter && address.isNotBlank() && address != "null" &&
                deviceInteractionEnabledState.value,
        )
    }

    override fun observeActiveGesture(): Flow<Int?> {
        if (!useActiveGestureSnapshots) return updates.map { getActiveGesture().gestureId }
        return ParameterStoreV3.updates
            .mapNotNull { WidgetStateBridgeV3.getCurrent(it.deviceAddress, it.parameterID, it.dataCode) }
            .filter { snapshot -> activeGestureTarget?.let {
                snapshot.addressDevice == it.addressDevice && snapshot.parameterID == it.parameterID &&
                    snapshot.dataCode == it.dataCode
            } == true }
            .map { activeGestureId(it) }
    }

    private fun activeGestureId(snapshot: ParameterSnapshotV3Bridge): Int? {
        if (snapshot.codecId != "CURRENT_GESTURE") return null
        return runCatching {
            Json.parseToJsonElement(snapshot.serializedValue).jsonObject["currentGesture"]?.jsonPrimitive?.intOrNull
        }.getOrNull()
    }

    override fun requestActiveGesture(deviceAddress: String): Boolean {
        if (validateActiveGestureDeviceContext && !canSendTo(deviceAddress)) return false
        enqueuePacket(BLECommandsV3.request(PWCE_GET_CURRENT_GESTURE_NUM.number.toInt()))
        return true
    }

    override fun getRotationGroupGestureIds(): List<Int>? {
        if (!hasRotationGroupParameter()) return null
        val typed = ParameterStoreV3.get(rotationGroupInfo)
            ?: ParameterInfoRegistry.getMeta(rotationGroupInfo)?.let { meta ->
                ParameterCodecRegistryV3.decodeFromSerialized(meta.codecId, ParameterProvider.getParameterV3(rotationGroupInfo).data)
            }
        return (typed as? ParameterTypedValueV3.RotationGroup)?.value?.toGestureList()
            ?.map { it.first }?.filter { it != 0 }
    }

    override fun observeRotationGroup(): Flow<List<Int>?> {
        if (!observeRotationGroupSnapshots) return rotationGroupUpdates.map { getRotationGroupGestureIds() }
        return ParameterStoreV3.updates
            .mapNotNull { WidgetStateBridgeV3.getCurrent(it.deviceAddress, it.parameterID, it.dataCode) }
            .filter { snapshot -> snapshot.codecId == "ROTATION_GROUP" && rotationGroupTarget?.let {
                snapshot.addressDevice == it.addressDevice && snapshot.parameterID == it.parameterID &&
                    snapshot.dataCode == it.dataCode
            } == true }
            .map { rotationGroupIds(it.serializedValue) }
    }

    private fun rotationGroupIds(serialized: String): List<Int> {
        val fields = runCatching { Json.parseToJsonElement(serialized).jsonObject }.getOrNull() ?: return emptyList()
        return (1..8).mapNotNull { index ->
            (fields["gesture${index}Id"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        }
    }

    override fun requestRotationGroup(deviceAddress: String): Boolean {
        if (validateRotationGroupBeforeSending && (!canSendTo(deviceAddress) || !hasRotationGroupParameter())) return false
        enqueuePacket(BLECommandsV3.request(PWCE_GET_GESTURE_GROUPE.number.toInt()))
        return true
    }

    override fun setRotationGroup(deviceAddress: String, gestureIds: List<Int>): Boolean {
        if (validateRotationGroupBeforeSending) {
            if (!canSendTo(deviceAddress) || !hasRotationGroupParameter()) return false
            if (gestureIds.size > 8 || gestureIds.any { it !in 1..255 }) return false
        }
        fun id(index: Int) = gestureIds.getOrNull(index) ?: 0
        // Existing V3 writes use the gesture ID for both ID and image, with zero-filled empty slots.
        val packet = BLECommandsV3.sendRotationGroup(RotationGroup(
            id(0), id(0), id(1), id(1), id(2), id(2), id(3), id(3),
            id(4), id(4), id(5), id(5), id(6), id(6), id(7), id(7),
        ))
        if (saveRotationGroupBeforeSending) {
            val typed = ParameterTypedValueV3.RotationGroup(RotationGroupV3(
                id(0), id(0), id(1), id(1), id(2), id(2), id(3), id(3),
                id(4), id(4), id(5), id(5), id(6), id(6), id(7), id(7),
            ))
            saveTypedValue(rotationGroupInfo, typed)
        }
        enqueuePacket(packet)
        return true
    }

    private fun hasRotationGroupParameter(): Boolean = GlobalParameters.baseSubDevicesInfoStructSetV3.any { device ->
        device.deviceAddress == rotationGroupInfo.deviceAddress && device.parametersList.any {
            it.ID == rotationGroupInfo.parameterID && it.dataCode == rotationGroupInfo.dataCode
        }
    }

    override fun selectGesture(deviceAddress: String, gestureId: Int): Boolean {
        if (validateActiveGestureDeviceContext && !canSendTo(deviceAddress)) return false
        if (saveActiveGestureBeforeSending) {
            val info = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
            val typed = ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(currentGesture = gestureId))
            // Preserve the optimistic value, profile persistence and packet order from the adapter.
            saveTypedValue(info, typed)
        }
        enqueuePacket(BLECommandsV3.sendSubcommand(PWCE_SET_CURRENT_GESTURE_NUM.number.toInt(), gestureId))
        return true
    }

    private fun saveTypedValue(info: ParameterInfo<Int, Int, Int, Int>, typed: ParameterTypedValueV3) {
        ParameterStoreV3.put(info, typed)
        saveBleValue(info, typed)
        ParameterInfoRegistry.getMeta(info)?.let { meta ->
            ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, typed)?.let { encoded ->
                ParameterProvider.getParameterV3(info).data = encoded
            }
        }
    }

    private fun canSendTo(address: String): Boolean = getActiveGesture().let {
        it.isInteractionEnabled && it.deviceAddress == address
    }
}
