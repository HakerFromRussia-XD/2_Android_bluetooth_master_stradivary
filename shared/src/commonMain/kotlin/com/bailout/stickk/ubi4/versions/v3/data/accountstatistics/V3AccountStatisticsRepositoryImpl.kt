package com.bailout.stickk.ubi4.versions.v3.data.accountstatistics

import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3AccountStatistics
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3AccountStatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Job
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridge

class V3AccountStatisticsRepositoryImpl(
    private val readCustomGestureName: (Int) -> String?,
    private val requestTelemetry: () -> Unit,
    private val counters: Flow<TelemetryGestureCounters> = WidgetState.telemetryGestureCountersFlow,
    private val updates: Flow<Unit> = UiState.updateFlow.map { Unit },
    private val isV3: () -> Boolean = { UiState.isInterfaceV3Activated },
    private val subscribeTelemetryCounters: ((TelemetryGestureCounters) -> Unit) -> Job = WidgetStateBridge::observeTelemetryGestureCounters,
) : V3AccountStatisticsRepository {
    // Swift cannot omit Kotlin default constructor arguments.
    constructor(
        readCustomGestureName: (Int) -> String?,
        requestTelemetry: () -> Unit,
        isV3: () -> Boolean,
    ) : this(
        readCustomGestureName,
        requestTelemetry,
        WidgetState.telemetryGestureCountersFlow,
        UiState.updateFlow.map { Unit },
        isV3,
    )

    constructor(
        readCustomGestureName: (Int) -> String?,
        requestTelemetry: () -> Unit,
        isV3: () -> Boolean,
        subscribeTelemetryCounters: ((TelemetryGestureCounters) -> Unit) -> Job,
    ) : this(readCustomGestureName, requestTelemetry, WidgetState.telemetryGestureCountersFlow,
        UiState.updateFlow.map { Unit }, isV3, subscribeTelemetryCounters)

    override fun observeCounters(callback: (V3AccountStatistics) -> Unit): Job =
        subscribeTelemetryCounters { value ->
            callback(V3AccountStatistics(
                baseGestureCounts = value.baseGestureMovementCount.toList(),
                customGestureCounts = value.customGestureMovementCount.toList(),
            ))
        }

    override fun observeStatistics(): Flow<V3AccountStatistics> =
        combine(counters, updates.onStart { emit(Unit) }) { value, _ ->
            V3AccountStatistics(
                baseGestureCounts = value.baseGestureMovementCount.toList(),
                customGestureCounts = value.customGestureMovementCount.toList(),
                customGestureNames = value.customGestureMovementCount.mapIndexed { index, count ->
                    // The old screen did not read preferences for unused telemetry slots.
                    if (count <= 0L) null else readCustomGestureName(index)
                },
            )
        }

    override fun requestStatistics() {
        if (isV3()) requestTelemetry()
    }
}
