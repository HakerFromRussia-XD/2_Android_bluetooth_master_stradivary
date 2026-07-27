package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.achievements.AchievementDefinitions
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementProgress
import com.bailout.stickk.ubi4.achievements.AchievementProgressCalculator
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object AchievementsState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val telemetryProgressSources:
        Map<AchievementId, (TelemetryGestureCounters) -> Long> = mapOf(
            AchievementId.CYBORG to TelemetryGestureCounters::totalPerformedGestures,
            AchievementId.PRECISION to TelemetryGestureCounters::totalPrecisionGestures,
            AchievementId.POWER to TelemetryGestureCounters::totalPowerGestures
        )

    val progressByAchievement: StateFlow<Map<AchievementId, AchievementProgress>> =
        WidgetState.telemetryGestureCountersFlow
            .map(::calculateTelemetryProgress)
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = calculateTelemetryProgress(
                    WidgetState.telemetryGestureCountersFlow.value
                )
            )

    private fun calculateTelemetryProgress(
        counters: TelemetryGestureCounters
    ): Map<AchievementId, AchievementProgress> =
        telemetryProgressSources.mapValues { (achievementId, currentValueSource) ->
            AchievementProgressCalculator.calculate(
                currentValue = currentValueSource(counters),
                definition = AchievementDefinitions[achievementId]
            )
        }
}


internal fun TelemetryGestureCounters.totalPerformedGestures(): Long =
    baseGestureMovementCount
        .drop(1)
        .sumOf { it.coerceAtLeast(0L) } +
        customGestureMovementCount.sumOf { it.coerceAtLeast(0L) }

internal fun TelemetryGestureCounters.totalPowerGestures(): Long =
    totalBaseGestures(
        GestureEnum.GESTURE_FIST.number,
        GestureEnum.GESTURE_FIST_THUMB_OVER.number
    )

internal fun TelemetryGestureCounters.totalPrecisionGestures(): Long =
    totalBaseGestures(
        GestureEnum.GESTURE_PINCH.number,
        GestureEnum.GESTURE_KEY.number,
        GestureEnum.GESTURE_TWIZZERS.number
    )

private fun TelemetryGestureCounters.totalBaseGestures(
    vararg gestureIndexes: Int
): Long =
    gestureIndexes.sumOf { gestureIndex ->
        baseGestureMovementCount
            .getOrNull(gestureIndex)
            ?.coerceAtLeast(0L)
            ?: 0L
    }
