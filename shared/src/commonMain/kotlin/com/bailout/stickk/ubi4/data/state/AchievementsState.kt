package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.achievements.AchievementDefinitions
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementProgress
import com.bailout.stickk.ubi4.achievements.AchievementProgressCalculator
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

    val cyborgProgress: StateFlow<AchievementProgress> =
        WidgetState.telemetryGestureCountersFlow
            .map { counters ->
                AchievementProgressCalculator.calculate(
                    currentValue = counters.totalPerformedGestures(),
                    definition = AchievementDefinitions[AchievementId.CYBORG]
                )
            }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = AchievementProgressCalculator.calculate(
                    currentValue =
                        WidgetState.telemetryGestureCountersFlow.value.totalPerformedGestures(),
                    definition = AchievementDefinitions[AchievementId.CYBORG]
                )
            )
}


internal fun TelemetryGestureCounters.totalPerformedGestures(): Long =
    baseGestureMovementCount
        .drop(1)
        .sumOf { it.coerceAtLeast(0L) } +
        customGestureMovementCount.sumOf { it.coerceAtLeast(0L) }
