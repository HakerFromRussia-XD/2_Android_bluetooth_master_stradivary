package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3GestureUsage

data class V3AccountStatisticsUiState(
    val gestures: List<V3GestureUsage> = emptyList(),
)
