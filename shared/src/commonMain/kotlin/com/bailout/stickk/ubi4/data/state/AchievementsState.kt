package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.achievements.AchievementDefinitions
import com.bailout.stickk.ubi4.achievements.AchievementId
import com.bailout.stickk.ubi4.achievements.AchievementProgress
import com.bailout.stickk.ubi4.achievements.AchievementProgressCalculator
import com.bailout.stickk.ubi4.achievements.AnniversaryElapsedDaysCalculator
import com.bailout.stickk.ubi4.achievements.ChampionProgressCalculator
import com.bailout.stickk.ubi4.data.local.repository.AchievementEventManager
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

object AchievementsState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val anniversaryElapsedDays = MutableStateFlow(0L)

    private val telemetryProgressSources:
        Map<AchievementId, (TelemetryGestureCounters) -> Long> = mapOf(
            AchievementId.CYBORG to TelemetryGestureCounters::totalPerformedGestures,
            AchievementId.PRECISION to TelemetryGestureCounters::totalPrecisionGestures,
            AchievementId.POWER to TelemetryGestureCounters::totalPowerGestures
        )

    val progressByAchievement: StateFlow<Map<AchievementId, AchievementProgress>> =
        combine(
            WidgetState.telemetryGestureCountersFlow,
            AchievementEventManager.observeUniqueSubjectCount(AchievementId.GET_A_GRIP),
            AchievementEventManager.observeUniqueSubjectCount(AchievementId.ALWAYS_CONNECTED),
            anniversaryElapsedDays
        ) { counters, configuredCustomGripCount, connectedDaysCount, elapsedDays ->
            calculateProgressMap(
                counters = counters,
                configuredCustomGripCount = configuredCustomGripCount,
                connectedDaysCount = connectedDaysCount,
                anniversaryElapsedDays = elapsedDays
            )
        }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = calculateProgressMap(
                    counters = WidgetState.telemetryGestureCountersFlow.value,
                    configuredCustomGripCount = 0L,
                    connectedDaysCount = 0L,
                    anniversaryElapsedDays = 0L
                )
            )

    fun refreshAnniversaryProgress(dateOfReceipt: String?) {
        anniversaryElapsedDays.value =
            AnniversaryElapsedDaysCalculator.calculate(dateOfReceipt)
    }

    private fun calculateTelemetryProgress(
        counters: TelemetryGestureCounters
    ): Map<AchievementId, AchievementProgress> =
        telemetryProgressSources.mapValues { (achievementId, currentValueSource) ->
            calculateProgress(achievementId, currentValueSource(counters))
        }

    private fun calculateProgressMap(
        counters: TelemetryGestureCounters,
        configuredCustomGripCount: Long,
        connectedDaysCount: Long,
        anniversaryElapsedDays: Long
    ): Map<AchievementId, AchievementProgress> {
        val progressWithoutChampion = calculateTelemetryProgress(counters) +
            mapOf(
                AchievementId.GET_A_GRIP to calculateProgress(
                    AchievementId.GET_A_GRIP,
                    currentValue = configuredCustomGripCount
                ),
                AchievementId.ANNIVERSARY to calculateProgress(
                    AchievementId.ANNIVERSARY,
                    currentValue = anniversaryElapsedDays
                ),
                AchievementId.ALWAYS_CONNECTED to calculateProgress(
                    AchievementId.ALWAYS_CONNECTED,
                    currentValue = connectedDaysCount
                )
            )

        return progressWithoutChampion +
            (
                AchievementId.CHAMPION to
                    ChampionProgressCalculator.calculate(progressWithoutChampion)
            )
    }

    private fun calculateProgress(
        achievementId: AchievementId,
        currentValue: Long
    ): AchievementProgress = AchievementProgressCalculator.calculate(
        currentValue = currentValue,
        definition = AchievementDefinitions[achievementId]
    )
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
