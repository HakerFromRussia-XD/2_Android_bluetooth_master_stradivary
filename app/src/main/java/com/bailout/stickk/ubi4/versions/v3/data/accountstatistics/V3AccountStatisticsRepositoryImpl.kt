package com.bailout.stickk.ubi4.versions.v3.data.accountstatistics

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3AccountStatistics
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3AccountStatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class V3AccountStatisticsRepositoryImpl(
    private val preferences: SharedPreferences,
    private val requestTelemetry: () -> Unit,
    private val counters: Flow<TelemetryGestureCounters> = WidgetState.telemetryGestureCountersFlow,
    private val updates: Flow<Unit> = UiState.updateFlow.map { Unit },
    private val isV3: () -> Boolean = { UiState.isInterfaceV3Activated },
) : V3AccountStatisticsRepository {
    override fun observeStatistics(): Flow<V3AccountStatistics> =
        combine(counters, updates.onStart { emit(Unit) }) { value, _ ->
            V3AccountStatistics(
                baseGestureCounts = value.baseGestureMovementCount.toList(),
                customGestureCounts = value.customGestureMovementCount.toList(),
                customGestureNames = value.customGestureMovementCount.mapIndexed { index, count ->
                    // The old screen did not read preferences for unused telemetry slots.
                    if (count <= 0L) null else {
                        val mac = preferences.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "").orEmpty()
                        preferences.getString(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + mac + index, null)
                    }
                },
            )
        }

    override fun requestStatistics() {
        if (isV3()) requestTelemetry()
    }
}
