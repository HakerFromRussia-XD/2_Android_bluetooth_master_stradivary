package com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job

interface V3AccountStatisticsRepository {
    fun observeStatistics(): Flow<V3AccountStatistics>
    /** Telemetry events only; names are resolved by native UI when applying the event. */
    fun observeCounters(callback: (V3AccountStatistics) -> Unit): Job
    fun requestStatistics()
}
