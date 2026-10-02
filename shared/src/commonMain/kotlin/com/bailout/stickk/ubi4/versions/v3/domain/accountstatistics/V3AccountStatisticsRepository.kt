package com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics

import kotlinx.coroutines.flow.Flow

interface V3AccountStatisticsRepository {
    fun observeStatistics(): Flow<V3AccountStatistics>
    fun requestStatistics()
}
