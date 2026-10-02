package com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics

import kotlinx.coroutines.flow.map

class ObserveAccountStatisticsUseCaseV3(private val repository: V3AccountStatisticsRepository) {
    operator fun invoke() = repository.observeStatistics().map { statistics ->
        // Telemetry slots 0..15 match factory IDs; slot 0 is "no gesture".
        val base = statistics.baseGestureCounts.mapIndexedNotNull { index, count ->
            if (index !in 1..15 || count <= 0L) null else V3GestureUsage(index, count)
        }
        val custom = statistics.customGestureCounts.mapIndexedNotNull { index, count ->
            if (count <= 0L) null else V3GestureUsage(
                gestureId = 64 + index,
                count = count,
                customGestureIndex = index,
                customName = statistics.customGestureNames.getOrNull(index),
            )
        }
        (base + custom).sortedWith(compareByDescending<V3GestureUsage> { it.count }.thenBy { it.gestureId })
    }
}
