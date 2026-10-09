package com.bailout.stickk.ubi4.versions.v3.data.sensors

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.versions.v3.data.device.deviceInteractionEnabledState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.ThresholdsV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterTypeEnum
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.ParameterSnapshotV3Bridge
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_PLOT
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class V3SensorsPlotRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val saveBleValue: (ParameterInfo<Int, Int, Int, Int>, ParameterTypedValueV3) -> Unit =
        SettingsProfileManager::saveBleValue,
    private val readThresholds: (() -> V3PlotThresholds?)? = null,
    private val saveValueAfterSending: Boolean = true,
    private val sampleTargets: List<ParameterRef>? = null,
    private val observeThresholdSnapshots: Boolean = false,
    private val thresholdTarget: ParameterRef? = null,
) : V3SensorsPlotRepository {
    private val thresholdsInfo = ParameterInfoRegistry.require(P_KEY_OPEN_CLOSE_THRESHOLD)
    private val plotInfo = ParameterInfoRegistry.require(P_KEY_PLOT)
    override val interactionEnabled = deviceInteractionEnabledState

    override fun getThresholds(): V3PlotThresholds? {
        if (readThresholds != null) return readThresholds()
        val meta = ParameterInfoRegistry.getMeta(thresholdsInfo) ?: return null
        val typed = ParameterStoreV3.get(thresholdsInfo) ?: ParameterCodecRegistryV3.decodeFromSerialized(
            meta.codecId, ParameterProvider.getParameterV3(thresholdsInfo).data,
        )
        return (typed as? ParameterTypedValueV3.Thresholds)?.value?.let { V3PlotThresholds(it.openThreshold, it.closeThreshold) }
    }

    override fun observeThresholds(): Flow<V3PlotThresholds?> {
        if (observeThresholdSnapshots) return ParameterStoreV3.updates
            .mapNotNull { WidgetStateBridgeV3.getCurrent(it.deviceAddress, it.parameterID, it.dataCode) }
            .filter(::matchesThresholdSnapshot)
            .map(::thresholdsFromSnapshot)

        return ParameterStoreV3.updates
            .filter { it == ParameterStoreV3.toKey(thresholdsInfo) }
            .map { getThresholds() }
            .onStart { emit(getThresholds()) }
    }

    private fun matchesThresholdSnapshot(snapshot: ParameterSnapshotV3Bridge): Boolean {
        if (snapshot.codecId != "THRESHOLDS") return false
        val target = thresholdTarget
        return if (target != null) {
            snapshot.addressDevice == target.addressDevice && snapshot.parameterID == target.parameterID &&
                canonicalThresholdDataCode(snapshot.dataCode) == canonicalThresholdDataCode(target.dataCode)
        } else {
            sampleTargets.orEmpty().any {
                it.addressDevice == snapshot.addressDevice && it.parameterID == snapshot.parameterID
            }
        }
    }

    private fun canonicalThresholdDataCode(dataCode: Int): Int = when (dataCode) {
        0x2F, 0x30, 0x1A -> 0x2F
        else -> dataCode
    }

    private fun thresholdsFromSnapshot(snapshot: ParameterSnapshotV3Bridge): V3PlotThresholds? = runCatching {
        val fields = Json.parseToJsonElement(snapshot.serializedValue).jsonObject
        val open = fields["openThreshold"]?.jsonPrimitive?.intOrNull
        val close = fields["closeThreshold"]?.jsonPrimitive?.intOrNull
        if (open == null || close == null) null else V3PlotThresholds(open, close)
    }.getOrNull()

    override fun observeSamples(): Flow<List<Int>> {
        val targets = sampleTargets
        if (targets != null) return WidgetState.plotArrayFlow
            .filter { sample -> targets.any { it.addressDevice == sample.addressDevice && it.parameterID == sample.parameterID } }
            .map { it.dataPlots.take(2).toList() }

        return WidgetState.plotArrayFlow
            .filter { it.addressDevice == plotInfo.deviceAddress && it.parameterID == plotInfo.parameterID }
            .map { it.dataPlots.take(6).toList() }
            // Partial packets used to keep the preceding values of omitted channels.
            .scan(List(6) { 0 }) { previous, incoming -> List(6) { incoming.getOrNull(it) ?: previous[it] } }
    }

    override fun getChannelCount(): Int {
        // The hardcoded V3/INDY3 Plot uses two curves. The old adapter only consulted
        // parameter type/size for PDCE_EMG_CH_1_3_VAL, not for the current P_KEY_PLOT.
        if (plotInfo.dataCode != ParameterDataCodeEnum.PDCE_EMG_CH_1_3_VAL.number) return 2
        // Retain the metadata lookup used by the existing graph until shared is reorganized.
        val parameter = ParameterProvider.getParameter(plotInfo.deviceAddress, plotInfo.parameterID)
        val size = ParameterTypeEnum.entries.getOrNull(parameter.type)?.sizeOf ?: 0
        return if (size != 0) (parameter.parameterDataSize / size).coerceIn(0, 6) else 0
    }

    override fun arePlotPointsPaused() = WidgetState.pausePlotPointsDuringTransition

    override fun requestThresholds(parameterID: Int, dataCode: Int) {
        WidgetCommandBridgeV3.buildReadRequest(parameterID, dataCode)?.let(enqueuePacket)
    }

    override fun setThresholds(thresholds: V3PlotThresholds) {
        enqueuePacket(BLECommandsV3.sendThresholds(thresholds.open, thresholds.close))
        if (!saveValueAfterSending) return
        val typed = ParameterTypedValueV3.Thresholds(ThresholdsV3(thresholds.open, thresholds.close))
        ParameterStoreV3.put(thresholdsInfo, typed)
        saveBleValue(thresholdsInfo, typed)
        val meta = ParameterInfoRegistry.getMeta(thresholdsInfo) ?: return
        ParameterCodecRegistryV3.encodeToSerialized(meta.codecId, typed)?.let {
            ParameterProvider.getParameterV3(thresholdsInfo).data = it
        }
    }
}
